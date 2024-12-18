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

import graphql.annotations.annotationTypes.GraphQLDefaultValue;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.util.GqlUtils;
import org.jahia.modules.tools.gql.admin.osgi.BundleWithDependencies;
import org.jahia.modules.tools.gql.admin.osgi.FindExportPackage;
import org.jahia.modules.tools.gql.admin.osgi.FindImportPackage;
import org.jahia.modules.tools.gql.admin.osgi.OSGIPackageHeaderChecker;

import java.util.List;


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
    @GraphQLDescription("List of matching bundles.")
    public List<BundleWithDependencies> bundles(
            @GraphQLName("nameRegExp") @GraphQLDescription("Only return bundles whose symbolic names match the given regular expression") String nameRegExp,
            @GraphQLName("areModules") @GraphQLDescription("Allows to filter on whether the bundles are Jahia modules or not. If the parameter is set to 'true', only bundles that are also Jahia modules are returned. If set to 'false', only bundles that are not Jahia modules are returned. By default, both Jahia modules and non Jahia modules are returned.") Boolean areModules,
            @GraphQLName("withUnsupportedDependenciesOnly") @GraphQLDescription("Only return bundles that have 1 or more dependencies configured with an unsupported version range") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean withUnsupportedDependenciesOnly
    ) {
        return OSGIPackageHeaderChecker.findBundles(nameRegExp, areModules, withUnsupportedDependenciesOnly);
    }

}
