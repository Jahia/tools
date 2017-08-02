<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="org.jahia.bin.Jahia"%>
<%@page import="org.jahia.services.SpringContextSingleton"%>
<%@page import="org.jahia.settings.readonlymode.ReadOnlyModeController"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@ include file="css.jspf" %>
<title>System Maintenance</title>
</head>
<c:url var="imgOn" value="images/nav_plain_green.png"/>
<c:url var="imgOff" value="images/nav_plain_red.png"/>
<c:if test="${not empty param.maintenance}">
<%
Jahia.setMaintenance(Boolean.valueOf(request.getParameter("maintenance")));
%>
</c:if>
<% pageContext.setAttribute("maintenance", Boolean.valueOf(Jahia.isMaintenance())); %>
<c:if test="${not empty param.fullReadOnlyMode}">
<%
Boolean fullReadOnly = Boolean.valueOf(request.getParameter("fullReadOnlyMode"));
((ReadOnlyModeController)SpringContextSingleton.getBean("ReadOnlyModeController")).switchReadOnlyMode(fullReadOnly);
%>
</c:if>
<% pageContext.setAttribute("fullReadOnlyMode", Boolean.valueOf(Jahia.getSettings().isFullReadOnlyMode())); %>
<body>
<c:if test="${not empty param.readOnlyMode}">
<%
Boolean readOnly = Boolean.valueOf(request.getParameter("readOnlyMode"));
Jahia.getSettings().setReadOnlyMode(readOnly);
%>
</c:if>
<% pageContext.setAttribute("readOnlyMode", Boolean.valueOf(Jahia.getSettings().isReadOnlyMode())); %>
<h1>System Maintenance</h1>
<p><a href="maintenance.jsp" title="Refresh"><img src="<c:url value='/icons/refresh.png'/>" alt="Refresh" title="Refresh" height="16" width="16"/>&nbsp; Refresh status</a></p>
<c:set var="modeLabel" value="${maintenance ? 'ON' : 'OFF'}"/>
<h2><img src="${maintenance ? imgOn : imgOff}" alt="${modeLabel}" title="${modeLabel}" height="16" width="16"/> Maintenance Mode</h2>
<p>Please note that if you switch the maintenance mode flag, <strong>the changes are only valid during server run time and are not persisted between server restarts.</strong><br/>
If you would like to persist the flag value, use the jahia.properties file.</p>
<p>
If the maintenance mode is enabled only requests to the Tools Area are allowed. Requests to all other pages, will be blocked.<br/>
The maintenance mode is currently <strong>${modeLabel}</strong>.<br/>Click here to <a href="?maintenance=${!maintenance}">${maintenance ? 'disable' : 'enable'} maintenance mode</a>
</p>
<c:set var="modeLabel" value="${readOnlyMode ? 'ON' : 'OFF'}"/>
<h2><img src="${readOnlyMode ? imgOn : imgOff}" alt="${modeLabel}" title="${modeLabel}" height="16" width="16"/> Read-only Mode</h2>
<p>Please note that if you switch the read-only mode flag, <strong>the changes are only valid during server run time and are not persisted between server restarts.</strong><br/>
If you would like to persist the flag value, use the jahia.properties file.</p>
<p>
If the read-only mode is enabled, requests to the edit/contribute/studio/administration modes will be blocked.<br/>
The read-only mode is currently <strong>${modeLabel}</strong>.<br/>Click here to <a href="?readOnlyMode=${!readOnlyMode}">${readOnlyMode ? 'disable' : 'enable'} read-only mode</a>
</p>
<c:set var="modeLabel" value="${fullReadOnlyMode ? 'ON' : 'OFF'}"/>
<h2><img src="${fullReadOnlyMode ? imgOn : imgOff}" alt="${modeLabel}" title="${modeLabel}" height="16" width="16"/> Full Read-Only Mode</h2>
<p>
The full read-only mode is currently <strong>${modeLabel}</strong>.<br/>Click here to <a href="?fullReadOnlyMode=${!fullReadOnlyMode}">${fullReadOnlyMode ? 'disable' : 'enable'} full read-only mode</a>
</p>
<%@ include file="gotoIndex.jspf" %>
</body>
</html>