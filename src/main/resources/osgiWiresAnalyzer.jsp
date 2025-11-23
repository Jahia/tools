<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.OSGIAnalyzer" %>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.FindWires" %>
<%@ page import="org.jahia.modules.tools.config.DeprecationConfig" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="java.util.Arrays" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="OSGI Java packages wires analyzer"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>

<body>
<%@ include file="commons/header.jspf" %>
<div>
    <p>
        <a data-src="#toolInfoArea" data-fancybox><img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="help" title="What does this tool do?"/> What does this tool do?</a>
    </p>

    <!-- Jahia Deprecated Packages Analysis Section -->
    <fieldset>
        <legend><strong>Option 1:</strong> Analyze Jahia Deprecated Packages wires(Pre-configured by Jahia)</legend>
        <p>
            This analysis uses <strong>Jahia-maintained configuration files</strong> that define deprecated packages from Jahia core.
            These configuration files are managed by Jahia and list packages that are planned for removal or replacement in future versions.
            Results are filtered to show only packages provided by the Jahia framework (<code>org.apache.felix.framework</code>).
        </p>

        <%
            DeprecationConfig deprecationConfig = BundleUtils.getOsgiService(DeprecationConfig.class, null);
            pageContext.setAttribute("deprecationConfig", deprecationConfig);
            if (deprecationConfig != null) {
                pageContext.setAttribute("configuredPatterns", deprecationConfig.getDeprecatedPatterns());
            }
        %>

        <c:choose>
            <c:when test="${not empty configuredPatterns}">
                <p>
                    <a data-src="#configuredPatternsArea" data-fancybox><img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="info" title="View configured patterns"/> View currently configured Jahia deprecated patterns (${fn:length(configuredPatterns)})</a>
                </p>
                <p><em>These patterns are provided by Jahia via configuration files (PID: <code>org.jahia.modules.tools.deprecation-*</code>)</em></p>
            </c:when>
            <c:otherwise>
                <p><strong>⚠ No Jahia deprecated patterns configured.</strong></p>
                <details>
                    <summary style="cursor: pointer;">Configuration information (for Jahia administrators)</summary>
                    <div style="margin-top: 10px;">
                        <p><strong>Note:</strong> These configuration files are typically managed by Jahia, not end users.</p>
                        <p><strong>Location:</strong> <code>digital-factory-data/karaf/etc/</code></p>
                        <p><strong>File name pattern:</strong> <code>org.jahia.modules.tools.deprecation-{name}.yaml</code></p>
                        <pre># Example: org.jahia.modules.tools.deprecation-spring.yaml
patterns:
  - "org\\.springframework\\..*"
  - "javax\\.servlet\\..*"</pre>
                    </div>
                </details>
            </c:otherwise>
        </c:choose>

        <form id="deprecatedAnalysis" action="?" method="get">
            <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
            <input type="submit" name="analyze" value="Run Jahia Deprecated Packages Analysis"
                   title="Analyze using Jahia-configured deprecated patterns"
                   ${empty configuredPatterns ? 'disabled="disabled"' : ''} />
        </form>
        <c:if test="${empty configuredPatterns}">
            <p><em>Button disabled: No Jahia deprecated patterns configured.</em></p>
        </c:if>
    </fieldset>

    <div class="section-separator"></div>

    <!-- Custom Pattern Analysis Section -->
    <fieldset>
        <legend><strong>Option 2:</strong> Analyze Custom Pattern (Ad-hoc Testing)</legend>
        <p>
            Test specific package patterns without pre-configuration. This is useful for one-time checks,
            exploring dependencies, or analyzing packages from any provider bundle.
        </p>

        <form id="customAnalysis" action="?" method="get">
            <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>

            <label for="customPattern"><strong>Package Pattern (Regular Expression):</strong></label>
            <br/>
            <input type="text" name="customPattern" id="customPattern" style="width: 600px;"
                   value="${not empty param.customPattern ? param.customPattern : ''}"
                   placeholder="e.g., org\.springframework\..*  or  javax\.servlet\..*  or  com\.google\.common\\..*" />
            <br/>

            <label for="providerBundle" style="margin-top: 10px; display: inline-block;"><strong>Filter by Provider Bundle (Optional):</strong></label>
            <br/>
            <input type="text" name="providerBundle" id="providerBundle" style="width: 600px;"
                   value="${not empty param.providerBundle ? param.providerBundle : ''}"
                   placeholder="e.g., org.apache.felix.framework (leave empty to show wires from all providers)" />
            <br/>

            <input type="submit" name="analyze" value="Run Custom Pattern Analysis"
                   title="Analyze using the custom pattern you provided" style="margin-top: 10px;" />
        </form>
    </fieldset>

    <!-- Combined Results Section -->
    <c:if test="${not empty param.analyze}">
        <%
            FindWires wireResult = null;
            String analysisType = null;
            String customPattern = null;
            String providerBundle = null;
            String analysisError = null;

            // Determine which analysis to run
            if (request.getParameter("customPattern") != null && !request.getParameter("customPattern").trim().isEmpty()) {
                // Custom pattern analysis
                customPattern = request.getParameter("customPattern");
                providerBundle = request.getParameter("providerBundle");
                analysisType = "Custom Pattern";

                try {
                    wireResult = OSGIAnalyzer.findWires(Arrays.asList(customPattern), providerBundle);
                } catch (Exception e) {
                    analysisError = e.getMessage();
                }
            } else {
                // Jahia deprecated packages analysis
                analysisType = "Jahia Deprecated Packages";
                wireResult = OSGIAnalyzer.findDeprecatedWires();
            }

            pageContext.setAttribute("wireResult", wireResult);
            pageContext.setAttribute("analysisType", analysisType);
            pageContext.setAttribute("customPattern", customPattern);
            pageContext.setAttribute("providerBundle", providerBundle);
            pageContext.setAttribute("analysisError", analysisError);
        %>

        <c:choose>
            <c:when test="${not empty analysisError}">
                <div class="default">
                    <fieldset>
                        <legend style="color: red;"><strong>❌ Analysis Error</strong></legend>
                        <p style="color: red;">
                            <strong>Invalid pattern:</strong> ${analysisError}
                        </p>
                    </fieldset>
                </div>
            </c:when>
            <c:when test="${empty wireResult}">
                <div class="default">
                    <fieldset style="border-color: orange;">
                        <legend style="color: orange;"><strong>⚠ No Pattern Provided</strong></legend>
                        <p>Please enter a package pattern in the custom pattern field or use the Jahia deprecated packages analysis.</p>
                    </fieldset>
                </div>
            </c:when>
            <c:otherwise>
                <!-- Results Section -->
                <div class="live">
                    <fieldset>
                        <legend>
                            <strong>${analysisType} Analysis Results: (${wireResult.totalCount})</strong> Matching Dependencies Found
                        </legend>

                        <c:if test="${analysisType == 'Custom Pattern' && not empty customPattern}">
                            <p><strong>Analyzed Pattern:</strong> <code>${customPattern}</code></p>
                            <c:if test="${not empty providerBundle}">
                                <p><strong>Provider Bundle Filter:</strong> <code>${providerBundle}</code></p>
                            </c:if>
                            <c:if test="${empty providerBundle}">
                                <p><em>No provider bundle filter applied - showing wires from all providers.</em></p>
                            </c:if>
                        </c:if>

                        <c:if test="${analysisType == 'Jahia Deprecated Packages'}">
                            <p><em>Filtered to show only packages from <code>org.apache.felix.framework</code> (Jahia framework).</em></p>
                        </c:if>

                        <c:choose>
                            <c:when test="${empty wireResult.bundles}">
                                <p style="color: green; font-weight: bold;">
                                    ✓ No matching dependencies found! None of your Jahia modules use these packages.
                                </p>
                                <p><em>This means your modules are either:</em></p>
                                <ul>
                                    <li>Not importing the analyzed packages at all</li>
                                    <li>Importing them but not actually using them at runtime</li>
                                    <c:if test="${not empty providerBundle || analysisType == 'Jahia Deprecated Packages'}">
                                        <li>Using these packages from a different provider bundle</li>
                                    </c:if>
                                </ul>
                            </c:when>
                            <c:otherwise>
                                <p style="color: orange; font-weight: bold;">
                                    ⚠ Found ${wireResult.totalCount} matching package wire(s) in ${wireResult.bundles.size()} module(s).
                                </p>
                                <p><em>The modules listed below have <strong>active runtime dependencies</strong> on the analyzed packages.
                                    This means they are actively loading and using classes from these packages.</em></p>

                                <p><strong>Affected Modules:</strong></p>
                                <ul>
                                    <c:forEach items="${wireResult.bundles}" var="entry">
                                        <li>
                                            <a href="<c:url value='/tools/osgi/console/bundles/${entry.id}'/>"
                                               title="View bundle details in OSGI console"
                                               target="_blank">
                                                [${entry.id}]
                                            </a>
                                            <strong>${entry.displayName}</strong>
                                            <strong style="color: red;">(${entry.matchingWires.size()} matching wire<c:if test="${entry.matchingWires.size() > 1}">s</c:if>)</strong>

                                            <details style="margin-left: 20px;">
                                                <summary style="cursor: pointer;">
                                                    Show package dependencies (${entry.matchingWires.size()})
                                                </summary>
                                                <ul>
                                                    <c:forEach items="${entry.matchingWires}" var="wire">
                                                        <li><code>${wire}</code></li>
                                                    </c:forEach>
                                                </ul>
                                                <p><em>Each line shows: <code>package.name (from provider-bundle [bundle-id])</code></em></p>
                                            </details>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </c:otherwise>
                        </c:choose>
                    </fieldset>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>
</div>

<div class="section-separator"></div>

<!-- Hidden fancybox content areas -->
<div style="display: none;">
    <!-- Tool information popup -->
    <div id="toolInfoArea">
        <h3>ℹ️ What does this tool do?</h3>
        <p>
            This tool helps you identify which Jahia modules are using specific Java packages or classes.
            It analyzes the <strong>actual runtime dependencies</strong> (called "wires" in OSGI terminology)
            between modules, including dynamically loaded classes.
        </p>
        <h4>Use cases:</h4>
        <ul>
            <li>Find modules using deprecated APIs (e.g., old Spring Framework versions)</li>
            <li>Identify dependencies on specific libraries before upgrades</li>
            <li>Audit Java package usage across your modules</li>
            <li>Detect potential compatibility issues</li>
        </ul>
        <h4>What is an OSGI wire?</h4>
        <p>
            A "wire" represents a connection between two modules where one module uses Java classes from another module.
            When your module imports and uses classes like <code>org.springframework.web.servlet.ModelAndView</code>,
            OSGI creates a "wire" connecting your module to the Spring module that provides those classes.
        </p>
    </div>

    <!-- Configured patterns popup -->
    <c:if test="${not empty configuredPatterns}">
        <div id="configuredPatternsArea">
            <h3>Currently configured Jahia deprecated patterns</h3>
            <p>These patterns are provided by Jahia via configuration files:</p>
            <ul>
                <c:forEach items="${configuredPatterns}" var="pattern">
                    <li><code>${pattern}</code></li>
                </c:forEach>
            </ul>
            <p><em>Configuration PID: <code>org.jahia.modules.tools.deprecation-*</code></em></p>
        </div>
    </c:if>
</div>

<%@ include file="commons/footer.jspf" %>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/fancybox.tools.bundle.js'/>"></script>
</body>
</html>
