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
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;

@GraphQLName("BundleWithDependencies")
@GraphQLDescription("Result of the dependency inspector operation.")
public class BundleWithDependencies extends BundleResultEntry{

    private final List<Dependency> dependencies = new ArrayList<>();

    public BundleWithDependencies(Bundle bundle) {
        super(bundle);
    }

    @GraphQLField
    @GraphQLName("dependencies")
    @GraphQLDescription("List of bundle dependencies (packages and modules).")
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

}
