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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.felix.utils.manifest.Parser;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.services.modulemanager.util.ModuleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@GraphQLName("BundleWithDependencies")
@GraphQLDescription("Result of the dependency inspector operation.")
public class BundleWithDependencies extends BundleResultEntry {

    private final Map<Dependency.Status, List<Dependency>> dependenciesByStatus;

    public BundleWithDependencies(Bundle bundle) {
        super(bundle);
        // take all dependencies and create a map, using the status as the key and the list of dependencies as the value
        this.dependenciesByStatus =
                Stream.concat(getImportPackageDependencies(bundle), getJahiaDependsDependencies(bundle))
                        .collect(Collectors.groupingBy(Dependency::getStatus));
    }

    private Stream<Dependency> getImportPackageDependencies(Bundle bundle) {
        String importPackageHeader = bundle.getHeaders().get(Constants.IMPORT_PACKAGE);
        return Optional.ofNullable(importPackageHeader)
                .map(Parser::parseHeader).stream()
                .flatMap(Arrays::stream)
                .map(Dependency::parse);
    }

    private Stream<Dependency> getJahiaDependsDependencies(Bundle bundle) {
        String jahiaDependsHeader = bundle.getHeaders().get("Jahia-Depends");
        return Optional.ofNullable(jahiaDependsHeader)
                .map(ModuleUtils::toDependsArray).stream()
                .flatMap(Arrays::stream)
                .map(String::trim)
                .map(Dependency::parse);
    }

    @GraphQLField
    @GraphQLName("dependencies")
    @GraphQLDescription("List of bundle dependencies (packages and modules).")
    public List<Dependency> dependencies(
            @GraphQLName("supported") @GraphQLDescription("Allows to filter the list of dependencies returned. If the parameter is set to 'true', only the supported dependencies are returned. If set to 'false', only the unsupported ones are returned. By default, all dependencies are returned. This parameter cannot be used with the 'statuses' one.") Boolean supported,
            @GraphQLName("statuses") @GraphQLDescription("Return dependencies matching the provided statuses. This parameter cannot be used with the 'supported' one.") Collection<Dependency.Status> statusesRequested
    ) {
        final Collection<Dependency.Status> statuses;
        if (statusesRequested == null) {
            // by default return all statuses
            statuses = Arrays.stream(Dependency.Status.values())
                    .filter(dep -> supported == null || (supported ? dep.isSupported() : dep.isUnsupported()))
                    .collect(Collectors.toList());
        } else {
            if (supported != null) {
                throw new GqlJcrWrongInputException("The 'supported' and 'statuses' parameters cannot be used together");
            }
            // validate statuses are not empty if provided
            if (CollectionUtils.isEmpty(statusesRequested)) {
                throw new GqlJcrWrongInputException("At least one status must be provided (via the 'statuses' parameter)");
            }
            statuses = statusesRequested;
        }
        // filter dependencies by status
        return dependenciesByStatus.entrySet().stream()
                .filter(entry -> statuses.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    /**
     * Check if the bundle has at least one dependency with an unsupported version range.
     *
     * @return <code>true</code> if the bundle has at least one dependency with an unsupported version range, <code>false</code> otherwise
     */
    public boolean hasUnsupportedDependencies() {
        return dependenciesByStatus.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().isSupported() && !entry.getValue().isEmpty());

    }

}
