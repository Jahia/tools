<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.services.content.JCRContentUtils" %>
<%@page import="org.jahia.utils.DateUtils" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="JCR Data Store Garbage Collection"/>

<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<c:if test="${param.gc == 'true'}">
    <% System.gc(); %>
</c:if>
<c:if test="${param.action == 'gc'}">
    <%
        long timer = System.currentTimeMillis();
        int deleted = 0;
        try {
            deleted = JCRContentUtils.callDataStoreGarbageCollector();
        } catch (Exception e) {

        } finally {
            pageContext.setAttribute("took", DateUtils.formatDurationWords(System.currentTimeMillis() - timer));
            pageContext.setAttribute("deleted", deleted);
        }
    %>
    <p style="color: blue">Successfully executed in <strong>${took}</strong>. <strong>${deleted}</strong> data record(s)
        deleted.</p>
</c:if>
<p>Available actions:</p>
<ul>
    <li><a href="?action=gc&toolAccessToken=${toolAccessToken}"
           onclick="return confirm('You are about to start the DataStore Garbage Collector. All unused files in the data store will be permanently deleted. Do you want to continue?');">Run
        JCR DataStore garbage collector now</a></li>
    <li><a href="?action=gc&amp;gc=true&toolAccessToken=${toolAccessToken}"
           onclick="return confirm('You are about to start the DataStore Garbage Collector. All unused files in the data store will be permanently deleted. Do you want to continue?');">Run
        Java GC first and than run JCR DataStore garbage collector now</a></li>
</ul>
<%@ include file="commons/footer.jspf" %>
</body>
</html>

