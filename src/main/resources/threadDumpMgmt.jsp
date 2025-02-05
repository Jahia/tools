<%@page import="org.jahia.services.SpringContextSingleton"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@page import="org.jahia.settings.SettingsBean"%>
<%@page import="org.jahia.tools.jvm.ThreadMonitor" %>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"  prefix="c" %>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@ include file="css.jspf" %>
<title>Thread Dump Management</title>
</head>
<body>
<%@ include file="logout.jspf" %>
<h1>Thread Dump Management</h1>
<c:if test="${empty param.threadDumpCount && (param.threadDump == 'sysout' || param.threadDump == 'file')}">
<% pageContext.setAttribute("outputFile", ThreadMonitor.getInstance().dumpThreadInfo("sysout".equals(request.getParameter("threadDump")), "file".equals(request.getParameter("threadDump")))); %>
<p style="color: blue">
Thread dump created<c:if test="${not empty outputFile}"> in a file:<br/><code>${outputFile}</code></c:if>
</p>
</c:if>
<c:if test="${not empty param.threadDumpCount && (param.threadDump == 'sysout' || param.threadDump == 'file')}">
<% pageContext.setAttribute("outputFile", ThreadMonitor.getInstance().dumpThreadInfoWithInterval("sysout".equals(request.getParameter("threadDump")), "file".equals(request.getParameter("threadDump")), Integer.parseInt(StringUtils.defaultIfEmpty(request.getParameter("threadDumpCount"), "10")), Integer.parseInt(StringUtils.defaultIfEmpty(request.getParameter("threadDumpInterval"), "10")))); %>
<p style="color: blue">
Thread dump task started<c:if test="${not empty outputFile}">. The output fill be done into a file:<br/><code>${outputFile}</code></c:if>
</p>
</c:if>
<c:if test="${not empty param.threadDumpMonitorActive}">
    <% ThreadMonitor.getInstance().setActivated(Boolean.valueOf(request.getParameter("threadDumpMonitorActive"))); %>
    <p style="color: blue">
    Thread Dump Monitor has been <strong>${param.threadDumpMonitorActive ? 'started' : 'stopped'}</strong>. 
    </p>
</c:if>
<c:if test="${not empty param.loadAverage && not empty param.threadDumpOnHighLoad}">
    <% pageContext.setAttribute("loadAverage", SpringContextSingleton.getBean(request.getParameter("loadAverage"))); %>
    <c:set target="${loadAverage}" property="threadDumpOnHighLoad" value="${param.threadDumpOnHighLoad ? true : false}"/>
    <p style="color: blue">
    Thread dump on high load for <strong>${loadAverage.displayName}</strong> has been <strong>${param.threadDumpOnHighLoad ? 'enabled' : 'disabled'}</strong>. 
    </p>
</c:if>
<a href="?refresh=true&toolAccessToken=${toolAccessToken}"><img src="<c:url value='/icons/refresh.png'/>" height="16" width="16" alt=" " align="top"/>Refresh</a>
<p>Active thread count: <strong><%=Thread.activeCount() %></strong></p>
<ul>
    <li><img src="<c:url value='/icons/filePreview.png'/>" height="16" width="16" alt=" " align="top"/>&nbsp;<a href="<c:url value='threadDump.jsp'/>" target="_blank">Perform thread dump (view in a new browser window)</a></li>
	<li><img src="<c:url value='/icons/download.png'/>" height="16" width="16" alt=" " align="top"/>&nbsp;<a href="<c:url value='threadDump.jsp?file=true&toolAccessToken=${toolAccessToken}'/>" target="_blank">Perform thread dump (download as a file)</a></li>
    <li><img src="<c:url value='/icons/tab-workflow.png'/>" height="16" width="16" alt=" " align="top"/>&nbsp;<a href="?threadDump=sysout&toolAccessToken=${toolAccessToken}">Perform thread dump (System.out)</a></li>
    <li><img src="<c:url value='/icons/globalRepository.png'/>" height="16" width="16" alt=" " align="top"/>&nbsp;<a href="?threadDump=file&toolAccessToken=${toolAccessToken}">Perform thread dump (File)</a>&nbsp;*</li>
    <li>
        <img src="<c:url value='/icons/workflowManager.png'/>" height="16" width="16" alt=" " align="top"/>&nbsp;
        <a href="#dump" onclick="this.href='?threadDump=file&amp;threadDumpCount=' + document.getElementById('threadDumpCount').value + '&amp;threadDumpInterval=' + document.getElementById('threadDumpInterval').value + '&amp;toolAccessToken=${toolAccessToken}'; return true;">Perform thread dump (multiple to a file)</a>&nbsp;*
        &nbsp;&nbsp;
        <label for="threadDumpCount">count:&nbsp;</label><input type="text" id="threadDumpCount" name="threadDumpCount" size="2" value="${not empty param.threadDumpCount ? param.threadDumpCount : '10'}"/>
        &nbsp;&nbsp;
        <label for="threadDumpInterval">interval:&nbsp;</label><input type="text" id="threadDumpInterval" name="threadDumpInterval" size="2" value="${not empty param.threadDumpInterval ? param.threadDumpInterval : '10'}"/>&nbsp;seconds
    </li>
</ul>
<c:url var="imgOn" value="images/nav_plain_green.png"/>
<c:url var="imgOff" value="images/nav_plain_red.png"/>

<% pageContext.setAttribute("threadDumpMonitorActive", Boolean.valueOf(ThreadMonitor.getInstance().isActivated())); %>
<c:set var="modeLabel" value="${threadDumpMonitorActive ? 'ON' : 'OFF'}"/>
<h3><img src="${threadDumpMonitorActive ? imgOn : imgOff}" alt="${modeLabel}" title="${modeLabel}" height="16" width="16"/> Thread Dump Monitor</h3>
<p>The Thread Dump Monitor is currently <strong>${threadDumpMonitorActive ? 'started' : 'stopped'}</strong>.<br/>
Click to <a href="?threadDumpMonitorActive=${!threadDumpMonitorActive}&toolAccessToken=${toolAccessToken}">${threadDumpMonitorActive ? 'stop' : 'start'} the Thread Dump Monitor</a>&nbsp;**
<p>

<p>------------------------------------------------------------------------------------------------------------------------------------<br/>
* - The thread dumps are performed into a folder:
<pre>        <%= SettingsBean.getThreadDir() %></pre>
This location can be overridden with a system property named <code>jahia.thread.dir</code>,<br/>
e.g. by adding <code>-Djahia.thread.dir=/var/logs/jahia/threads</code> to the JVM options (<code>CATALINA_OPTS</code> for Apache Tomcat).
<br/><br/>
** - Please note that <strong>the changes are only valid during server run time and are not persisted between server restarts.</strong>
If you would like to persist the flag value, use the <code>jahia.properties</code> file.
</p>
<%@ include file="gotoIndex.jspf" %>
</body>
</html>
