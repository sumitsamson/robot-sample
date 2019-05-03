package com.ca.cdd.plugins.gradletesting.utils.responses;

import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.shared.async.responses.CommandResponse;
import com.ca.rp.plugins.dto.model.ExternalTaskResult;

/**
 * Created by menyo01 on 31/12/2017.
 */
public class GradleRunTestResponse extends CommandResponse {

    private ExternalTaskResult result;

    public GradleRunTestResponse(CommandStatus commandStatus) {
        super(commandStatus);
    }

    public GradleRunTestResponse(CommandStatus commandStatus, ExternalTaskResult result) {
        super(commandStatus);
        this.result = result;
    }

    public ExternalTaskResult getResult() {
        return result;
    }
}
