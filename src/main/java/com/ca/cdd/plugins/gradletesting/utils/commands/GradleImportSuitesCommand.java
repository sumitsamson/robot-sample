package com.ca.cdd.plugins.gradletesting.utils.commands;

import com.ca.cdd.plugins.shared.async.Command;
import com.ca.cdd.plugins.shared.async.CommandNotifier;
import com.ca.cdd.plugins.gradletesting.utils.ExecutionUtils;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleImportSuitesResponse;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.rp.plugins.dto.model.ExternalTestSourceInput;
import com.ca.rp.plugins.dto.model.ExternalTestSourceResponse;
import com.ca.rp.plugins.dto.model.TestSuite;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static com.ca.cdd.plugins.gradletesting.utils.InitScriptUtils.INIT_SCRIPT_TEST_EXPORT;

public class GradleImportSuitesCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(GradleImportSuitesCommand.class);

    private final ExternalTestSourceInput testSourceInput;
    private final String executionId;

    public GradleImportSuitesCommand(ExternalTestSourceInput testSourceInput, String executionId, CommandNotifier notifier) {
        super(notifier);
        this.testSourceInput = testSourceInput;
        this.executionId = executionId;
    }

    @Override
    public void run() {
        //todo: treat failures
        logger.trace("[START] run()");
        String projectRootFolder = RepositoryUtils.buildCloneLocation(executionId);
        String gradleVersion = ExecutionUtils.extractGradleVersion(testSourceInput);
        logger.trace("Connecting to {} project, gradle version {}", projectRootFolder, gradleVersion);
        ProjectConnection connection = ExecutionUtils.createProjectConnection(projectRootFolder, gradleVersion);

        Map<String, File> allProjects;
        try {
            allProjects = ExecutionUtils.getSubProjectFolders(connection);
        } finally {
            logger.trace("Closing connectionv (Gradle Tooling API -> Gradle Project)");
            connection.close();
        }

        logger.trace("Importing testSuites");
        final HashMap<String, Map<String, List<String>>> testSuitesPerProject = importTestSuites(projectRootFolder, allProjects);

        logger.trace("Importing testSuites [DONE]");
        List<TestSuite> testSuites = toTestSuites(testSuitesPerProject);
        final ExternalTestSourceResponse response = ExternalTestSourceResponse.createResponseForFinished("Done", "Import finished successfully", testSuites);
        logger.trace("[DONE] run()");
        getCommandNotifier().updateResponse(new GradleImportSuitesResponse(CommandStatus.FINISHED, response));
    }

    private HashMap<String, Map<String, List<String>>> importTestSuites(String cloneLocation, Map<String, File> projName2folder) {
        logger.trace("[START] importTestSuites");
        HashMap<String, Map<String, List<String>>> tests = new HashMap<>();

        //(4) For each project - load the list of its tests
        for (Map.Entry<String, File> entry : projName2folder.entrySet()) {
            String projectName = entry.getKey();
            File projectFolder = entry.getValue();

            String gradleVersion = ExecutionUtils.extractGradleVersion(testSourceInput);
            logger.debug("Importing for project {}, in folder {}, gradle version {}", projectName, projectFolder, gradleVersion);
            ProjectConnection projConnection = ExecutionUtils.createProjectConnection(projectFolder, gradleVersion);
            logger.trace("Got connection");
            try {
                final String initScript = cloneLocation + File.separator + INIT_SCRIPT_TEST_EXPORT;
                final String logFile = ExecutionUtils.absoluteLogFilePath("getTestModel", executionId);

                ModelBuilder<Map> testModelBuilder = projConnection
                        .model(Map.class) // todo: Use some less common name (class) for model
                        .withArguments("--init-script", initScript);
                testModelBuilder = ExecutionUtils.configureLauncher(testModelBuilder, logFile);

                logger.info("***************************************************************");
                logger.info("*** Getting TEST MODEL, for: {} ***", projectName);
                logger.info("*** Init Script : {} ***", initScript);
                logger.info("***************************************************************");

                Map<String, List<String>> testModel = testModelBuilder.get();

                if ( null != testModel && hasTests(testModel) ) {
                    logger.debug("Found some test suites");
                    tests.put(projectName, testModel);
                } else {
                    logger.debug("No test suites found in project");
                }
            } finally {
                logger.debug("Closing connection");
                projConnection.close();
            }
        }

        logger.trace("[DONE] importTestSuites");
        return tests;
    }

    private List<TestSuite> toTestSuites(Map<String, Map<String, List<String>>> testSuitesPerProject) {
        final LinkedList<TestSuite> testSuites = new LinkedList<>();

        for (Map.Entry<String, Map<String, List<String>>> entry : testSuitesPerProject.entrySet()) {
            final String projectName = entry.getKey();
            final Map<String, List<String>> task2TestClasses = entry.getValue();
            for (Map.Entry<String, List<String>> entry2 : task2TestClasses.entrySet()) {
                final String taskName = entry2.getKey();
                final List<String> testClasses = entry2.getValue();
                for (String testClass : testClasses) {
                    final String testSuiteId = ExecutionUtils.buildTestSuiteId(projectName, taskName, testClass);
                    final String testSuiteName = testClass.substring(testClass.lastIndexOf('.') + 1);
                    testSuites.add( createTestSuite(testSuiteName, testSuiteId) );
                }
            }
        }

        return testSuites;
    }

    private TestSuite createTestSuite(String name, String externalId) {
        logger.trace("[START] createTestSuite");
        final TestSuite testSuite = new TestSuite();
        testSuite.setCreationDate(System.currentTimeMillis());
        testSuite.setName(name);
        testSuite.setExternalId(externalId);
        logger.trace("[DONE] createTestSuite");
        return testSuite;
    }




    private static boolean hasTests(Map<String, List<String>> task2TestSuites) {
        for (List<String> taskTests : task2TestSuites.values()) {
            if (!taskTests.isEmpty()) {
                return true;
            }
        }

        return false;
    }

}