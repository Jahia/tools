<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.bin.errors.ErrorFileDumper" %>
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Error File Dumper"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<c:if test="${not empty param.active}">
    <%
        ErrorFileDumper.setFileDumpActivated(Boolean.valueOf(request.getParameter("active")));
    %>
</c:if>
<% pageContext.setAttribute("active", Boolean.valueOf(ErrorFileDumper.isFileDumpActivated())); %>
<c:if test="${active}">
    <p>The dumping of error and thread information to a file is currently <strong>ON</strong>.<br/>Click here to <a
            href="?active=false&toolAccessToken=${toolAccessToken}">disable error file dumper</a></p>
</c:if>
<c:if test="${not active}">
    <p>The dumping of error and thread information to a file is currently <strong>OFF</strong>.<br/>Click here to <a
            href="?active=true&toolAccessToken=${toolAccessToken}">enable error file dumper</a></p>
</c:if>
<p>Please note that these settings are valid only during server run time and are not persisted between server
    restarts.</p>

<p>The error file dumper is using the following directory:</p>
<pre>        <%= SettingsBean.getErrorDir() %></pre>
<p>
    This location can be overridden with a system property named <code>jahia.error.dir</code>,<br/>
    e.g. by adding <code>-Djahia.error.dir=/var/logs/jahia/errors</code> to the JVM options (<code>CATALINA_OPTS</code>
    for Apache Tomcat).
</p>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
