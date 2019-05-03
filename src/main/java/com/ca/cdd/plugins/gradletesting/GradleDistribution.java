package com.ca.cdd.plugins.gradletesting;

import com.ca.cdd.plugins.shared.async.utils.OsUtils;
import com.ca.cdd.plugins.shared.async.utils.RepositoryUtils;
import org.eclipse.jgit.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GradleDistribution {
    private static final Logger logger = LoggerFactory.getLogger(GradleDistribution.class);

    public static final String DEFAULT_GRADLE_VERSION = "3.5";
    private static final Map<String, File> GRADLE_VERSION_INSTALLATION_FOLDER = new HashMap<>();
    private static final String DISTRIBUTIONS_HOME_WIN = "\\gradle";
    private static final String DISTRIBUTIONS_HOME_NIX = "/opt/gradle";
    private static final String[] VERSIONS = new String[]{
            "4.4",
            "4.0.2",
            "3.5",
            "3.0",
            "2.14"
    };

    static {
        for (String version : VERSIONS) {
            File installationDir = new File(getDistributionsHome() +  File.separator + "gradle-" + version);
            if (!installationDir.exists()) {
                logger.error("Installation folder for gradle version {} does not exist! {}", version, installationDir.getPath());
                continue;
            }
            if (!installationDir.isDirectory()) {
                logger.error("Installation folder for gradle version {} is not a folder! {}", version, installationDir.getPath());
                continue;
            }

            GRADLE_VERSION_INSTALLATION_FOLDER.put(version, installationDir);
        }
    }

    private static String getDistributionsHome() {
        if (OsUtils.isWindows()) {
            return DISTRIBUTIONS_HOME_WIN;
        }
        return DISTRIBUTIONS_HOME_NIX;
    }

    public static @Nullable File getInstallationDir(String version){
        if (version == null) {
            return null;
        }
        return GRADLE_VERSION_INSTALLATION_FOLDER.get(version);
    }

}