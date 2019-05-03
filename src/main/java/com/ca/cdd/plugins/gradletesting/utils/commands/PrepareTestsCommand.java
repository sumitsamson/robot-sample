package com.ca.cdd.plugins.gradletesting.utils.commands;

import com.ca.cdd.plugins.gradletesting.GradleBuildResult;
import com.ca.cdd.plugins.shared.async.Command;
import com.ca.cdd.plugins.shared.async.CommandNotifier;
import com.ca.cdd.plugins.gradletesting.utils.ExecutionUtils;
import com.ca.cdd.plugins.gradletesting.utils.InitScriptUtils;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import com.ca.cdd.plugins.gradletesting.utils.responses.PrepareTestsResponse;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.rp.plugins.dto.model.ExternalTestSourceInput;
import com.ca.rp.plugins.dto.model.ExternalTestSourceResponse;
import org.gradle.tooling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.ca.cdd.plugins.gradletesting.utils.InitScriptUtils.INIT_SCRIPT_PREPARE_TESTS;

public class PrepareTestsCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(PrepareTestsCommand.class);

    private final ExternalTestSourceInput testSourceInput;
    private final String executionId;

    public PrepareTestsCommand(ExternalTestSourceInput testSourceInput, String executionId, CommandNotifier notifier) {
        super(notifier);
        this.testSourceInput = testSourceInput;
        this.executionId = executionId;
    }

    @Override
    public void run() {
        logger.trace("[START] run");
        String projectRootFolder = RepositoryUtils.buildCloneLocation(executionId);
        logger.debug("Project root folder: {}", projectRootFolder);

        // copy gradle scripts
        InitScriptUtils.copyImportScripts(projectRootFolder);

        String gradleVersion = ExecutionUtils.extractGradleVersion(testSourceInput);
        logger.trace("Connecting to {} project, gradle version {}", projectRootFolder, gradleVersion);
        ProjectConnection connection = ExecutionUtils.createProjectConnection(projectRootFolder, gradleVersion);

        try {
            logger.trace("Preparing for tests");
            prepareTests(projectRootFolder, connection);
        } finally {
            logger.trace("Closing connection (ToolingAPI -> Gradle Project)");
            connection.close();
        }
        logger.trace("[DONE] run");
    }

    private void prepareTests(String projectLocation, ProjectConnection connection) {
        logger.trace("[START] prepareTests");
        //(2) Get inner project names and folders
        final String logFile = ExecutionUtils.absoluteLogFilePath("prepareTests", executionId);

        BuildLauncher buildLauncher = connection.newBuild();
        buildLauncher = ExecutionUtils.configureLauncher(buildLauncher, logFile);
        final String initScript = projectLocation + File.separator + INIT_SCRIPT_PREPARE_TESTS;
        buildLauncher = buildLauncher.withArguments("--init-script", initScript)
                .forTasks("prepareForTests");

        logger.info("***************************************************************");
        logger.info("*** Running prepareTests, for: {} ***", projectLocation);
        logger.info("*** Init Script : {} ***", initScript);
        logger.info("***************************************************************");

        GradleBuildResult buildResult = ExecutionUtils.executeGradleBuildSafely(buildLauncher);

        if (buildResult.isSuccess()) {
            getCommandNotifier().updateResponse(new PrepareTestsResponse(CommandStatus.FINISHED));
        } else {
            ExternalTestSourceResponse testSourceResponse =
                    ExternalTestSourceResponse.createResponseForFailure(buildResult.getFailureReason(),
                            buildResult.getDetailedFailureReason());
            getCommandNotifier().updateResponse(new PrepareTestsResponse(CommandStatus.FAILED, testSourceResponse));
        }

        logger.trace("[DONE] prepareTests");
    }
}