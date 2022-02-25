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
