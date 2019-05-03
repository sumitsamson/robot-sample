package com.ca.cdd.plugins.gradletesting.services;

import com.ca.rp.plugins.dto.model.ExternalTestSourceInput;
import com.ca.rp.plugins.dto.model.ExternalTestSourceResponse;

public interface ImportTestsService {
    ExternalTestSourceResponse importTests(ExternalTestSourceInput testSourceInput, int maxResult);
}
