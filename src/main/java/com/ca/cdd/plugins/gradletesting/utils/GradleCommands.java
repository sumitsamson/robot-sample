package com.ca.cdd.plugins.gradletesting.utils;

import com.ca.cdd.plugins.gradletesting.utils.commands.creators.GradleImportSuitesCommandCreator;
import com.ca.cdd.plugins.gradletesting.utils.commands.creators.GradleRunTestCommandCreator;
import com.ca.cdd.plugins.gradletesting.utils.commands.creators.PrepareTestsCommandCreator;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleImportSuitesResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleRunTestResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.PrepareTestsResponse;
import com.ca.cdd.plugins.shared.async.CommandExecutor;
import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import com.ca.rp.plugins.dto.model.ExternalTestSourceInput;

public class GradleCommands {

    public static GradleRunTestResponse runGradleTest(String testSuiteId, String executorId, String commandId, ExternalTaskInputs taskInputs) {
        return CommandExecutor.getInstance().executeCommand(new GradleRunTestCommandCreator(testSuiteId, executorId, taskInputs), commandId);
    }

    public static GradleImportSuitesResponse importGradleTestSuites(String commandId, ExternalTestSourceInput testSourceInput, String executionId) {
        return CommandExecutor.getInstance().executeCommand(new GradleImportSuitesCommandCreator(testSourceInput, executionId), commandId);
    }

    public static PrepareTestsResponse prepareTests(String commandId, ExternalTestSourceInput testSourceInput, String executionId) {
        return CommandExecutor.getInstance().executeCommand(new PrepareTestsCommandCreator(testSourceInput, executionId), commandId);

    }

}
