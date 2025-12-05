/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools.gql.admin.jcr;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.modules.tools.gql.admin.jcr.types.GqlJcrNode;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * GraphQL query root for JCR Browser operations
 */
@GraphQLName("JcrBrowser")
@GraphQLDescription("JCR Browser operations")
public class JcrBrowserGraphQL {
    private static final Logger logger = LoggerFactory.getLogger(JcrBrowserGraphQL.class);
    private static final String DEFAULT_ROOT_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    @GraphQLField
    @GraphQLDescription("Get a JCR node by UUID or path")
    public GqlJcrNode node(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("path") @GraphQLDescription("Node path") String path,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name (default or live)") String workspace
    ) throws RepositoryException {
        try {
         // Get JCR session
            Session session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);

            Node node;
            if (path != null && !path.isEmpty()) {
                // Get by path
                String escapedPath = JCRContentUtils.escapeNodePath(path);
                node = session.getNode(escapedPath);
            } else {
                // Get by UUID (default to root if not specified)
                String nodeUuid = (uuid != null && !uuid.isEmpty()) ? uuid : DEFAULT_ROOT_UUID;
                node = session.getNodeByIdentifier(nodeUuid);
            }

            return new GqlJcrNode(node, workspace);

        }
        catch (Exception e) {
            logger.error("Error retrieving node", e);
            throw new RepositoryException("Failed to retrieve node: " + e.getMessage(), e);
        }
    }
}
