package com.ca.cdd.plugins.gradletesting.services.impl;

import com.ca.cdd.plugins.gradletesting.Constants;
import com.ca.cdd.plugins.gradletesting.services.ImportTestsService;
import com.ca.cdd.plugins.gradletesting.utils.GradleCommands;
import com.ca.cdd.plugins.shared.SharedConstants;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.shared.async.responses.CloneResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleImportSuitesResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.PrepareTestsResponse;
import com.ca.cdd.plugins.shared.utils.SourceControlUtils;
import com.ca.rp.plugins.dto.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ImportTestsServiceImpl implements ImportTestsService {
    private static final Logger logger = LoggerFactory.getLogger(ImportTestsServiceImpl.class);

    @Override
    public ExternalTestSourceResponse importTests(ExternalTestSourceInput testSourceInput, int maxResult) {

        logger.trace("In importTests. " + testSourceInput);

        Map<String, String> executionContext = testSourceInput.getExecutionContext();
        Map<String, String> endPointProperties = testSourceInput.getEndPointProperties();
        Map<String, String> testSourceProperties = testSourceInput.getTestSourceProperties();

        CloneResponse cloneResponse;
        String executionId = testSourceInput.getExecutionId();
        Path repoPath = Paths.get(RepositoryUtils.buildCloneLocation(executionId));
        String state = executionContext.get(SharedConstants.STATE);

        // (1) Clone the repo (if needed)
        if (Files.notExists(repoPath) || Constants.ImportStates.CLONING.equals(state)) {
            try {
                cloneResponse = SourceControlUtils.cloneRepository(executionContext, endPointProperties, testSourceProperties.get(SharedConstants.BRANCH), executionId);
            } catch (Exception e) {
                cloneResponse = new CloneResponse(CommandStatus.FAILED, e.getMessage());
            }

            if (CommandStatus.FAILED.equals(cloneResponse.getCommandStatus())) {
                //deleteFolderInFailure(repoPath);
                return ExternalTestSourceResponse.createResponseForFailure("Failed to clone repository", cloneResponse.getMessage());
            }

            if (CommandStatus.RUNNING.equals(cloneResponse.getCommandStatus())) {
                return ExternalTestSourceResponse.createResponseForRunning("Running", "Cloning repository", 10f, 3000, executionContext);
            }

            if (CommandStatus.FINISHED.equals(cloneResponse.getCommandStatus())) {
                logger.debug("Finished to clone/checkout repository clone location = '{}'", cloneResponse.getCloneLocation());
                executionContext.put(SharedConstants.STATE, Constants.ImportStates.CLONE_FINISHED);
                return ExternalTestSourceResponse.createResponseForRunning("Running", "Cloning repository", 10f, 0, executionContext);
            }
        }

        // (2) Prepare the tests:
        if (Constants.ImportStates.CLONE_FINISHED       .equals(state) ||
            Constants.ImportStates.PREPARING_FOR_IMPORT.equals(state)) {

            String commandId = executionContext.get(Constants.ImportCommands.PREPARE_TESTS);
            PrepareTestsResponse prepareResponse = GradleCommands.prepareTests(commandId, testSourceInput, executionId);

            CommandStatus commandStatus = prepareResponse.getCommandStatus();

            if (CommandStatus.FAILED.equals(commandStatus)) {
                //deleteFolderInFailure(repoPath);
                return prepareResponse.getResult();
            }

            if (CommandStatus.RUNNING.equals(commandStatus)) {
                executionContext.put(SharedConstants.STATE, Constants.ImportStates.PREPARING_FOR_IMPORT);
                executionContext.put(Constants.ImportCommands.PREPARE_TESTS, prepareResponse.getCommandId());
                return ExternalTestSourceResponse.createResponseForRunning("Running", "Preparing tests", 10f, 3000, executionContext);
            }

            if (CommandStatus.FINISHED.equals(commandStatus)) {
                logger.debug("Finished preparing tests");
                executionContext.put(SharedConstants.STATE, Constants.ImportStates.PREPARING_FINISHED);
                return ExternalTestSourceResponse.createResponseForRunning("Running", "Finished preparing tests", 10f, 0, executionContext);
            }
        }

        // (3) Import test suites
        String commandId = executionContext.get(Constants.ImportCommands.IMPORT_TEST_SUITES);
        GradleImportSuitesResponse importResponse = GradleCommands.importGradleTestSuites(commandId, testSourceInput, executionId);

        if (CommandStatus.RUNNING.equals(importResponse.getCommandStatus())) {
            logger.debug("Importing test suites");
            executionContext.put(Constants.ImportCommands.IMPORT_TEST_SUITES, importResponse.getCommandId());
            return ExternalTestSourceResponse.createResponseForRunning("Running", "Importing test suites", 50f, 2000, executionContext);
        }

        return importResponse.getResult();
    }
}