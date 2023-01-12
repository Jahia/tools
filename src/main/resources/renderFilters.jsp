<%@ page contentType="text/html;charset=UTF-8" language="java"
        %><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="org.jahia.services.render.RenderService"%>
<%@ page import="org.jahia.services.render.filter.RenderFilter" %>
<%@ page import="java.util.Collections" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<%
    String beanName = request.getParameter("bean");
    String s= request.getParameter("switch");
    float p= request.getParameter("priority") == null ? 0 : (float) Double.parseDouble(request.getParameter("priority"));
    float pp= request.getParameter("previousPriority") == null ? 0 : (float)
            Double.parseDouble(request.getParameter("previousPriority"));

    if (beanName != null) {
        for (RenderFilter f : RenderService.getInstance().getRenderChainInstance().getFilters()) {
            if (f.getClass().getName().equals(beanName) && (p == 0 || p == f.getPriority())) {
                System.out.print("processing render filter " + beanName + "(" + f.getName() + ")");
                if ("true".equals(s)) {
                    f.setDisabled(!f.isDisabled());
                    System.out.println(f.isDisabled() ? " disabled" : " enabled");
                } else if ("priority".equals(s)) {
                    System.out.println(" change priority from " + f.getPriority() + " to " + pp);
                    f.setPriority(pp);
                    RenderService.getInstance().getRenderChainInstance().doSortFilters();
                }
            }
        }
    }
%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Render Filters</title>
    <%@ include file="css.jspf" %>
</head>
<%
    pageContext.setAttribute("filters", RenderService.getInstance().getRenderChainInstance().getFilters());
    pageContext.setAttribute("newline", "\n");
%>
<body>
<h1>Render Filters (${functions:length(filters)} found)</h1>
<table border="1" cellspacing="0" cellpadding="5">
    <thead>
    <tr>
        <th>#</th>
        <th>Priority</th>
        <th>Class</th>
        <th>Description</th>
        <th>Conditions</th>
        <th>status</th>
        <th>actions</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${filters}" var="filter" varStatus="status">
        <%
            RenderFilter f = (RenderFilter) pageContext.getAttribute("filter");
			pageContext.setAttribute("filterClassName", f.getClass().getName());
        %>
        <tr id="${filterClassName}">
            <td align="center"><span style="font-size: 0.8em;">${status.index + 1}</span></td>
            <td align="center"><strong>${filter.priority}</strong></td>
            <td title="${filterClassName}">${fn:escapeXml(filter.name)}</td>
            <td>
                ${fn:escapeXml(filter.description)}
            </td>
            <td>${fn:escapeXml(filter.conditionsSummary)}</td>
            <td>${filter.disabled?"<font color='red'>disable</font>":"<font color='green'>enable</font>"}</td>
            <td>
                <a href="renderFilters.jsp?bean=${filterClassName}&switch=true&priority=${filter.priority}&toolAccessToken=${toolAccessToken}">${filter.disabled ? "enable" : "disable"}</a>
                <a href="renderFilters.jsp?bean=${filterClassName}&switch=priority&priority=${filter.priority}&previousPriority=${(!empty previousPriority ? previousPriority : filter.priority) -1}&toolAccessToken=${toolAccessToken}">down</a>
            </td>
        </tr>
        <c:set var="previousPriority" value="${filter.priority}"/>
    </c:forEach>
    </tbody>
</table>
<%@ include file="gotoIndex.jspf" %>
</body>
</html>