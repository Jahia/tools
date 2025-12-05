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
import org.jahia.modules.tools.gql.admin.jcr.JcrBrowserGraphQL;
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
            @GraphQLName("RegExp") @GraphQLDescription("Regular expression for the import packages. When specified, only import packages matching this regular expression are retrieved") String regExp,
            @GraphQLName("version") @GraphQLDescription("Version for the import package. When specified, only import packages of an exact version or a version range matching this version are retrieved. Note that this parameter does not apply on the versions of the bundles, but on the versions of the import packages") String version,
            @GraphQLName("versionMissing") @GraphQLDescription("Filter the import packages on whether their version is missing (when the flag is set to 'true') or is set (when the flag is set to 'false'). If not specified, import packages with and without version are retrieved") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean versionMissing
    ) {
        return OSGIPackageHeaderChecker.findImportPackages(regExp, version, versionMissing);
    }

    @GraphQLField
    @GraphQLDescription("Will return all the duplicate export packages and the bundles that export them")
    public FindExportPackage findMatchingExportPackages(
            @GraphQLName("RegExp") @GraphQLDescription("Regular expression for the export packages. When specified, only export packages matching this regular expression are retrieved.") String regExp,
            @GraphQLName("duplicates") @GraphQLDescription("When set to 'true', only return export packages found multiple times for the same package name. By default (or when set to 'false'), all export packages are retrieved, regardless of how many times they are exported.") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean duplicates
    ) {
        return OSGIPackageHeaderChecker.findExportPackages(regExp, duplicates);
    }

    @GraphQLField
    @GraphQLDescription("List of matching bundles.")
    public List<BundleWithDependencies> bundles(
            @GraphQLName("nameRegExp") @GraphQLDescription("Only return bundles whose symbolic names match the given regular expression") String nameRegExp,
            @GraphQLName("areModules") @GraphQLDescription("Allows to filter on whether the bundles are Jahia modules or not. If the parameter is set to 'true', only bundles that are also Jahia modules are returned. If set to 'false', only bundles that are not Jahia modules are returned. By default, both Jahia modules and non Jahia modules are returned.") Boolean areModules,
            @GraphQLName("withUnsupportedDependenciesOnly") @GraphQLDescription("When set to 'true', only return bundles that have at least one dependency with an unsupported version range (to be considered supported, a version range should be open to minor upgrade based on SemVer)") @GraphQLDefaultValue(GqlUtils.SupplierFalse.class) boolean withUnsupportedDependenciesOnly
    ) {
        return OSGIPackageHeaderChecker.findBundles(nameRegExp, areModules, withUnsupportedDependenciesOnly);
    }

    @GraphQLField
    @GraphQLDescription("JCR Browser operations")
    public JcrBrowserGraphQL jcrBrowser() {
        return new JcrBrowserGraphQL();
    }

}
