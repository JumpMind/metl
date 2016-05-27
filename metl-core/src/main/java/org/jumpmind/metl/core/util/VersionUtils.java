package org.jumpmind.metl.core.util;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class VersionUtils {

    static final Logger log = LoggerFactory.getLogger(VersionUtils.class);

    private static String version;
    
    private static String buildTime;
    
    private static String scmVersion;
    
    private static String scmBranch;

    private VersionUtils() {
    }

    static {
        InputStream is = null;
        try {
            Enumeration<URL> resources = VersionUtils.class.getClassLoader().getResources(
                    "META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                is = resources.nextElement().openStream();
                Manifest manifest = new Manifest(is);
                Attributes attributes = manifest.getMainAttributes();
                String projectName = attributes.getValue("Project-Artifact");
                if (isNotBlank(projectName) && projectName.toLowerCase().startsWith("metl-core")) {
                    version = attributes.getValue("Build-Version");
                    buildTime = attributes.getValue("Build-Time");
                    scmVersion = attributes.getValue("Build-Scm-Version");
                    scmBranch = attributes.getValue("Build-Scm-Branch");
                }
            }
        } catch (IOException e) {
            // nothing to do, really
        } finally {
            IOUtils.closeQuietly(is);
            
            if (isBlank(version)) {
                try {
                    Properties gradleProperties = new Properties();
                    is = new FileInputStream(new File("../metl-assemble/gradle.properties"));
                    gradleProperties.load(is);
                    version = gradleProperties.getProperty("version");
                } catch (Exception e) {
                    IOUtils.closeQuietly(is);
                }
            }
        }
    }

    public static String getCurrentVersion() {
        return version;
    }

    public static String getBuildTime() {
        return buildTime;
    }

    public static String getScmVersion() {
        return scmVersion;
    }

    public static String getScmBranch() {
        return scmBranch;
    }

}
