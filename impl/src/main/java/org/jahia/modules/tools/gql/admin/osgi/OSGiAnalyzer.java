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
import org.jahia.modules.tools.config.DeprecatedPackageWiresConfig;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for the OSGI Import-Package, Export-Package, Jahia-Depends checker, wire analyzer, etc.
 *
 * @author jkevan
 */
public class OSGiAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(OSGiAnalyzer.class);

    /**
     * Manifest header identifying the Jahia group a module belongs to.
     */
    private static final String JAHIA_GROUP_ID_HEADER = "Jahia-GroupId";

    /**
     * Group ID used by internal Jahia modules — excluded from wire results by default.
     */
    private static final String JAHIA_INTERNAL_GROUP_ID = "org.jahia.modules";


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

    /**
     * Returns all active package wires where the package name matches at least one of the
     * given regex patterns for each corresponding provider bundle.
     * <p>
     * Each entry in the map associates a provider bundle symbolic name with the set of
     * package regex patterns to match against. An empty string key means "any provider".
     * The set of active Jahia module wirings is collected once and shared across all entries,
     * so this method is preferred over calling {@link #getPackageWires(String, Collection, boolean)}
     * in a loop. Results are sorted by {@link PackageWire#compareTo}.
     * <p>
     *
     * @param patternsByProviderSymbolicName map of provider bundle symbolic name → package regex patterns;
     *                                       use an empty string key to match wires from any provider
     * @param includeInternalModules         if {@code true}, modules whose {@code Jahia-GroupId} manifest
     *                                       header is {@code org.jahia.modules} are included in results;
     *                                       pass {@code false} to exclude them (recommended default)
     * @return sorted list of matching package wires across all entries
     */
    public static List<PackageWire> getPackageWires(Map<String, Collection<String>> patternsByProviderSymbolicName, boolean includeInternalModules) {
        List<BundleWiring> wirings = collectActiveModuleWirings(includeInternalModules);
        return patternsByProviderSymbolicName.entrySet().stream()
                .map(entry -> toWireQuery(entry.getKey(), entry.getValue()))
                .flatMap((WireQuery query) -> streamMatchingWires(wirings, query))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns all active package wires where the package name matches at least one of the
     * given regex patterns, optionally restricted to a specific provider bundle.
     * <p>
     * Results are sorted by {@link PackageWire#compareTo}.
     *
     * @param providerBundleSymbolicName symbolic name of the provider bundle to filter by,
     *                                   or {@code null} / empty to match wires from any provider
     * @param packageRegexes             regex patterns to match against package names
     * @param includeInternalModules     if {@code true}, modules whose {@code Jahia-GroupId} manifest
     *                                   header is {@code org.jahia.modules} are included in results;
     *                                   pass {@code false} to exclude them (recommended default)
     * @return sorted list of matching package wires
     * @throws IllegalArgumentException if the provider bundle cannot be resolved or a regex is invalid
     */
    public static List<PackageWire> getPackageWires(String providerBundleSymbolicName, Collection<String> packageRegexes, boolean includeInternalModules) {
        return streamMatchingWires(collectActiveModuleWirings(includeInternalModules), toWireQuery(providerBundleSymbolicName, packageRegexes))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns all active package wires matching the deprecated package patterns
     * configured via the {@link DeprecatedPackageWiresConfig} OSGi service.
     * <p>
     * Results are sorted by {@link PackageWire#compareTo}.
     * <p>
     * Throws on configuration error — misconfigured deprecation patterns are a server/admin issue
     * and should fail fast rather than silently skip entries.
     *
     * @param includeInternalModules if {@code true}, modules whose {@code Jahia-GroupId} manifest
     *                               header is {@code org.jahia.modules} are included in results;
     *                               pass {@code false} to exclude them (recommended default)
     * @return sorted list of wires to deprecated packages
     * @throws IllegalStateException    if {@link DeprecatedPackageWiresConfig} is unavailable or has no patterns configured
     * @throws IllegalArgumentException if a configured pattern or provider is invalid
     */
    public static List<PackageWire> getDeprecatedPackageWires(boolean includeInternalModules) {
        DeprecatedPackageWiresConfig deprecatedPackageWiresConfig = BundleUtils.getOsgiService(DeprecatedPackageWiresConfig.class, null);
        if (deprecatedPackageWiresConfig == null) {
            throw new IllegalStateException("DeprecationConfig service not available");
        }
        Map<String, Collection<String>> patternsByProvider = deprecatedPackageWiresConfig.getDeprecatedPatternsByProvider();
        if (patternsByProvider.isEmpty()) {
            throw new IllegalStateException("No deprecated patterns configured");
        }

        return getPackageWires(patternsByProvider, includeInternalModules);
    }

    /**
     * Holds the resolved inputs for a single wire-matching operation:
     * the optional provider bundle filter and a single combined pattern
     * built from the OR-merge of all individual regex strings.
     */
    private static final class WireQuery {
        /**
         * The provider bundle to filter by, or {@code null} to match any provider.
         */
        final Bundle providerBundle;
        /**
         * Single OR-alternation pattern compiled from all supplied regexes,
         * e.g. {@code (?:p1)|(?:p2)|(?:p3)}.
         * One {@link java.util.regex.Matcher#matches()} call per wire replaces iterating N patterns.
         */
        final Pattern combinedPattern;

        WireQuery(Bundle providerBundle, Pattern combinedPattern) {
            this.providerBundle = providerBundle;
            this.combinedPattern = combinedPattern;
        }
    }

    /**
     * Collects the {@link BundleWiring} of every active (ACTIVE or STARTING) Jahia module bundle.
     * <p>
     * This is the most expensive step in wire analysis: it calls {@code getBundles()},
     * checks {@code isJahiaModuleBundle()} and bundle state for each candidate, then calls
     * {@code adapt(BundleWiring.class)}. The result is computed once per public method call
     * and reused across all queries.
     *
     * @param includeInternalModules if {@code false}, bundles whose {@code Jahia-GroupId} header
     *                               equals {@code org.jahia.modules} are excluded
     * @return list of non-null {@link BundleWiring} instances for active Jahia module bundles
     */
    private static List<BundleWiring> collectActiveModuleWirings(boolean includeInternalModules) {
        return Arrays.stream(FrameworkService.getBundleContext().getBundles())
                .filter(BundleUtils::isJahiaModuleBundle)
                .filter(b -> b.getState() == Bundle.ACTIVE || b.getState() == Bundle.STARTING)
                .filter(b -> includeInternalModules || !JAHIA_INTERNAL_GROUP_ID.equals(b.getHeaders().get(JAHIA_GROUP_ID_HEADER)))
                .map(b -> b.adapt(BundleWiring.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Resolves a provider symbolic name to a {@link Bundle} and merges the raw regex strings
     * into a single combined OR {@link Pattern} (e.g. {@code (?:p1)|(?:p2)|(?:p3)}),
     * returning a {@link WireQuery} ready for {@link #streamMatchingWires(List, WireQuery)}.
     *
     * @param providerBundleSymbolicName symbolic name of the provider bundle, or empty/null for any provider
     * @param packageRegexes             raw regex strings to match against package names
     * @return resolved {@link WireQuery}
     * @throws IllegalArgumentException if the bundle is not found or a regex is syntactically invalid
     */
    private static WireQuery toWireQuery(String providerBundleSymbolicName, Collection<String> packageRegexes) {
        Bundle providerBundle = null;
        if (StringUtils.isNotEmpty(providerBundleSymbolicName)) {
            providerBundle = BundleUtils.getBundleBySymbolicName(providerBundleSymbolicName, null);
            if (providerBundle == null) {
                throw new IllegalArgumentException("Bundle with symbolic name '" + providerBundleSymbolicName + "' not found");
            }
        }
        // Build a single OR-alternation pattern: (?:regex1)|(?:regex2)|...
        // One Matcher.matches() call per wire replaces iterating N patterns.
        String combined = packageRegexes.stream()
                .map(r -> "(?:" + r + ")")
                .collect(Collectors.joining("|"));
        try {
            return new WireQuery(providerBundle, Pattern.compile(combined));
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regular expression: " + e.getMessage(), e);
        }
    }

    /**
     * Streams all package wires from the pre-collected module wirings whose package name
     * matches the combined pattern in the given {@link WireQuery}, optionally filtered to its
     * provider bundle.
     *
     * @param wirings pre-collected list of active Jahia module {@link BundleWiring} instances
     * @param query   resolved {@link WireQuery} containing the provider filter and combined pattern
     * @return stream of matching {@link PackageWire} instances
     */
    private static Stream<PackageWire> streamMatchingWires(List<BundleWiring> wirings, WireQuery query) {
        return wirings.stream()
                // getRequiredResourceWires returns ALL current wires including dynamically-established ones.
                // getRequiredWires (the alternative) only returns wires from bundle resolution time
                // and misses DynamicImport-Package wires created after the bundle became active.
                // The entries are always BundleWire instances in the package namespace.
                .flatMap(w -> w.getRequiredResourceWires(PackageNamespace.PACKAGE_NAMESPACE).stream())
                .filter(Objects::nonNull) // defensive: OSGi spec allows null entries in the list
                .map(w -> (BundleWire) w)
                .map(bw -> matchWire(bw, query))
                .filter(Objects::nonNull);
    }

    /**
     * Attempts to match a single {@link BundleWire} against the provider and combined pattern
     * in the given {@link WireQuery}.
     *
     * @param bundleWire the wire to evaluate
     * @param query      the resolved query containing the provider filter and combined pattern
     * @return a {@link PackageWire} if the wire matches, or {@code null} otherwise
     */
    private static PackageWire matchWire(BundleWire bundleWire, WireQuery query) {
        Bundle wireProvider = bundleWire.getProvider().getBundle();
        if (query.providerBundle != null && !Objects.equals(query.providerBundle, wireProvider)) {
            return null;
        }
        String packageName = (String) bundleWire.getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
        if (query.combinedPattern.matcher(packageName).matches()) {
            return new PackageWire(wireProvider, packageName, bundleWire.getRequirer().getBundle());
        }
        return null;
    }

}
