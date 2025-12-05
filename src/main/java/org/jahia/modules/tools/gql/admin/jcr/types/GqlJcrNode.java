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
package org.jahia.modules.tools.gql.admin.jcr.types;

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphQL type representing a JCR node
 */
@GraphQLName("JcrNode")
@GraphQLDescription("JCR node representation")
public class GqlJcrNode {
    private final Node node;
    private final String workspace;

    public GqlJcrNode(Node node, String workspace) {
        this.node = node;
        this.workspace = workspace;
    }

    @GraphQLField
    @GraphQLName("uuid")
    @GraphQLDescription("Node UUID")
    public String getUuid() throws RepositoryException {
        return node.getIdentifier();
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("Node name")
    public String getName() throws RepositoryException {
        return node.getName();
    }

    @GraphQLField
    @GraphQLName("path")
    @GraphQLDescription("Node path")
    public String getPath() throws RepositoryException {
        return node.getPath();
    }

    @GraphQLField
    @GraphQLName("primaryNodeType")
    @GraphQLDescription("Primary node type")
    public String getPrimaryNodeType() throws RepositoryException {
        return node.getPrimaryNodeType().getName();
    }

    @GraphQLField
    @GraphQLName("mixinNodeTypes")
    @GraphQLDescription("Mixin node types")
    public List<String> getMixinNodeTypes() throws RepositoryException {
        List<String> mixins = new ArrayList<>();
        NodeType[] mixinTypes = node.getMixinNodeTypes();
        for (NodeType mixin : mixinTypes) {
            mixins.add(mixin.getName());
        }
        return mixins;
    }

    @GraphQLField
    @GraphQLName("properties")
    @GraphQLDescription("Node properties")
    public List<GqlJcrProperty> getProperties() throws RepositoryException {
        List<GqlJcrProperty> result = new ArrayList<>();
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            result.add(new GqlJcrProperty(properties.nextProperty()));
        }
        return result;
    }

    @GraphQLField
    @GraphQLName("children")
    @GraphQLDescription("Child nodes with optional pagination")
    public List<GqlJcrNode> getChildren(
            @GraphQLName("limit") @GraphQLDescription("Maximum number of children to return") Integer limit,
            @GraphQLName("offset") @GraphQLDescription("Number of children to skip") Integer offset
    ) throws RepositoryException {
        List<GqlJcrNode> result = new ArrayList<>();
        NodeIterator children = node.getNodes();
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = 0;
        }
        // Skip offset
        int skipped = 0;
        while (children.hasNext() && skipped < offset) {
            children.nextNode();
            skipped++;
        }

        // Collect children up to limit
        int count = 0;
        while (children.hasNext() && count < limit) {
            result.add(new GqlJcrNode(children.nextNode(), workspace));
            count++;
        }

        return result;
    }

    @GraphQLField
    @GraphQLName("hasChildren")
    @GraphQLDescription("Whether this node has child nodes")
    public boolean hasChildren() throws RepositoryException {
        return node.hasNodes();
    }

    @GraphQLField
    @GraphQLName("childrenCount")
    @GraphQLDescription("Total number of child nodes")
    public long getChildrenCount() throws RepositoryException {
        NodeIterator children = node.getNodes();
        return children.getSize();
    }

    @GraphQLField
    @GraphQLName("locked")
    @GraphQLDescription("Whether this node is locked")
    public boolean isLocked() throws RepositoryException {
        try {
            return node.isLocked();
        } catch (Exception e) {
            return false;
        }
    }

    @GraphQLField
    @GraphQLName("versionable")
    @GraphQLDescription("Whether this node is versionable")
    public boolean isVersionable() throws RepositoryException {
        return node.isNodeType("mix:versionable");
    }

    @GraphQLField
    @GraphQLName("workspace")
    @GraphQLDescription("Workspace name")
    public String getWorkspace() {
        return workspace;
    }

    @GraphQLField
    @GraphQLName("depth")
    @GraphQLDescription("Node depth in the hierarchy")
    public int getDepth() throws RepositoryException {
        return node.getDepth();
    }
}
