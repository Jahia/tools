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
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Utility class for the OSGI Import-Package, Export-Package, Jahia-Depends checker
 *
 * @author jkevan
 */
public class OSGIPackageHeaderChecker {

    /**
     * Perform the OSGI Import-Package checker. This method will check all bundles in the OSGI
     * framework and return a list of matching import packages.
     *
     * @param regex                    The regular expression to match against the import package name.
     * @param matchVersion             The version to match against the import package version.
     * @param matchVersionRangeMissing If true, will only return import packages that are missing a version range.
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
     * @param regex      The regular expression to match against the export package name.
     * @param duplicates If true, will only return export packages found multiple times.
     * @return The result of the export package checker.
     */
    public static FindExportPackage findExportPackages(String regex, boolean duplicates) {
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


    /**
     * Find all bundles in the OSGI framework that match the given parameters.
     *
     * @param nameRegExp                      Only return bundles whose names match the given regular expression.
     * @param areModules                      If <code>true</code>, will only return bundles that are also Jahia modules. If <code>false</code>, will only return bundles that are not Jahia modules. If <code>null</code>, will return all bundles.
     * @param withUnsupportedDependenciesOnly If <code>true</code>, will only return bundles that have 1 or more dependencies configured with an unsupported version range.
     * @return The list of bundles.
     */
    public static List<BundleWithDependencies> findBundles(String nameRegExp, Boolean areModules, boolean withUnsupportedDependenciesOnly) {

        // validate and compile the regular expression
        final Pattern pattern;
        if (StringUtils.isEmpty(nameRegExp)) {
            pattern = null;
        } else {
            try {
                pattern = Pattern.compile(nameRegExp);
            } catch (PatternSyntaxException e) {
                throw new GqlJcrWrongInputException("Invalid regular expression: " + nameRegExp, e);
            }
        }
        Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
        return Arrays.stream(bundles)
                .filter(bundle -> pattern == null || (bundle.getSymbolicName() != null && pattern.matcher(bundle.getSymbolicName()).matches()))
                .filter(bundle -> areModules == null || (areModules == BundleUtils.isJahiaModuleBundle(bundle)))
                .map(BundleWithDependencies::new)
                .filter(entry -> !withUnsupportedDependenciesOnly || entry.hasUnsupportedDependencies())
                .collect(Collectors.toList());
    }
}
