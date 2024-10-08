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
package org.jahia.modules.tools.gql.admin;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.modules.tools.gql.admin.osgi.FindExportPackage;
import org.jahia.modules.tools.gql.admin.osgi.FindDependencies;
import org.jahia.modules.tools.gql.admin.osgi.OSGIPackageHeaderChecker;
import org.jahia.modules.tools.gql.admin.osgi.FindImportPackage;


@GraphQLName("AdminTools")
@GraphQLDescription("Jahia admin tools")
public class ToolsGraphQL {
    @GraphQLField
    @GraphQLDescription("Will return all the import packages matching the given parameters.")
    public FindImportPackage findMatchingImportPackages(
            @GraphQLName("RegExp") @GraphQLDescription("will return only import-package matching the RegExp") String regExp,
            @GraphQLName("version") @GraphQLDescription("will return only import-package matching the given version") String version,
            @GraphQLName("versionMissing") @GraphQLDescription("will return only import-package with no version range limitations") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean versionMissing
    ) {
        return OSGIPackageHeaderChecker.findImportPackages(regExp, version, versionMissing);
    }

    @GraphQLField
    @GraphQLDescription("Will return all the duplicate export packages and the bundles that export them")
    public FindExportPackage findMatchingExportPackages(
            @GraphQLName("RegExp") @GraphQLDescription("will return only export-package matching the RegExp") String regExp,
            @GraphQLName("duplicates") @GraphQLDescription("will return only export-package found multiple times for a same package name") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean duplicates
    ) {
        return OSGIPackageHeaderChecker.findExportPackages(regExp, duplicates);
    }

    @GraphQLField
    @GraphQLDescription("Will return dependencies of a bundle (modules or packages)")
    public FindDependencies findDependencies(
            @GraphQLName("RegExp") @GraphQLDescription("will return only bundle name matching the RegExp") String regExp,
            @GraphQLName("ModulesOnly") @GraphQLDescription("will return only dependencies of Jahia modules (not bundles)") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class)  boolean modulesOnly,
            @GraphQLName("StrictVersionOnly") @GraphQLDescription("will return only dependencies with a strict version specified (a version that reprobates upgrade of minor ones)") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class)  boolean strictVersionsOnly
    ) {
        return OSGIPackageHeaderChecker.findRestrictivesDependencies(regExp, modulesOnly, strictVersionsOnly);
    }
}
