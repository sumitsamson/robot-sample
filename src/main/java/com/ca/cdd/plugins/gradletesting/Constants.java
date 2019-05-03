package com.ca.cdd.plugins.gradletesting;

import com.ca.cdd.plugins.shared.SharedConstants;

public class Constants {
    //properties constants
    public static final String URL = "url";
    public static final String AUTH_TYPE = "authType";
    public static final String APIKEY = "apiKey";
    public static final String APIKEY_ID = "apiKeyId";
    public static final String APIKEY_SECRET = "apiKeySecret";

    public static final String TESTS = "tests";
    public static final String TEST = "test";

    public static final String ALL_ID = "ALL";
    public static final String FILTER_KEY = "filter";
    public static final String MAX_RESULTS = "max_results";


    public static final String GRADLE_VERSION = "gradleVersion";

    public static final String BRANCH = "branch";
    public static final String RUN_TEST_COMMAND_ID = "run_test_command_id";
    public static final String JOB_ID = "jobId";

    public interface ImportCommands {
        String PREPARE_TESTS = "prepare_test_command_id";
        String IMPORT_TEST_SUITES = "import_suites_command_id";
    }

    public interface ImportStates {
        String CLONING = SharedConstants.CLONING;
        String CLONE_FINISHED = SharedConstants.CLONE_FINISHED;
        String PREPARING_FOR_IMPORT = "preparing_for_import";
        String PREPARING_FINISHED = "preparing_finished";
        String IMPORTING = "importing";
        String IMPORTING_FINISHED = "importing_finished";
    }
}