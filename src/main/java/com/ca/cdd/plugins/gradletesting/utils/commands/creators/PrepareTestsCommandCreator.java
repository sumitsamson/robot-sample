package com.ca.cdd.plugins.gradletesting.utils.commands.creators;

import com.ca.cdd.plugins.shared.async.CommandCreator;
import com.ca.cdd.plugins.shared.async.CommandNotifier;
import com.ca.cdd.plugins.shared.async.Command;
import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.gradletesting.utils.commands.PrepareTestsCommand;
import com.ca.cdd.plugins.shared.async.responses.CommandResponse;
import com.ca.cdd.plugins.gradletesting.utils.responses.PrepareTestsResponse;
import com.ca.rp.plugins.dto.model.ExternalTestSourceInput;

/**
 * Created by menyo01 on 02/01/2018.
 */
public class PrepareTestsCommandCreator implements CommandCreator {

    private final ExternalTestSourceInput testSourceInput;
    private final String executionId;

    public PrepareTestsCommandCreator(ExternalTestSourceInput testSourceInput, String executionId) {
        this.testSourceInput = testSourceInput;
        this.executionId = executionId;
    }


    @Override
    public Command create(CommandNotifier notifier) {
        return new PrepareTestsCommand(testSourceInput, executionId, notifier);
    }

    @Override
    public CommandResponse createResponseForRunning() {
        return new PrepareTestsResponse(CommandStatus.RUNNING);
    }
}
