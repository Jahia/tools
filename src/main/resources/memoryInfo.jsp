<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@page import="org.jahia.bin.errors.ErrorFileDumper" %>
<%@page import="java.io.PrintWriter" %>
<%@page import="java.io.StringWriter" %>
<%@page import="java.util.Date" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title">Memory Status at <%= new Date() %>
</c:set>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<c:if test="${param.action == 'gc'}">
    <% System.gc(); %>
    <p style="color: blue">
        Garbage Collection triggered
    </p>
</c:if>
<c:if test="${param.action == 'dump'}">
    <p style="color: blue">
        Heap dump performed into a file: <%= ErrorFileDumper.performHeapDump() %>
    </p>
</c:if>
<body>
<%
    StringWriter s = new StringWriter();
    ErrorFileDumper.outputSystemInfo(new PrintWriter(s), false, false, true, false, false, false, false);
    pageContext.setAttribute("info", s.toString().replace("\n", "<br/>"));
    pageContext.setAttribute("isHeapDumpSupported", ErrorFileDumper.isHeapDumpSupported());
%>
<c:set var="headerActions">
    <li><a href="?refresh=true&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">refresh</span>Refresh</a></li>
    <li> <a href="?action=gc&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">recycling</span>Run Garbage Collector</a></li>
    <c:if test="${isHeapDumpSupported}">
        <li><a href="?action=dump&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">download</span>Perform Heap Dump</a></li>
    </c:if>
</c:set>
<%@ include file="commons/header.jspf" %>

<p>
    ${info}
</p>
<br/>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
