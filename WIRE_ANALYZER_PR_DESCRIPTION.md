# Module Java Dependencies Analyzer (Wire Analyzer)

## Overview
This PR introduces a new tool to help identify which Jahia modules are using specific Java packages by analyzing actual runtime dependencies (OSGI "wires"). Unlike the existing Import-Package checker that only examines manifest headers, this tool inspects real runtime connections between modules, including dynamically loaded classes.

## Features

### 1. Jahia Deprecated Packages Analysis (Pre-configured)
A built-in analyzer that uses Jahia-maintained configuration files to detect modules using deprecated packages from Jahia core.

**Key capabilities:**
- Automatically checks against Jahia-defined patterns for deprecated APIs
- Filters results to show only packages from the Jahia framework (`org.apache.felix.framework`)
- Configuration-driven via YAML files managed by Jahia
- Helps customers prepare for Jahia upgrades by identifying modules that need refactoring

**Configuration format** (managed by Jahia):
```yaml
# File: org.jahia.modules.tools.deprecation-spring.yaml
patterns:
  - "org\\.springframework\\..*"
  - "javax\\.servlet\\..*"
```

### 2. Custom Pattern Analysis (Ad-hoc Testing)
An open form allowing users to perform one-time dependency analysis with custom package patterns.

**Key capabilities:**
- Analyze any Java package usage with regex patterns
- Optional provider bundle filtering (e.g., filter by specific library bundles)
- Useful for:
  - Preparing for library upgrades
  - Auditing package usage across modules
  - Detecting potential compatibility issues
  - Custom dependency exploration

**Example use cases:**
- Find all modules using Spring Framework: `org\.springframework\..*`
- Check Guava library usage: `com\.google\.common\..*`
- Audit legacy Java EE packages: `javax\.servlet\..*`

### 3. GraphQL API Integration
Both analysis options are exposed as GraphQL endpoints alongside existing OSGI tools.

**Available queries:**
- `findDeprecatedWires()` - Analyzes Jahia-configured deprecated packages
- `findWires(patterns, providerBundleSymbolicName)` - Custom pattern analysis with optional provider filtering

**Example GraphQL query:**
```graphql
query {
  admin {
    tools {
      findDeprecatedWires {
        totalCount
        bundles {
          displayName
          matchingWires
        }
      }
    }
  }
}
```

## User Interface

### Web Tool Location
`/tools/osgiWiresAnalyzer.jsp`

**Features:**
- Clean, user-friendly interface with help popups (using fancybox)
- Two analysis options clearly separated
- Real-time results showing:
  - Number of affected modules
  - Specific package dependencies per module
  - Provider bundle information for each wire
  - Links to OSGI console for detailed inspection

### What Makes This Different from Import-Package Checker?
- **Import-Package Checker**: Analyzes manifest headers (declared imports)
- **Wire Analyzer**: Analyzes actual runtime wires (real usage including dynamic imports)

The wire analyzer catches packages that are actually loaded and used at runtime, providing a more accurate picture of module dependencies.

## Technical Changes

### New Components
1. **`DeprecationConfig`** - OSGI ManagedServiceFactory for managing deprecated package patterns
2. **`OSGIAnalyzer.findWires()`** - Core analysis method for bundle wire inspection
3. **`OSGIAnalyzer.findDeprecatedWires()`** - Convenience method using Jahia configurations
4. **`BundleWithWires`** - Result model for wire analysis data
5. **`FindWires`** - Container for complete analysis results

### Architecture Improvements
- **OSGI Services**: Migrated to pure OSGI service-based architecture
- **Spring Context Removed**: Eliminated Spring XML configuration files in favor of OSGI Declarative Services annotations
- **Configuration-Driven**: Deprecated patterns managed via YAML configuration files (hot-reloadable)

### Analysis Scope
- **Target**: Started Jahia module bundles only
- **Method**: Uses `bundle.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)`
- **Includes**: Dynamic imports and runtime-resolved wires
- **Provider Filtering**: Optional filtering by provider bundle symbolic name

## Benefits

### For Jahia
- Proactively notify customers about deprecated API usage before major releases
- Centralized management of deprecation patterns via configuration
- Better visibility into module dependencies across customer installations

### For Customers
- **Upgrade Preparation**: Identify modules needing updates before Jahia upgrades
- **Dependency Auditing**: Understand which modules depend on specific libraries
- **Custom Analysis**: Flexible tool for ad-hoc dependency exploration
- **No Downtime**: Analysis runs on live system without restart

## Files Changed

### Created
- `org.jahia.modules.tools.config.DeprecationConfig.java`
- `org.jahia.modules.tools.gql.admin.osgi.BundleWithWires.java`
- `org.jahia.modules.tools.gql.admin.osgi.FindWires.java`
- `osgiWiresAnalyzer.jsp`
- `org.jahia.modules.tools.deprecation-spring.yaml.example`

### Modified
- `org.jahia.modules.tools.gql.admin.osgi.OSGIAnalyzer.java` - Added wire analysis methods
- `org.jahia.modules.tools.gql.admin.ToolsGraphQL.java` - Added GraphQL endpoints
- `tools.css` - Added utility styles for info boxes

### Removed
- Spring context XML files (replaced with OSGI services)

## Example Output

When modules are found using deprecated packages:
```
⚠ Found 15 matching package wire(s) in 3 module(s).

Affected Modules:
- [123] My Custom Module (5 matching wires)
  • org.springframework.web.servlet (from org.apache.felix.framework [0])
  • org.springframework.jdbc.core (from org.apache.felix.framework [0])
  ...
```

When no issues are found:
```
✓ No matching dependencies found! All Jahia modules are clean.
```

## Migration Notes
No migration required for existing functionality. This is a purely additive feature that extends the existing OSGI tools suite.

## Testing Recommendations
1. Test with Jahia-provided deprecation configurations
2. Verify custom pattern analysis with various regex patterns
3. Test provider bundle filtering functionality
4. Validate GraphQL endpoints return expected results
5. Ensure UI displays results correctly for both empty and populated states

