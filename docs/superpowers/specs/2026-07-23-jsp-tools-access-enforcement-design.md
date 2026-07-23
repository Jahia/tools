# JSP-local tools-access enforcement

**Date:** 2026-07-23
**Branch:** `fix/sec-245-jsp-root`
**Status:** Approved design

## Problem

Every tool JSP in this module escalates to a root/system JCR session inline
(`JCRTemplate.doExecuteWithSystemSessionAsUser(...)`,
`JCRSessionFactory.getCurrentSystemSession(...)`) to carry out privileged work.
Historically the JSPs assumed that *reaching execution* meant the caller was
authorized, trusting the perimeter Shiro rule `perms[/tools:systemToolsAccess]`
matched on the request URL. That URL-matching rule proved bypassable (SEC-245),
letting an unauthorized request reach a JSP and inherit its root escalation.

An earlier attempt on this branch added a module-scoped servlet filter
(`ToolsAuthorizationFilter`) that re-asserted the permission using servlet
`urlPatterns` (`/modules/tools/*`, `/tools/*`). This is rejected as the fix:
Jahia gives a module no private entrypoint — everything plugs into the global
filter chain — so a module-scoped filter is just *another* global filter
carrying the **same URL-matching weakness** it is meant to compensate for. Any
authorization that keys off URL matching shares a bypass surface with the
resource it protects: if the dispatch layer resolves a path to a tool JSP that
the matching layer did not recognize as "tools", the filter is skipped but the
JSP still runs with root.

## Principle

Global filtering belongs upstream in Jahia's chain (out of this module's hands,
and out of scope here). The module's responsibility is a **second, independent
layer**: each tool JSP enforces the security context *at the resource*, before
any privileged work. Because the check lives in the JSP, it fires regardless of
how the request was routed there — no URL-pattern dependency.

Since Jahia offers no super-JSP to centralize this, the check is factored into a
single custom JSP tag that each JSP invokes as its first executable statement.

## Components

### 1. `ToolsAccessGuard` (`org.jahia.modules.tools.security`)
Single source of truth for the permission check. Holds the constants and one
method:

- `TOOLS_PERMISSION_NODE = "/tools"`
- `TOOLS_PERMISSION = "systemToolsAccess"`
- `boolean isGranted()` — resolve the current user via
  `JCRSessionFactory.getInstance().getCurrentUser()`; if null, deny. Otherwise
  `JCRTemplate.getInstance().doExecute(currentUser, null, null, session ->
  session.getNode("/tools").hasPermission("systemToolsAccess"))`. Any exception
  is caught and treated as denial (default-deny). Logic lifted verbatim from the
  filter being deleted.

### 2. `RequireToolsAccessTag` (`org.jahia.modules.tools.taglibs`)
`SimpleTagSupport`, alongside the existing `GroovyConsoleHelper`. In `doTag()`:

- If `ToolsAccessGuard.isGranted()` returns `true`, do nothing (page proceeds).
- Otherwise: log a warning (mirroring the deleted filter), obtain the
  `HttpServletResponse` from the `PageContext`, call
  `sendError(HttpServletResponse.SC_FORBIDDEN)`, and throw
  `javax.servlet.jsp.SkipPageException` to abort the page before any escalation.

Empty body content; renders nothing.

### 3. `tools.tld`
Add a `<tag>` entry to the existing taglib (uri
`http://www.jahia.org/tags/tools`, short-name `tools`):

```xml
<tag>
    <description>Enforces that the current user holds systemToolsAccess on
        /tools; sends 403 and aborts the page otherwise.</description>
    <name>requireToolsAccess</name>
    <tag-class>org.jahia.modules.tools.taglibs.RequireToolsAccessTag</tag-class>
    <body-content>empty</body-content>
</tag>
```

### 4. All 49 tool JSPs (`impl/src/main/resources/*.jsp`)
Each JSP gets:

- `<%@ taglib prefix="tools" uri="http://www.jahia.org/tags/tools" %>` (48 of
  them; `groovyConsole.jsp` already declares it).
- `<tools:requireToolsAccess/>` as the **first executable line** — after the
  page/taglib directives (which are translation-time, not executable) but ahead
  of every top-of-page scriptlet and any JCR work.

### 5. Guard-presence test
A JUnit test that scans `impl/src/main/resources/*.jsp` and asserts each file
contains `<tools:requireToolsAccess/>`. This makes "someone forgot to guard a
new tool JSP" a build failure rather than a silent hole — the mitigation for the
inherent weakness of per-resource enforcement. Introduces `impl/src/test`
(no test directory exists today); build/test wiring (surefire) may need adding.

### 6. Delete `ToolsAuthorizationFilter.java`
Remove `impl/src/main/java/org/jahia/modules/tools/security/ToolsAuthorizationFilter.java`.
Its permission-check logic survives in `ToolsAccessGuard`.

## Data flow

```
Request
  -> upstream global filters (may be bypassed — not this module's concern)
  -> JSP dispatched (via OSGi proxy /modules/tools/* or /tools/*)
  -> <tools:requireToolsAccess/> executes FIRST
       granted -> page proceeds, escalates to root as before
       denied  -> 403 + SkipPageException; page aborts, no escalation reached
```

## Error handling — fails closed everywhere

- No current user → deny.
- Exception while resolving the permission → deny + log.
- Denied → `sendError(403)` + `SkipPageException`.

## Edge cases

- **Nested `<jsp:include>`:** `search.jsp` guards, then request-dispatches
  `searchIndexCheck.jsp`, which also guards → the check runs twice and is
  idempotent (both granted). The deny path (`sendError` inside an included
  response, which some containers reject) is never reached in the nested case
  because the parent guard already passed. Direct hits on `searchIndexCheck.jsp`
  are top-level requests where `sendError` behaves normally. Both JSPs are
  guarded.
- **`indexEnterprise.jsp`:** included by `index.jsp` but shipped by the
  enterprise module, not this repository. Out of scope; it must carry its own
  guard in that module. Flagged, not fixed here.

## Testing & honest limitations

- **Presence-scanner (new JUnit):** static invariant — every tool JSP carries
  the guard.
- **`tests/cypress/e2e/toolsAccessSecurity.spec.ts` (existing on branch):**
  runtime behavior — an unauthorized request to a tool JSP returns 403.
- **`ToolsAccessGuard` unit test:** deliberately not written as a hollow mock.
  The guard depends on Jahia's static singletons (`JCRSessionFactory`,
  `JCRTemplate`), so meaningful behavioral coverage comes from the Cypress e2e,
  not a mock. This is a conscious choice, not an omission.

## Files touched

- **Delete:** `impl/src/main/java/org/jahia/modules/tools/security/ToolsAuthorizationFilter.java`
- **Add:** `impl/src/main/java/org/jahia/modules/tools/security/ToolsAccessGuard.java`
- **Add:** `impl/src/main/java/org/jahia/modules/tools/taglibs/RequireToolsAccessTag.java`
- **Edit:** `impl/src/main/resources/META-INF/tools.tld`
- **Edit:** all 49 `impl/src/main/resources/*.jsp`
- **Add:** JUnit guard-presence test under `impl/src/test/...` (+ test wiring if needed)
- **Keep:** `tests/cypress/e2e/toolsAccessSecurity.spec.ts`