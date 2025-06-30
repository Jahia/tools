<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.services.content.JCRStoreProvider" %>
<%@ page import="org.jahia.services.content.JCRStoreService" %>
<%@ page import="java.util.Map" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<c:set var="title">JCR Providers (${functions:length(providers)} found)</c:set>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="commons/html_header.jspf" %>
</head>

<%
    Map<String,JCRStoreProvider> providers = JCRStoreService.getInstance().getSessionFactory().getProviders();
    pageContext.setAttribute("providers",providers);
%>

<body>
<%@ include file="commons/header.jspf" %>
<table border="1" cellspacing="0" cellpadding="5">
    <thead>
    <tr>
        <th>#</th>
        <th>Provider Name</th>
        <th>Mount point</th>
        <th>Actions</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${providers}" var="provider" varStatus="status">
    <tr>
        <td align="center"><span style="font-size: 0.8em;">${status.index + 1}</span></td>
        <td>${provider.key}</td>
        <td>${provider.value.mountPoint}</td>
        <c:url var="browseUrl" value="jcrBrowser.jsp">
            <c:param name="path" value="${provider.value.mountPoint}"/>
            <c:param name="toolAccessToken" value="${toolAccessToken}"/>
        </c:url>
        <td align="center"><a href="${browseUrl}" title="Browse with the JCR Browser"><img src="<c:url value='/icons/search.png'/>" height="16" width="16" title="Browse with the JCR Browser" border="0" style="vertical-align: middle;"/></a></td>
    </tr>
    </c:forEach>
    </tbody>
</table>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
