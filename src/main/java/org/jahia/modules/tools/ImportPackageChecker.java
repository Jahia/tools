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
package org.jahia.modules.tools;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for the OSGI Import-Package checker page.
 *
 * @author Sergiy Shyrkov
 */
public class ImportPackageChecker {

    public class ImportPackageCheckerResult {

        int totalMatchCount = 0;
        List<ImportPackageCheckerResultEntry> entries = new ArrayList<>();

        public void add(ImportPackageCheckerResultEntry entry) {
            entries.add(entry);
            totalMatchCount += entry.getMatchingImportedPackage().size();
        }

        public int getTotalMatchCount() {
            return totalMatchCount;
        }

        public List<ImportPackageCheckerResultEntry> getEntries() {
            return entries;
        }
    }

    public class ImportPackageCheckerResultEntry {
        private String bundleName;
        private long bundleId;
        private Set<String> matchingImportedPackage = new HashSet<>();

        public ImportPackageCheckerResultEntry(Bundle bundle) {
            this.bundleName = bundle.getHeaders().get("Bundle-Name") != null ?
                    bundle.getHeaders().get("Bundle-Name") + " (" + bundle.getSymbolicName() + ")" :
                    bundle.getSymbolicName();
            this.bundleId = bundle.getBundleId();
        }

        public String getBundleName() {
            return bundleName;
        }

        public long getBundleId() {
            return bundleId;
        }

        public Set<String> getMatchingImportedPackage() {
            return matchingImportedPackage;
        }

        public void addMatchingImportedPackage(String importedPackage) {
            matchingImportedPackage.add(importedPackage);
        }
    }

    public ImportPackageCheckerResult performChecker(String regex, String matchVersion, boolean matchVersionRangeMissing) {
        ImportPackageCheckerResult results = new ImportPackageCheckerResult();
        if (StringUtils.isEmpty(regex)) {
            return results;
        }

        Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            ImportPackageCheckerResultEntry entry = new ImportPackageCheckerResultEntry(bundle);
            String importPackageHeader = bundle.getHeaders().get("Import-Package");
            if (importPackageHeader != null) {
                Clause[] importedPackages = Parser.parseHeader(importPackageHeader);
                for (Clause importedPackageClause : importedPackages) {
                    if (importedPackageClause.getName().matches(regex)) {
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
}
