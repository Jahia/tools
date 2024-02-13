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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.osgi.framework.Bundle;

public abstract class BundleResultEntry {
    private final String bundleName;
    private final String bundleSymbolicName;
    private final String bundleDisplayName;
    private final long bundleId;

    public BundleResultEntry(Bundle bundle) {
        this.bundleName = bundle.getHeaders().get("Bundle-Name");
        this.bundleSymbolicName = bundle.getSymbolicName();
        this.bundleDisplayName = bundleName != null ?
                bundleName + " (" + bundleSymbolicName + ")" :
                bundleSymbolicName;
        this.bundleId = bundle.getBundleId();
    }

    @GraphQLField
    @GraphQLName("bundleName")
    @GraphQLDescription("Name of the bundle.")
    public String getBundleName() {
        return bundleName;
    }

    @GraphQLField
    @GraphQLName("bundleSymbolicName")
    @GraphQLDescription("Symbolic name of the bundle.")
    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    @GraphQLField
    @GraphQLName("bundleDisplayName")
    @GraphQLDescription("Display name of the bundle.")
    public String getBundleDisplayName() {
        return bundleDisplayName;
    }

    @GraphQLField
    @GraphQLName("bundleId")
    @GraphQLDescription("ID of the bundle.")
    public long getBundleId() {
        return bundleId;
    }
}
