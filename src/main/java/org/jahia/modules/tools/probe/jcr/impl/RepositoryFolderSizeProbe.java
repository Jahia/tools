/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools.probe.jcr.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.FileDataStore;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports current folder sizes of repository, index and eventually file data store.
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + RepositoryFolderSizeProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + RepositoryFolderSizeProbe.NAME,
        Probe.CATEGORY + "=" + RepositoryFolderSizeProbe.CATEGORY, Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + RepositoryFolderSizeProbe.CATEGORY
                + ",name=" + RepositoryFolderSizeProbe.KEY })
public class RepositoryFolderSizeProbe implements ProbeMBean {

    static final String CATEGORY = "jcr";

    static final String KEY = "repository-folder-size";

    private static final Logger logger = LoggerFactory.getLogger(RepositoryFolderSizeProbe.class);

    static final String NAME = "JCR repository folder size";

    private static void appendInfo(String label, File folder, StringBuilder out) throws IOException {
        if (out.length() > 0) {
            out.append("\n");
        }
        out.append(label).append(" [").append(folder.getCanonicalPath()).append("]: ").append(org.jahia.utils.FileUtils
                .humanReadableByteCount(FileUtils.sizeOfDirectoryAsBigInteger(folder).longValue(), true));
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        StringBuilder out = new StringBuilder(512);
        try {
            File repoHome = SettingsBean.getInstance().getRepositoryHome();
            appendInfo("Repository home", repoHome, out);
            appendInfo("System workspace index", new File(repoHome, "index"), out);
            appendInfo("Default workspace index", new File(repoHome, "workspaces/default/index"), out);
            appendInfo("Live workspace index", new File(repoHome, "workspaces/default/index"), out);
            DataStore dataStore = BundleCacheProbe.getJcrRepositoryContext().getDataStore();
            if (dataStore instanceof FileDataStore) {
                appendInfo("Data store", new File(((FileDataStore) dataStore).getPath()), out);
            }
        } catch (IOException e) {
            logger.error("Unable to get JCR repository folder info. Cause: " + e.getMessage(), e);
        }

        return out.toString();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
