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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.time.FastDateFormat;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeService;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for generating support information archive.
 * 
 * @author Sergiy Shyrkov
 */
public class SupportInfoHelper {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm-ss-SSS");

    static final String ENCODING = "UTF-8";

    private static final Logger logger = LoggerFactory.getLogger(SupportInfoHelper.class);
    
    private SupportInfoHelper() {
        throw new IllegalAccessError("Utility class");
    }

    private static void exportConfigurationFiles(File dumpDir, HttpServletRequest request) {
        ConfigurationCopier.copy(new File(dumpDir, "config"), request.getParameter("digital-factory-config") != null,
                request.getParameter("digital-factory-data") != null, request.getParameter("webapp") != null);
    }

    /**
     * Performs the action of generating the exported ZIP file for the support information, depending on the supplied request parameters.
     * 
     * @param targetDir the parent directory, the ZIP will be generated in
     * @param request current HTTP request object
     * @param response current HTTP response object
     * @throws IOException in case of I/O errors during ZIP generation
     */
    public static void exportInfo(File targetDir, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        long timeTaken = System.currentTimeMillis();
        File dumpDir = new File(targetDir, "support-info-" + DATE_FORMAT.format(System.currentTimeMillis()));
        FileUtils.deleteDirectory(dumpDir);
        FileUtils.forceMkdir(dumpDir);

        try {
            for (List<Probe> probesByCategory : getProbes().values()) {
                for (Probe p : probesByCategory) {
                    exportProbeData(p, dumpDir, request);
                }
            }

            exportConfigurationFiles(dumpDir, request);

            File generatedFile = null;
            try {
                generatedFile = zip(dumpDir);
            } catch (ArchiveException e) {
                throw new IOException(e);
            }

            timeTaken = System.currentTimeMillis() - timeTaken;

            logger.info("Support information exported in {} ms. Generated file: {}", timeTaken, generatedFile);

            if ("download".equals(request.getParameter("action"))) {
                writeZipContentToResponse(generatedFile, response);
            } else {
                request.setAttribute("generatedInfo", generatedFile);
                request.setAttribute("generationTime", timeTaken);
            }
        } finally {
            FileUtils.deleteQuietly(dumpDir);
        }
    }

    private static void writeZipContentToResponse(File generatedFile, HttpServletResponse response) throws IOException {
        // write the ZIP file content into the response output
        try (ServletOutputStream os = response.getOutputStream()) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + generatedFile.getName() + "\"");
            if (generatedFile.length() <= Integer.MAX_VALUE) {
                response.setContentLength((int) generatedFile.length());
            } else {
                response.setHeader("Content-Length", Long.toString(generatedFile.length()));
            }
            FileUtils.copyFile(generatedFile, os);
            os.flush();
        } finally {
            FileUtils.deleteQuietly(generatedFile);
        }
    }
    
    private static void exportProbeData(Probe p, File dumpDir, HttpServletRequest request) {
        if (request.getParameter(p.getCategory() + '|' + p.getKey()) != null) {
            long startTime = System.currentTimeMillis();
            try {
                FileUtils.write(
                        new File(new File(dumpDir, p.getCategory().toLowerCase()), p.getKey().toLowerCase() + ".txt"),
                        p.getData(), ENCODING);
                logger.info("Exported probe data for {}/{} in {} ms",
                        new Object[] { p.getCategory(), p.getKey(), System.currentTimeMillis() - startTime });
            } catch (Exception e) {
                logger.error("Error exporting probe data for " + p.getCategory() + "/" + p.getKey(), e);
            }
        }
    }

    /**
     * Retrieves registered probes grouped by their category.
     * 
     * @return registered probes grouped by their category
     */
    public static Map<String, List<Probe>> getProbes() {
        return BundleUtils.getOsgiService(ProbeService.class, null).getProbesByCategory();
    }

    private static File zip(File dumpDir) throws ArchiveException, IOException {
        File target = new File(dumpDir.getParentFile(), dumpDir.getName() + ".zip");
        int basePathLength = dumpDir.getAbsolutePath().length() + 1;
        // delete current zip if any and register new zip to be deleted on JVM termination
        FileUtils.deleteQuietly(target);
        target.deleteOnExit();
        try (ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP,
                new FileOutputStream(target))) {

            for (File f : FileUtils.listFiles(dumpDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                os.putArchiveEntry(new ZipArchiveEntry(f, f.getAbsolutePath().substring(basePathLength)));
                FileUtils.copyFile(f, os);
                os.closeArchiveEntry();
            }
            os.finish();
        }

        return target;
    }

}
