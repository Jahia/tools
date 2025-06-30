<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.bin.Jahia" %>
<%@ page import="org.jahia.services.SpringContextSingleton" %>
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ page import="org.jahia.settings.readonlymode.ReadOnlyModeController" %>
<%@ page import="org.jahia.settings.readonlymode.ReadOnlyModeController.ReadOnlyModeStatus" %>
<%@ page import="org.jahia.settings.readonlymode.ReadOnlyModeStatusInfo" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.function.Function" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="System Maintenance"/>
<head>
<%@ include file="commons/html_header.jspf" %>
</head>
<c:url var="imgOn" value="images/nav_plain_green.png"/>
<c:url var="imgOff" value="images/nav_plain_red.png"/>
<c:if test="${not empty param.maintenance}">
    <%
    Jahia.setMaintenance(Boolean.valueOf(request.getParameter("maintenance")));
    %>
</c:if>
<% pageContext.setAttribute("maintenance", Boolean.valueOf(Jahia.isMaintenance())); %>
<c:if test="${not empty param.readOnlyMode}">
    <%
    Boolean readOnly = Boolean.valueOf(request.getParameter("readOnlyMode"));
    Jahia.getSettings().setReadOnlyMode(readOnly);
    %>
</c:if>
<c:if test="${not empty param.fullReadOnlyMode}">
    <%
    Boolean fullReadOnlyParameter = Boolean.valueOf(request.getParameter("fullReadOnlyMode"));
    ReadOnlyModeController readOnlyModeController = ((ReadOnlyModeController) SpringContextSingleton.getBean("ReadOnlyModeController"));
    pageContext.setAttribute("currentReadOnlyStatus", readOnlyModeController.getReadOnlyStatus());
    try {
        readOnlyModeController.switchReadOnlyMode(fullReadOnlyParameter);
        pageContext.setAttribute("readOnlyStateChangeResult", "done");
    } catch (IllegalStateException e) {
        // we are not allowed to switch the state
    }
    %>
    <c:if test="${empty readOnlyStateChangeResult}">
        <p style="color: red">Unable to switch full read only mode to <strong>${param.fullReadOnlyMode ? 'ON' : 'OFF'}</strong> as the current state (<strong>${currentReadOnlyStatus}</strong>) does not allow the requested mode change.</p>
    </c:if>
</c:if>
<% pageContext.setAttribute("readOnlyMode", Boolean.valueOf(Jahia.getSettings().isReadOnlyMode())); %>
<% pageContext.setAttribute("fullReadOnlyModeStatus", ReadOnlyModeController.getInstance().getReadOnlyStatus().toString()); %>
<% pageContext.setAttribute("fullReadOnlyModeController", ReadOnlyModeController.getInstance()); %>
<% pageContext.setAttribute("settings", SettingsBean.getInstance()); %>
<body>
<%@ include file="commons/header.jspf" %>
<p><a href="maintenance.jsp" title="Refresh"><img src="<c:url value='/icons/refresh.png'/>" alt="Refresh" title="Refresh" height="16" width="16"/>&nbsp; Refresh status</a></p>
<c:set var="modeLabel" value="${maintenance ? 'ON' : 'OFF'}"/>
<h2><img src="${maintenance ? imgOn : imgOff}" alt="${modeLabel}" title="${modeLabel}" height="16" width="16"/> Maintenance Mode</h2>
<p>Please note that if you switch the maintenance mode flag, <strong>the changes are only valid during server run time and are not persisted between server restarts.</strong><br/>
If you would like to persist the flag value, use the jahia.properties file.</p>
<p>
If the maintenance mode is enabled only requests to the Tools Area are allowed. Requests to all other pages, will be blocked.<br/>
The maintenance mode is currently <strong>${modeLabel}</strong>.<br/>Click here to <a href="?maintenance=${!maintenance}&toolAccessToken=${toolAccessToken}">${maintenance ? 'disable' : 'enable'} maintenance mode</a>
</p>
<c:set var="modeLabel" value="${readOnlyMode ? 'ON' : 'OFF'}"/>
<h2><img src="${readOnlyMode ? imgOn : imgOff}" alt="${modeLabel}" title="${modeLabel}" height="16" width="16"/> Read-only Mode</h2>
<p>Please note that if you switch the read-only mode flag, <strong>the changes are only valid during server run time and are not persisted between server restarts.</strong><br/>
If you would like to persist the flag value, use the jahia.properties file.</p>
<p>
If the read-only mode is enabled, requests to the edit/contribute/studio/administration modes will be blocked.<br/>
The read-only mode is currently <strong>${modeLabel}</strong>.<br/>Click here to <a href="?readOnlyMode=${!readOnlyMode}&toolAccessToken=${toolAccessToken}">${readOnlyMode ? 'disable' : 'enable'} read-only mode</a>
</p>
<c:choose>
    <c:when test="${settings.clusterActivated}">
        <%
            // can't use lambda or method references unless JSP source is set to 1.8 or above
            Function<ReadOnlyModeStatusInfo, ReadOnlyModeStatus> getStatusValue = new Function<ReadOnlyModeStatusInfo, ReadOnlyModeStatus>() {
                @Override public ReadOnlyModeStatus apply(ReadOnlyModeStatusInfo readOnlyModeStatusInfo) {
                    return readOnlyModeStatusInfo.getValue();
                }
            };

            List<ReadOnlyModeStatusInfo> statuses = ReadOnlyModeController.getInstance().getReadOnlyStatuses();
            boolean statusesSynched = statuses.stream().map(getStatusValue).distinct().count() == 1;
            pageContext.setAttribute("fullReadOnlyStatusesSynched", statusesSynched);
            pageContext.setAttribute("fullReadOnlyStatuses", statuses);
        %>
        <c:set value="${fullReadOnlyStatusesSynched && fn:endsWith(fullReadOnlyModeStatus, 'ON')}" var="fullReadOnlyEnabled"/>
    </c:when>
    <c:otherwise>
        <c:set value="${fn:endsWith(fullReadOnlyModeStatus, 'ON')}" var="fullReadOnlyEnabled"/>
    </c:otherwise>
</c:choose>
<h2>
<img src="${fullReadOnlyEnabled ? imgOn : imgOff}" alt="${fullReadOnlyModeStatus}" title="${fullReadOnlyModeStatus}" height="16" width="16"/> Full Read-Only Mode
</h2>
<c:if test="${settings.clusterActivated && not settings.processingServer}">
<p>
    Please note that enabling or disabling full read-only mode will only be applied cluster-wide when initiated from the processing
    server. <strong>Enabling or disabling it from here will only update the status of this node.</strong>
</p>
</c:if>
<p>
<c:choose>
    <c:when test="${settings.clusterActivated && not fullReadOnlyStatusesSynched}">
        The full read-only mode is currently <strong>${fullReadOnlyModeStatus}</strong> for the current node.
        <ul style="margin: 0">
        <c:forEach items="${fullReadOnlyStatuses}" var="status" begin="1">
            <li>The full read-only mode is currently <strong>${status.value}</strong> for node <strong>${status.origin}</strong></li>
        </c:forEach>
        </ul>
    </c:when>
    <c:otherwise>
        The full read-only mode is currently <strong>${fullReadOnlyModeStatus}</strong>.
    </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${settings.clusterActivated && not fullReadOnlyStatusesSynched}">
        <br/>The status of the cluster is inconsistent. Click here to <a href="?fullReadOnlyMode=false&toolAccessToken=${toolAccessToken}">disable</a> full read-only mode, or <a href="?fullReadOnlyMode=true&toolAccessToken=${toolAccessToken}">enable</a> it.
    </c:when>
    <c:when test="${fn:startsWith(fullReadOnlyModeStatus, 'PARTIAL')}">
        <br/>The previous operation failed. Click here to retry <a href="?fullReadOnlyMode=${fullReadOnlyEnabled}&toolAccessToken=${toolAccessToken}">${fullReadOnlyEnabled ? 'enabling' : 'disabling'}</a> full read-only mode, or <a href="?fullReadOnlyMode=${!fullReadOnlyEnabled}&toolAccessToken=${toolAccessToken}">${fullReadOnlyEnabled ? 'disable' : 'enable'}</a> it.
    </c:when>
    <c:when test="${not fn:startsWith(fullReadOnlyModeStatus, 'PENDING') and not fn:startsWith(fullReadOnlyModeStatus, 'PARTIAL')}">
        <br/>Click here to <a href="?fullReadOnlyMode=${!fullReadOnlyEnabled}&toolAccessToken=${toolAccessToken}">${fullReadOnlyEnabled ? 'disable' : 'enable'} full read-only mode</a>
    </c:when>
</c:choose>
</p>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
