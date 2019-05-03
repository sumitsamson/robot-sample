package com.ca.cdd.plugins.gradletesting;

public class GradleTestSuite {
    private String id;
    private String project;
    private String task;
    private String testClass;

    public GradleTestSuite(String id, String project, String task, String testClass) {
        this.id = id;
        this.project = project;
        this.task = task;
        this.testClass = testClass;
    }

    public String getId() {
        return id;
    }

    public String getProject() {
        return project;
    }

    public String getTask() {
        return task;
    }

    public String getTestClass() {
        return testClass;
    }

    @Override
    public String toString() {
        return "GradleTestSuite{" +
                "project='" + project + '\'' +
                ", task='" + task + '\'' +
                ", testClass='" + testClass + '\'' +
                '}';
    }
}