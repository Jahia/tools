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
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
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

import java.util.Comparator;

@GraphQLName("PackageWire")
@GraphQLDescription("Represents a single resolved OSGi package wire: a runtime binding between a requirer bundle " +
        "(one that declares an Import-Package or DynamicImport-Package) and the provider bundle that satisfies it " +
        "(one that declares a matching Export-Package). Only wires from active Jahia module bundles are returned.")
public class PackageWire implements Comparable<PackageWire> {

    private static final Comparator<PackageWire> COMPARATOR = Comparator
            .comparingLong((PackageWire w) -> w.providerBundle.getId())
            .thenComparingLong(w -> w.requirerBundle.getId())
            .thenComparing(w -> w.packageName);

    private final ProviderBundle providerBundle;
    private final String packageName;
    private final RequirerBundle requirerBundle;

    public PackageWire(Bundle providerBundle, String packageName, Bundle requirerBundle) {
        this.providerBundle = new ProviderBundle(providerBundle);
        this.packageName = packageName;
        this.requirerBundle = new RequirerBundle(requirerBundle);
    }

    @GraphQLField
    @GraphQLName("providerBundle")
    @GraphQLDescription("The bundle that exports the package — i.e. the one whose Export-Package manifest header satisfies this wire.")
    public ProviderBundle getProviderBundle() {
        return providerBundle;
    }

    @GraphQLField
    @GraphQLName("packageName")
    @GraphQLDescription("The fully-qualified Java package name that is wired, e.g. 'org.springframework.core.io'.")
    public String getPackageName() {
        return packageName;
    }

    @GraphQLField
    @GraphQLName("requirerBundle")
    @GraphQLDescription("The Jahia module bundle that imports the package — i.e. the one whose Import-Package or DynamicImport-Package manifest header created this wire.")
    public RequirerBundle getRequirerBundle() {
        return requirerBundle;
    }

    @Override
    public int compareTo(PackageWire other) {
        return COMPARATOR.compare(this, other);
    }
}
