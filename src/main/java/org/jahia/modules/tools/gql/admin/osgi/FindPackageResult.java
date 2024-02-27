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

import java.util.List;

/**
 * Short description of the class
 *
 * @author Jerome Blanchard
 */
@GraphQLName("FindPackage")
@GraphQLDescription("Result of the search package inspector operation.")
public class FindPackageResult {

    int bundlesCount = 0;
    int importPackageCount = 0;
    int exportPackageCount = 0;

    List<BundleResultEntry> results;

    public FindPackageResult(List<BundleResultEntry> results) {
        this.results = results;
    }

    @GraphQLField
    @GraphQLName("bundlesCount")
    @GraphQLDescription("Total number of bundles containing matching packages.")
    public int getBundlesCount() {
        return results.size();
    }

    @GraphQLField
    @GraphQLName("importPackageCount")
    @GraphQLDescription("Total number of matching importPackages")
    public int getImportPackageCount() {
        return results.stream().mapToInt(b -> b.getImports().size()).sum();
    }

    @GraphQLField
    @GraphQLName("exportPackageCount")
    @GraphQLDescription("Total number of matching exportPackages")
    public int getExportPackageCount() {
        return results.stream().mapToInt(b -> b.getExports().size()).sum();
    }

    @GraphQLField
    @GraphQLName("results")
    @GraphQLDescription("Bundles found with matching packages")
    public List<BundleResultEntry> getResults() {
        return results;
    }

}
