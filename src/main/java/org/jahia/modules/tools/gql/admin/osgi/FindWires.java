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
package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.ArrayList;
import java.util.List;

@GraphQLName("FindWires")
@GraphQLDescription("Result of the bundle wire analysis.")
public class FindWires {

    private int totalCount = 0;
    private final List<BundleWithWires> bundles = new ArrayList<>();

    public void add(BundleWithWires bundleWithWires) {
        bundles.add(bundleWithWires);
        totalCount += bundleWithWires.getMatchingWires().size();
    }

    @GraphQLField
    @GraphQLName("totalCount")
    @GraphQLDescription("Total number of matching package wires found.")
    public int getTotalCount() {
        return totalCount;
    }

    @GraphQLField
    @GraphQLName("bundles")
    @GraphQLDescription("List of bundles with matching package wires.")
    public List<BundleWithWires> getBundles() {
        return bundles;
    }
}
