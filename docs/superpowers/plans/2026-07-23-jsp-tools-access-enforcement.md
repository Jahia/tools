# JSP-local tools-access enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce the `systemToolsAccess` permission inside every tools JSP, as a second security layer independent of any URL-matching filter, so a request that slips past the perimeter chain cannot reach a JSP's root escalation.

**Architecture:** A single custom JSP tag `<tools:requireToolsAccess/>`, backed by a shared `ToolsAccessGuard` helper, is placed as the first executable statement of every tool JSP. It checks the permission as the connected user and, on denial, sends `403` and aborts the page before any privileged work. The module-scoped `ToolsAuthorizationFilter` (same URL-matching weakness it tried to fix) is deleted. A data-driven Cypress spec enumerates all JSPs and asserts the invariant.

**Tech Stack:** Java 8 / OSGi module (maven-bundle-plugin), JSP 2.1 taglib, Jahia `JCRSessionFactory`/`JCRTemplate` APIs, Cypress e2e (TypeScript).

## Global Constraints

- License header: every new `.java` file starts with the Apache-2.0 header block used across the module (copy verbatim from `GroovyConsoleHelper.java`, lines 1-15).
- Package layout: permission logic in `org.jahia.modules.tools.security`; JSP tag handler in `org.jahia.modules.tools.taglibs` (next to `GroovyConsoleHelper`).
- Jahia dependencies are `provided` scope; do not add new runtime dependencies.
- Taglib coordinates are fixed: prefix `tools`, uri `http://www.jahia.org/tags/tools` (already declared in `impl/src/main/resources/META-INF/tools.tld`).
- Fail closed: any null user or exception is treated as denial.
- Do NOT add a JUnit/surefire harness — this module has no Java tests by design; behavioral coverage is the Cypress e2e.

---

### Task 1: Enforcement mechanism (guard helper + tag + tld) and filter removal

**Files:**
- Create: `impl/src/main/java/org/jahia/modules/tools/security/ToolsAccessGuard.java`
- Create: `impl/src/main/java/org/jahia/modules/tools/taglibs/RequireToolsAccessTag.java`
- Modify: `impl/src/main/resources/META-INF/tools.tld`
- Delete: `impl/src/main/java/org/jahia/modules/tools/security/ToolsAuthorizationFilter.java`

**Interfaces:**
- Consumes: Jahia `JCRSessionFactory.getInstance().getCurrentUser()`, `JCRTemplate.getInstance().doExecute(JahiaUser, String, Locale, JCRCallback)`.
- Produces:
  - `org.jahia.modules.tools.security.ToolsAccessGuard.isGranted() : boolean` (static)
  - `org.jahia.modules.tools.taglibs.RequireToolsAccessTag` (SimpleTagSupport)
  - tld tag `<tools:requireToolsAccess/>` (empty body) — consumed by Task 2.

- [ ] **Step 1: Create the guard helper**

Create `impl/src/main/java/org/jahia/modules/tools/security/ToolsAccessGuard.java`:

```java
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
package org.jahia.modules.tools.security;

import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single source of truth for the tools authorization check. Evaluated at the resource (inside the tool JSPs) as a
 * second layer of defense that does not depend on any URL-pattern matching: it fires however the request was routed.
 */
public final class ToolsAccessGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolsAccessGuard.class);

    /** JCR node the tools access permission is evaluated against. */
    private static final String TOOLS_PERMISSION_NODE = "/tools";

    /** Privilege required to use the support tools, identical to the Shiro rule {@code perms[/tools:systemToolsAccess]}. */
    private static final String TOOLS_PERMISSION = "systemToolsAccess";

    private ToolsAccessGuard() {
        // utility class
    }

    /**
     * Checks, as the currently-connected user, whether they are allowed to use the support tools. Any missing user or
     * error while resolving the permission is treated as a denial (default-deny).
     *
     * @return {@code true} only if the connected user holds {@code systemToolsAccess} on {@code /tools}
     */
    public static boolean isGranted() {
        JahiaUser currentUser = JCRSessionFactory.getInstance().getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        try {
            return JCRTemplate.getInstance().doExecute(currentUser, null, null,
                    session -> session.getNode(TOOLS_PERMISSION_NODE).hasPermission(TOOLS_PERMISSION));
        } catch (Exception e) {
            LOGGER.debug("Unable to evaluate tools access for user {}, denying access", currentUser.getName(), e);
            return false;
        }
    }
}
```

- [ ] **Step 2: Create the tag handler**

Create `impl/src/main/java/org/jahia/modules/tools/taglibs/RequireToolsAccessTag.java`:

```java
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
package org.jahia.modules.tools.taglibs;

import org.jahia.modules.tools.security.ToolsAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * Enforces, at the very top of every tool JSP, that the connected user is authorized to use the support tools. On
 * denial it returns {@code 403 Forbidden} and aborts the page, so the JSP never reaches its root JCR escalation.
 *
 * @see ToolsAccessGuard
 */
public class RequireToolsAccessTag extends SimpleTagSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequireToolsAccessTag.class);

    @Override
    public void doTag() throws JspException, IOException {
        if (ToolsAccessGuard.isGranted()) {
            return;
        }
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Denying unauthorized access to tools resource {}", request.getRequestURI());
        }
        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        // Abort the rest of the page so no privileged work runs.
        throw new SkipPageException();
    }
}
```

- [ ] **Step 3: Register the tag in the tld**

In `impl/src/main/resources/META-INF/tools.tld`, add this `<tag>` element immediately after the opening block and before the first `<function>` element (after the `<uri>...</uri>` line):

```xml
    <tag>
        <description>Enforces that the connected user holds systemToolsAccess on /tools; sends 403 and aborts the page
            otherwise. Must be the first executable statement of every tool JSP.</description>
        <name>requireToolsAccess</name>
        <tag-class>org.jahia.modules.tools.taglibs.RequireToolsAccessTag</tag-class>
        <body-content>empty</body-content>
    </tag>
```

- [ ] **Step 4: Delete the module filter**

Run:

```bash
git rm impl/src/main/java/org/jahia/modules/tools/security/ToolsAuthorizationFilter.java
```

Expected: `rm 'impl/src/main/java/org/jahia/modules/tools/security/ToolsAuthorizationFilter.java'`

- [ ] **Step 5: Build to verify it compiles and the bundle assembles**

Run: `mvn -q -pl impl -am clean install -DskipTests`
Expected: `BUILD SUCCESS`. No compilation error referencing `ToolsAuthorizationFilter`, `ToolsAccessGuard`, or `RequireToolsAccessTag`.

- [ ] **Step 6: Commit**

```bash
git add impl/src/main/java/org/jahia/modules/tools/security/ToolsAccessGuard.java \
        impl/src/main/java/org/jahia/modules/tools/taglibs/RequireToolsAccessTag.java \
        impl/src/main/resources/META-INF/tools.tld
git commit -m "feat(sec-245): add tools access guard tag, drop module authorization filter"
```

---

### Task 2: Apply the guard to every tool JSP

**Files:**
- Modify (bulk, 47 files): `impl/src/main/resources/*.jsp` EXCEPT `support.jsp` and `jobInfo.jsp`
- Modify (explicit edits): `impl/src/main/resources/jobInfo.jsp`, `impl/src/main/resources/support.jsp`
- Modify (cleanup): `impl/src/main/resources/groovyConsole.jsp`

**Interfaces:**
- Consumes: tld tag `<tools:requireToolsAccess/>` and taglib uri `http://www.jahia.org/tags/tools` from Task 1.
- Produces: every tool JSP has the guard as its first executable statement (relied on by Task 3's enumeration).

**Why two files are handled separately:** in the 47 bulk files, line 1 is a self-contained JSP directive (translation-time only) with no executable code, so inserting the guard as line 2 places it ahead of all output and scriptlets. But `support.jsp` and `jobInfo.jsp` open a directive on line 1 that only closes on line 2 (`%>` on the next line) — inserting after line 1 would split the directive and break the file. `support.jsp` is also the sharp case: it calls `SupportInfoHelper.exportInfo(...)` on line 3 before any HTML, so the guard must precede that call.

- [ ] **Step 1: Insert the taglib directive + guard after line 1 of the 47 bulk JSPs**

Run:

```bash
cd /home/jerome/Work/tools
for f in impl/src/main/resources/*.jsp; do
  b=$(basename "$f")
  if [ "$b" = "support.jsp" ] || [ "$b" = "jobInfo.jsp" ]; then continue; fi
  awk 'NR==1{
        print;
        print "<%@ taglib prefix=\"tools\" uri=\"http://www.jahia.org/tags/tools\" %>";
        print "<tools:requireToolsAccess/>";
        next
      }
      {print}' "$f" > "$f.tmp" && mv "$f.tmp" "$f"
done
```

- [ ] **Step 2: Guard `jobInfo.jsp` (directive spans lines 1-2, no privileged work before line 5)**

Insert the taglib directive + guard after the first directive closes. Exact Edit — replace:

```
<%@ page import="org.jahia.registries.ServicesRegistry, org.jahia.services.scheduler.SchedulerService, org.quartz.*, java.text.SimpleDateFormat, java.util.Date"
%>
```

with:

```
<%@ page import="org.jahia.registries.ServicesRegistry, org.jahia.services.scheduler.SchedulerService, org.quartz.*, java.text.SimpleDateFormat, java.util.Date"
%>
<%@ taglib prefix="tools" uri="http://www.jahia.org/tags/tools" %>
<tools:requireToolsAccess/>
```

- [ ] **Step 3: Guard `support.jsp` (privileged `exportInfo` runs on line 3 — guard must precede it)**

The taglib directive on line 1 closes with `%>` at the start of line 2, immediately followed by a page-import directive and then the `<% File targetDir ... %>` scriptlet that leads into `exportInfo`. Place the `tools` taglib directive before the page-import directive and the guard tag right before the scriptlet. Exact Edit — replace:

```
%><%@ page import="org.jahia.modules.tools.SupportInfoHelper, java.io.File" %><% File targetDir
```

with:

```
%><%@ taglib prefix="tools" uri="http://www.jahia.org/tags/tools" %><%@ page import="org.jahia.modules.tools.SupportInfoHelper, java.io.File" %><tools:requireToolsAccess/><% File targetDir
```

- [ ] **Step 4: Remove the now-duplicate taglib declaration in groovyConsole.jsp**

`groovyConsole.jsp` already declared the `tools` prefix; Step 1 added a second identical declaration. Remove the pre-existing one (the later occurrence) to avoid a duplicate directive.

Run:

```bash
cd /home/jerome/Work/tools
grep -n 'tags/tools' impl/src/main/resources/groovyConsole.jsp
```

Expected: two lines — line 2 (the one just inserted, keep it) and a later line (~27, the original). Delete the later one with an exact Edit, removing only:

```
<%@ taglib prefix="tools" uri="http://www.jahia.org/tags/tools" %>
```

at the later line so a single declaration remains at line 2. Re-run the `grep -n 'tags/tools'` and confirm exactly one match remains.

- [ ] **Step 5: Verify every JSP carries the guard exactly once**

Run:

```bash
cd /home/jerome/Work/tools
echo "jsp files:   $(ls impl/src/main/resources/*.jsp | wc -l)"
echo "with guard:  $(grep -lc '<tools:requireToolsAccess/>' impl/src/main/resources/*.jsp | wc -l)"
grep -c '<tools:requireToolsAccess/>' impl/src/main/resources/*.jsp | grep -v ':1$' || echo "all files contain exactly one guard"
```

Expected: `jsp files: 49`, `with guard: 49`, and `all files contain exactly one guard`.

- [ ] **Step 6: Rebuild to verify all JSPs still translate/compile**

Run: `mvn -q -pl impl -am clean install -DskipTests`
Expected: `BUILD SUCCESS` (JSP pre-compilation, if run by the build, reports no translation error for the `tools:` prefix).

- [ ] **Step 7: Commit**

```bash
git add impl/src/main/resources/*.jsp
git commit -m "feat(sec-245): enforce tools access guard at the top of every tool JSP"
```

---

### Task 3: Cypress enumeration of the 403 invariant

**Files:**
- Modify: `tests/cypress/plugins/index.js`
- Modify: `tests/cypress/e2e/toolsAccessSecurity.spec.ts`

**Interfaces:**
- Consumes: the guard applied in Task 2 (runtime 403 behavior).
- Produces: a `listToolJsps` Cypress task returning `string[]` of JSP basenames; an expanded security spec.

- [ ] **Step 1: Add the `listToolJsps` task to the Cypress plugins**

In `tests/cypress/plugins/index.js`, add `fs`/`path` requires near the top (with the other `require`s):

```js
const fs = require('fs');
const path = require('path');
```

Then add a `listToolJsps` entry inside the existing `on('task', { ... })` object, alongside `sshCommand`:

```js
        listToolJsps() {
            // Enumerate the module's tool JSPs from source so the security sweep cannot drift.
            const jspDir = path.resolve(__dirname, '../../../impl/src/main/resources');
            return fs.readdirSync(jspDir).filter(name => name.endsWith('.jsp'));
        },
```

- [ ] **Step 2: Rewrite the security spec to sweep every JSP**

Replace the entire contents of `tests/cypress/e2e/toolsAccessSecurity.spec.ts` with:

```ts
/*
 * SEC-245 - Tools access authorization is enforced locally by every tool JSP (a second layer, independent of the
 * perimeter Shiro filter chain and of any URL-pattern matching). The check lives in the resource itself, so a request
 * that slips past upstream filters still cannot reach a JSP's root escalation.
 *
 * This spec enumerates every tool JSP from source and asserts an unauthenticated request is denied with 403 on each.
 * Denial aborts the page before its body runs, so the sweep triggers no tool side effects.
 */
describe('SEC-245 - tools access authorization is enforced by every tool JSP', () => {
    // A representative tool page and the privileged JCR browser (the reported exploit target), used for the
    // authorized-access check. The authorized sweep is deliberately NOT run across all JSPs: many have side effects
    // (GC, thread dumps, maintenance mode) on a plain GET.
    const TOOL_URL = '/modules/tools/index.jsp';
    const PRIVILEGED_TOOL_URL = '/modules/tools/jcrBrowser.jsp';

    let jsps: string[] = [];

    before(() => {
        cy.task('listToolJsps').then((names: string[]) => {
            jsps = names;
            expect(jsps.length, 'tool JSPs discovered').to.be.greaterThan(0);
        });
    });

    it('denies an unauthenticated user with 403 on every tool JSP', () => {
        cy.clearCookies();
        cy.wrap(null).then(() => {
            jsps.forEach(name => {
                cy.request({url: `/modules/tools/${name}`, failOnStatusCode: false}).then(response => {
                    expect(response.status, `unauthenticated GET /modules/tools/${name}`).to.eq(403);
                });
            });
        });
    });

    it('allows an authorized administrator with 200 on representative tool pages', () => {
        cy.login();
        cy.request({url: TOOL_URL, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(200);
        });
        cy.request({url: PRIVILEGED_TOOL_URL, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(200);
        });
        cy.logout();
    });
});
```

- [ ] **Step 3: Lint the changed test sources**

Run: `cd tests && yarn eslint cypress/e2e/toolsAccessSecurity.spec.ts cypress/plugins/index.js`
Expected: no errors (warnings tolerated as elsewhere in the repo).

- [ ] **Step 4: Commit**

```bash
git add tests/cypress/plugins/index.js tests/cypress/e2e/toolsAccessSecurity.spec.ts
git commit -m "test(sec-245): assert 403 on every tool JSP for unauthenticated users"
```

---

## Whole-branch verification (run before opening the PR)

The behavioral gate is the Cypress e2e, which requires a running Jahia with the module deployed (the project's `tests/` docker-compose / CI path — not a quick local unit run). After deploying the freshly built module:

- `toolsAccessSecurity.spec.ts` passes: 403 on all 49 JSPs unauthenticated, 200 on the two representative pages authenticated.
- Confirm `ToolsAuthorizationFilter` is gone: `git grep -n ToolsAuthorizationFilter` returns nothing.