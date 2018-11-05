<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="org.jahia.services.render.RenderInfo" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="css.jspf" %>
    <title>Render chain debug</title>
</head>
<body>
<h1>Render chain debug</h1>
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

<%@ include file="gotoIndex.jspf" %>
</body>
</html>