<%@page import="org.jahia.bin.errors.ErrorFileDumper,java.io.PrintWriter,java.text.SimpleDateFormat,java.util.Date" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%>
<c:if test="${param.file}"><%
    response.setContentType("text/plain; charset=ISO-8859-1");
    response.setHeader("Content-Disposition", "attachment; filename=\"thread-dump-"
            + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".out\"");
%>System Status Information at <%= new java.util.Date() %><% pageContext.getOut().append("\n"); %>
    <% ErrorFileDumper.outputSystemInfo(new PrintWriter(pageContext.getOut()), false, false, false, false, false, true, false, false); %>
</c:if><c:if test="${not param.file}">
    <%@ page contentType="text/html; charset=UTF-8" language="java" %>
    <!DOCTYPE html>
    <html>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <c:set var="title">Thread State Information at <%= new Date() %>
    </c:set>
    <head>
        <%@ include file="commons/html_header.jspf" %>
    </head>
    <body>
    <c:set var="dump"><% ErrorFileDumper.outputSystemInfo(new PrintWriter(pageContext.getOut()), false, false, false, false, false, true, false, false); %>
    </c:set>
    <pre><c:out value="${dump}"/></pre>
    </body>
    </html>
</c:if>
