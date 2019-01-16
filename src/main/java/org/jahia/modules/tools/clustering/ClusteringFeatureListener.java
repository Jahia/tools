/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools.clustering;

import org.jahia.bin.Jahia;
import org.jahia.osgi.BundleLifecycleUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the clustering feature installed event to trigger refresh of the tools-ee fragment bundle.
 *
 * @author Sergiy Shyrkov
 */
@Component(name = "org.jahia.tools.clustering.listener", service = EventHandler.class, property = {
        Constants.SERVICE_PID + "=org.jahia.tools.clustering.listener",
        Constants.SERVICE_DESCRIPTION + "=DX clustring feature listener",
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        EventConstants.EVENT_TOPIC + "=" + FrameworkService.EVENT_TOPIC_LIFECYCLE, EventConstants.EVENT_FILTER
                + "=(type=" + FrameworkService.EVENT_TYPE_CLUSTERING_FEATURE_INSTALLED + ")" }, immediate = true)
public class ClusteringFeatureListener implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClusteringFeatureListener.class);

    @Override
    public void handleEvent(Event event) {
        logger.info("Notified about clustering feature being installed");
        Bundle toolsEeBundle = BundleUtils.getBundleBySymbolicName("tools-ee", null);
        if (toolsEeBundle != null && toolsEeBundle.getState() != Bundle.UNINSTALLED) {
            logger.info("Refreshing tools-ee bundle...");
            BundleLifecycleUtils.refreshBundle(toolsEeBundle);
            logger.info("...done refreshing tools-ee bundle.");
        }
    }
}
