package com.ca.cdd.plugins.gradletesting;

import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HTMLReportLocatorImpl implements HTMLReportLocator {
    private static final Logger logger = LoggerFactory.getLogger(HTMLReportLocator.class);

    private static final String TASK_NAME_PLACEHOLDER = ":taskName:";
    private static final String RELATIVE_HTML_REPORT_ROOT = "/reports/tests/"+TASK_NAME_PLACEHOLDER;

    private static final String TEST_SUITE_NAME_PLACEHOLDER = ":testSuiteName:";
    private static final String ENTRY_POINT = "/classes/"+TEST_SUITE_NAME_PLACEHOLDER+".html";

    // I am assuming in onReportReadyUnsafe() below that it is a single line !!!!
    private static final List<String> EXTRA_CSS = Collections.singletonList(".breadcrumbs { display: none; }");

    private static final String[] EXTRA_FILES = new String[]{
            "/css/base-style.css",
            "/css/style.css",
            "/js/report.js"
    };

    private final ExternalTaskInputs taskInputs;
    private final String entryPoint;
    private final String rootFolder;

    public HTMLReportLocatorImpl(ExternalTaskInputs taskInputs, File buildFolder, GradleTestSuite testSuite) {
        this.taskInputs = taskInputs;

        this.rootFolder = buildFolder.getAbsolutePath() + RELATIVE_HTML_REPORT_ROOT.replace(TASK_NAME_PLACEHOLDER, testSuite.getTask());
        this.entryPoint = ENTRY_POINT.replace(TEST_SUITE_NAME_PLACEHOLDER, testSuite.getTestClass());
    }

    @Override
    public ExternalTaskInputs getTaskInputs() {
        return taskInputs;
    }

    @Override
    public String getEntryPoint() {
        return entryPoint;
    }

    @Override
    public String getRootFolder() {
        return rootFolder;
    }

    @Override
    public String[] getExtraFiles() {
        return Arrays.copyOf(EXTRA_FILES,EXTRA_FILES.length);
    }

    @Override
    public void onReportReady() {
        try {
            onReportReadyUnsafe();
        } catch (Exception e) {
            logger.error("Can't modify test report", e);
        }
    }

    public void onReportReadyUnsafe() throws IOException {
        Path css = Paths.get(this.rootFolder, "css", "style.css");
        String lastLine = getLastLine(css);
        if (EXTRA_CSS.get(0).equals(lastLine)) {
            return;
        }

        Files.write(css, EXTRA_CSS, StandardOpenOption.APPEND);
    }


    private String getLastLine(Path css) throws IOException {
        String lastLine = null;
        BufferedReader bReader = Files.newBufferedReader(css);

        String line;
        while ( (line = bReader.readLine()) != null ) {
            lastLine = line;
        }
        return lastLine;
    }
}