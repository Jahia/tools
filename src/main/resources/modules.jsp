<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@page import="org.jahia.osgi.FrameworkService" %>
<%@page import="org.jahia.registries.ServicesRegistry" %>
<%@page import="org.jahia.services.templates.JahiaTemplateManagerService" %>
<%@page import="org.osgi.framework.Bundle" %>
<%@page import="org.osgi.framework.startlevel.BundleStartLevel" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="Modules information"/>
    <head>
        <%@ include file="commons/html_header.jspf" %>
        <style>
            table {
                border-spacing: 0; border-collapse: collapse;
            }

            table td, table th {
                padding: 6px;
                border: solid 1px #dadada;
            }
        </style>
    </head>

    <body>
        <%@ include file="commons/header.jspf" %>
        <div>

<%
    JahiaTemplateManagerService service = ServicesRegistry.getInstance().getJahiaTemplateManagerService() ;
    pageContext.setAttribute("modules", service.getModuleStates());

    if (request.getParameter("set") != null) {
        FrameworkService.getBundleContext().getBundle(Integer.parseInt(request.getParameter("set"))).adapt(BundleStartLevel.class).setStartLevel(90);
    }
%>

        <table>
            <tr>
                <th>Bundle id</th>
                <th>Bundle name</th>
                <th>Version</th>
                <th>Status</th>
                <th>Start level</th>
                <th>Actions</th>
            </tr>

            <c:forEach var="item" items="${modules}">
            <tr>
                <c:set var="bundle" value="${item.getKey()}"/>
                <%
                    int startLevel = ((Bundle)pageContext.getAttribute("bundle")).adapt(BundleStartLevel.class).getStartLevel();
                    pageContext.setAttribute("startLevel", startLevel);
                %>

                <td>${bundle.bundleId}</td>
                <td>${bundle.symbolicName}</td>
                <td>${bundle.version}</td>
                <td style="${item.value ne 'Started' ? 'color:red' : ''}">${item.value}</td>
                <td style="${startLevel le 80 ? 'color:red' : ''}">
                    ${startLevel}
                </td>
                <td>
                <c:if test="${startLevel le 80}">
                    <a href="modules.jsp?set=${bundle.bundleId}">Set startlevel to 90</a>
                </c:if>
                </td>
            <tr>
            </c:forEach>
        </table>
    </div>

    <%@ include file="commons/footer.jspf" %>

    </body>
</html>
