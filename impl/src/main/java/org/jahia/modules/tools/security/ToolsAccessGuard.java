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
package org.jahia.modules.tools.security;

import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single source of truth for the tools authorization check. Evaluated at the resource (inside the tool JSPs) as a
 * second layer of defense that does not depend on any URL-pattern matching: it fires however the request was routed.
 */
public final class ToolsAccessGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsAccessGuard.class);

    /** JCR node the tools access permission is evaluated against. */
    private static final String TOOLS_PERMISSION_NODE = "/tools";

    /** Privilege required to use the support tools, identical to the Shiro rule {@code perms[/tools:systemToolsAccess]}. */
    private static final String TOOLS_PERMISSION = "systemToolsAccess";

    private ToolsAccessGuard() {
        // utility class
    }

    /**
     * Checks, as the currently-connected user, whether they are allowed to use the support tools. Any missing user or
     * error while resolving the permission is treated as a denial (default-deny).
     *
     * @return {@code true} only if the connected user holds {@code systemToolsAccess} on {@code /tools}
     */
    public static boolean isGranted() {
        JahiaUser currentUser = JCRSessionFactory.getInstance().getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        try {
            return JCRTemplate.getInstance().doExecute(currentUser, null, null,
                    session -> session.getNode(TOOLS_PERMISSION_NODE).hasPermission(TOOLS_PERMISSION));
        } catch (Exception e) {
            LOGGER.debug("Unable to evaluate tools access for user {}, denying access", currentUser.getName(), e);
            return false;
        }
    }
}