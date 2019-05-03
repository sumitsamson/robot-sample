package com.ca.cdd.plugins.gradletesting.services.impl;

import com.ca.cdd.plugins.gradletesting.Constants;
import com.ca.cdd.plugins.gradletesting.services.ExecuteTestsService;
import com.ca.cdd.plugins.gradletesting.utils.GradleCommands;
import com.ca.cdd.plugins.gradletesting.utils.ExecutionUtils;
import com.ca.cdd.plugins.shared.SharedConstants;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.shared.async.responses.CloneResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleRunTestResponse;
import com.ca.cdd.plugins.shared.utils.SourceControlUtils;
import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import com.ca.rp.plugins.dto.model.ExternalTaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ExecuteTestsServiceImpl implements ExecuteTestsService {
    private static final Logger logger = LoggerFactory.getLogger(ExecuteTestsServiceImpl.class);

    @Override
    public ExternalTaskResult start(ExternalTaskInputs taskInputs) {
        logger.trace("In start. " + taskInputs);

        Map<String, String> executionContext = taskInputs.getExecutionContext();
        Map<String, String> endPointProperties = taskInputs.getEndPointProperties();
        Map<String, String> taskProperties = taskInputs.getTaskProperties();

        CloneResponse cloneResponse;
        Path repoPath = Paths.get(RepositoryUtils.buildCloneLocation(taskInputs.getExecutorId()));
        String state = executionContext.get(SharedConstants.STATE);

        if (Files.notExists(repoPath) || SharedConstants.CLONING.equals(state)) {
            cloneResponse = SourceControlUtils.cloneRepository(executionContext, endPointProperties, taskProperties.get(Constants.BRANCH), taskInputs.getExecutorId());

            if (CommandStatus.FAILED.equals(cloneResponse.getCommandStatus())) {
                deleteFolderInFailure(repoPath);
                return ExternalTaskResult.createResponseForFailure("Failed to clone repository", cloneResponse.getMessage());
            }

            if (CommandStatus.RUNNING.equals(cloneResponse.getCommandStatus())) {
                return ExternalTaskResult.createResponseForRunning("Running", "Cloning repository", 10f, 3000, executionContext);
            }

            if (CommandStatus.FINISHED.equals(cloneResponse.getCommandStatus())) {
                logger.debug("Finished to clone/checkout repository clone location = '{}'", cloneResponse.getCloneLocation());
                executionContext.put(SharedConstants.STATE, SharedConstants.CLONE_FINISHED);
                return ExternalTaskResult.createResponseForRunning("Running", "Cloning repository", 10f, 0, executionContext);
            }

        }

        String testSuiteId = ExecutionUtils.getTestSuiteId(taskInputs);

        logger.debug("Start run test suite with id = '{}'", testSuiteId);

        String commandId = executionContext.get(Constants.RUN_TEST_COMMAND_ID);

        GradleRunTestResponse gradleRunTestResponse = GradleCommands.runGradleTest(testSuiteId, taskInputs.getExecutorId(), commandId, taskInputs);

        if (CommandStatus.RUNNING.equals(gradleRunTestResponse.getCommandStatus())) {
            logger.debug("test suite with id '{}' is running", testSuiteId);
            executionContext.put(Constants.RUN_TEST_COMMAND_ID , gradleRunTestResponse.getCommandId());
            return ExternalTaskResult.createResponseForRunning("Running", "Run suite", 50f, 2000, executionContext);
        }

        return gradleRunTestResponse.getResult();

    }

    private void deleteFolderInFailure(Path repoPath) {
        try {
            Files.delete(repoPath);
        } catch (IOException e) {
            logger.error("Failed to try delete folder due to '{}'", e.getMessage(), e);
        }
    }

    @Override
    public ExternalTaskResult stop(ExternalTaskInputs taskInputs) {
        logger.info("Got a STOP request, not treating it.");
        logger.debug("TestSuiteId: " + ExecutionUtils.getTestSuiteId(taskInputs));

        return null;
    }






}
