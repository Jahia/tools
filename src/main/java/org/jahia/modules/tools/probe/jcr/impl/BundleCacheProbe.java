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
package org.jahia.modules.tools.probe.jcr.impl;

import org.apache.commons.lang.reflect.FieldUtils;
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
