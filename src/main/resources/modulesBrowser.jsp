<%@ page contentType="text/html;charset=UTF-8" language="java"
%>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="org.jahia.data.templates.JahiaTemplatesPackage" %>
<%@page import="org.jahia.services.SpringContextSingleton" %>
<%@ page import="org.jahia.services.content.JCRCallback" %>
<%@ page import="org.jahia.services.content.JCRNodeWrapper" %>
<%@ page import="org.jahia.services.content.JCRSessionWrapper" %>
<%@ page import="org.jahia.services.content.JCRTemplate" %>
<%@ page import="org.jahia.services.content.nodetypes.ExtendedNodeType" %>
<%@ page import="org.jahia.services.content.nodetypes.NodeTypeRegistry" %>
<%@ page import="org.jahia.services.templates.JahiaTemplateManagerService" %>
<%@ page import="javax.jcr.NodeIterator" %>
<%@ page import="javax.jcr.RepositoryException" %>
<%@ page import="javax.jcr.nodetype.NodeTypeIterator" %>
<%@ page import="javax.jcr.nodetype.NodeTypeManager" %>
<%@ page import="javax.jcr.query.Query" %>
<%@ page import="javax.jcr.query.QueryResult" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Installed Modules Browser</title>
</head>
<%
    JahiaTemplateManagerService jahiaTemplateManagerService = (JahiaTemplateManagerService) SpringContextSingleton.getInstance().getContext().getBean(
            "JahiaTemplateManagerService");
    List<JahiaTemplatesPackage> availableTemplatePackages = new ArrayList<JahiaTemplatesPackage>(
            jahiaTemplateManagerService.getAvailableTemplatePackages());
    Collections.sort(availableTemplatePackages, JahiaTemplateManagerService.TEMPLATE_PACKAGE_NAME_COMPARATOR);
    pageContext.setAttribute("availablePackages", availableTemplatePackages);

    if (request.getParameter("delete") != null) {
        final String packName = request.getParameter("delete");
        final JahiaTemplatesPackage pack = jahiaTemplateManagerService.getTemplatePackage(packName);
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                NodeTypeIterator nti = NodeTypeRegistry.getInstance().getNodeTypes(pack.getId());
                while (nti.hasNext()) {
                    ExtendedNodeType next = (ExtendedNodeType) nti.next();
                    System.out.println(next.getName());
                    Query q = session.getWorkspace().getQueryManager().createQuery("select * from [" + next.getName() + "]", Query.JCR_SQL2);
                    QueryResult result = q.execute();
                    NodeIterator ni = result.getNodes();
                    while (ni.hasNext()) {
                        JCRNodeWrapper o = (JCRNodeWrapper) ni.next();
                        if (next.isMixin()) {
//                            o.removeMixin(next.getName());
                        }
                        if (o.isNodeType(next.getName())) {
                            o.remove();
                        }
                        System.out.println(o.getPath());
                    }
                }
                session.save();
                NodeTypeManager ntm = session.getProviderSession(session.getNode("/").getProvider()).getWorkspace().getNodeTypeManager();
                while (nti.hasNext()) {
                    ExtendedNodeType next = (ExtendedNodeType) nti.next();
                    ntm.unregisterNodeType(next.getName());
                }
                session.getNode("/modules/" + pack.getId()).remove();
                session.save();
                return null;
            }
        });
    }
%>
<body id="dt_example">
<%@ include file="logout.jspf" %>
<%@ include file="gotoIndex.jspf" %>
<div class="container-fluid">
    <table id="moduleTable" class="table table-striped compact" data-table="dataTable">
        <thead>
        <tr>
            <th>N°</th>
            <th>Name</th>
            <th>Description</th>
            <th>Root Folder</th>
            <th>Dependencies</th>
            <th>Definitions</th>
            <th>Rules</th>
            <th>Import Files</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${availablePackages}" var="pkg" varStatus="pstatus">
            <tr class="gradeA">
                <td align="center">${pstatus.count}</td>
                <td><a name="${pkg.name}" href="definitionsBrowser.jsp?#${pkg.name}">${pkg.name}</a>
                    <br>
                    <a href="modulesBrowser.jsp?delete=${pkg.name}&toolAccessToken=${toolAccessToken}">Delete</a>

                </td>
                <td>${pkg.description}</td>
                <td>${pkg.rootFolderPath}</td>
                <td>
                    <ol>
                        <c:forEach items="${pkg.dependencies}" var="dep">
                            <li><a href="#${dep.name}">${dep.name}</a></li>
                        </c:forEach>
                    </ol>
                </td>
                <td>
                    <ol>
                        <c:forEach items="${pkg.definitionsFiles}" var="defFile">
                        </c:forEach>
                    </ol>
                </td>
                <td>
                    <ol>
                        <c:forEach items="${pkg.rulesFiles}" var="defFile">
                        </c:forEach>
                    </ol>
                </td>
                <td>
                    <ol>
                        <c:forEach items="${pkg.initialImports}" var="defFile">
                            <li>${defFile}</li>
                        </c:forEach>
                    </ol>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/datatable.tools.bundle.js'/>"></script>
</body>
</html>
