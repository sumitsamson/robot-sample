package com.ca.cdd.plugins.gradletesting.utils.responses;

import com.ca.cdd.plugins.shared.async.CommandStatus;
import com.ca.cdd.plugins.shared.async.responses.CommandResponse;
import com.ca.rp.plugins.dto.model.ExternalTestSourceResponse;

/**
 * Created by yomen007 on 31/14/2017.
 */
public class PrepareTestsResponse extends CommandResponse {

    private ExternalTestSourceResponse result;

    public PrepareTestsResponse(CommandStatus commandStatus) {
        super(commandStatus);
    }

    public PrepareTestsResponse(CommandStatus commandStatus, ExternalTestSourceResponse result) {
        super(commandStatus);
        this.result = result;
    }

    public ExternalTestSourceResponse getResult() {
        return result;
    }
}
