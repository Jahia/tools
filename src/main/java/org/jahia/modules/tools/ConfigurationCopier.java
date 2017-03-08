/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.tools;

import static org.jahia.modules.tools.SupportInfoHelper.ENCODING;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Utility class for copying configuration files (used in the process of generating support information archive).
 * 
 * @see SupportInfoHelper
 * @author Sergiy Shyrkov
 */
class ConfigurationCopier {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationCopier.class);
    
    public static final String REPOSITORY_FOLDER  = "repository";
    
    private ConfigurationCopier() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Copies the configuration files specified, replacing the sensitive data like usernames, passwords etc..
     * 
     * @param targetDir the parent directory, the config file will be copied to
     * @param fromDigitalFactoryConfig consider files from digital-factory-config folder?
     * @param fromDigitalFactoryData consider files from digital-factory-data folder?
     * @param fromWebapp consider files from Web application folder?
     * @throws IOException in case of I/O errors during copying of files
     */
    public static void copy(File targetDir, boolean fromDigitalFactoryConfig, boolean fromDigitalFactoryData,
            boolean fromWebapp) {

        if (fromDigitalFactoryConfig) {
            exportConfigFromDigitalFactoryConfig(targetDir);
        }

        if (fromDigitalFactoryData) {
            exportConfigFromDigitalFactoryData(targetDir);
        }

        if (fromWebapp) {
            exportConfigFromWebapp(targetDir);
        }
    }

    private static void copyDirectory(File sourceDir, File destDir, String... excludedFileNames) throws IOException {
        final Set<String> excluded = new HashSet<>(Arrays.asList(excludedFileNames));
        FileUtils.copyDirectory(sourceDir, destDir, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return !excluded.contains(f.getName());
            }
        });
    }

    private static void copyFileReplaceSensitiveValues(File source, File target, String... sensitiveAttributes)
            throws IOException {
        if (!source.exists()) {
            return;
        }
        String content = FileUtils.readFileToString(source, ENCODING);
        for (String attr : sensitiveAttributes) {
            content = replaceSensitiveAttributeValue(content, attr);
        }

        FileUtils.writeStringToFile(target, content, ENCODING);
    }

    private static void exportConfigFromDigitalFactoryConfig(File cfg) {
        long startTime = System.currentTimeMillis();
        File cfgDir = null;
        try {
            cfgDir = getDigitalFactoryConfigDir();
            if (cfgDir != null) {
                File destDir = new File(cfg, "digital-factory-config");
                FileUtils.copyDirectory(cfgDir, destDir);
                replaceSensitivePropertyValue(new File(destDir, "jahia/jahia.properties"), "jahiaToolManagerPassword");
            } else {
                logger.warn("Unable to detect location of DX configuration folder. Skipping exporting config files.");
            }
        } catch (IOException e) {
            logger.error("Error exporting configuration files from folder " + cfgDir, e);
        }
        logger.info("Exported configuration from digital-factory-config folder in {} ms", System.currentTimeMillis() - startTime);
    }

    private static void exportConfigFromDigitalFactoryData(File cfg) {
        long startTime = System.currentTimeMillis();
        File destDir = new File(cfg, "digital-factory-data");
        File sourceDir = new File(SettingsBean.getInstance().getJahiaVarDiskPath());
        try {
            copyDirectory(new File(sourceDir, "karaf/etc"), new File(destDir, "karaf/etc"), "host.key",
                    "keys.properties", "users.properties");
        } catch (IOException e) {
            logger.error("Error exporting configuration files from folder " + sourceDir, e);
        }

        // now export JCR repository configuration
        try {
            sourceDir = SettingsBean.getInstance().getRepositoryHome();
            File destRepoDir = new File(destDir, REPOSITORY_FOLDER);
            FileUtils.copyFile(new File(sourceDir, "workspaces/default/workspace.xml"),
                    new File(destRepoDir, "workspaces/default/workspace.xml"));
            FileUtils.copyFile(new File(sourceDir, "workspaces/live/workspace.xml"),
                    new File(destRepoDir, "workspaces/live/workspace.xml"));
            FileUtils.copyFileToDirectory(new File(System.getProperty("jahia.jackrabbit.searchIndex.workspace.config")),
                    destRepoDir);
            FileUtils.copyFileToDirectory(
                    new File(System.getProperty("jahia.jackrabbit.searchIndex.versioning.config")), destRepoDir);
        } catch (IOException e) {
            logger.error("Error exporting configuration files from JCR repository home", e);
        }

        logger.info("Exported configuration from digital-factory-data folder in {} ms",
                System.currentTimeMillis() - startTime);
    }

    private static void exportConfigFromWebapp(File cfg) {
        long startTime = System.currentTimeMillis();
        File destDir = new File(cfg, "webapp");
        File sourceDir = new File(System.getProperty("jahiaWebAppRoot"));
        try {
            File metaInf = new File(sourceDir, "META-INF");
            if (metaInf.isDirectory()) {
                // copy context.xml removing sensitive data
                copyFileReplaceSensitiveValues(new File(sourceDir, "META-INF/context.xml"),
                        new File(destDir, "META-INF/context.xml"), "username", "password");

                // copy the rest of the files from META-INF (like updates etc.)
                copyDirectory(metaInf, new File(destDir, "META-INF"), "context.xml");
            }

            // XML files from WEB-INF
            File webInf = new File(sourceDir, "WEB-INF");
            File destWebInf = new File(destDir, "WEB-INF");
            for (File f : FileUtils.listFiles(webInf, new String[] { "xml" }, false)) {
                FileUtils.copyFileToDirectory(f, destWebInf);
            }

            // XML files from classes
            File destClassesDir = new File(destWebInf, "classes");
            for (File f : FileUtils.listFiles(new File(webInf, "classes"), new String[] { "xml" }, false)) {
                FileUtils.copyFileToDirectory(f, destClassesDir);
            }
            // jBPM files from classes
            FileUtils.copyDirectoryToDirectory(new File(webInf, "classes/jbpm"), destClassesDir);

            // WEB-INF/etc
            File etcDir = new File(SettingsBean.getInstance().getJahiaEtcDiskPath());
            File destEtcDir = new File(destWebInf, "etc");
            FileUtils.copyDirectoryToDirectory(new File(etcDir, "config"), destEtcDir);
            FileUtils.copyDirectoryToDirectory(new File(etcDir, "spring"), destEtcDir);

            copyDirectory(new File(etcDir, REPOSITORY_FOLDER), new File(destEtcDir, REPOSITORY_FOLDER), "root-mail-server.xml",
                    "root-user.xml");
            copyFileReplaceSensitiveValues(new File(etcDir, "repository/root-mail-server.xml"),
                    new File(destEtcDir, "repository/root-mail-server.xml"), "j:uri");
            copyFileReplaceSensitiveValues(new File(etcDir, "repository/root-user.xml"),
                    new File(destEtcDir, "repository/root-user.xml"), "j:password");
        } catch (IOException e) {
            logger.error("Error exporting configuration files from Web application folder " + sourceDir, e);
        }

        logger.info("Exported configuration from Web application folder in {} ms",
                System.currentTimeMillis() - startTime);
    }

    private static File getDigitalFactoryConfigDir() throws IOException {
        for (Resource r : SpringContextSingleton.getInstance().getResources("classpath*:jahia/jahia.properties")) {
            if (r != null && r.exists()) {
                try {
                    return r.getFile().getParentFile().getParentFile();
                } catch (IOException e) {
                    logger.debug("Could not retrieve grand-parent of " + r.getDescription(), e);
                    continue;
                }
            }
        }

        return null;
    }

    private static String replaceSensitiveAttributeValue(String content, String attributeName) {
        return content.replaceAll(attributeName + "=\"[^\"]*\"", attributeName + "=\"***\"");
    }

    private static void replaceSensitivePropertyValue(File file, String key) throws IOException {
        String content = FileUtils.readFileToString(file, ENCODING);
        int pos = content.indexOf(key);
        if (pos == -1) {
            return;
        } else {
            FileUtils.writeStringToFile(file, content.substring(0, content.indexOf('=', pos) + 1) + " ***"
                    + content.substring(content.indexOf('\n', pos + key.length())), ENCODING);
        }
    }

}
