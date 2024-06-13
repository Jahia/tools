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
import org.jahia.modules.tools.gql.admin.osgi.*;

@GraphQLName("AdminTools")
@GraphQLDescription("Jahia admin tools")
public class ToolsGraphQL {
    @GraphQLField
    @GraphQLDescription("Will return all the import packages matching the given parameters.")
    public FindImportPackage findMatchingImportPackages(
            @GraphQLName("RegExp") @GraphQLDescription("will return only import-package matching the RegExp") String regExp,
            @GraphQLName("version") @GraphQLDescription("will return only import-package matching the given version") String version,
            @GraphQLName("versionMissing") @GraphQLDescription("will return only import-package matching the given version") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean versionMissing
    ) {
        return OSGIPackageHeaderChecker.findImportPackages(regExp, version, versionMissing);
    }

    @GraphQLField
    @GraphQLDescription("Will return all the duplicate export packages and the bundles that export them")
    public FindExportPackage findMatchingExportPackages(
            @GraphQLName("RegExp") @GraphQLDescription("will return only export-package matching the RegExp") String regExp,
            @GraphQLName("duplicates") @GraphQLDescription("will return only export-package found multiple times for a same package name") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean duplicates,
            @GraphQLName("location") @GraphQLDescription("will return the list of files where package export has been found") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean location
    ) {
        return OSGIPackageHeaderChecker.findExportPackages(regExp, duplicates, location);
    }

    @GraphQLField
    @GraphQLDescription("Will search packages in bundles applying name filter (regexp) in the exported and/or imported ones")
    public FindPackageResult findPackages(
            @GraphQLName("filter") @GraphQLDescription("Package name should match the filter (regexp)") String filter,
            @GraphQLName("version") @GraphQLDescription("Package version should match") String version,
            @GraphQLName("duplicates") @GraphQLDescription("Only return if matched packages contains duplicates") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean duplicates,
            @GraphQLName("imports") @GraphQLDescription("Only search for package matching in bundle imports") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class) boolean imports,
            @GraphQLName("exports") @GraphQLDescription("Only search for package matching in bundle exports") @GraphQLDefaultValue(GqlUtils.SupplierTrue.class) boolean exports,
            @GraphQLName("subtree") @GraphQLDescription("Include for each package (import/export) other bundle usage") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean subtree
    ) {
        return new FindPackageResult(OSGIPackageHeaderChecker.findPackages(filter, version, duplicates, imports, exports, subtree));
    }
}
