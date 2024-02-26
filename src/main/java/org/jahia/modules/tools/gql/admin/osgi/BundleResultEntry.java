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
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;

public class BundleResultEntry {
    private final long bundleId;
    private final String bundleName;
    private final String bundleSymbolicName;
    private final String bundleDisplayName;
    private final List<BundleResultImport> bundleImports = new ArrayList<>();
    private final List<BundleResultExport> bundleExports = new ArrayList<>();


    public BundleResultEntry(Bundle bundle) {
        this.bundleName = bundle.getHeaders().get("Bundle-Name");
        this.bundleSymbolicName = bundle.getSymbolicName();
        this.bundleDisplayName = bundleName != null ?
                bundleName + " (" + bundleSymbolicName + ")" :
                bundleSymbolicName;
        this.bundleId = bundle.getBundleId();
    }

    public BundleResultEntry(long bundleId, String bundleName, String bundleSymbolicName, String bundleDisplayName,
            List<BundleResultImport> bundleImports, List<BundleResultExport> bundleExports) {
        this.bundleId = bundleId;
        this.bundleName = bundleName;
        this.bundleSymbolicName = bundleSymbolicName;
        this.bundleDisplayName = bundleDisplayName;
        this.bundleImports.addAll(bundleImports);
        this.bundleExports.addAll(bundleExports);
    }

    @GraphQLField
    @GraphQLName("bundleName")
    @GraphQLDescription("Name of the bundle.")
    public String getBundleName() {
        return bundleName;
    }

    @GraphQLField
    @GraphQLName("bundleSymbolicName")
    @GraphQLDescription("Symbolic name of the bundle.")
    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    @GraphQLField
    @GraphQLName("bundleDisplayName")
    @GraphQLDescription("Display name of the bundle.")
    public String getBundleDisplayName() {
        return bundleDisplayName;
    }

    @GraphQLField
    @GraphQLName("bundleId")
    @GraphQLDescription("ID of the bundle.")
    public long getBundleId() {
        return bundleId;
    }

    @GraphQLField
    @GraphQLName("bundleImports")
    @GraphQLDescription("Imports of the bundle.")
    public List<BundleResultImport> getBundleImports() {
        return bundleImports;
    }

    @GraphQLField
    @GraphQLName("bundleExports")
    @GraphQLDescription("Exports of the bundle.")
    public List<BundleResultExport> getBundleExports() {
        return bundleExports;
    }

    public void addImport(String clause, String name, String version) {
        bundleImports.add(new BundleResultImport(clause, name, version));
    }

    public void addExport(String clause, String name, String version, List<String> uses) {
        bundleExports.add(new BundleResultExport(clause, name, version, uses));
    }

    public static class BundleResultImport {

        private final String clause;
        private final String name;
        private final String version;

        public BundleResultImport(String clause, String name, String version) {
            this.clause = clause;
            this.name = name;
            this.version = version;
        }

        @GraphQLField
        @GraphQLName("importPackageClause")
        @GraphQLDescription("Display full import clause.")
        public String getClause() {
            return clause;
        }

        @GraphQLField
        @GraphQLName("importPackageName")
        @GraphQLDescription("Display import package name.")
        public String getName() {
            return name;
        }

        @GraphQLField
        @GraphQLName("importPackageVersion")
        @GraphQLDescription("Display import package version.")
        public String getVersion() {
            return version;
        }
    }

    public static class BundleResultExport {

        private final String clause;
        private final String name;
        private final String version;
        private final List<String> uses;

        public BundleResultExport(String clause, String name, String version, List<String> uses) {
            this.clause = clause;
            this.name = name;
            this.version = version;
            this.uses = uses;
        }

        @GraphQLField
        @GraphQLName("exportPackageClause")
        @GraphQLDescription("Display full export clause.")
        public String getClause() {
            return clause;
        }

        @GraphQLField
        @GraphQLName("exportPackageName")
        @GraphQLDescription("Display export package name.")
        public String getName() {
            return name;
        }

        @GraphQLField
        @GraphQLName("exportPackageVersion")
        @GraphQLDescription("Display export package version.")
        public String getVersion() {
            return version;
        }

        @GraphQLField
        @GraphQLName("exportPackageUses")
        @GraphQLDescription("Display export package uses.")
        public List<String> getUses() {
            return uses;
            }
    }
}
