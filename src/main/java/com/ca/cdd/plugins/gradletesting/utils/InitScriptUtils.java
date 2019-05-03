package com.ca.cdd.plugins.gradletesting.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class InitScriptUtils {
    private static final Logger logger = LoggerFactory.getLogger(InitScriptUtils.class);

    public static final String INIT_SCRIPT_TEST_EXPORT = "cdd.init.testExport.gradle";
    public static final String INIT_SCRIPT_PREPARE_TESTS = "cdd.init.preTestExport.gradle";
    public static final String INIT_SCRIPT_TEST = "cdd.init.test.gradle";

    public static void copyImportScripts(String cloneLocation) {
        copyScripts(cloneLocation, INIT_SCRIPT_PREPARE_TESTS, INIT_SCRIPT_TEST_EXPORT);
    }

    public static void copyTestScripts(String cloneLocation) {
        copyScripts(cloneLocation, INIT_SCRIPT_TEST);
    }

    private static void copyScripts(String cloneLocation, String... scripts) {
        try {
            for (String script : scripts) {
                copyScript(cloneLocation, script);
            }
        } catch (IOException e) {
            logger.error("Failed to copy gradle scripts to clone location = '{}' due to = '{}'", cloneLocation, e.getMessage(), e);
            throw new RuntimeException("Failed to copy script to clone location");
        }
    }

    private static void copyScript(String cloneLocation, String scriptName) throws IOException {
        Path path = Paths.get(cloneLocation + "/" + scriptName);
        InputStream resource = InitScriptUtils.class.getClassLoader().getResourceAsStream(scriptName);
        Files.copy(resource, path, StandardCopyOption.REPLACE_EXISTING);
    }
}
