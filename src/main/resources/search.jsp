<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="java.io.File"%>
<%@page import="java.io.FileFilter"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="org.apache.commons.io.filefilter.DirectoryFileFilter"%>
<%@page import="org.jahia.settings.SettingsBean"%>
<%@page import="org.jahia.services.search.spell.CompositeSpellChecker"%>
<%@ page import="org.jahia.services.content.JCRSessionFactory" %>
<%@ page import="org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository" %>
<%@ page import="org.apache.jackrabbit.core.JahiaRepositoryImpl" %>
<%@ page import="org.apache.jackrabbit.core.state.ItemStateException" %>
<%@ page import="org.apache.jackrabbit.core.state.NoSuchItemStateException" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="css.jspf" %>
    <title>Search Engine Manager</title>
    <link type="text/css" href="<c:url value='/modules/assets/css/jquery.fancybox.css'/>" rel="stylesheet"/>
    <script type="text/javascript" src="<c:url value='/modules/jquery/javascript/jquery.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/modules/assets/javascript/jquery.fancybox.pack.js'/>"></script>
    <script type="text/javascript">
        $(document).ready(function() {
            $('.detailsLink').fancybox({
                        'hideOnContentClick': false,
                        'titleShow' : false,
                        'transitionOut' : 'none'
                    });
        });
    </script>
</head>
<body>
<h1>Search Engine Management</h1>
<c:if test="${not empty param.action}">
	<c:choose>
		<c:when test="${param.action == 'updateSpellCheckerIndex'}">
			<% CompositeSpellChecker.updateSpellCheckerIndex(); %>
			<p style="color: blue">Spell checker index update triggered</p>
		</c:when>
        <c:when test="${param.action == 'reindex'}">
            <% FileUtils.touch(new File(SettingsBean.getInstance().getRepositoryHome(), "reindex")); %>
            <p style="color: blue">Re-indexing of the repository content will be done on next Digital Experience Manager startup</p>
        </c:when>
        <c:when test="${param.action == 'reindex-undo'}">
            <% new File(SettingsBean.getInstance().getRepositoryHome(), "reindex").delete(); %>
            <p style="color: blue">Re-indexing of the repository content undone</p>
        </c:when>
        <c:when test="${param.action == 'reindex-now'}">
        	<c:if test="${param.ws == 'all'}">
            	<% ((JahiaRepositoryImpl)((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository()).scheduleReindexing(); %>
            </c:if>
        	<c:if test="${param.ws != 'all'}">
            	<% ((JahiaRepositoryImpl)((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository()).scheduleReindexing(request.getParameter("ws")); %>
            </c:if>
            <p style="color: blue">Re-indexing of the repository content will be done now</p>
        </c:when>
        <c:when test="${param.action == 'reindex-tree'}">
            <c:if test="${(param.ws == 'default' || param.ws == 'live') && not empty param.uuid}">
                <%
                long treeReindexStartTime = System.currentTimeMillis();
                ((JahiaRepositoryImpl)((SpringJackrabbitRepository) JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository()).reindexTree(request.getParameter("uuid"), request.getParameter("ws"));
                %>
                <p style="color: blue">Re-indexing tree in workspace <strong>${fn:escapeXml(param.ws)}</strong>, starting from node <strong>${fn:escapeXml(param.uuid)}</strong> has been completed in <strong><%= System.currentTimeMillis() - treeReindexStartTime %></strong> ms</p>
            </c:if>
        </c:when>
        <c:when test="${param.action == 'index-fix'}">
            <% FileUtils.touch(new File(SettingsBean.getInstance().getRepositoryHome(), "index-fix")); %>
            <p style="color: blue">Repository indexes check and fix will be done on next Digital Experience Manager startup</p>
        </c:when>
        <c:when test="${param.action == 'index-fix-undo'}">
            <% new File(SettingsBean.getInstance().getRepositoryHome(), "index-fix").delete(); %>
            <p style="color: blue">Repository indexes check and fix undone</p>
        </c:when>
        <c:when test="${param.action == 'index-check'}">
            <% FileUtils.touch(new File(SettingsBean.getInstance().getRepositoryHome(), "index-check")); %>
            <p style="color: blue">Repository indexes check (no repair) will be done on next Digital Experience Manager startup</p>
        </c:when>
        <c:when test="${param.action == 'index-check-undo'}">
            <% new File(SettingsBean.getInstance().getRepositoryHome(), "index-check").delete(); %>
            <p style="color: blue">Repository indexes check (no repair) undone</p>
        </c:when>
        <c:when test="${param.action == 'index-check-physical'}">
            <% long actionTime = System.currentTimeMillis(); %>
            <p style="color: blue">Start checking indexes for repository home <%= SettingsBean.getInstance().getRepositoryHome() %> (<%= org.jahia.utils.FileUtils.humanReadableByteCount(FileUtils.sizeOfDirectory(SettingsBean.getInstance().getRepositoryHome())) %>)</p>
            <jsp:include page="/modules/tools/searchIndexCheck.jsp">
                <jsp:param name="indexPath" value="index"/>
            </jsp:include>
            <jsp:include page="/modules/tools/searchIndexCheck.jsp">
                <jsp:param name="indexPath" value="workspaces/default/index"/>
            </jsp:include>
            <jsp:include page="/modules/tools/searchIndexCheck.jsp">
                <jsp:param name="indexPath" value="workspaces/live/index"/>
            </jsp:include>
            <%  pageContext.setAttribute("took", Long.valueOf(System.currentTimeMillis() - actionTime)); %>
            <p style="color: blue">...done in ${took} ms</p>
        </c:when>
	</c:choose>
</c:if>
<fieldset>
<legend>Immediate</legend>
<ul>
    <li><a href="?action=reindex-now&ws=all&toolAccessToken=${toolAccessToken}" onclick="return confirm('This will schedule a background task for re-indexing content of the whole repository. Would you like to continue?')">Whole repository re-indexing</a> - Do whole repository (live, default and system indexes + spellchecker) re-indexing now</li>
    <li><a href="?action=reindex-now&ws=live&toolAccessToken=${toolAccessToken}" onclick="return confirm('This will schedule a background task for re-indexing content of the live repository workspace. Would you like to continue?')">Live repository re-indexing</a> - Do repository re-indexing now</li>
    <li><a href="?action=reindex-now&ws=default&toolAccessToken=${toolAccessToken}" onclick="return confirm('This will schedule a background task for re-indexing content of the default repository workspace. Would you like to continue?')">Default repository re-indexing</a> - Do repository re-indexing now</li>
    <li><a href="?action=reindex-now&toolAccessToken=${toolAccessToken}" onclick="return confirm('This will schedule a background task for re-indexing content of the system repository. Would you like to continue?')">System repository re-indexing</a> - Do repository re-indexing now</li>
    <li><a href="?action=updateSpellCheckerIndex&toolAccessToken=${toolAccessToken}" onclick="return confirm('This will schedule a background task for updating the spellchecker index for live and default workspaces. Would you like to continue?')">Spell checker index update</a> - triggers an immediate update (no restart needed) of the spell checker dictionary index used by the "Did you mean" search feature</li>
    <li>
        <a href="#reindex-tree" onclick="var cbW = document.getElementById('reindexTreeWorkspace'); var ws = cbW.options[cbW.selectedIndex].value; var uuid = document.getElementById('reindexTreeUuid').value; if (uuid.length == 0) { alert('You have not provided the node UUID to start re-indexing from'); return false; } if (confirm('This will execute (synchronously) a re-indexing of the JCR sub-tree in the specified workspace, starting with the specified node. Would you like to continue?')) { this.href='?action=reindex-tree&amp;ws=' + ws + '&amp;uuid=' + uuid + '&amp;toolAccessToken=${toolAccessToken}'; return true; } else { return false; }">Re-index the sub-tree</a>
        &nbsp;&nbsp;
        <label for="reindexTreeWorkspace">workspace:&nbsp;</label>
        <select id="reindexTreeWorkspace" name="reindexTreeWorkspace">
            <option value="default">default</option>
            <option value="live" ${param.ws == 'live' ? 'selected="selected"' : ''}>live</option>
        </select>
        &nbsp;&nbsp;
        <label for="reindexTreeUuid">start with node (UUID):&nbsp;</label><input type="text" id="reindexTreeUuid" name="reindexTreeUuid" value="${not empty param.uuid ? fn:escapeXml(param.uuid) : ''}" style="width: 270px"/>
        <a title="Lookup UUID in JCR Browser" href="<c:url value='jcrBrowser.jsp'/>" target="_blank">
            <img src="<c:url value='/icons/search.png'/>" width="16"height="16" alt="lookup" title="Lookup UUID in JCR Browser">
        </a>
        
    </li>
</ul>
</fieldset>
<fieldset>
<legend>On next Digital Experience Manager startup</legend>
<ul>
    <li>
    <% pageContext.setAttribute("markerExists", new File(SettingsBean.getInstance().getRepositoryHome(), "reindex").exists()); %>
    <c:if test="${markerExists}">
    	<a href="?action=reindex-undo&toolAccessToken=${toolAccessToken}">Undo repository re-indexing</a> - Remove marker file to skip repository re-indexing on the next Digital Experience Manager start
    </c:if>
    <c:if test="${!markerExists}">
    	<a href="?action=reindex&toolAccessToken=${toolAccessToken}">Repository re-indexing</a> - Do repository re-indexing on the next Digital Experience Manager start
    </c:if>
    </li>
    <li>
    <% pageContext.setAttribute("markerExists", new File(SettingsBean.getInstance().getRepositoryHome(), "index-fix").exists()); %>
    <c:if test="${markerExists}">
    	<a href="?action=index-fix-undo&toolAccessToken=${toolAccessToken}">Undo repository index check and fix</a> - Remove marker file to skip repository search indexes logical check and fix inconsistencies on the next Digital Experience Manager start
    </c:if>
    <c:if test="${!markerExists}">
    	<a href="?action=index-fix&toolAccessToken=${toolAccessToken}">Repository index check and fix</a> - Do repository search indexes logical check and fix inconsistencies on the next Digital Experience Manager start
    </c:if>
    </li>
    <li>
    <% pageContext.setAttribute("markerExists", new File(SettingsBean.getInstance().getRepositoryHome(), "index-check").exists()); %>
    <c:if test="${markerExists}">
    	<a href="?action=index-check-undo&toolAccessToken=${toolAccessToken}">Undo repository index check (no repair)</a> - Remove marker file to skip repository search indexes logical check just reporting inconsistencies in the log on the next Digital Experience Manager start
    </c:if>
    <c:if test="${!markerExists}">
    	<a href="?action=index-check&toolAccessToken=${toolAccessToken}">Repository index check (no repair)</a> - Do repository search indexes logical check just reporting inconsistencies in the log on the next Digital Experience Manager start
    </c:if>
    </li>
</ul>
</fieldset>
<fieldset>
<legend>Index health</legend>
<ul>
    <li><a href="?action=index-check-physical&toolAccessToken=${toolAccessToken}">Repository index physical check</a> - Do immediate repository search indexes physical consistency check and print out the results (Lucene CheckIndex tool)</li>

</ul>
</fieldset>
<%@ include file="gotoIndex.jspf" %>
</body>
</html>