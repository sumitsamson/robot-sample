package com.ca.cdd.plugins.gradletesting;

import com.ca.rp.plugins.dto.model.ExternalTaskResult;
import com.ca.rp.plugins.dto.model.TestCaseResult;
import com.ca.rp.plugins.dto.model.TestSuiteResult;
import com.ca.rp.plugins.dto.model.impl.TestSuiteResultImpl;
import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XMLReportReader {
    private static final Logger logger = LoggerFactory.getLogger(XMLReportReader.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
    private static final String TEST_SUITE_DESCRIPTION_TEMPLATE = "Total test cases: [%s], of which: skipped: [%s], failed: [%s], errors: [%s]";

    private static final String TAG_TESTSUITE = "testsuite";
    private static final String TAG_TESTCASE = "testcase";
    private static final String TAG_FAILURE = "failure";
    private static final String TAG_SKIPPED = "skipped";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_TIME = "time";
    private static final String ATTRIBUTE_TIMESTAMP = "timestamp";
    private static final String ATTRIBUTE_MESSAGE = "message";

    private final GradleTestSuite testSuite;
    private final File buildFolder;
    private final File reportFile;
    private final HTMLReportLocatorImpl htmlReportLocator;

    /**
     * Create only after report files are ready !!!
     *  @param testSuite
     * @param buildFolder
     * @param htmlReportLocator
     */
    public XMLReportReader(GradleTestSuite testSuite, File buildFolder, HTMLReportLocatorImpl htmlReportLocator) {
        logger.trace("Creating XMLReportReader, for ({}), folder: {}", testSuite, buildFolder);
        this.testSuite = testSuite;
        this.buildFolder = buildFolder;
        this.htmlReportLocator = htmlReportLocator;
        this.reportFile = getResultXMLFile(testSuite, buildFolder);
        if (reportFile == null) {
            logger.error("XML reportFile is missing");
        }
    }

    public ExternalTaskResult read() {
        logger.trace("[START] read");
        try {
            ExternalTaskResult taskResult = readSuiteResults();
            logger.trace("[END] read");
            return taskResult;
        } catch (ParserConfigurationException | IOException | SAXException ex) {
            logger.error("Can't parse test suite result XML", ex);
            return ExternalTaskResult.createResponseForFinished("cant parse XML report","Can't parse test suite result XML. " + ex.getMessage());
        }
    }

    private File getResultXMLFile(GradleTestSuite testSuite, File buildFolder) {
        File testResultsFolder = new File(buildFolder, "test-results");
        if (!testResultsFolder.exists()) {
            logger.error("Test result folder not found, for test suite: [{}]. Build folder: [{}]", testSuite, buildFolder);
            return null;
        }
        File taskTestResults = new File(testResultsFolder, testSuite.getTask());
        if (!taskTestResults.exists()) {
            logger.error("Test TASK result folder not found, for test suite: [{}]. Build folder: [{}]", testSuite, buildFolder);
            return null;
        }
        String testSuitesResultFileName = String.format("%s-%s.xml", testSuite.getTask().toUpperCase(), testSuite.getTestClass());
        File file = new File(taskTestResults, testSuitesResultFileName);
        if (!file.exists()){
            logger.error("Test result file not found, for test suite: [{}]. Build folder: [{}]", testSuite, buildFolder);
            return null;
        }
        return file;
    }

    private ExternalTaskResult readSuiteResults() throws ParserConfigurationException, IOException, SAXException {
        logger.trace("[START] readSuiteResults");
        ExternalTaskResult response = new ExternalTaskResult();
        TestSuiteResult suiteResult = new TestSuiteResultImpl();
        response.setTestSuiteResult(suiteResult);
        if (this.reportFile == null) {
            // ? response.setExternalTaskExecutionStatus(ExternalTaskResult.ExternalTaskExecutionStatus.FAILED);
            response.setDetailedInfo("Could not find the XML report file to parse");
            logger.error("no reportFile");
            return response;
        }

        // (1) Create XML Document, from file:
        logger.trace("[START] Creating XML Document");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(this.reportFile);
        logger.trace("[DONE] Creating XML Document");

        // (2) Find "testsuite" node
        logger.trace("Looking for {} node", TAG_TESTSUITE);
        NodeList suiteNodes = doc.getElementsByTagName(TAG_TESTSUITE);
        DeferredElementImpl suiteNode = null;
        for (int i=0; i<suiteNodes.getLength(); i++) {
            DeferredElementImpl aNode = (DeferredElementImpl) suiteNodes.item(i);
            if (aNode.getAttribute(ATTRIBUTE_NAME).equals(testSuite.getTestClass())) {
                suiteNode = aNode;
            }
        }

        if (suiteNode == null){
                logger.error("testsuite tag not found for test suite: [{}], XML file: [{}]", testSuite, reportFile);
                response.setDetailedInfo("testsuite tag not found in XML report");
            return response;
        }
        logger.trace("Got a {} node", TAG_TESTSUITE);

        // (3) Read TestSuite data:
        response = parseTestSuiteResult(suiteNode);

        logger.trace("[DONE] readSuiteResults");
        return response;
    }

    private ExternalTaskResult parseTestSuiteResult(DeferredElementImpl suiteNode) {
        logger.trace("[START] parseTestSuiteResult");
        ExternalTaskResult response = new ExternalTaskResult();
        TestSuiteResult suiteResult = new TestSuiteResultImpl();

        try {
            registerSuiteReport(suiteResult);
        } catch (Exception e) {
            logger.error("Could not register test suite result report", e);
        }

        String timestamp = suiteNode.getAttribute(ATTRIBUTE_TIMESTAMP);

        Date startDate;
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
            startDate = df.parse(timestamp);
        } catch (ParseException e) {
            logger.error("Can't parse suite start date: " + timestamp);
            startDate = new Date();
        }
        String suiteDescription = String.format(TEST_SUITE_DESCRIPTION_TEMPLATE,
                suiteNode.getAttribute("tests"),
                suiteNode.getAttribute("skipped"),
                suiteNode.getAttribute("failures"),
                suiteNode.getAttribute("errors")
        );
        response.setDetailedInfo(suiteDescription);
        logger.info("Test suite: " + suiteDescription);

        String time = suiteNode.getAttribute(ATTRIBUTE_TIME);
        Double suiteSeconds = Double.valueOf(time);

        Long endTime = startDate.getTime() + (int)(suiteSeconds * 1000);
//        suiteResult.setId("?");
        suiteResult.setExecutionStartDate(startDate.getTime());
        suiteResult.setExecutionEndDate(endTime);

        logger.debug("Starting to parse TestCases...");
        boolean hasFailures = false;
        Long testStartTime = startDate.getTime();
        NodeList testCaseNodes = suiteNode.getElementsByTagName(TAG_TESTCASE);
        for (int i=0; i<testCaseNodes.getLength(); i++) {
            logger.trace("In test case {}",  i);
            DeferredElementImpl testCase = (DeferredElementImpl) testCaseNodes.item(i);
            TestCaseResult testCaseResult = parseTestCaseResult(testStartTime, testCase);
            hasFailures = hasFailures || TestCaseResult.TestCaseStatus.FAILED.equals(testCaseResult.getExecutionStatus());
            suiteResult.addTestCaseResult(testCaseResult);
            testStartTime = testCaseResult.getExecutionEndDate();
        }

        if (hasFailures) {
            logger.info("Got failed tests");
            response = ExternalTaskResult.createResponseForFailure("Got failed tests", "");
        }

        response.setTestSuiteResult(suiteResult);
        logger.trace("[END] parseTestSuiteResult");
        return response;
    }

    private void registerSuiteReport(TestSuiteResult suiteResult) {

        final File htmlReportRootFolder = new File(htmlReportLocator.getRootFolder());
        final File htmlReport = new File(htmlReportRootFolder, htmlReportLocator.getEntryPoint());

        if ( !htmlReport.exists() ){
            // Use XML report instead
            suiteResult.setReportFilename(
                    htmlReportLocator.getTaskInputs(),
                    reportFile.getParentFile().getAbsolutePath(),
                    reportFile.getName()
            );
        } else {
            htmlReportLocator.onReportReady();
            suiteResult.setReportFilename(
                    htmlReportLocator.getTaskInputs(),
                    htmlReportLocator.getRootFolder(),
                    htmlReportLocator.getEntryPoint(),
                    htmlReportLocator.getExtraFiles()
            );
        }
    }

    private TestCaseResult parseTestCaseResult(Long testStartTime, DeferredElementImpl testCase) {
        logger.trace("[START] parseTestCaseResult");
        String testName = testCase.getAttribute(ATTRIBUTE_NAME);
        logger.trace("Test name: {}", testName);
        String timeLength = testCase.getAttribute(ATTRIBUTE_TIME);
        long milliSeconds = (long) (Double.valueOf(timeLength) * 1000);

        TestCaseResult testCaseResult = new TestCaseResult();
//            testCaseResult.setId();
        testCaseResult.setName(testName);
        testCaseResult.setExecutionDuration(milliSeconds);
        testCaseResult.setExecutionStartDate(testStartTime);
        testCaseResult.setExecutionEndDate(testStartTime+milliSeconds);
        testCaseResult.setExecutionStatus(TestCaseResult.TestCaseStatus.PASSED);
//            testCaseResult.setResultMessage("All the same..");
        testCaseResult.setExternalId(testName);

        if (testCase.hasChildNodes()) {

            // Treat skipped:
            NodeList skipped = testCase.getElementsByTagName(TAG_SKIPPED);
            if (skipped.getLength() > 0){
                logger.debug("Skipped test: " + testName);
                testCaseResult.setExecutionStatus(TestCaseResult.TestCaseStatus.SKIPPED);
            }
            // (OR) Treat failures:
            NodeList failures = testCase.getElementsByTagName(TAG_FAILURE);
            if (failures.getLength() > 0 ) {
                logger.debug("Failed test: " + testName);
                DeferredElementImpl failure = (DeferredElementImpl) failures.item(0);
                String message = failure.getAttribute(ATTRIBUTE_MESSAGE);
                testCaseResult.setResultMessage(message);
                testCaseResult.setExecutionStatus(TestCaseResult.TestCaseStatus.FAILED);
                logger.trace("Failure message: " + message);
            }

        }

        logger.trace("[DONE] parseTestCaseResult");
        return testCaseResult;
    }
}