package com.ca.cdd.plugins.gradletesting.utils;

import com.ca.cdd.plugins.gradletesting.Constants;
import com.ca.cdd.plugins.gradletesting.GradleBuildResult;
import com.ca.cdd.plugins.gradletesting.GradleDistribution;
import com.ca.cdd.plugins.gradletesting.GradleTestSuite;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import com.ca.rp.plugins.dto.model.ExternalTaskInputs;
import com.ca.rp.plugins.dto.model.ExternalTestSourceInput;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.gradle.tooling.*;
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException;
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException;
import org.gradle.tooling.model.GradleProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutionUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionUtils.class);

    private static final OutputStream PREFIXED_OUT_STREAM = new PrefixedOutputStream("[G]", System.out);
    private static final OutputStream PREFIXED_ERROR_STREAM = new PrefixedOutputStream("[GE]", System.err);

    private static final String GRADLE_BUILD_LOGS_FOLDER = "cdd.gradle.test.logs";

    private static String TEST_SUITE_ID_KEY = "test_source.test_suite_id";
    private static String TEST_SUITE_ID_DELIMITER = "::";

    public static String buildTestSuiteId(String projectName, String taskName, String testClass) {
        return String.join(TEST_SUITE_ID_DELIMITER, projectName, taskName, testClass);
    }

    public static String getTestSuiteId(ExternalTaskInputs taskInputs) {
        return taskInputs.getTaskProperties().get(TEST_SUITE_ID_KEY);
    }

    public static GradleTestSuite toGradleTestSuite(String testSuiteId) {
        if (testSuiteId == null) {
            throw new IllegalStateException("Missing test-suite id");
        }

        List<String> idParts = parseTestSuiteId(testSuiteId);
        if (idParts.size() != 3) {
            throw new IllegalStateException("Expecting test suite id to contain 3 part delimited by :: got instead: "
                    + testSuiteId);
        }

        return new GradleTestSuite(testSuiteId, idParts.get(0), idParts.get(1), idParts.get(2));
    }

    public static String extractGradleVersion(Map<String, String> endPointProperties) {
        return endPointProperties.get(Constants.GRADLE_VERSION);
    }

    public static String extractGradleVersion(ExternalTestSourceInput testSrcInputs) {
        Map<String, String> endPointProperties = testSrcInputs.getEndPointProperties();
        return extractGradleVersion(endPointProperties);
    }

    public static String extractGradleVersion(ExternalTaskInputs taskInputs) {
        Map<String, String> endPointProperties = taskInputs.getEndPointProperties();
        return extractGradleVersion(endPointProperties);
    }

    public static ProjectConnection createProjectConnection(String projectFolder, String gradleVersion) {
        return createProjectConnection(new File(projectFolder), gradleVersion);
    }

    public static ProjectConnection createProjectConnection(File projectFolder, String gradleVersion) {
        GradleConnector gradleConnector = GradleConnector.newConnector().useBuildDistribution();
        File gradleInstallationDir = GradleDistribution.getInstallationDir(gradleVersion);
        if (gradleInstallationDir != null) {
            logger.debug("Gradle installation not found: ({}). Using default: ({}), or the version defined in the project itself.",
                    gradleVersion, GradleDistribution.DEFAULT_GRADLE_VERSION);
            gradleConnector = gradleConnector.useInstallation(gradleInstallationDir);
        }
        return gradleConnector
                .forProjectDirectory(projectFolder)
                .connect();
    }

    //todo: cache !
    public static Map<String, File> getSubProjectFolders(ProjectConnection connection) {
        HashMap<String, File> projName2folder = new HashMap<>();

        //(3) Get all project names
        ModelBuilder<GradleProject> projectModel = connection.model(GradleProject.class);
        logger.info("***************************************************************");
        logger.info("*** Getting project structure, for ***");
        logger.info("*** Init Script : NONE ***");
        logger.info("***************************************************************");
        GradleProject rootProject = projectModel.get();
        extractAllProjectFolders(rootProject, projName2folder);

        return projName2folder;
    }

    //todo: cache !
    public static Map<String, GradleProject> getSubProjects(ProjectConnection connection) {
        HashMap<String, GradleProject> projName2Proj = new HashMap<>();

        ModelBuilder<GradleProject> projectModel = connection.model(GradleProject.class);
        GradleProject rootProject = projectModel.get();
        projName2Proj.put(rootProject.getPath(), rootProject);
        extractAllProjects(rootProject, projName2Proj);

        return projName2Proj;
    }

    private static void extractAllProjectFolders(GradleProject project, Map<String, File> projName2folder) {
        projName2folder.put(project.getPath(), project.getProjectDirectory());
        for (GradleProject subProject : project.getChildren()) {
            extractAllProjectFolders(subProject, projName2folder);
        }
    }

    private static void extractAllProjects(GradleProject project, Map<String, GradleProject> projName2Proj) {
        projName2Proj.put(project.getPath(), project);
        for (GradleProject subProject : project.getChildren()) {
            extractAllProjects(subProject, projName2Proj);
        }
    }

    public static <L extends ConfigurableLauncher> L configureLauncher(ConfigurableLauncher<L> launcher, String logFile) {
        final FileOutputStream fileOutputStream = getFileOutputStream(logFile);
        OutputStream outStream;
        OutputStream errStream;
        if (fileOutputStream != null) {
            outStream = new TeeOutputStream(PREFIXED_OUT_STREAM, fileOutputStream);
            errStream = new TeeOutputStream(PREFIXED_ERROR_STREAM, fileOutputStream);
        } else {
            outStream = PREFIXED_OUT_STREAM;
            errStream = PREFIXED_ERROR_STREAM;
        }

        launcher.setColorOutput(false);
        launcher.setStandardOutput(outStream);
        return launcher.setStandardError(errStream);
    }

    private static FileOutputStream getFileOutputStream(String logFile) {
        if (logFile == null) {
            logger.error("Got NULL logFile");
            return null;
        }
        final File file = new File(logFile);
        try {
            if (file.getParentFile().mkdirs() && !file.createNewFile()) {
                logger.error("Couldn't create log file ,might exists already" + logFile);
                return null;
            }
            ;
        } catch (IOException e) {
            logger.error("Couldn't create log file " + logFile, e);
            return null;
        }

        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("Couldn't create log FileOutputStream", e);
            return null;
        }
    }

    protected static List<String> parseTestSuiteId(String testSuiteId) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(testSuiteId)) {
            Pattern pattern = Pattern.compile("(.*)::(.*)::(.*)");
            Matcher matcher = pattern.matcher(testSuiteId);
            while (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    list.add(matcher.group(i));
                }
            }
        }
        return list;
    }

    public static GradleBuildResult executeGradleBuildSafely(BuildLauncher buildLauncher) {
        try {

            buildLauncher.run();
            return GradleBuildResult.createSuccessfull();

        } catch (UnsupportedOperationConfigurationException ex) {
            logger.error("E1", ex);
            return new GradleBuildResult("Unsupported Operation Configuration",
                    "The target Gradle version does not support some requested configuration option such as withArguments(String...).");
            //todo: make constant
        } catch (UnsupportedVersionException ex) {
            logger.error("E2", ex);
            return new GradleBuildResult("Unsupported Gradle Version",
                    "The target Gradle version does not support build execution.");
        } catch (UnsupportedBuildArgumentException ex) {
            logger.error("E3", ex);
            return new GradleBuildResult("Unsupported Build Argument",
                    "There is a problem with build arguments provided by withArguments(String...).");
            //todo: What are the arguments ??
        } catch (BuildException ex) {
            logger.error("E4", ex);
            return new GradleBuildResult("Build Failure",
                    "Failure executing the Gradle build.");
        } catch (BuildCancelledException ex) {
            logger.error("E5", ex);
            return new GradleBuildResult("Build Cancelled",
                    "The operation was cancelled before it completed successfully.");
        } catch (GradleConnectionException ex) {
            logger.error("E6", ex);
            return new GradleBuildResult("Gradle Connection Exception",
                    "Some general failure using the connection.");
        } catch (IllegalStateException ex) {
            logger.error("E7", ex);
            return new GradleBuildResult("Plugin-Gradle connection closed",
                    "Plugin-Gradle connection has been closed or is closing.");
        }
    }

    public static String absoluteLogFilePath(String fileName, String executionId) {
        final StringBuffer sb = new StringBuffer();
        return sb
                .append(RepositoryUtils.buildCloneLocation(executionId))
                .append(File.separator)
                .append(GRADLE_BUILD_LOGS_FOLDER)
                .append(File.separator)
                .append(fileName)
                .append(".log")
                .toString();
    }


}