package com.ca.cdd.plugins.gradletesting.utils.commands.creators;

import com.ca.cdd.plugins.shared.async.CommandCreator;
import com.ca.cdd.plugins.shared.async.CommandNotifier;
import com.ca.cdd.plugins.shared.async.Command;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.gradletesting.utils.commands.GradleRunTestCommand;
import com.ca.cdd.plugins.shared.async.responses.CommandResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.GradleRunTestResponse;
import com.ca.rp.plugins.dto.model.ExternalTaskInputs;

/**
 * Created by menyo01 on 02/01/2018.
 */
public class GradleRunTestCommandCreator implements CommandCreator {

    private final String testSuiteId;
    private final String executorId;
    private final ExternalTaskInputs taskInputs;

    public GradleRunTestCommandCreator(String testSuiteId, String executorId, ExternalTaskInputs taskInputs) {
        this.testSuiteId = testSuiteId;
        this.executorId = executorId;
        this.taskInputs = taskInputs;
    }


    @Override
    public Command create(CommandNotifier notifier) {
        return new GradleRunTestCommand(testSuiteId, executorId, taskInputs, notifier);
    }

    @Override
    public CommandResponse createResponseForRunning() {
        return new GradleRunTestResponse(CommandStatus.RUNNING);
    }
}
