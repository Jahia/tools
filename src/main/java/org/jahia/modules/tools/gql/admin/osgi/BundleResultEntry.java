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
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.*;
import java.util.stream.Collectors;

public class BundleResultEntry {
    private final long bundleId;
    private final String bundleName;
    private final String bundleSymbolicName;
    private final String bundleDisplayName;
    private final BundleImports imports;
    private final BundleExports exports;


    public BundleResultEntry(Bundle bundle) {
        this.bundleName = bundle.getHeaders().get("Bundle-Name");
        this.bundleSymbolicName = bundle.getSymbolicName();
        this.bundleDisplayName = bundleName != null ?
                bundleName + " (" + bundleSymbolicName + ")" :
                bundleSymbolicName;
        this.bundleId = bundle.getBundleId();
        this.imports = new BundleImports(new ArrayList<>());
        this.exports = new BundleExports(new ArrayList<>());
    }

    public BundleResultEntry(long bundleId, String bundleName, String bundleSymbolicName, String bundleDisplayName,
            List<BundleImport> importPackages, List<BundleExport> exportPackages) {
        this.bundleId = bundleId;
        this.bundleName = bundleName;
        this.bundleSymbolicName = bundleSymbolicName;
        this.bundleDisplayName = bundleDisplayName;
        this.imports = new BundleImports(importPackages);
        this.exports = new BundleExports(exportPackages);
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
    @GraphQLName("imports")
    @GraphQLDescription("Import packages of the bundle.")
    public BundleImports getImports() {
        return imports;
    }

    @GraphQLField
    @GraphQLName("exports")
    @GraphQLDescription("Export packages of the bundle.")
    public BundleExports getExports() {
        return exports;
    }

    public void addImport(String clause, String name, String version) {
        imports.add(clause, name, version);
    }

    public void addExport(String clause, String name, String version, List<String> uses) {
        exports.add(clause, name, version, uses);
    }

    public static class BundleImports {
        private final List<BundleImport> imports;

        public BundleImports(List<BundleImport> imports) {
            this.imports = imports;
        }

        public void add(String clause, String name, String version) {
            imports.add(new BundleImport(clause, name, version));
        }

        public void filterImportsForVersion(String version) {
            List<BundleImport> evict = imports.stream()
                    .filter(i -> i.getVersion() != null && !(new VersionRange(i.getVersion()).includes(Version.parseVersion(version))))
                    .collect(Collectors.toList());
            imports.removeIf(evict::contains);
        }

        public void keepDuplicateNamesOnly() {
            Set<String> elements = new HashSet<>();
            List<String> duplicates =
                    imports.stream().map(BundleImport::getName).filter(n -> !elements.add(n)).collect(Collectors.toList());
            imports.removeIf(e -> !duplicates.contains(e.getName()));
        }

        public int size() {
            return imports.size();
        }

        @GraphQLField
        @GraphQLName("compact")
        @GraphQLDescription("Import packages of the bundle in a compact form.")
        public String[] getCompact() {
            return imports.stream().map(i -> i.getName().concat((StringUtils.isNotEmpty(i.getVersion()))?
                    ",".concat(i.getVersion()):"")).toArray(String[]::new);
        }

        @GraphQLField
        @GraphQLName("detailed")
        @GraphQLDescription("Import packages of the bundle in a detailed form.")
        public List<BundleImport> getDetailed() {
            return imports;
        }
    }

    public static class BundleImport {

        private final String clause;
        private final String name;
        private final String version;
        private final List<BundleResultEntry> exports;

        public BundleImport(String clause, String name, String version) {
            this.clause = clause;
            this.name = name;
            this.version = version;
            this.exports = new ArrayList<>();
        }

        public void addExports(Set<BundleResultEntry> bundles) {
            this.exports.addAll(bundles);
        }

        @GraphQLField
        @GraphQLName("clause")
        @GraphQLDescription("Display full import package clause.")
        public String getClause() {
            return clause;
        }

        @GraphQLField
        @GraphQLName("name")
        @GraphQLDescription("Display import package name.")
        public String getName() {
            return name;
        }

        @GraphQLField
        @GraphQLName("version")
        @GraphQLDescription("Display import package version.")
        public String getVersion() {
            return version;
        }

        @GraphQLField
        @GraphQLName("exports")
        @GraphQLDescription("Bundles that exports that package")
        public List<BundleResultEntry> getExports() {
            return exports;
        }
    }

    public static class BundleExports {
        private final List<BundleExport> exports;

        public BundleExports(List<BundleExport> exports) {
            this.exports = exports;
        }

        public void add(String clause, String name, String version, List<String> uses) {
            exports.add(new BundleExport(clause, name, version, uses));
        }

        public void filterExportsForVersion(String version) {
            List<BundleExport> evict = exports.stream()
                            .filter(e -> e.getVersion() != null && !e.getVersion().startsWith(version))
                            .collect(Collectors.toList());
            exports.removeIf(evict::contains);
        }

        public void keepDuplicateNamesOnly() {
            Set<String> elements = new HashSet<>();
            List<String> duplicates = exports.stream().map(BundleExport::getName).filter(n -> !elements.add(n)).collect(Collectors.toList());
            exports.removeIf(e -> !duplicates.contains(e.getName()));
        }

        public int size() {
            return exports.size();
        }

        @GraphQLField
        @GraphQLName("compact")
        @GraphQLDescription("Export packages of the bundle in a compact form.")
        public String[] getCompact() {
            return exports.stream().map(e -> e.getName().concat((StringUtils.isNotEmpty(e.getVersion()))?
                    ",".concat(e.getVersion()):"")).toArray(String[]::new);
        }

        @GraphQLField
        @GraphQLName("detailed")
        @GraphQLDescription("Export packages of the bundle in a detailed form.")
        public List<BundleExport> getDetailed() {
            return exports;
        }
    }

    public static class BundleExport {

        private final String clause;
        private final String name;
        private final String version;
        private final List<String> uses;
        private final List<BundleResultEntry> imports;

        public BundleExport(String clause, String name, String version, List<String> uses) {
            this.clause = clause;
            this.name = name;
            this.version = version;
            this.uses = uses;
            this.imports = new ArrayList<>();
        }

        public void addImports(Set<BundleResultEntry> bundles) {
            this.imports.addAll(bundles);
        }

        @GraphQLField
        @GraphQLName("clause")
        @GraphQLDescription("Display full export package clause.")
        public String getClause() {
            return clause;
        }

        @GraphQLField
        @GraphQLName("name")
        @GraphQLDescription("Display export package name.")
        public String getName() {
            return name;
        }

        @GraphQLField
        @GraphQLName("version")
        @GraphQLDescription("Display export package version.")
        public String getVersion() {
            return version;
        }

        @GraphQLField
        @GraphQLName("uses")
        @GraphQLDescription("Display export package uses.")
        public List<String> getUses() {
            return uses;
        }

        @GraphQLField
        @GraphQLName("imports")
        @GraphQLDescription("List of bundles that import that package")
        public List<BundleResultEntry> imports() {
            return imports;
        }
    }
}
