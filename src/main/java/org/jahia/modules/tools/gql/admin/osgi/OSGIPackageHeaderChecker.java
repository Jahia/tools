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
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     *
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
     * Perform the OSGI Import-Package and Jahia-Depends checker. This method will check all bundles in the OSGI
     * framework and return a list of bundles that contains restrictives dependencies. A restrictive dependency is
     * either an imported package or a jahia-depends occurrence that enforce a version that will prevent dependent modules
     * to be updated correctly on minor versions.
     * The check is done only on Jahia Modules that are not uninstalled.
     *
     * @return The result of the restrictive dependencies checker.
     */
    public static FindRestrictiveDependency findRestrictivesDependencies() {
        FindRestrictiveDependency results = new FindRestrictiveDependency();

        Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            boolean isModule = BundleUtils.isJahiaModuleBundle(bundle);
            if (isModule && bundle.getState() != Bundle.UNINSTALLED) {
                BundleWithRestrictiveDependencies entry = new BundleWithRestrictiveDependencies(bundle);

                String importPackageHeader = bundle.getHeaders().get(Constants.IMPORT_PACKAGE);
                if (importPackageHeader != null) {
                    List<Dependency> dependencies = parseImportPackage(bundle.getSymbolicName(), importPackageHeader);
                    for (Dependency dependency : dependencies) {
                        if (dependency.hasVersion()) {
                            if (!dependency.isRange()) {
                                dependency.setMessage("Strict version dependency");
                            }
                            if (dependency.getVersion().isExact()) {
                                dependency.setMessage("Single version range dependency");
                            }
                            if (dependency.cannotBumpMinorVersion()) {
                                dependency.setMessage("Version range too restrictive to upgrade minor version");
                            }
                            if (dependency.isSuspicious()) {
                                entry.addRestrictiveDependency(dependency.toString());
                            }
                        }
                    }
                }

                String jahiaDependsHeader = bundle.getHeaders().get("Jahia-Depends");
                if (jahiaDependsHeader != null) {
                    List<Dependency> dependencies = parseJahiaDepends(bundle.getSymbolicName(), jahiaDependsHeader);
                    for (Dependency dependency : dependencies) {
                        if (dependency.hasVersion()) {
                            if (dependency.getVersion().isExact()) {
                                dependency.setMessage("Single version range dependency");
                            }
                            if (dependency.cannotBumpMinorVersion()) {
                                dependency.setMessage("Version range too restrictive to upgrade minor version");
                            }
                            if (dependency.isSuspicious()) {
                                entry.addRestrictiveDependency(dependency.toString());
                            }
                        }
                    }
                }

                if (!entry.getRestrictiveDependencies().isEmpty()) {
                    results.add(entry);
                }
            }
        }
        return results;
    }

    private static List<Dependency> parseImportPackage (String bundle, String importPackageHeader) {
        List<Dependency> result = new ArrayList<>();
        Clause[] importedPackageClauses = Parser.parseHeader(importPackageHeader);
        for (Clause importedPackageClause : importedPackageClauses) {
            try {
                result.add(Dependency.parse(bundle, importedPackageClause));
            } catch (IllegalArgumentException e) {
                //LOGGER.warn("Unable to parse Jahia-Depends Header: {}", importedPackageClause.toString(), e);
                //TODO add an error field in dependency to reflect parsing error
            }
        }
        return result;
    }

    private static List<Dependency> parseJahiaDepends (String bundle, String jahiaDependsHeader) {
        List<Dependency> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^,\\[\\]()]+|\\[[^\\]]*\\]|\\([^)]*\\)");
        Matcher matcher = pattern.matcher(jahiaDependsHeader);
        while (matcher.find()) {
            try {
                result.add(Dependency.parse(bundle, matcher.group()));
            } catch (IllegalArgumentException e) {
                //LOGGER.warn("Unable to parse Jahia-Depends Header: {}", importedPackageClause.toString(), e);
                //TODO add an error field in dependency to reflect parsing error
            }
        }
        return result;
    }

    private static class Dependency {
        private final String type;
        private final String name;
        private final boolean optional;
        private final VersionRange version;
        private String message;

        public Dependency(String type, String name, VersionRange version, boolean optional) {
            this.type = type;
            this.name = name;
            this.version = version;
            this.optional = optional;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public VersionRange getVersion() {
            return version;
        }

        public boolean isOptional() {
            return optional;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean hasVersion() {
            return version != null;
        }

        public boolean isRange() {
            return version.getRight() != null;
        }

        public boolean cannotBumpMinorVersion() {
            if (version == null || version.isExact()) {
                return true;
            }
            Version bumped = new Version(version.getLeft().getMajor(), version.getLeft().getMinor()+1, 0);
            return !version.includes(bumped);
        }

        public boolean isSuspicious() {
            return message != null && !message.isEmpty();
        }

        public static Dependency parse(String bundle, String dependency) {
            String cleanedDep = dependency.replace(";optional", "");
            cleanedDep = dependency.replace("optional", "");
            boolean optional = !cleanedDep.equals(dependency);

            String[] parts = dependency.split("=");
            if (parts.length == 2) {
                return new Dependency("Jahia-Depends", parts[0], VersionRange.valueOf(parts[1]), optional);
            } else {
                return new Dependency("Jahia-Depends", dependency, null, optional);
            }
        }

        public static Dependency parse(String bundle, Clause importedPackageClause) {
            String version = importedPackageClause.getAttribute(Constants.VERSION_ATTRIBUTE);
            boolean optional = (importedPackageClause.getAttribute(Constants.RESOLUTION_OPTIONAL)!=null);
            if (version != null) {
                return new Dependency("Import-Package", importedPackageClause.getName(), VersionRange.valueOf(version), optional);
            } else {
                return new Dependency("Import-Package", importedPackageClause.getName(), null, optional);
            }
        }

        @Override public String toString() {
            return "type='" + type + '\'' + ", name='" + name + '\'' + ", version=" + version + ", explanation=" + message;
        }
    }

}
