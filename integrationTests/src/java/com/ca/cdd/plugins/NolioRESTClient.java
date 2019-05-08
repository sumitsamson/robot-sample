package com.ca.cdd.plugins;


import com.ca.rp.plugins.dto.error.PluginErrorDto;
import com.ca.rp.plugins.dto.metadata.PluginMetaDataHolder;
import com.ca.rp.plugins.dto.model.*;
import com.ca.rp.plugins.dto.model.externalCommit.ExternalCommitSourceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.http.HttpStatus;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;


@RobotKeywords
public class NolioRESTClient {

    private static int IGNORE_EXPECTED_STATUS = -1;
    @RobotKeyword("Get Manifest")
    public String getManifest(String url) {
        Response response = executeRESTCall(url, HttpMethod.GET, null);
        return response.readEntity(String.class);
    }

    @RobotKeyword("Validate Manifest Fields")
    public String validateManifestFields(String json) throws IOException {
        ValidatorService validatorService = new ValidatorServiceImpl();
        PluginMetaDataHolder responseAsObject;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode pluginMetaNode = mapper.readTree(json);
        try {
            responseAsObject = PluginJsonObjectMapper.getJsonObjectMapper().treeToValue(pluginMetaNode, PluginMetaDataHolder.class);
        } catch (Exception e) {
            throw new RobotException("Failed to parse the Response. to a PluginMetaDataHolder: " + e.getMessage());
        }
        validatorService.validate(responseAsObject);
        return responseAsObject.toString();
    }

    @RobotKeyword("Execute Task")
    public ExternalTaskResult executeTask(String url, String body, String action) {
        try {
            url += "?action="+action;
            Response response = executeRESTCall(url, HttpMethod.POST, body);
            return response.readEntity(ExternalTaskResult.class);
        } catch (ProcessingException e) {
            throw new RobotException("Failed to parse the Response. Reason: " + e.getMessage());
        }
    }

    @RobotKeyword("Execute Task Loop")
    public ExternalTaskResult executeTaskLoop(String url, String body, String action) {
        return executePollableLoop(url, body, action, ExternalTaskResult.class, ExternalTaskInputs.class);
    }

    @RobotKeyword("Execute Get Content Loop")
    public ExternalContentSourceResponse executeGetContentLoop(String url, String body) {
        return executePollableLoop(url, body, "xxx", ExternalContentSourceResponse.class, ExternalContentSourceInput.class);
    }

    private <R extends PollableResult, I extends PollableInput> R executePollableLoop(String url, String body, String action, Class<R> resultClass, Class<I> inputClass) {
        R externalResult;
        Response response;
        url += "?action=" + action;
        try {
            do {
                response = executeRESTCall(url, HttpMethod.POST, body);
                externalResult = response.readEntity(resultClass);
                body = updateExecutionContextInBody(externalResult.getExecutionContext(), body, inputClass);
                long waitTimeMs = externalResult.getDelayTillNextPoll();
                sleepIfRequired(waitTimeMs);
            } while (externalResult.getExecutionStatus().equals(PollableResult.ExecutionStatus.RUNNING));
        } catch (ProcessingException e) {
            throw new RobotException("Failed to parse the Response. Reason: " + e.getMessage());
        } catch (IOException e) {
            throw new RobotException("Illegal ExternalTaskInputs body: " + body);
        }
        return externalResult;
    }

    @RobotKeyword("Execute Import Loop")
    public ExternalTestSourceResponse executeImportLoop(String url, String body) {
        ExternalTestSourceResponse externalTestSourceResponse;
        Response response;
        try {
            do {
                response = executeRESTCall(url, HttpMethod.POST, body);
                externalTestSourceResponse = response.readEntity(ExternalTestSourceResponse.class);
                body = updateExecutionContextInBody(externalTestSourceResponse.getExecutionContext(), body, ExternalTestSourceInput.class);
                long waitTimeMs = externalTestSourceResponse.getDelayTillNextPoll();
                sleepIfRequired(waitTimeMs);
            } while (externalTestSourceResponse.getExecutionStatus() != null &&
                     externalTestSourceResponse.getExecutionStatus().equals(PollableResult.ExecutionStatus.RUNNING));
        } catch (ProcessingException e) {
            throw new RobotException("Failed to parse the Response. Reason: " + e.getMessage());
        } catch (IOException e) {
            throw new RobotException("Illegal ExternalTaskInputs body: " + body);
        }
        return externalTestSourceResponse;
    }

    static void sleepIfRequired(long waitTimeMs) {
        if (waitTimeMs > 0) {
            try {
                System.out.println(String.format("Waiting [%s] milliseconds before next task execution",
                        waitTimeMs));
                Thread.sleep(waitTimeMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String updateExecutionContextInBody(Map<String, String> executionContext, String body, Class<? extends PollableInput> pollableInputClass) throws IOException {
        if (MapUtils.isEmpty(executionContext)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        PollableInput taskInputs = mapper.readValue(body, pollableInputClass);
        taskInputs.setExecutionContext(executionContext);
        return mapper.writeValueAsString(taskInputs);
    }

    private <T> Object executeGenericPostOperation(Class<T> responseClass, String url, String body, int expectedStatus) {
        Object output;
        try {
            Response response = executeRESTCall(url, HttpMethod.POST, body);
            if (expectedStatus == IGNORE_EXPECTED_STATUS) {
                output = response.readEntity(responseClass);
            } else if (expectedStatus == response.getStatus()) {
                output = response.getStatus() == HttpStatus.SC_OK ?
                         response.readEntity(responseClass) :
                         response.readEntity(PluginErrorDto.class);
            } else {
                String errorMessage = String.format("Expected status code [%d] and got instead [%s] on url [%s]",
                                                     expectedStatus, response.getStatus(), url);
                throw new RobotException(errorMessage);
            }
        } catch (ProcessingException e) {
            throw new RobotException("Failed to parse the Response. Reason: " + e.getMessage());
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    @RobotKeyword("Execute POST Operation Class")
    public <T> T executePostOperationClass(String url, String body, String className) {
        try {
            Class<T> aClass = (Class<T>) Class.forName(className);
            return (T) executeGenericPostOperation(aClass, url, body, IGNORE_EXPECTED_STATUS);
        } catch (ClassNotFoundException e) {
            throw new RobotException("Failed to find " + className + ". Reason: " + e.getMessage());
        }
    }

    @RobotKeyword("Execute POST Operation")
    public String executePostOperation(String url, String body) {
        return (String) executeGenericPostOperation(String.class, url, body, IGNORE_EXPECTED_STATUS);
    }

    @RobotKeyword("Execute Dynamic Values POST Operation")
    public DynamicValuesOutput executeDynamicValuesPostOperation(String url, String body) {
        return (DynamicValuesOutput) executeDynamicValuesOperation(url, body, HttpStatus.SC_OK);
    }

    @RobotKeyword("Execute Dynamic Values Operation")
    public Object executeDynamicValuesOperation(String url, String body, int expectedStatus) {
        return executeGenericPostOperation(DynamicValuesOutput.class, url, body, expectedStatus);
    }

    @RobotKeyword("Execute Import Tests Operation")
    public ExternalTestSourceResponse executeImportTestsOperation(String url, String body) {
        return (ExternalTestSourceResponse) executeGenericPostOperation(ExternalTestSourceResponse.class, url, body, HttpStatus.SC_OK);
    }

    @RobotKeyword("Execute Import Tests Operation")
    public Object executeImportTestsOperation(String url, String body, int expectedStatus) {
        return executeGenericPostOperation(ExternalTestSourceResponse.class, url, body, expectedStatus);
    }

    @RobotKeyword("Execute Import Commits Operation")
    public Object executeImportCommitsOperation(String url, String body, int expectedStatus) {
        return executeGenericPostOperation(ExternalCommitSourceResponse.class, url, body, expectedStatus);
    }

    @RobotKeyword("Check Connectivity")
    public ConnectivityResult checkConnectivity(String url, String body) {
        return (ConnectivityResult)executeGenericPostOperation(ConnectivityResult.class, url, body, IGNORE_EXPECTED_STATUS);
    }

    static Response executeRESTCall(String url, String method, String body) {
        Client httpClient = ClientBuilder.newClient();
        Response response;
        if (body != null && !body.isEmpty()) {
            response = httpClient.target(url).request().method(method, Entity.json(body));
        } else {
            response = httpClient.target(url).request().method(method);
        }
        if (response != null) {
            return response;
        } else {
            throw new RobotException("Failed to execute, No response");
        }
    }

    @RobotKeyword("Is Start With")
    public boolean isStartWith(String word, String prefix) {
        return word.toLowerCase().startsWith(prefix.toLowerCase());
    }


}
