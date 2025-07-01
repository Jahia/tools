<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ page import="org.jahia.modules.tools.SupportInfoHelper, java.io.File" %><% File targetDir = new File(System.getProperty("jahia.log.dir"), "jahia-support").getCanonicalFile();
%><c:if test="${param.action == 'download' || param.action == 'server'}"><% SupportInfoHelper.exportInfo(targetDir, request, response); %></c:if><c:if test="${param.action != 'download'}"><%@ page contentType="text/html;charset=UTF-8" language="java"
%><%@ page contentType="text/html; charset=UTF-8" language="java" %>
    <!DOCTYPE html>
    <html>
    <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<c:set var="title" value="Export Support Information"/>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="commons/html_header.jspf" %>
    <script type="text/javascript">
        function selectAll(selector) {
            var ele = document.querySelectorAll(selector);
            for (var i = 0; i < ele.length; i++) {
                ele[i].checked = true;
            }
        }

        function deSelectAll(selector) {
            var ele = document.querySelectorAll(selector);
            for (var i = 0; i < ele.length; i++) {
                ele[i].checked = false;

            }
        }
    </script>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<p>Allows you to export as a ZIP file useful support data about this Jahia instance.</p>
<fieldset style="background-color:#dfe8f6;border-color:#c3dbee;color:#000">
	<legend><img src="<c:url value='/icons/warning.png'/>" height="16" width="16" alt="(!)" align="top"/> Caution</legend>
	Please, note, that no information is sent directly to Jahia support when using this action. You will be able to review and adjust the resulted file.<br/>
	We are filtering out Jahia sensitive information (usernames, passwords, etc.) before generating the ZIP file
	<a class="fancybox-link" title="Sensitive information" href="#infoArea" data-src="#infoArea" data-fancybox><img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="Sensitive information" title="Sensitive information"></a><br/>
	Please ensure that no custom sensitive information appear in the ZIP file (<code>jahia.properties</code>, etc.). If so, please hide them (using <code>***</code>) before sending the ZIP file to Jahia.
</fieldset>
<c:if test="${not empty generatedInfo}">
<p style="color: blue">
Support information exported in ${fn:escapeXml(generationTime)} ms to file: <strong>${fn:escapeXml(generatedInfo)}</strong>
</p>
</c:if>

<% pageContext.setAttribute("allProbes", SupportInfoHelper.getProbes()); %>
<form id="support" action="<c:url value='support.jsp'/>" method="get">
    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
<p>
<a href="#all-on" title="Select all" onclick="selectAll('.cbProbe'); return false;">select all</a> | <a href="#all-off" title="Unselect all" onclick="deSelectAll('.cbProbe'); return false;">unselect all</a>
<c:forEach var="probesPerCategory" items="${allProbes}">
<fieldset>
    <legend>&nbsp;${fn:escapeXml(probesPerCategory.key == 'jcr' ? 'JCR' : functions:capitalize(probesPerCategory.key))}&nbsp;
    (<a href="#all-on" title="Select all" onclick="selectAll('.cbProbe.category-${probesPerCategory.key}'); return false;">all</a> | <a href="#all-off" title="Unselect all" onclick="deSelectAll('.cbProbe.category-${probesPerCategory.key}'); return false;">none</a>)
    </legend>
    <c:forEach var="probe" items="${probesPerCategory.value}">
        <c:set var="probeKey" value="${probe.category}|${probe.key}"/>
        <input type="checkbox" name="${probeKey}" id="${probe.category}-${probe.key}" class="cbProbe category-${probe.category}" ${empty param.do || not empty param[probeKey] ? 'checked="checked"' : ''}/><label for="${probe.category}-${probe.key}">${fn:escapeXml(probe.name)}</label><br/>
    </c:forEach>
</fieldset>
</c:forEach>
<fieldset>
    <legend>&nbsp;Configuration files&nbsp;
    (<a href="#all-on" title="Select all" onclick="selectAll('.cbProbe.category-cfg'); return false;">all</a> | <a href="#all-off" title="Unselect all" onclick="deSelectAll('.cbProbe.category-cfg'); return false;">none</a>)
    </legend>
    <input type="checkbox" name="digital-factory-config" id="digital-factory-config" class="cbProbe category-cfg" ${empty param.do || not empty param['digital-factory-config'] ? 'checked="checked"' : ''}/><label for="digital-factory-config">digital-factory-config</label><br/>
    <input type="checkbox" name="digital-factory-data" id="digital-factory-data" class="cbProbe category-cfg" ${empty param.do || not empty param['digital-factory-data'] ? 'checked="checked"' : ''}/><label for="digital-factory-data">digital-factory-data (configuration only)</label><br/>
    <input type="checkbox" name="webapp" id="webapp" class="cbProbe category-cfg" ${empty param.do || not empty param['webapp'] ? 'checked="checked"' : ''}/><label for="webapp">&lt;dx-webapp-dir&gt; (configuration only)</label><br/>
</fieldset>
</p>
<p>
    <input type="radio" name="action" id="download" value="download" ${empty param.do || param.action == 'download' ? 'checked="checked"' : ''}/><label for="download">Download the generated ZIP file</label><br/>
    <input type="radio" name="action" id="server" value="server" ${param.action == 'server' ? 'checked="checked"' : ''}/><label for="server">Generate and keep the ZIP file on server&nbsp;*</label><br/>
</p>
<p>
    <input type="submit" name="do" value="Export"  title="Performs the export of the support information into a ZIP file" />
</p>
</form>

<p>------------------------------------------------------------------------------------------------------------------------------------<br/>
* - The support information ZIP will be located in the folder:
<pre>        <%= targetDir %></pre>
</p>
<%@ include file="commons/footer.jspf" %>
<div style="display: none;">
    <div id="infoArea">
        <h3>Sensitive information</h3>

        <h4>Excluded files</h4>
        <p>By default we exclude from the generated support ZIP file the following configuration files with sensitive data:
        <ul>
        	<li><code>&lt;digital-factory-data&gt;/karaf/etc/host.key</code></li>
        	<li><code>&lt;digital-factory-data&gt;/karaf/etc/keys.properties</code></li>
        	<li><code>&lt;digital-factory-data&gt;/karaf/etc/users.properties</code></li>
        </ul>
        </p>

        <h4>Filtered out information</h4>
        <p>
        The following values are replaced in the corresponding files with <code>***</code>:
        <ul>
        	<li>Database username/password: <code>&lt;dx-webapp-dir&gt;/META-INF/context.xml</code> - attributes <code>username</code> and <code>password</code></li>
        	<li>Mail server configuration: <code>&lt;dx-webapp-dir&gt;/WEB-INF/etc/repository/root-mail-server.xml</code> - attribute <code>j:uri</code></li>
        	<li>Jahia root user password hash: <code>&lt;dx-webapp-dir&gt;/WEB-INF/etc/repository/root-user.xml</code> - attribute <code>j:password</code></li>
        </ul>
        </p>
    </div>
</div>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/fancybox.tools.bundle.js'/>"></script>
</body>
</html>
</c:if>
