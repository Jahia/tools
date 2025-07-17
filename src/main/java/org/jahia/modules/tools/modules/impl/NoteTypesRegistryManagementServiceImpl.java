/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools.modules.impl;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.tools.modules.NoteTypesRegistryManagementService;
import org.jahia.osgi.BundleResource;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.content.nodetypes.ParseException;
import org.jahia.services.templates.ModuleVersion;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.DatabaseUtils;
import org.ops4j.pax.swissbox.extender.BundleURLScanner;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.jahia.services.content.nodetypes.NodeTypesDBServiceImpl.DEFINITIONS_PROPERTIES;

/**
 * Utility class to reload all or a given definition from a module
 */
@Component(service = NoteTypesRegistryManagementService.class, immediate = true)
public class NoteTypesRegistryManagementServiceImpl implements NoteTypesRegistryManagementService {

    private static final Logger logger = LoggerFactory.getLogger(NoteTypesRegistryManagementServiceImpl.class);
    private static final BundleURLScanner CND_SCANNER = new BundleURLScanner("META-INF", "*.cnd", false);

    private volatile boolean serviceEnabled = false;

    @Activate
    public void start() {
        logger.info("Starting NoteTypesRegistryManagementService");
        SettingsBean settingsBean = SettingsBean.getInstance();
        boolean developmentMode = settingsBean.isDevelopmentMode();
        boolean clusterActivated = settingsBean.isClusterActivated();
        this.serviceEnabled = developmentMode && !clusterActivated;
        if (!this.serviceEnabled) {
            logger.warn("NoteTypesRegistryManagementService is disabled. Reason(s):{}{}",
                    developmentMode ? "" : " not in dev mode",
                    clusterActivated ? " cluster activated" : "");
        }
    }

    @Override
    public void reloadNodeTypesFromJahiaModules() {
        if (!serviceEnabled) {
            logger.warn("NoteTypesRegistryManagementService is not enabled");
            throw new IllegalStateException("Service is not enabled - development mode required");
        }
        logger.info("Reloading all definitions");

        cleanNodeTypesTable();

        try {
            List<JahiaTemplatesPackage> packages = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageRegistry().getAvailablePackages();

            for (JahiaTemplatesPackage templatesPackage : packages) {
                if (templatesPackage.getBundle().getState() == Bundle.ACTIVE) {
                    try {
                        reloadNodeTypesFromJahiaModule(templatesPackage);
                    } catch (Exception e) {
                        logger.error("Failed to reload CND definitions for bundle: " + templatesPackage.getName(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to reload all CND definitions", e);
        }
    }

    private void cleanNodeTypesTable() {
        String sql = "DELETE FROM jahia_nodetypes_provider WHERE filename NOT LIKE 'system-%' AND filename <> ?";

        try (Connection conn = DatabaseUtils.getDatasource().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, DEFINITIONS_PROPERTIES);
            stmt.execute();
            logger.info("Cleaned node types table");
        } catch (SQLException e) {
            logger.error("Failed to clean node types table", e);
            throw new RuntimeException("Database cleanup failed", e);
        }
    }

    @Override
    public void reloadNodeTypesFromJahiaModule(JahiaTemplatesPackage jahiaTemplatesPackage) throws IOException, RepositoryException, ParseException {

        if (!serviceEnabled) {
            logger.warn("NoteTypesRegistryManagementService is not enabled");
            throw new IllegalStateException("Service is not enabled - development mode and no cluster required");
        }
        Bundle bundle = jahiaTemplatesPackage.getBundle();
        String systemId = bundle.getSymbolicName();

        List<URL> urls = CND_SCANNER.scan(bundle);
        if (urls.isEmpty()) {
            logger.info("No CND definitions found for bundle: {}", systemId);
        }

        NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
        JCRStoreService jcrStoreService = ServicesRegistry.getInstance().getJCRStoreService();

        List<Resource> resources = createBundleResources(urls, bundle);

        // Unregister existing definitions and register new ones
        nodeTypeRegistry.unregisterNodeTypes(systemId);
        nodeTypeRegistry.addDefinitionsFile(resources, systemId);

        // Deploy definitions
        ModuleVersion moduleVersion = jahiaTemplatesPackage.getVersion();
        long lastModified = getLastModified(resources);
        jcrStoreService.deployDefinitions(systemId, moduleVersion.toString(), lastModified);

        logger.info("Successfully registered definitions for bundle: {}", BundleUtils.getDisplayName(bundle));
    }

    private List<Resource> createBundleResources(List<URL> urls, Bundle bundle) {
        List<Resource> resources = new ArrayList<>();
        for (URL url : urls) {
            logger.debug("Processing CND file: {} in bundle: {}", url, bundle.getSymbolicName());
            resources.add(new BundleResource(url, bundle));
        }
        return resources;
    }

    private long getLastModified(List<Resource> resources) {
        return resources.stream().mapToLong(resource -> {
            try {
                return resource.lastModified();
            } catch (IOException e) {
                logger.warn("Failed to get last modified time for resource", e);
                return 0;
            }
        }).max().orElse(0);
    }
}
