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
package org.jahia.modules.tools.modules;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.nodetypes.ParseException;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service to manage node types in the Jahia node type registry.
 * <p>
 * This service provides functionality to reload node type definitions from Jahia modules,
 * allowing for dynamic registration and management of JCR node types defined in CND files.
 * </p>
 *
 * @author dgriffon
 * @since 1.0
 */
public interface NoteTypesRegistryManagementService {

    /**
     * Reloads node types from all available Jahia modules.
     * <p>
     * This method scans all active Jahia template packages and attempts to reload
     * their node type definitions. It provides a summary of successful and failed
     * reload operations.
     * </p>
     *
     * @return a map containing two lists:
     *         <ul>
     *         <li>"successfulBundles" - list of bundle names that were successfully reloaded</li>
     *         <li>"failedBundles" - list of bundle names that failed to reload with error messages</li>
     *         </ul>
     * @see #reloadNodeTypesFromJahiaModule(JahiaTemplatesPackage)
     */
    Map<String, List<String>> reloadNodeTypesFromJahiaModules();

    /**
     * Reloads node types from a specific Jahia module.
     * <p>
     * This method processes CND (Compact Node Definition) files from the specified
     * Jahia template package and registers the node types in the JCR node type registry.
     * </p>
     *
     * @param jahiaTemplatesPackage the Jahia template package containing node type definitions
     * @return {@code true} if the node types were successfully reloaded, {@code false} otherwise
     * @throws IOException if an I/O error occurs while reading CND files
     * @throws RepositoryException if a JCR repository error occurs during node type registration
     * @throws ParseException if the CND file parsing fails due to syntax errors
     * @throws IllegalArgumentException if the jahiaTemplatesPackage is null
     */
    boolean reloadNodeTypesFromJahiaModule(JahiaTemplatesPackage jahiaTemplatesPackage)
            throws IOException, RepositoryException, ParseException;
}
