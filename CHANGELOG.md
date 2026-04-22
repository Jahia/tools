# tools Changelog

## 0.1.0

### New Features

* Added wire packages analyzer (#304)

  Adds a new tool to identify which active Jahia modules are wired at runtime to specific Java packages, useful to detect modules that still depend on packages scheduled for removal (deprecated packages).

* Migrated from Blueprint to OSGi DS (#306)

### Bug Fixes

* Switched to StringUtils from Apache commons lang3 to remove plexus dependency (#313)
