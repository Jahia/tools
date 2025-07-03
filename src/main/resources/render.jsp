<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.services.render.RenderInfo" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Render chain debug"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<c:if test="${not empty param.active}">
    <%
        RenderInfo.setEnabled(Boolean.valueOf(request.getParameter("active")));
    %>
</c:if>
<% pageContext.setAttribute("active", Boolean.valueOf(RenderInfo.isEnabled())); %>
<c:if test="${active}">
    <p>Render chain profiling is <strong>ON</strong>.<br/>Click here to <a href="?active=false&toolAccessToken=${toolAccessToken}">disable</a></p>
</c:if>
<c:if test="${not active}">
    <p>Render chain profiling is <strong>OFF</strong>.<br/>Click here to <a href="?active=true&toolAccessToken=${toolAccessToken}">enable</a></p>
</c:if>
<p>Please note that these settings are valid only during server run time and are not persisted between server restarts.</p>

<c:if test="${active}">
    <p><a href="renderDump.jsp">Dump now</a></p>
</c:if>

<%@ include file="commons/footer.jspf" %>
</body>
</html>
