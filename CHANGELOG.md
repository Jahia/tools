# tools Changelog

## 5.4.0

### New Features

* Added wire packages analyzer (#304)

  Adds a new tool to identify which active Jahia modules are wired at runtime to specific Java packages, useful to detect modules that still depend on packages scheduled for removal (deprecated packages).

* HTML-escape node names rendered in the JCR query results view, including inside the delete-action handler, matching the JCR browser view.

* Migrated from Blueprint to OSGi DS (#306)

### Bug Fixes

* URL-encode and HTML-escape the node path rendered in the "Open in Repository Explorer" link of the JCR query results view, completing the escaping of that view.

* Switched to StringUtils from Apache commons lang3 to remove plexus dependency (#313)

* Improve security by enforcing authorization in all jsp (#329)

* Removed the preview feature in the ehcache table (#315)
