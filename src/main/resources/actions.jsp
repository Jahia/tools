<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@page import="org.jahia.registries.ServicesRegistry"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<c:set var="title" value="Actions (${functions:length(actions)} found)"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<%
    pageContext.setAttribute("actions", ServicesRegistry.getInstance().getJahiaTemplateManagerService().getActions().values());
%>
<body>
<%@ include file="commons/header.jspf" %>
<table border="1" cellspacing="0" cellpadding="5">
    <thead>
    <tr>
        <th>#</th>
        <th>Name</th>
        <th>Require Authenticated User</th>
        <th>Required Permission</th>
        <th>Required Workspace</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${actions}" var="action" varStatus="status">
        <%-- we split the action class name to remove the hash code before display it to the end user --%>
        <c:set var="splitActionClassName" value="<%= pageContext.getAttribute(\"action\").toString().split(\"@\") %>"/>
        <tr>
            <td align="center"><span style="font-size: 0.8em;">${status.count}</span></td>
            <td title='${splitActionClassName[0]}'><strong>${action.name}</strong></td>
            <td align="center">${action.requireAuthenticatedUser}</td>
            <td align="center">${action.requiredPermission}</td>
            <td align="center">${action.requiredWorkspace}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
