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

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for the OSGI Import-Package, Export-Package checker.
 *
 * @author jkevan
 */
public class OSGIPackageHeaderChecker {

    /**
     * Perform the OSGI Import-Package checker. This method will check all bundles in the OSGI
     * framework and return a list of matching import packages.
     *
     * @param regex The regular expression to match against the import package name.
     * @param matchVersion The version to match against the import package version.
     * @param matchVersionRangeMissing If true, will only return import packages that are missing a version range.
     *
     * @return The result of the import package checker.
     */
    public static FindImportPackage findImportPackages(String regex, String matchVersion, boolean matchVersionRangeMissing) {
        FindImportPackage results = new FindImportPackage();

        Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            BundleWithImportPackages entry = new BundleWithImportPackages(bundle);
            String importPackageHeader = bundle.getHeaders().get("Import-Package");
            if (importPackageHeader != null) {
                Clause[] importedPackages = Parser.parseHeader(importPackageHeader);
                for (Clause importedPackageClause : importedPackages) {
                    if (StringUtils.isEmpty(regex) || importedPackageClause.getName().matches(regex)) {
                        String versionStr = importedPackageClause.getAttribute("version");

                        // Version missing check
                        if (matchVersionRangeMissing) {
                            if (versionStr == null) {
                                entry.addMatchingImportedPackage(importedPackageClause.toString());
                            }
                            continue;
                        }

                        // Version match check
                        if (StringUtils.isNotEmpty(matchVersion)) {
                            if (versionStr == null) {
                                entry.addMatchingImportedPackage(importedPackageClause.toString());
                            } else {
                                Version osgiVersion = Version.parseVersion(matchVersion);
                                VersionRange osgiVersionRange = new VersionRange(versionStr);
                                if (osgiVersionRange.includes(osgiVersion)) {
                                    entry.addMatchingImportedPackage(importedPackageClause.toString());
                                }
                            }
                            continue;
                        }

                        // No condition check
                        entry.addMatchingImportedPackage(importedPackageClause.toString());
                    }
                }
            }
            if (!entry.getMatchingImportedPackage().isEmpty()) {
                results.add(entry);
            }
        }
        return results;
    }

    /**
     * Perform the OSGI Export-Package checker. This method will check all bundles in the OSGI
     * framework and return a list of matching export packages.
     *
     * @param regex The regular expression to match against the export package name.
     * @param duplicates If true, will only return export packages found multiple times.
     * @param location If true, will return the modules that import the package.
     *
     * @return The result of the export package checker.
     */
    public static FindExportPackage findExportPackages(String regex, boolean duplicates, boolean location) {
        // Collect export packages
        Map<String, List<BundleWithExportPackage>> packages = new HashMap<>();
        Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            String exportPackageHeader = bundle.getHeaders().get("Export-Package");
            if (exportPackageHeader != null) {
                Clause[] exportPackages = Parser.parseHeader(exportPackageHeader);
                for (Clause exportPackage : exportPackages) {
                    if (StringUtils.isEmpty(regex) || exportPackage.getName().matches(regex)) {
                        BundleWithExportPackage resultEntry = new BundleWithExportPackage(exportPackage.toString(), bundle);
                        //TODO include the location
                        if (location) {
                            //resultEntry.setExportPackageLocations();
                        }
                        if (packages.containsKey(exportPackage.getName())) {
                            packages.get(exportPackage.getName()).add(resultEntry);
                        } else {
                            packages.put(exportPackage.getName(), new ArrayList<>(Collections.singletonList(resultEntry)));
                        }
                    }
                }
            }
        }

        // filter duplicates if needed
        FindExportPackage results = new FindExportPackage();
        for (Map.Entry<String, List<BundleWithExportPackage>> packageEntries : packages.entrySet()) {
            if (!duplicates || packageEntries.getValue().size() > 1) {
                ExportPackages result = new ExportPackages(packageEntries.getKey());
                result.setExportPackages(packageEntries.getValue());
                results.add(result);
            }
        }
        return results;
    }

    public static List<BundleResultEntry> findPackages(String filter, String version, boolean imports, boolean exports,
            boolean duplicates) {
        final List<BundleResultEntry> bundles = loadBundles();
        //Filter bundles that have only matching import or export packages depending on options set
        List<BundleResultEntry> results = bundles.stream().filter(b ->
                (imports && b.getImports().getDetailed().stream().anyMatch(p -> p.getName().matches(filter))) ||
                (exports && b.getExports().getDetailed().stream().anyMatch(p -> p.getName().matches(filter))))
                .map(e -> new BundleResultEntry(e.getBundleId(), e.getBundleName(), e.getBundleSymbolicName(), e.getBundleDisplayName(),
                        (imports)?e.getImports().getDetailed().stream().filter(p -> p.getName().matches(filter)).collect(Collectors.toList()):Collections.emptyList(),
                        (exports)?e.getExports().getDetailed().stream().filter(p -> p.getName().matches(filter)).collect(Collectors.toList()):Collections.emptyList()))
                .collect(Collectors.toList());
        //Apply a version filtering if needed
        if (StringUtils.isNotEmpty(version)) {
            results.forEach(b -> b.getExports().filterExportsForVersion(version));
            results.forEach(b -> b.getImports().filterImportsForVersion(version));
        }
        //Apply a duplicates only filtering if needed
        if (duplicates) {
            results.forEach(b -> b.getExports().keepDuplicateNamesOnly());
            results.forEach(b -> b.getImports().keepDuplicateNamesOnly());
            results = results.stream().filter(b -> b.getImports().size() > 1 || b.getExports().size() > 1).collect(Collectors.toList());
        }
        //Remove the bundles that have no more matching import or export packages
        results.removeIf(b -> b.getImports().size() == 0 && b.getExports().size() == 0);
        //Populate the imports and exports with the bundles that export or import the package
        results.forEach(b -> b.getImports().getDetailed().forEach(p -> p.addExports(bundles.stream().filter(b2 -> b2.getExports().getDetailed().stream().anyMatch(p2 -> p2.getName().equals(p.getName()))).collect(Collectors.toSet()))));
        results.forEach(b -> b.getExports().getDetailed().forEach(p -> p.addImports(bundles.stream().filter(b2 -> b2.getImports().getDetailed().stream().anyMatch(p2 -> p2.getName().equals(p.getName()))).collect(Collectors.toSet()))));
        return results;
    }

    /**
     * Load all bundles in the OSGI framework and return a list of them.
     *
     * @return The list of bundles.
     */
    public static List<BundleResultEntry> loadBundles() {
        List<BundleResultEntry> entries = new ArrayList<>();
        Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            BundleResultEntry entry = new BundleResultEntry(bundle);
            String importPackageHeader = bundle.getHeaders().get("Import-Package");
            if (importPackageHeader != null) {
                Clause[] importedPackages = Parser.parseHeader(importPackageHeader);
                for (Clause importedPackageClause : importedPackages) {
                    String name = importedPackageClause.getName();
                    String version = importedPackageClause.getAttribute("version");
                    entry.addImport(importedPackageClause.toString(), name, version);
                }
            }
            String exportPackageHeader = bundle.getHeaders().get("Export-Package");
            if (exportPackageHeader != null) {
                Clause[] exportPackages = Parser.parseHeader(exportPackageHeader);
                for (Clause exportPackageClause : exportPackages) {
                    String name = exportPackageClause.getName();
                    String version = exportPackageClause.getAttribute("version");
                    String clause = exportPackageClause.getDirective("uses");
                    List<String> uses = (clause!=null)?
                            Arrays.stream(exportPackageClause.getDirective("uses").split(",")).map(String::trim).collect(Collectors.toList()):Collections.emptyList();
                    entry.addExport(exportPackageClause.toString(), name, version, uses);
                }
            }
            entries.add(entry);
        }
        return entries;
    }
}
