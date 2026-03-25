package org.jahia.modules.tools.config;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * OSGi {@link ManagedServiceFactory} that aggregates deprecated-package patterns from one or more
 * configuration instances, each identified by a factory PID of the form
 * {@code org.jahia.modules.tools.deprecatedpackagewires-{name}}.
 * <p>
 * Each configuration instance maps provider bundle symbolic names to lists of Java regex patterns.
 * When multiple instances are active (e.g. one shipped with Jahia, one supplied by a customer),
 * their entries are merged: patterns for the same provider are unioned across PIDs, with duplicates
 * removed (insertion order preserved via {@link LinkedHashSet}).
 * <p>
 * <strong>Configuration format</strong> (YAML, e.g.
 * {@code org.jahia.modules.tools.deprecatedpackagewires-libs.yml}):
 * <pre>
 * providers:
 *   org.apache.felix.framework:
 *     - "org\\.springframework(\\..*)?"
 *     - "org\\.drools(\\..*)?"
 *   org.eclipse.gemini.blueprint.core:
 *     - "org\\.eclipse\\.gemini(\\..*)?"
 * </pre>
 * The OSGi ConfigAdmin YAML parser flattens this into properties of the form:
 * <pre>
 * providers.org.apache.felix.framework[0] = org\\.springframework(\\..*)?
 * providers.org.apache.felix.framework[1] = org\\.drools(\\..*)?
 * providers.org.eclipse.gemini.blueprint.core[0] = org\\.eclipse\\.gemini(\\..*)?
 * </pre>
 *
 */
@Component(
        service = {DeprecatedPackageWiresConfig.class, ManagedServiceFactory.class},
        immediate = true,
        property = "service.pid=org.jahia.modules.tools.deprecatedpackagewires"
)
public class DeprecatedPackageWiresConfig implements ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeprecatedPackageWiresConfig.class);

    private static final String PROVIDERS_PROPERTY_PREFIX = "providers.";

    /**
     * pid -> (providerSymbolicName -> patterns)
     * <p>
     * Accesses to this map must be done while holding the {@link #mergedProviders} lock
     * to keep it consistent with the merged view.
     */
    private final Map<String, Map<String, List<String>>> configs = new LinkedHashMap<>();

    /**
     * Merged view across all pids: providerSymbolicName -> patterns
     */
    private final Map<String, Set<String>> mergedProviders = new LinkedHashMap<>();

    @Override
    public String getName() {
        return "Jahia Tools Deprecated Package Wires Config Factory";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        logger.info("Updating deprecation configuration for PID: {}", pid);
        Map<String, List<String>> extracted = extractProvidersFromProperties(properties);
        synchronized (mergedProviders) {
            configs.put(pid, extracted);
            rebuildMergedProviders();
        }
    }

    @Override
    public void deleted(String pid) {
        logger.info("Deleting deprecation configuration for PID: {}", pid);
        synchronized (mergedProviders) {
            configs.remove(pid);
            rebuildMergedProviders();
        }
    }

    /**
     * Recomputes {@link #mergedProviders} from scratch by iterating all active configuration instances.
     * Patterns for the same provider contributed by different PIDs are unioned; duplicates (whether
     * within a single PID or across PIDs) are removed because the per-provider collection is a
     * {@link LinkedHashSet} (insertion order preserved).
     * <p>
     * Must be called while holding the {@link #mergedProviders} lock.
     */
    private void rebuildMergedProviders() {
        mergedProviders.clear();

        for (Map.Entry<String, Map<String, List<String>>> configEntry : configs.entrySet()) {
            for (Map.Entry<String, List<String>> providerEntry : configEntry.getValue().entrySet()) {
                String providerSymbolicName = providerEntry.getKey();
                mergedProviders
                        .computeIfAbsent(providerSymbolicName, k -> new LinkedHashSet<>())
                        .addAll(providerEntry.getValue());
            }
        }

        logger.info("Merged deprecation providers updated. Total providers: {}, total patterns: {}",
                mergedProviders.size(),
                mergedProviders.values().stream().mapToInt(Set::size).sum());
    }

    /**
     * Parses the flat OSGi properties dictionary produced by the YAML ConfigAdmin parser
     * into a map of provider symbolic name → pattern list.
     * <p>
     * Expected key format: {@code providers.<providerSymbolicName>[<index>]}.
     * Keys that do not start with {@code providers.} are silently skipped (they are standard
     * OSGi framework properties such as {@code service.pid}).
     * Keys that start with {@code providers.} but lack a {@code [} index suffix, or whose
     * provider name is empty, are logged as warnings and skipped — they indicate a malformed
     * configuration entry.
     * The index value inside the brackets is not validated — ordering follows the natural
     * enumeration order of the {@link Dictionary}.
     * Blank pattern values are silently skipped.
     *
     * @param properties the OSGi configuration properties dictionary
     * @return ordered map of provider symbolic name to its ordered list of regex patterns;
     * never {@code null}, may be empty
     */
    private Map<String, List<String>> extractProvidersFromProperties(Dictionary<String, ?> properties) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            if (!key.startsWith(PROVIDERS_PROPERTY_PREFIX)) {
                // Standard OSGi framework properties (e.g. service.pid) — ignored
                continue;
            }

            // key format: "providers.<providerSymbolicName>[<index>]"
            // strip the "providers." prefix, then strip the trailing "[<index>]"
            String remainder = key.substring(PROVIDERS_PROPERTY_PREFIX.length());
            int bracketIndex = remainder.lastIndexOf('[');
            if (bracketIndex < 0) {
                logger.warn("Ignoring malformed deprecation config key '{}': expected format is 'providers.<bundleSymbolicName>[<index>]'", key);
                continue;
            }

            String providerSymbolicName = remainder.substring(0, bracketIndex);
            if (providerSymbolicName.isEmpty()) {
                logger.warn("Ignoring deprecation config key '{}': provider symbolic name must not be empty", key);
                continue;
            }

            Object value = properties.get(key);
            if (value == null) {
                continue;
            }
            String pattern = value.toString().trim();
            if (!pattern.isEmpty()) {
                result.computeIfAbsent(providerSymbolicName, k -> new ArrayList<>()).add(pattern);
                logger.debug("Added pattern '{}' for provider '{}' from properties", pattern, providerSymbolicName);
            }
        }

        return result;
    }

    /**
     * Returns a snapshot of the merged deprecated-package patterns, keyed by provider bundle symbolic name.
     * <p>
     * The returned map and its value collections are independent copies; modifying them has no effect
     * on the internal state. The method is thread-safe: it acquires the {@link #mergedProviders} lock
     * for the duration of the copy.
     *
     * @return ordered map of provider bundle symbolic name → collection of Java regex patterns;
     * never {@code null}, empty when no configuration instances are active
     */
    public Map<String, Collection<String>> getDeprecatedPatternsByProvider() {
        synchronized (mergedProviders) {
            Map<String, Collection<String>> copy = new LinkedHashMap<>();
            mergedProviders.forEach((provider, patterns) -> copy.put(provider, new ArrayList<>(patterns)));
            return copy;
        }
    }
}
