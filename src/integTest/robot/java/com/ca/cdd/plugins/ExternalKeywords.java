package com.ca.cdd.plugins;

import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import com.ca.rp.plugins.dto.model.ExternalTaskResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@RobotKeywords
public class ExternalKeywords {
    private static final String ACTION_EXECUTE = "execute";
    private static final String ACTION_STOP = "stop";

    @RobotKeyword("Execute Task Loop But Stop In")
    public ExternalTaskResult executeTaskLoopButStopIn(String url, String body, Integer stopInSeconds) {
        return executeTaskLoopButStopAfterTimeoutAndANumberOfRequests(url, body, stopInSeconds, 1);
    }

    @RobotKeyword("Execute Task Loop But Stop After Timeout And A Number Of Requests")
    public ExternalTaskResult executeTaskLoopButStopAfterTimeoutAndANumberOfRequests(String url, String body, Integer stopInSeconds, Integer minimumOfExecutedRequests) {
        ExternalTaskResult externalTaskResult;
        String requestUrl = url + "?action=" + ACTION_EXECUTE;;
        String stopUrl = url + "?action=" + ACTION_STOP;;
        Long stopIn = null;
        if (stopInSeconds != null) {
            stopIn = new Date().getTime() + (stopInSeconds * 1000);
        }
        int numberOfExecutedRequests = 0;
        boolean stopRequestSent = false;

        try {
            do {
                externalTaskResult = processRequest(requestUrl, body);
                numberOfExecutedRequests++;
                body = updateExecutionContextInBody(externalTaskResult.getExecutionContext(), body);
                long waitTimeMs = externalTaskResult.getDelayTillNextPoll();

                NolioRESTClient.sleepIfRequired(waitTimeMs);

                if (!stopRequestSent && stopIn != null && new Date().getTime() >= stopIn && numberOfExecutedRequests >= minimumOfExecutedRequests) {
                    externalTaskResult = processRequest(stopUrl, body);
                    stopRequestSent = true;
                }
            } while (externalTaskResult != null && externalTaskResult.getExecutionStatus().equals(ExternalTaskResult.ExecutionStatus.RUNNING));
        } catch (ProcessingException e) {
            throw new RobotException("Failed to parse the Response. Reason: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RobotException("Illegal ExternalTaskInputs body: " + body);
        }
        return externalTaskResult;
    }

    private ExternalTaskResult processRequest(String url, String body) throws IOException {
        Response response = NolioRESTClient.executeRESTCall(url, HttpMethod.POST, body);
        String data = response.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(data, ExternalTaskResult.class);
    }


    private String updateExecutionContextInBody(Map<String, String> executionContext, String body) throws IOException {
        if (MapUtils.isEmpty(executionContext)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        ExternalTaskInputs taskInputs = mapper.readValue(body, ExternalTaskInputs.class);
        taskInputs.setExecutionContext(executionContext);
        return mapper.writeValueAsString(taskInputs);
    }
}
