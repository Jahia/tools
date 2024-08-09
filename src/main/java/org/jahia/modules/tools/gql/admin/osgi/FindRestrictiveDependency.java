/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.ArrayList;
import java.util.List;

@GraphQLName("FindRestrictivesDependencies")
@GraphQLDescription("Result of the dependency inspector operation.")
public class FindRestrictiveDependency {

    int totalCount = 0;
    List<BundleWithRestrictiveDependencies> bundleWithRestrictiveDependencies = new ArrayList<>();

    public void add(BundleWithRestrictiveDependencies bundleWithRestrictiveDependency) {
        bundleWithRestrictiveDependencies.add(bundleWithRestrictiveDependency);
        totalCount += bundleWithRestrictiveDependency.getRestrictiveDependencies().size();
    }

    @GraphQLField
    @GraphQLName("totalCount")
    @GraphQLDescription("Total number of restrictive dependencies.")
    public int getTotalCount() {
        return totalCount;
    }

    @GraphQLField
    @GraphQLName("bundles")
    @GraphQLDescription("List of matching bundles.")
    public List<BundleWithRestrictiveDependencies> getBundles() {
        return bundleWithRestrictiveDependencies;
    }
}
