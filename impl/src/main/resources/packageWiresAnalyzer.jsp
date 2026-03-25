<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.OSGiAnalyzer" %>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.PackageWire" %>
<%@ page import="org.jahia.modules.tools.config.DeprecatedPackageWiresConfig" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.jahia.osgi.FrameworkService" %>
<%@ page import="org.osgi.framework.Bundle" %>
<%@ page import="com.fasterxml.jackson.core.type.TypeReference" %>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="com.fasterxml.jackson.core.JsonParser" %>
<%@ page import="com.fasterxml.jackson.core.exc.StreamReadException" %>
<%@ page import="java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="OSGi Java package wires analyzer"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
    <style>
        /* ── form ───────────────────────────────────────────── */
        .radio-group { margin: 8px 0 12px; }
        .radio-group label { margin-left: 6px; cursor: pointer; }
        .custom-fields { margin-top: 10px; padding: 10px 14px; background: #f9f9f9;
                         border: 1px dashed #ccc; border-radius: 4px; }

        /* mapping editor */
        .mapping-block { border: 1px solid #c8d8ea; border-radius: 4px;
                         background: #fff; margin-bottom: 10px; overflow: hidden; }
        .mapping-header { display: flex; align-items: center; gap: 8px;
                          background: #eaf0f8; padding: 6px 10px;
                          border-bottom: 1px solid #c8d8ea; }
        .mapping-header select { flex: 1; font-family: monospace; font-size: 12px;
                                 padding: 3px 4px; border: 1px solid #bbb; border-radius: 3px; }
        .mapping-header .lbl { font-weight: bold; font-size: 12px;
                               white-space: nowrap; color: #1a3a5c; }
        .mapping-body { padding: 8px 10px; }
        .pattern-row { display: flex; align-items: center; gap: 6px; margin-bottom: 5px; }
        .pattern-row input[type=text] { flex: 1; font-family: monospace; font-size: 12px;
                                        padding: 3px 6px; border: 1px solid #bbb; border-radius: 3px; }
        .btn-add-pattern { font-size: 12px; padding: 2px 8px; cursor: pointer;
                           background: #e8f4e8; border: 1px solid #88c888;
                           border-radius: 3px; color: #2a602a; }
        .btn-add-pattern:hover { background: #d0ecd0; }
        .btn-add-mapping { font-size: 12px; padding: 4px 12px; cursor: pointer;
                           background: #e8f0f8; border: 1px solid #6a9fd8;
                           border-radius: 3px; color: #1a3a5c; margin-top: 4px; }
        .btn-add-mapping:hover { background: #d4e4f4; }
        .btn-remove { font-size: 13px; line-height: 1; padding: 1px 6px; cursor: pointer;
                      background: #fde8e8; border: 1px solid #f0a0a0;
                      border-radius: 3px; color: #900; }
        .btn-remove:hover { background: #fcd0d0; }

        /* ── results ─────────────────────────────────────────── */
        .results { margin-top: 20px; }
        .results-header { display: flex; justify-content: space-between;
                          align-items: baseline; margin-bottom: 10px; }
        .badge { font-size: 12px; border-radius: 10px; padding: 2px 10px; border: 1px solid; }
        .badge-warn { background: #fff0cc; border-color: #e0a000; color: #7a5000; }
        .badge-ok   { background: #e0f0e0; border-color: #60a060;  color: #2a602a; }
        .badge-err  { background: #ffe8e8; border-color: #f0a0a0;  color: #900; }

        /* grouped table */
        .wires-table { border-collapse: collapse; width: 100%; font-size: 12px; }
        .wires-table th { background: #e8e8e8; border: 1px solid #bbb;
                          padding: 6px 8px; text-align: left; white-space: nowrap; }
        .group-header td { background: #eaf0f8; color: #1a3a5c;
                           padding: 6px 10px; font-weight: bold;
                           border-top: 2px solid #6a9fd8;
                           border-bottom: 1px solid #b8d0ea; }
        .group-header a { color: #1a3a5c; text-decoration: none; }
        .group-header a:hover { text-decoration: underline; }
        .group-header .wire-count { float: right; font-size: 11px;
                                    font-weight: normal; background: #c00;
                                    color: #fff; border-radius: 8px; padding: 1px 8px; }
        .data-row td { border: 1px solid #e0e0e0; padding: 4px 10px; }
        .data-row:hover td { background: #fff8e8; }
        .data-row td:first-child { padding-left: 24px; }
        .symbolic { font-size: 10px; font-weight: normal; color: #5a7a9a; margin-left: 4px; }
        .provider-cell { color: #444; }
        .provider-cell a { color: #00509e; text-decoration: none; }
        .provider-cell a:hover { text-decoration: underline; }
    </style>
</head>

<body>
<%@ include file="commons/header.jspf" %>

<%-- ═══════════════════════════════════════════════════════════════
     PREPARE DATA
     ═══════════════════════════════════════════════════════════════ --%>
<%
    /* All bundles — used to populate the provider dropdown */
    Bundle[] allBundles = FrameworkService.getBundleContext().getBundles();
    Arrays.sort(allBundles, new Comparator<Bundle>() {
        public int compare(Bundle a, Bundle b) {
            return Long.compare(a.getBundleId(), b.getBundleId());
        }
    });
    pageContext.setAttribute("allBundles", allBundles);

    /* Pre-build display labels for the dropdown — avoids EL touching Dictionary */
    LinkedHashMap<Long, String> bundleLabels = new LinkedHashMap<Long, String>();
    for (Bundle b : allBundles) {
        String symName  = b.getSymbolicName() == null ? "" : b.getSymbolicName();
        String bundleName = b.getHeaders().get("Bundle-Name");
        String label = "[" + b.getBundleId() + "] " + (bundleName != null ? bundleName : symName) + " (" + symName + ")";
        bundleLabels.put(b.getBundleId(), label);
    }
    pageContext.setAttribute("bundleLabels", bundleLabels);

    /* Deprecation config */
    DeprecatedPackageWiresConfig deprecatedPackageWiresConfig = BundleUtils.getOsgiService(DeprecatedPackageWiresConfig.class, null);
    pageContext.setAttribute("patternsByProvider", deprecatedPackageWiresConfig.getDeprecatedPatternsByProvider());
%>

<div>
<p><a data-src="#toolInfoArea" data-fancybox>
    <img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="help"/> What does this tool do?
</a></p>

<%-- ═══════════════════════════════════════════════════════════════
     FORM
     ═══════════════════════════════════════════════════════════════ --%>
<fieldset>
    <legend>Analysis parameters</legend>

    <form id="wiresForm" action="?" method="get">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <%-- Preserve showInternal across form submissions --%>
        <c:if test="${not empty param.showInternal}">
            <input type="hidden" name="showInternal" value="${param.showInternal}"/>
        </c:if>

        <div class="radio-group">
            <strong>Mode:</strong><br/>
            <input data-test-id="mode" type="radio" name="mode" id="modeDeprecated" value="deprecated"
                   ${(empty param.mode || param.mode == 'deprecated') ? 'checked' : ''}
                   onchange="document.getElementById('customFields').style.display='none'"/>
            <label for="modeDeprecated">
                Jahia deprecated packages
                <em style="font-weight:normal;color:#666;">(pre-configured via <code>org.jahia.modules.tools.deprecatedpackagewires-*</code> files)</em>
            </label>
            <c:if test="${not empty patternsByProvider}">
                <div style="margin-top:8px; margin-left:20px;">
                    <details data-test-id="configured-patterns-details" style="display:inline-block;">
                        <summary data-test-id="configured-patterns-summary" style="cursor:pointer; user-select:none; font-size:11px; color:#0066cc; text-decoration:underline;">
                            View configured patterns
                        </summary>
                        <div style="margin-top:8px;">
                            <table data-test-id="configured-patterns-table" style="border-collapse:collapse; font-size:11px; background:#fafafa; border:1px solid #ddd;">
                                <tbody>
                                <c:forEach items="${patternsByProvider}" var="entry">
                                    <c:set var="patterns" value="${entry.value}"/>
                                    <c:forEach items="${patterns}" var="pattern" varStatus="patternStatus">
                                        <tr>
                                            <c:if test="${patternStatus.first}">
                                                <td data-test-id="configured-patterns-provider"
                                                    style="padding:4px 8px; border:1px solid #ddd; font-weight:bold; background:#f0f0f0; vertical-align:top; white-space:nowrap;">
                                                    ${entry.key}
                                                </td>
                                            </c:if>
                                            <c:if test="${not patternStatus.first}">
                                                <td style="padding:4px 8px; border:1px solid #ddd;"></td>
                                            </c:if>
                                            <td data-test-id="configured-patterns-pattern"
                                                style="padding:4px 8px; border:1px solid #ddd; font-family:monospace; font-size:10px;">
                                                ${pattern}
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </details>
                </div>
            </c:if>
            <c:if test="${empty patternsByProvider}">
                <strong style="color:#c00; display:block; margin-top:4px;"> ⚠ no patterns configured. Prior to Jahia 8.2.4.0, the configuration files have to be provided in custom bundles/modules.</strong>
            </c:if>
            <br/>
            <input data-test-id="mode" type="radio" name="mode" id="modeCustom" value="custom"
                   ${param.mode == 'custom' ? 'checked' : ''}
                   onchange="document.getElementById('customFields').style.display='block'"/>
            <label for="modeCustom">Custom patterns</label>
        </div>

        <%-- Custom mapping editor --%>
        <div id="customFields" class="custom-fields"
             style="display:${param.mode == 'custom' ? 'block' : 'none'};">
            <p style="margin:0 0 10px;">
                Define one or more <strong>provider → patterns</strong> mappings.<br/>
                <em style="color:#666;">Select "(any provider)" to match wires from all bundles.</em>
            </p>
            <div id="mappings"></div>
            <button type="button" class="btn-add-mapping" data-test-id="custom-add-mapping-btn" onclick="addMapping()">＋ Add provider mapping</button>

            <%-- Hidden template select: cloned by JS into each mapping block --%>
            <select id="providerSelectTemplate" style="display:none;">
                <option value="">(any provider)</option>
                <c:forEach items="${allBundles}" var="b">
                    <option value="${b.symbolicName}">${bundleLabels[b.bundleId]}</option>
                </c:forEach>
            </select>
        </div>
        <%-- Internal modules indicator — only visible to those who know about ?showInternal=true --%>
        <c:if test="${param.showInternal}">
            <p data-test-id="internal-jahia-modules-included" style="font-size:11px; color:#7a5000">
                ⚠ Internal Jahia modules (<code>Jahia-GroupId: org.jahia.modules</code>) are <strong>included</strong>.
            </p>
        </c:if>
        <input type="submit" id="analyzeBtn" data-test-id="analyze-btn" name="analyze" value="Analyze"
               ${(empty patternsByProvider && param.mode != 'custom' && (empty param.mode || param.mode == 'deprecated')) ? 'disabled="disabled"' : ''}/>
        <span id="analyzeBtnHint">
        <c:if test="${empty patternsByProvider && param.mode != 'custom' && (empty param.mode || param.mode == 'deprecated')}">
            <em style="color:#999; font-size:11px; margin-left:8px;">Disabled: no Jahia deprecated patterns configured.</em>
        </c:if>
        </span>
    </form>
</fieldset>

<%-- ═══════════════════════════════════════════════════════════════
     RESULTS
     ═══════════════════════════════════════════════════════════════ --%>
<c:if test="${not empty param.analyze}">
<%
    List<PackageWire> wires = null;
    String analysisError = null;
    String mode = request.getParameter("mode");
    if (mode == null || mode.isEmpty()) mode = "deprecated";

    /* Internal flag: opt-in via ?showInternal=true — not advertised in the UI */
    boolean showInternal = "true".equalsIgnoreCase(request.getParameter("showInternal"));


    if ("custom".equals(mode)) {
        String params = request.getParameter("params");
        if (params == null || params.trim().isEmpty()) {
            analysisError = "Please define at least one provider mapping with at least one pattern.";
        } else {
            try {
                /* Parse compact format: [{"providerSymbolicName":["pattern1","pattern2"]},...]
                   Each array entry is a single-key object mapping provider -> [patterns]. */
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, List<String>>> parsed = mapper.readValue(
                        params, new TypeReference<List<Map<String, List<String>>>>() {});

                LinkedHashMap<String, Collection<String>> regexesByProvider = new LinkedHashMap<String, Collection<String>>();
                for (Map<String, List<String>> entry : parsed) {
                    for (Map.Entry<String, List<String>> kv : entry.entrySet()) {
                        List<String> patterns = new ArrayList<String>();
                        for (String p : kv.getValue()) {
                            if (p != null && !p.trim().isEmpty()) patterns.add(p.trim());
                        }
                        if (!patterns.isEmpty()) {
                            Collection<String> existing = regexesByProvider.get(kv.getKey());
                            if (existing == null) {
                                regexesByProvider.put(kv.getKey(), patterns);
                            } else {
                                existing.addAll(patterns);
                            }
                        }
                    }
                }

                if (regexesByProvider.isEmpty()) {
                    analysisError = "Please enter at least one non-empty pattern.";
                } else {
                    long startTime = System.currentTimeMillis();
                    wires = OSGiAnalyzer.getPackageWires(regexesByProvider, showInternal);
                    pageContext.setAttribute("elapsedMs", Long.valueOf(System.currentTimeMillis() - startTime));
                }
            } catch (com.fasterxml.jackson.core.JacksonException e) {
                analysisError = "Invalid parameters format: " + e.getOriginalMessage();
            } catch (Exception e) {
                analysisError = e.getMessage();
            }
        }
    } else {
        try {
            long startTime = System.currentTimeMillis();
            wires = OSGiAnalyzer.getDeprecatedPackageWires(showInternal);
            pageContext.setAttribute("elapsedMs", Long.valueOf(System.currentTimeMillis() - startTime));
        } catch (IllegalArgumentException e) {
            analysisError = "The deprecation patterns are misconfigured, check the org.jahia.modules.tools.deprecatedpackagewires-* configuration files. Error: " + e.getMessage();
        } catch (Exception e) {
            analysisError = e.getMessage();
        }
    }

    pageContext.setAttribute("wires", wires);
    pageContext.setAttribute("analysisError", analysisError);
    pageContext.setAttribute("analysisMode", mode);
    pageContext.setAttribute("showInternal", showInternal);
%>

    <div class="results">

        <c:choose>
        <%-- ── Error ── --%>
        <c:when test="${not empty analysisError}">
            <div class="results-header">
                <strong>Results</strong>
                <span class="badge badge-err" data-test-id="results-error-badge">❌ Error</span>
            </div>
            <p style="color:#900;" data-test-id="results-error-message"><strong>${analysisError}</strong></p>
        </c:when>

        <%-- ── Empty results ── --%>
        <c:when test="${empty wires}">
            <div class="results-header">
                <strong>Results — ${analysisMode == 'custom' ? 'custom patterns' : 'Jahia deprecated packages'}</strong>
                <span class="badge badge-ok">✓ No wires found in ${elapsedMs}ms</span>
            </div>
            <p style="color:#2a602a;">
                No Jahia modules are wired to the requested packages. They are either not importing
                these packages at all, or importing them from a different provider not covered by your filter.
            </p>
        </c:when>

        <%-- ── Results table ── --%>
        <c:otherwise>
            <%
                /* Group wires by requirer bundle id for the table rendering */
                List<PackageWire> wireList = (List<PackageWire>) pageContext.getAttribute("wires");
                LinkedHashMap<Long, List<PackageWire>> grouped = new LinkedHashMap<Long, List<PackageWire>>();
                for (PackageWire w : wireList) {
                    long rid = w.getRequirerBundle().getId();
                    List<PackageWire> bucket = grouped.get(rid);
                    if (bucket == null) {
                        bucket = new ArrayList<PackageWire>();
                        grouped.put(rid, bucket);
                    }
                    bucket.add(w);
                }
                pageContext.setAttribute("grouped", grouped);
            %>

            <div class="results-header">
                <strong>Results — ${analysisMode == 'custom' ? 'custom patterns' : 'Jahia deprecated packages'}</strong>
                <span class="badge ${analysisMode == 'custom' ? 'badge-ok' : 'badge-warn'}" data-test-id="results-badge">
                    ${analysisMode == 'custom' ? '✓' : '⚠'}
                    <span data-test-id="results-wire-count">${fn:length(wires)}</span> wire${fn:length(wires) > 1 ? 's' : ''} in
                    <span data-test-id="results-module-count">${fn:length(grouped)}</span> module${fn:length(grouped) > 1 ? 's' : ''}, retrieved in ${elapsedMs}ms
                </span>
            </div>

            <table class="wires-table" data-test-id="wires-table">
                <thead>
                    <tr>
                        <th>Package</th>
                        <th>Provider bundle</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${grouped}" var="group">
                        <%-- group.key = requirerBundleId, group.value = List<PackageWire> --%>
                        <c:set var="firstWire" value="${group.value[0]}"/>
                        <tr class="group-header" data-test-id="wires-requirer-group"
                            data-requirer-symbolic-name="${firstWire.requirerBundle.symbolicName}">
                            <td colspan="2">
                                <a href="<c:url value='/tools/osgi/console/bundles/${firstWire.requirerBundle.id}'/>"
                                   title="Open in OSGi console" target="_blank">[${firstWire.requirerBundle.id}]</a>
                                ${firstWire.requirerBundle.name}
                                <span class="symbolic">(${firstWire.requirerBundle.symbolicName})</span>
                                <span class="wire-count">${fn:length(group.value)} wire${fn:length(group.value) > 1 ? 's' : ''}</span>
                            </td>
                        </tr>
                        <c:forEach items="${group.value}" var="wire">
                            <tr class="data-row" data-test-id="wires-row"
                                data-package-name="${wire.packageName}"
                                data-provider-symbolic-name="${wire.providerBundle.symbolicName}">
                                <td><code data-test-id="wires-row-package">${wire.packageName}</code></td>
                                <td class="provider-cell">
                                    <a href="<c:url value='/tools/osgi/console/bundles/${wire.providerBundle.id}'/>"
                                       title="Open in OSGi console" target="_blank">[${wire.providerBundle.id}]</a>
                                    ${wire.providerBundle.name}
                                    <span class="symbolic" data-test-id="wires-row-provider">(${wire.providerBundle.symbolicName})</span>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
        </c:choose>

    </div><%-- .results --%>
</c:if>

</div>

<div style="display:none;">
    <div id="toolInfoArea">
        <h3>What does this tool do?</h3>
        <p>This tool identifies which Jahia modules are using specific Java packages at runtime.
           It analyzes the <strong>actual OSGi wires</strong> — live connections created by the
           framework when a bundle's import is satisfied by another bundle's export.</p>
        <h4>Modes</h4>
        <ul>
            <li><strong>Jahia deprecated packages</strong> — uses pre-configured patterns from
                <code>org.jahia.modules.tools.deprecatedpackagewires-*</code> YAML files to find modules
                still wired to packages scheduled for removal.</li>
            <li><strong>Custom patterns</strong> — ad-hoc analysis: define your own
                provider → regex-pattern mappings.</li>
        </ul>
        <h4>What is an OSGi wire?</h4>
        <p>A wire is a runtime binding between a consumer bundle (Import-Package) and the
           provider bundle that satisfies it (Export-Package). Only <em>active</em> Jahia modules
           are analyzed.</p>
    </div>

    <c:if test="${not empty patternsByProvider}">
        <div id="configuredPatternsArea">
            <h3>Configured Jahia deprecated patterns</h3>
            <c:forEach items="${patternsByProvider}" var="entry">
                <p><strong>${entry.key}</strong></p>
                <ul>
                    <c:forEach items="${entry.value}" var="pattern">
                        <li><code>${pattern}</code></li>
                    </c:forEach>
                </ul>
            </c:forEach>
            <p><em>Configuration PID: <code>org.jahia.modules.tools.deprecatedpackagewires-*</code></em></p>
        </div>
    </c:if>
</div>

<%@ include file="commons/footer.jspf" %>

<%-- ═══════════════════════════════════════════════════════════════
     JS — mapping editor
     ═══════════════════════════════════════════════════════════════ --%>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/fancybox.tools.bundle.js'/>"></script>
<script>
    let mappingCount = 0;

    function addMapping(providerValue, patterns) {
        const id = mappingCount++;

        const selectTemplate = document.getElementById('providerSelectTemplate');
        const select = selectTemplate.cloneNode(true);
        select.id = '';
        select.style.display = '';
        select.name = 'provider[]';
        select.className = 'mapping-select';
        select.setAttribute('data-test-id', 'custom-mapping-provider');
        if (providerValue) select.value = providerValue;

        const block = document.createElement('div');
        block.className = 'mapping-block';
        block.id = 'mapping-' + id;
        block.setAttribute('data-test-id', 'custom-mapping-block');

        const header = document.createElement('div');
        header.className = 'mapping-header';
        const lbl = document.createElement('span');
        lbl.className = 'lbl';
        lbl.textContent = 'Provider:';
        const removeBtn = document.createElement('button');
        removeBtn.type = 'button';
        removeBtn.className = 'btn-remove';
        removeBtn.title = 'Remove this mapping';
        removeBtn.textContent = '✕';
        removeBtn.onclick = () => block.remove();
        header.appendChild(lbl);
        header.appendChild(select);
        header.appendChild(removeBtn);

        const body = document.createElement('div');
        body.className = 'mapping-body';
        body.id = 'patterns-' + id;
        (patterns || ['']).forEach(function(p) {
            body.appendChild(createPatternRow(p));
        });
        const addPatternBtn = document.createElement('button');
        addPatternBtn.type = 'button';
        addPatternBtn.className = 'btn-add-pattern';
        addPatternBtn.textContent = '＋ pattern';
        addPatternBtn.onclick = function() { addPattern(id); };
        body.appendChild(addPatternBtn);

        block.appendChild(header);
        block.appendChild(body);
        document.getElementById('mappings').appendChild(block);
    }

    function createPatternRow(value) {
        const row = document.createElement('div');
        row.className = 'pattern-row';

        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'pattern-input';
        input.name = 'pattern[]';
        input.placeholder = 'e.g. org.springframework.*';
        input.value = value || '';
        input.setAttribute('data-test-id', 'custom-mapping-pattern');

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn-remove';
        btn.title = 'Remove pattern';
        btn.textContent = '✕';
        btn.onclick = function() { row.remove(); };

        row.appendChild(input);
        row.appendChild(btn);
        return row;
    }


    function addPattern(mappingId) {
        const body = document.getElementById('patterns-' + mappingId);
        const btn  = body.querySelector('.btn-add-pattern');
        body.insertBefore(createPatternRow(''), btn);
    }

    // Initialise: restore submitted values from mappingState, or start with one empty mapping
    (function init() {
        const mode = document.querySelector('input[name="mode"]:checked');
        if (mode && mode.value === 'custom') {
            const stateParam = new URLSearchParams(window.location.search).get('params');
            if (stateParam) {
                try {
                    const submitted = JSON.parse(stateParam);
                    if (submitted.length > 0) {
                        submitted.forEach(function(entry) {
                            var provider = Object.keys(entry)[0];
                            var patterns = entry[provider];
                            addMapping(provider, patterns.length > 0 ? patterns : ['']);
                        });
                    } else {
                        addMapping('', ['']);
                    }
                } catch(e) {
                    addMapping('', ['']);
                }
            } else {
                addMapping('', ['']);
            }
        }

        // Inject a single hidden JSON field with the full mapping state before submit,
        // so the server can restore it exactly on page reload.
        document.getElementById('wiresForm').addEventListener('submit', function() {
            var state = [];
            document.querySelectorAll('.mapping-block').forEach(function(block) {
                var provider = block.querySelector('.mapping-select').value;
                var patterns = [];
                block.querySelectorAll('.pattern-input').forEach(function(input) {
                    if (input.value.trim()) patterns.push(input.value.trim());
                });
                if (patterns.length > 0) {
                    var entry = {};
                    entry[provider] = patterns;
                    state.push(entry);
                }
            });
            var hidden = document.createElement('input');
            hidden.type = 'hidden';
            hidden.name = 'params';
            hidden.value = JSON.stringify(state);
            document.getElementById('wiresForm').appendChild(hidden);
        });

        document.querySelectorAll('input[name="mode"]').forEach(function(r) {
            r.addEventListener('change', function() {
                document.getElementById('customFields').style.display =
                    r.value === 'custom' && r.checked ? 'block' : 'none';

                // Re-evaluate the Analyze button: disabled only in deprecated mode
                // when no patterns are configured server-side.
                var noDeprecatedPatterns = ${empty patternsByProvider ? 'true' : 'false'};
                var btn  = document.getElementById('analyzeBtn');
                var hint = document.getElementById('analyzeBtnHint');
                if (r.value === 'custom') {
                    btn.disabled = false;
                    hint.innerHTML = '';
                } else if (noDeprecatedPatterns) {
                    btn.disabled = true;
                    hint.innerHTML = '<em style="color:#999;font-size:11px;margin-left:8px;">Disabled: no Jahia deprecated patterns configured.</em>';
                } else {
                    btn.disabled = false;
                    hint.innerHTML = '';
                }
            });
        });
    })();
</script>
</body>
</html>
