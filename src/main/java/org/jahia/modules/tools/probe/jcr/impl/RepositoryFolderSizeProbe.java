/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
