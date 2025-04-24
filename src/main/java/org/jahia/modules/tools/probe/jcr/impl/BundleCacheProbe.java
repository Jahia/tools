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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.jackrabbit.core.JahiaRepositoryImpl;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.cache.AbstractCache;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports current Jackrabbit BundleCache status.
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + BundleCacheProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + BundleCacheProbe.NAME, Probe.CATEGORY + "=" + BundleCacheProbe.CATEGORY,
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + BundleCacheProbe.CATEGORY + ",name="
                + BundleCacheProbe.KEY })
public class BundleCacheProbe implements ProbeMBean {

    static final String CATEGORY = "jcr";

    static final String KEY = "bundle-cache";

    private static final Logger logger = LoggerFactory.getLogger(BundleCacheProbe.class);

    static final String NAME = "JCR bundle cache";

    private static void appendStats(String workspace, StringBuilder out) {
        try {
            RepositoryContext repoCtx = getJcrRepositoryContext();

            AbstractCache cache = (AbstractCache) FieldUtils
                    .getDeclaredField(AbstractBundlePersistenceManager.class, "bundles", true)
                    .get(workspace != null ? FieldUtils.readDeclaredField(
                            repoCtx.getWorkspaceManager().getWorkspaceStateManager(workspace), "persistMgr", true)
                            : repoCtx.getInternalVersionManager().getPersistenceManager());
            if (out.length() > 0) {
                out.append("\n");
            }
            out.append(cache.getCacheInfoAsString());
        } catch (Exception e) {
            logger.error("Unable to get the stats for the JCR bundle cache in workspace " + workspace + ". Cause: "
                    + e.getMessage(), e);
        }
    }

    static RepositoryContext getJcrRepositoryContext() {
        return ((JahiaRepositoryImpl) ((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider()
                .getRepository()).getRepository()).getContext();
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        StringBuilder out = new StringBuilder(512);
        appendStats("default", out);
        appendStats("live", out);
        appendStats(null, out);

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
