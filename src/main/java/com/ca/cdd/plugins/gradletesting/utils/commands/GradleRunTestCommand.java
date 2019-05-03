package com.ca.cdd.plugins.gradletesting.utils.commands;

import com.ca.cdd.plugins.gradletesting.GradleTestSuite;
import com.ca.cdd.plugins.gradletesting.HTMLReportLocatorImpl;
import com.ca.cdd.plugins.gradletesting.XMLReportReader;
import com.ca.cdd.plugins.shared.async.Command;
import com.ca.cdd.plugins.shared.async.CommandNotifier;
import com.ca.cdd.plugins.gradletesting.utils.ExecutionUtils;
import com.ca.cdd.plugins.gradletesting.utils.InitScriptUtils;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleRunTestResponse;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.shared.async.commands.GitCloneCommand;
import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import com.ca.rp.plugins.dto.model.ExternalTaskResult;
import org.gradle.tooling.*;
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException;
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException;
import org.gradle.tooling.model.GradleProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static com.ca.cdd.plugins.gradletesting.utils.InitScriptUtils.INIT_SCRIPT_TEST;

/**
 * Created by menyo01 on 31/12/2017.
 *
 */

public class GradleRunTestCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(GitCloneCommand.class);

    private static final String HTML_REPORT_PATH_TEMPLATE = "reports/tests/:testTaskName:/classes/:testClass:.html";
    private static final String HTML_REPORT_TASK_PLACEHOLDER = ":testTaskName:";
    private static final String HTML_REPORT_CLASS_PLACEHOLDER = ":testClass:";

    private static final String DEFAULT_REASON_PREFIX = "Unexpected exception: ";
    private static final Map<Class, String> ex2Reason = new HashMap();

    {
        ex2Reason.put(TestExecutionException.class, "One or more tests fail, or no tests for execution declared or no matching tests can be found");
        ex2Reason.put(UnsupportedVersionException.class, "The target Gradle version does not support test execution");
        ex2Reason.put(UnsupportedBuildArgumentException.class, "There is a problem with build arguments provided by withArguments(String...)");
        ex2Reason.put(UnsupportedOperationConfigurationException.class, "The target Gradle version does not support some requested configuration option");
        ex2Reason.put(BuildException.class, "A failure while executing the tests in the Gradle build");
        ex2Reason.put(BuildCancelledException.class, "The operation was cancelled before it completed successfully");
        ex2Reason.put(GradleConnectionException.class, "Failure using the connection");
        ex2Reason.put(IllegalStateException.class, "The connection has been closed or is closing");
    }

    private final String executorId;
    private final GradleTestSuite testSuite;
    private final ExternalTaskInputs taskInputs;

    public GradleRunTestCommand(String testSuiteId, String executorId, ExternalTaskInputs taskInputs, CommandNotifier commandNotifier) {
        super(commandNotifier);
        this.executorId = executorId;
        this.testSuite = ExecutionUtils.toGradleTestSuite(testSuiteId);
        this.taskInputs = taskInputs;
    }

    @Override
    public void run() {
        try {
            final String rootProjectFolder = RepositoryUtils.buildCloneLocation(executorId);

            String gradleVersion = ExecutionUtils.extractGradleVersion(taskInputs);
            ProjectConnection connection = ExecutionUtils.createProjectConnection(rootProjectFolder, gradleVersion);
            GradleProject project = getGradleProject(connection);  //Possibly sub-project, for current task

            File testProjectFolder = project.getProjectDirectory();

            logger.trace("Copying init-script(s) to the root project folder", rootProjectFolder);
            InitScriptUtils.copyTestScripts(rootProjectFolder);

            logger.trace("Connecting to test suite project ({}), gradle version: {}", testProjectFolder, gradleVersion);
            connection.close();
            connection = ExecutionUtils.createProjectConnection(testProjectFolder, gradleVersion);

            HTMLReportLocatorImpl htmlReportLocator = new HTMLReportLocatorImpl(taskInputs, project.getBuildDirectory(), testSuite);
            try {
                logger.trace("Starting the TestLauncher");
                String logFile = "test" + testSuite.getId().replace("::", ".").replace(":", ".");
                logFile = ExecutionUtils.absoluteLogFilePath(logFile, executorId);
                TestLauncher testLauncher = connection.newTestLauncher();
                testLauncher = ExecutionUtils.configureLauncher(testLauncher, logFile);

                final String initScript = rootProjectFolder + File.separator + INIT_SCRIPT_TEST;

                logger.info("***************************************************************");
                logger.info("*** Running TESTs, for: {} ***", testSuite.getTestClass());
                logger.info("*** Init Script : {} ***", initScript);
                logger.info("***************************************************************");

                testLauncher
                        .withArguments("--init-script", initScript, "--info")
                        .withJvmTestClasses(testSuite.getTestClass())
                        .run();

                logger.trace("TestSuite execution finished, going to parse the XML report");
                ExternalTaskResult taskResult = new XMLReportReader(testSuite, project.getBuildDirectory(), htmlReportLocator).read();
                getCommandNotifier().updateResponse(new GradleRunTestResponse(CommandStatus.FINISHED, taskResult));
            } catch (TestExecutionException ex) {
                logger.debug("Got a TestExecutionException, going to parse the XML report, to find errors", ex);

                ExternalTaskResult taskResult = new XMLReportReader(testSuite, project.getBuildDirectory(), htmlReportLocator).read();
                getCommandNotifier().updateResponse(new GradleRunTestResponse(CommandStatus.FAILED, taskResult));

            } catch (GradleConnectionException | IllegalStateException ex) {
                String reason = ex2Reason.get(ex.getClass());
                if (reason == null) {
                    reason = (ex.getMessage() != null) ? DEFAULT_REASON_PREFIX + ex.getMessage() : "?";
                }
                logger.error("TestSuite Execution Error " + reason, ex);

                getCommandNotifier().updateResponse(new GradleRunTestResponse(CommandStatus.FAILED, ExternalTaskResult.createResponseForFailure("Failed", reason)));
            } finally {
//                publishTestReport(project);

                logger.trace("Closing connection");
                connection.close();
            }
        } catch (Exception ex) {
            logger.error("Failed to run test suite with id '{}' due to '{}'", testSuite.getId(), ex.getMessage(), ex);
            getCommandNotifier().updateResponse(new GradleRunTestResponse(CommandStatus.FAILED, ExternalTaskResult.createResponseForFailure("Failed", "Failed to run test suite")));
        }
    }

    private void publishTestReport(GradleProject project) {
        final File buildDirectory = project.getBuildDirectory();
        final String htmlReportType = HTML_REPORT_PATH_TEMPLATE
                .replace(HTML_REPORT_TASK_PLACEHOLDER, testSuite.getTask())
                .replace(HTML_REPORT_CLASS_PLACEHOLDER, testSuite.getTestClass());
        final File htmlReport = new File(buildDirectory, htmlReportType);
        if (!htmlReport.exists()) {
            logger.error("HTML report not found, file: {}, testSuite: {} ", htmlReport, testSuite);
            return;
        }

        Path src = Paths.get(htmlReport.toURI());
        Path target = Paths.get(getHtmlReportTargetFolder() + "/" + testSuite.getTestClass() + ".html");

        try {
            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Can not publish HTML report", e);
        }

    }

    private GradleProject getGradleProject(ProjectConnection connection) {
        GradleProject project;
        try {
            logger.trace("Loading project structure");
            Map<String, GradleProject> subProjects = ExecutionUtils.getSubProjects(connection);
            logger.info("Got {} project modules", subProjects.size());
            project = subProjects.get(testSuite.getProject());
        } finally {
            logger.trace("Closing connection");
            connection.close();
        }

        if (project == null || project.getProjectDirectory() == null) {
            String msg = String.format("Can't find project folder. Project [%s], testSuiteId [%s]",
                    testSuite.getProject(), testSuite.getId());
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
        return project;
    }

    private String getHtmlReportTargetFolder() {
//        final String cloneLocation = RepositoryUtils.buildCloneLocation(this.executorId);
        return "c:/Users/kovan04/reports/yo";
    }
}