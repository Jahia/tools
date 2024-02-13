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
package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("ExportPackageInspectorResult")
@GraphQLDescription("Result of the export package inspector operation.")
public class ExportPackages {
    String packageName;

    List<BundleWithExportPackage> exportPackages;

    public ExportPackages(String packageName) {
        this.packageName = packageName;
    }

    @GraphQLField
    @GraphQLName("packageName")
    @GraphQLDescription("the package name.")
    public String getPackageName() {
        return packageName;
    }

    @GraphQLField
    @GraphQLName("matchingExportPackagesDetailed")
    @GraphQLDescription("The bundles that export the package.")
    public List<BundleWithExportPackage> getMatchingPackageDetailed() {
        return exportPackages;
    }

    @GraphQLField
    @GraphQLName("matchingExportPackages")
    @GraphQLDescription("Flat list of exported packages")
    public List<String> getMatchingPackages() {
        return exportPackages.stream().map(
                (BundleWithExportPackage entry) -> entry.getPackage() + " EXPOSED BY " + entry.getBundleDisplayName()
        ).collect(Collectors.toList());
    }

    public void setExportPackages(List<BundleWithExportPackage> entries) {
        this.exportPackages = entries;
    }
}
