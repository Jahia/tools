<%@ page contentType="text/html;charset=UTF-8" language="java"
%><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.jahia.bin.Jahia"%>
<%@page import="org.jahia.bin.listeners.JahiaContextLoaderListener"%>
<%@page import="org.jahia.osgi.BundleUtils"%>
<%@page import="org.jahia.registries.ServicesRegistry"%>
<%@page import="org.jahia.utils.DateUtils"%>
<%@page import="org.apache.commons.lang.time.DurationFormatUtils"%>
<%@ page import="org.jahia.modules.tools.modules.ModuleToolsHelper" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"  prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@ include file="css.jspf" %>
<% pageContext.setAttribute("currentDate", new java.util.Date()); %>
<title>Jahia Support Tools</title>
</head>
<body>
<h1>Support Tools <span style="font-size:0.7em;">(<%= Jahia.getFullProductVersion() %>)</span></h1>
<div style="position: absolute; right: 20px; top: 7px; font-size:1.0em;">
    <% if (Jahia.isEnterpriseEdition() && BundleUtils.getBundleBySymbolicName("tools-ee", null) != null) {
        if (Boolean.getBoolean("cluster.activated")) {
            %>
            Current Node Id: <%= System.getProperty("cluster.node.serverId", "N/A") %>
            <%
        }
    } %>
    <br/>
    Uptime: <%= DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - JahiaContextLoaderListener.getStartupTime(), true, true) %><br/>
    Since: <%= new java.util.Date(JahiaContextLoaderListener.getStartupTime()) %>
</div>
<table width="100%" border="0">
    <tr>
        <td width="50%">
            <fieldset>
                <legend>Logout</legend>
                <ul>
                    <li><a href='${pageContext.request.contextPath}/cms/logout?redirect=${pageContext.request.contextPath}/start'>Logout</a></li>
                </ul>
            </fieldset>
        </td>
    </tr>
    <tr>
        <td width="50%" valign="top">
<fieldset>
    <legend>System and Maintenance</legend>
    <ul>
        <li><a href="systemInfo.jsp">System information</a></li>
        <li><a href="threadDumpMgmt.jsp">Thread state information</a></li>
        <li><a href="memoryInfo.jsp">Memory information</a></li>
        <li><a href="jcrSessions.jsp">JCR sessions information</a></li>
        <li><a href="maintenance.jsp">System maintenance</a></li>
        <li><a href="precompileServlet">JSP pre-compilation</a></li>
        <li><a href="benchmarks.jsp">System benchmarks</a></li>
        <li><a href="karaf.jsp">Karaf command line</a></li>
        <li><a href="support.jsp">Export support information</a></li>
    </ul>
</fieldset>
<fieldset>
    <legend>Logging (runtime only)</legend>
    <ul>
        <li><a href="log4jAdmin.jsp">Log4j administration</a></li>
        <li><a href="errorFileDumper.jsp">Error file dumper</a></li>
    </ul>
</fieldset>
<fieldset>
    <legend>Administration and Guidance</legend>
    <ul>
        <li><a href="<c:url value='/tools/osgi/console/'/>">OSGi console</a></li>
        <li><a href="importPackageChecker.jsp">OSGi Import-Package checker</a></li>
        <li><a href="modules.jsp">Modules list</a></li>
        <li><a href="jobadmin.jsp">Background job administration</a></li>
        <li><a href="search.jsp">Search engine management</a></li>
        <li><a href="dbQuery.jsp">DB query tool</a></li>
        <li><a href="groovyConsole.jsp">Groovy console</a></li>
        <li><a href="workflows.jsp">Workflow monitoring</a></li>
        <li><a href="rules.jsp">Business rules</a></li>
        <li><a href="provisioning.jsp">Run provisioning script</a></li>
    </ul>
</fieldset>
            <% pageContext.setAttribute("moduleTools", ModuleToolsHelper.getInstance().getTools()); %>
            <c:if test="${not empty moduleTools}">
                <fieldset>
                    <legend>Modules</legend>
                    <ul>
                        <c:forEach var="tool" items="${moduleTools}">
                            <li><a href="${pageContext.request.contextPath}${tool.path}">${tool.name}</a></li>
                        </c:forEach>
                    </ul>
                </fieldset>
            </c:if>

            <% if (Jahia.isEnterpriseEdition() && BundleUtils.getBundleBySymbolicName("tools-ee", null) != null) { %>
<jsp:include page="/modules/tools/indexEnterprise.jsp" />
<% } %>
        </td>

        <td width="50%" valign="top">
<fieldset>
    <legend>JCR Data</legend>
    <ul>
        <li><a href="jcrBrowser.jsp">JCR repository browser</a></li>
        <li><a href="jcrQuery.jsp">JCR query tool</a></li>
        <li><a href="jcrQueryStats.jsp">JCR query statistics</a></li>
        <li><a href="jcrConsole.jsp">JCR console</a></li>
        <li><a href="jcrGc.jsp">JCR DataStore garbage collection</a></li>
        <li><a href="jcrVersionHistory.jsp">JCR version history management</a></li>
        <li><a href="jcrIntegrityTools.jsp">JCR integrity tools</a></li>
        <li><a href="jcrExternalProviders.jsp">JCR external providers</a></li>
        <li><a href="jcrJarsCleanup.jsp">JCR OSGi jars cleanup</a></li>
    </ul>
</fieldset>
<fieldset>
    <legend>JCR Rendering</legend>
    <ul>
        <li><a href="modulesBrowser.jsp">Installed modules browser</a></li>
        <li><a href="definitionsBrowser.jsp">Installed definitions browser</a></li>
        <li><a href="renderFilters.jsp">Render filters</a></li>
        <li><a href="actions.jsp">Actions</a></li>
        <li><a href="choicelistInitializersRenderers.jsp">Choicelist initializers &amp; renderers</a></li>
        <li><a href="render.jsp">Render chain dump</a></li>
    </ul>
</fieldset>
<fieldset>
    <legend>Cache</legend>
    <ul>
        <li><a href="cache.jsp">Cache management</a></li>
        <li><a href="ehcache/ehcache_stats.jsp">Output cache statistics</a></li>
        <li><a href="ehcache/ehcache_cj.jsp">Output cache</a></li>
        <li><a href="ehcache/ehcache_cj_dep.jsp">Output dependencies cache</a></li>
        <fmt:formatDate var="currentTime" value="${currentDate}" pattern="yyyy_MM_dd_HH_mm_ss"/>
        <li><a href="ehcache/ehcache_dump.jsp" download="ehcache_dump_${currentTime}.xml">Dump output cache</a></li>
    </ul>
</fieldset>
<fieldset>
    <legend>Miscellaneous Tools</legend>
    <ul>
        <li><a href="pwdEncrypt.jsp">Password encryption</a></li>
        <li><a href="docConverter.jsp">Document converter</a></li>
        <li><a href="textExtractor.jsp">Document text extractor</a></li>
        <li><a href="wcagChecker.jsp">WCAG checker</a></li>
        <li><a href="rewrite-status">URL rewriting rules</a></li>
        <li><a href="ckeditorConfig.jsp">CKEditor configuration</a></li>
    </ul>
</fieldset>
        </td>
    </tr>
</table>
<p>&copy; Copyright 2002-<%= new SimpleDateFormat("yyyy").format(new Date()) %> Jahia Solutions Group SA - All rights reserved.</p>
</body>
</html>
