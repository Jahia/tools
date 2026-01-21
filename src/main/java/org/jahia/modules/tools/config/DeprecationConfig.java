package org.jahia.modules.tools.config;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSGI configuration service factory for managing deprecated package patterns.
 * This service allows multiple configuration instances to be created, each providing
 * a set of package patterns that should be considered deprecated.
 *
 * Configuration can be provided via .cfg or .yaml files with PID: org.jahia.modules.tools.deprecation-{name}
 * Each configuration should contain a "patterns" property as an array of regex patterns.
 *
 * Example YAML configuration file (org.jahia.modules.tools.deprecation-spring.yaml):
 * patterns:
 *   - "org\\.springframework\\..*"
 *   - "org\\.springframework\\.web\\..*"
 *
 * This translates to properties like: patterns[0]=org\\.springframework\\..*
 *
 * @author jkevan
 */
@Component(
        service = { DeprecationConfig.class, ManagedServiceFactory.class },
        immediate = true,
        property = "service.pid=org.jahia.modules.tools.deprecation"
)
public class DeprecationConfig implements ManagedServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeprecationConfig.class);

    private static final String PATTERNS_PROPERTY_PREFIX = "patterns";

    private final Map<String, Dictionary<String, ?>> configs = new ConcurrentHashMap<>();
    private final Set<String> mergedPatterns = new LinkedHashSet<>();

    @Override
    public String getName() {
        return "Jahia Tools Deprecation Config Factory";
    }

    /**
     * Called when a new configuration is added or an existing one is updated.
     *
     * @param pid The persistent identifier of the configuration
     * @param properties The configuration properties
     * @throws ConfigurationException If the configuration is invalid
     */
    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        logger.info("Updating deprecation configuration for PID: {}", pid);
        configs.put(pid, properties);
        rebuildMergedPatterns();
    }

    /**
     * Called when a configuration is deleted.
     *
     * @param pid The persistent identifier of the configuration
     */
    @Override
    public void deleted(String pid) {
        logger.info("Deleting deprecation configuration for PID: {}", pid);
        configs.remove(pid);
        rebuildMergedPatterns();
    }

    /**
     * Rebuilds the merged patterns set from all active configurations.
     * Handles YAML array format where patterns are stored as patterns[0], patterns[1], etc.
     */
    private void rebuildMergedPatterns() {
        synchronized (mergedPatterns) {
            mergedPatterns.clear();

            for (Map.Entry<String, Dictionary<String, ?>> entry : configs.entrySet()) {
                Dictionary<String, ?> properties = entry.getValue();
                List<String> patterns = extractPatternsFromProperties(properties);

                for (String pattern : patterns) {
                    String trimmedPattern = pattern.trim();
                    if (!trimmedPattern.isEmpty()) {
                        mergedPatterns.add(trimmedPattern);
                        logger.debug("Added pattern: {} from PID: {}", trimmedPattern, entry.getKey());
                    }
                }
            }

            logger.info("Merged deprecation patterns updated. Total patterns: {}", mergedPatterns.size());
        }
    }

    /**
     * Extracts patterns from properties dictionary. Handles YAML array format where
     * patterns are stored as patterns[0], patterns[1], patterns[2], etc.
     *
     * @param properties The configuration properties
     * @return List of pattern strings extracted from the properties
     */
    private List<String> extractPatternsFromProperties(Dictionary<String, ?> properties) {
        List<String> patterns = new ArrayList<>();

        // Iterate through all keys to find pattern entries (patterns[0], patterns[1], etc.)
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            // Check if key starts with "patterns"
            if (key.startsWith(PATTERNS_PROPERTY_PREFIX)) {
                Object value = properties.get(key);

                if (value != null) {
                    String patternValue = value.toString();
                    if (!patternValue.isEmpty()) {
                        patterns.add(patternValue);
                    }
                }
            }
        }

        return patterns;
    }

    /**
     * Returns the merged set of all deprecated package patterns from all configurations.
     *
     * @return A collection of regex patterns for deprecated packages
     */
    public Collection<String> getDeprecatedPatterns() {
        synchronized (mergedPatterns) {
            return new ArrayList<>(mergedPatterns);
        }
    }

    /**
     * Checks if any deprecated patterns are configured.
     *
     * @return true if at least one pattern is configured, false otherwise
     */
    public boolean hasPatterns() {
        synchronized (mergedPatterns) {
            return !mergedPatterns.isEmpty();
        }
    }
}
