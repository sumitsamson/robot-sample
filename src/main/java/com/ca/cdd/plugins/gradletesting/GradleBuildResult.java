package com.ca.cdd.plugins.gradletesting;

import org.omg.PortableInterceptor.SUCCESSFUL;

public class GradleBuildResult {

    private static final GradleBuildResult SUCCESSFUL_RESULT =
            new GradleBuildResult(true, null, null);

    private final boolean success;
    private final String failureReason;
    private final String detailedFailureReason;

    public static GradleBuildResult createSuccessfull(){
        return SUCCESSFUL_RESULT;
    }

    public GradleBuildResult(String failureReason, String detailedFailureReason) {
        this(false, failureReason, detailedFailureReason);
    }

    public GradleBuildResult(boolean sucess, String failureReason, String detailedFailureReason) {
        this.success = sucess;
        this.failureReason = failureReason;
        this.detailedFailureReason = detailedFailureReason;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDetailedFailureReason() {
        return detailedFailureReason;
    }
}