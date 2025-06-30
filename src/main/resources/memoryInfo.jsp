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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
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
<%@ include file="commons/header.jspf" %>
<%
    StringWriter s = new StringWriter();
    ErrorFileDumper.outputSystemInfo(new PrintWriter(s), false, false, true, false, false, false, false);
    pageContext.setAttribute("info", s.toString().replace("\n", "<br/>"));
    pageContext.setAttribute("isHeapDumpSupported", ErrorFileDumper.isHeapDumpSupported());
%>
<p>
    <a href="?refresh=true&toolAccessToken=${toolAccessToken}"><img src="<c:url value='/icons/refresh.png'/>"
                                                                    height="16" width="16" alt=" " align="top"/>Refresh</a>
    &nbsp;
    <a href="?action=gc&toolAccessToken=${toolAccessToken}"><img src="<c:url value='/icons/showTrashboard.png'/>"
                                                                 height="16" width="16" alt=" " align="top"/>Run Garbage
        Collector</a>
    <c:if test="${isHeapDumpSupported}">
        &nbsp;
        <a href="?action=dump&toolAccessToken=${toolAccessToken}"><img src="<c:url value='/icons/export.png'/>"
                                                                       height="16" width="16" alt=" " align="top"/>Perform
            Heap Dump</a>
    </c:if>
    <br/>
    ${info}
</p>
<br/>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
