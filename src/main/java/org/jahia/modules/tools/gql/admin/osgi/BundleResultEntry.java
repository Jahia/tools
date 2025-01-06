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
package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDeprecate;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.osgi.framework.Bundle;

/**
 * An abstract class that provide basic bundle information for a GQL result that would involve bundle.
 * Can be implemented to automatically provide bundle GQL fields.
 */
public abstract class BundleResultEntry {
    private final String name;
    private final String symbolicName;
    private final String displayName;
    private final long id;

    public BundleResultEntry(Bundle bundle) {
        this.name = bundle.getHeaders().get("Bundle-Name");
        this.symbolicName = bundle.getSymbolicName();
        this.displayName = name != null ?
                name + " (" + symbolicName + ")" :
                symbolicName;
        this.id = bundle.getBundleId();
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("Name of the bundle.")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("bundleName")
    @GraphQLDescription("Name of the bundle.")
    @GraphQLDeprecate("Use name instead")
    @Deprecated
    public String getBundleName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("symbolicName")
    @GraphQLDescription("Symbolic name of the bundle.")
    public String getSymbolicName() {
        return symbolicName;
    }

    @GraphQLField
    @GraphQLName("bundleSymbolicName")
    @GraphQLDescription("Symbolic name of the bundle.")
    @GraphQLDeprecate("Use symbolicName instead")
    @Deprecated
    public String getBundleSymbolicName() {
        return symbolicName;
    }

    @GraphQLField
    @GraphQLName("displayName")
    @GraphQLDescription("Display name of the bundle.")
    public String getDisplayName() {
        return displayName;
    }

    @GraphQLField
    @GraphQLName("bundleDisplayName")
    @GraphQLDescription("Display name of the bundle.")
    @GraphQLDeprecate("Use displayName instead")
    @Deprecated
    public String getBundleDisplayName() {
        return displayName;
    }

    @GraphQLField
    @GraphQLName("id")
    @GraphQLDescription("ID of the bundle.")
    public long getId() {
        return id;
    }

    @GraphQLField
    @GraphQLName("bundleId")
    @GraphQLDescription("ID of the bundle.")
    @GraphQLDeprecate("Use id instead")
    @Deprecated
    public long getBundleId() {
        return id;
    }

}
