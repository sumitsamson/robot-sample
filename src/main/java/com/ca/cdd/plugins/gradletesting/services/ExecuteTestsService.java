package com.ca.cdd.plugins.gradletesting.services;

import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import com.ca.rp.plugins.dto.model.ExternalTaskResult;

public interface ExecuteTestsService {
    ExternalTaskResult start(ExternalTaskInputs taskInputs);

    ExternalTaskResult stop(ExternalTaskInputs taskInputs);
}
