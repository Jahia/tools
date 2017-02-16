<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" 
%><%@ page import="java.io.File, org.jahia.modules.tools.SupportInfoHelper" %><% File targetDir = new File(System.getProperty("jahia.log.dir"), "jahia-support").getCanonicalFile(); 
%><c:if test="${param.action == 'download' || param.action == 'server'}"><% SupportInfoHelper.exportInfo(targetDir, request, response); %></c:if><c:if test="${param.action != 'download'}"><%@ page contentType="text/html;charset=UTF-8" language="java"
%><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ page import="org.jahia.tools.jvm.ThreadMonitor" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@ include file="css.jspf" %>
<script type="text/javascript" src="<c:url value='/modules/jquery/javascript/jquery.min.js'/>"></script>
<title>Export Support Information</title>
</head>
<body>
<h1>Export Support Information</h1>
<p>Allows you to export as a ZIP file useful support data about this DX instance.<br/>
Please, note, that no information is sent directly to Jahia support when using this action and no sensitive information (usernames, passwords, etc.) is included into the ZIP file.<br/>
You will be able to view the resulted file.
</p>
<c:if test="${not empty generatedInfo}">
<p style="color: blue">
Support information exported in ${fn:escapeXml(generationTime)} ms to file: <strong>${fn:escapeXml(generatedInfo)}</strong>
</p>
</c:if>

<% pageContext.setAttribute("allProbes", SupportInfoHelper.getProbes()); %>
<form id="support" action="<c:url value='support.jsp'/>" method="get">
<p>
<a href="#all-on" title="Select all" onclick="$('.cbProbe').prop('checked', true); return false;">select all</a> | <a href="#all-off" title="Unselect all" onclick="$('.cbProbe').prop('checked', false); return false;">unselect all</a>
<c:forEach var="probesPerCategory" items="${allProbes}">
<fieldset>
    <legend>&nbsp;${fn:escapeXml(probesPerCategory.key == 'jcr' ? 'JCR' : functions:capitalize(probesPerCategory.key))}&nbsp;
    (<a href="#all-on" title="Select all" onclick="$('.cbProbe.category-${probesPerCategory.key}').prop('checked', true); return false;">all</a> | <a href="#all-off" title="Unselect all" onclick="$('.cbProbe.category-${probesPerCategory.key}').prop('checked', false); return false;">none</a>) 
    </legend>
    <c:forEach var="probe" items="${probesPerCategory.value}">
        <c:set var="probeKey" value="${probe.category}|${probe.key}"/>
        <input type="checkbox" name="${probeKey}" id="${probe.category}-${probe.key}" class="cbProbe category-${probe.category}" ${empty param.do || not empty param[probeKey] ? 'checked="checked"' : ''}/><label for="${probe.category}-${probe.key}">${fn:escapeXml(probe.name)}</label><br/>
    </c:forEach>
</fieldset>
</c:forEach>
<fieldset>
    <legend>&nbsp;Configuration files&nbsp;
    (<a href="#all-on" title="Select all" onclick="$('.cbProbe.category-cfg').prop('checked', true); return false;">all</a> | <a href="#all-off" title="Unselect all" onclick="$('.cbProbe.category-cfg').prop('checked', false); return false;">none</a>) 
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
<%@ include file="gotoIndex.jspf" %>
</body>
</html>
</c:if>