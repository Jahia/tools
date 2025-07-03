<%@page import="org.jahia.bin.errors.ErrorFileDumper,java.io.PrintWriter,java.text.SimpleDateFormat,java.util.Date"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"  prefix="c"
%><c:if test="${param.file}"><%
response.setContentType("text/plain; charset=ISO-8859-1");
response.setHeader("Content-Disposition", "attachment; filename=\"system-info-"
        + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".txt\"");
%>System Status Information at <%= new java.util.Date() %><% pageContext.getOut().append("\n"); %>
<% ErrorFileDumper.outputSystemInfo(new PrintWriter(pageContext.getOut())); %></c:if><c:if test="${not param.file}">
    <%@ page contentType="text/html; charset=UTF-8" language="java" %>
    <!DOCTYPE html>
    <html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title">System Status Information at <%= new Date() %></c:set>
<head>
<%@ include file="commons/html_header.jspf" %>
</head>
<body>
<c:set var="headerActions">
    <li><a href="?file=true&toolAccessToken=${toolAccessToken}" target="_blank"><span class="material-symbols-outlined">download</span>download as a file</a></li>
</c:set>
<%@ include file="commons/header.jspf" %>
<pre>
    <% ErrorFileDumper.outputSystemInfo(new PrintWriter(pageContext.getOut())); %>
</pre>
<br/>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
</c:if>
