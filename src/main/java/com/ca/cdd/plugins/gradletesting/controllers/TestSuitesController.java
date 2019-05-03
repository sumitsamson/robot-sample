package com.ca.cdd.plugins.gradletesting.controllers;

import com.ca.cdd.plugins.gradletesting.Constants;
import com.ca.cdd.plugins.gradletesting.services.ExecuteTestsService;
import com.ca.cdd.plugins.gradletesting.services.ImportTestsService;
import com.ca.cdd.plugins.gradletesting.services.impl.ExecuteTestsServiceImpl;
import com.ca.cdd.plugins.gradletesting.services.impl.ImportTestsServiceImpl;
import com.ca.rp.plugins.dto.model.*;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.ca.cdd.plugins.gradletesting.Constants.JOB_ID;


@Path("test-sources/test-suites")
public class TestSuitesController {

    private ImportTestsService importTestsService = new ImportTestsServiceImpl();
    private ExecuteTestsService executeTestsService = new ExecuteTestsServiceImpl();

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public ExternalTestSourceResponse importTestSuites(ExternalTestSourceInput testSourceInput,
                                                       @QueryParam(Constants.MAX_RESULTS) int maxResult) throws Exception {

        return importTestsService.importTests(testSourceInput, maxResult);
    }



    @POST @Path("/task") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public ExternalTaskResult executeTestSuite(@QueryParam(TaskConstants.ACTION_PARAM) String action, ExternalTaskInputs taskInputs) {

        if (TaskConstants.EXECUTE_ACTION.equalsIgnoreCase(action)) {
            return startOrContinueExecution(taskInputs);
        }

        if (TaskConstants.STOP_ACTION.equalsIgnoreCase(action)) {
            return executeTestsService.stop(taskInputs);
        }

        String detailedMessage = String.format("[%s] is not supported for this API as %s parameter", action, TaskConstants.ACTION_PARAM);
        return ExternalTaskResult.createResponseForFailure("UNKNOWN_ACTION",  // todo: const
                                                           detailedMessage,
                                                           taskInputs.getExecutionContext());
    }

    private ExternalTaskResult startOrContinueExecution(ExternalTaskInputs taskInputs) {
        try {
            //todo: validate endpoint properties
            //BlazeMeterValidator.validateEndpointProperties(taskInputs.getEndPointProperties());
            String jobId = taskInputs.getExecutionContext().get(JOB_ID);
            if (StringUtils.isNotBlank(jobId)) {
                //TODO: treat continue
                return null; //genericService.continueExecution(taskInputs, jobId);
            } else {
                return executeTestsService.start(taskInputs);
            }
        } catch (Exception e) {
            return ExternalTaskResult.createResponseForFailure("FAILED_TO_START",  // todo: const
                    "GradleTesting Plug-in Error: " + e.getMessage(),
                    taskInputs.getExecutionContext());
        }
    }


}
