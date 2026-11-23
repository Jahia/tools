# tools Changelog

## 5.4.1

* Added maven.jahia.com to the Maven repositories tools uses to download its dependencies.

* Fixed the JCR check and benchmark tools so node paths and error details are displayed as text.

* Fixed the Key column of the cache details page so keys containing quotes or markup characters are displayed as text.

* Fixed the job administration tool so the job group and name shown after an action are displayed as text.

* Fixed the Import-Package checker tool so bundle names, package clauses and search values are displayed as text.

* Fixed the JCR browser so that node names, paths, property names and error messages containing quotes or markup characters are displayed correctly, and breadcrumb links work for such names.

* Restricted caching of Support Tools pages so each request is answered by the server, not from a cache.

* Raised the minimum Jahia version to 8.2.1.0. Jahia 8.2.1.0 or later is now required to install the module.

* Values submitted to the text extractor tool are now escaped before they are written back into the form, so a value containing a quote or an angle bracket is displayed as text instead of altering the page markup.

* Fixed the module output cache pages so cache keys and the content of a viewed cache entry are displayed as text.

* Fixed the OSGi package wires analyzer so bundle names, symbolic names and package names are displayed as text.

* Restricted the Groovy console scriptURI parameter to the scripts packaged in active module bundles.

* Fixed the Dependencies column of the output dependencies cache page so dependent keys containing quotes or markup characters are displayed as text.

* Fixed the OSGi package wires analyzer so configured providers, patterns, errors and form values are displayed as text.

## 5.5.0

### Bug Fixes

* Restricted the Groovy console `scriptURI` parameter to the scripts packaged in active module bundles. It used to be opened as a URL, so `file://` and `http://` values turned it into an arbitrary file read, a server-side request forgery and a remote code execution vector, since whatever it returned was executed by the Groovy engine. Unknown values are now rejected with a `400`, and the script content shown in the preview is HTML-escaped.

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
