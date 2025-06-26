<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="org.apache.commons.collections.IteratorUtils" %>
<%@ page import="org.jahia.services.content.*" %>
<%@ page import="org.jahia.services.content.nodetypes.ExtendedNodeType" %>
<%@ page import="org.jahia.services.content.nodetypes.NodeTypeRegistry" %>
<%@ page import="javax.jcr.Node" %>
<%@ page import="javax.jcr.NodeIterator" %>
<%@ page import="javax.jcr.RepositoryException" %>
<%@ page import="javax.jcr.nodetype.NodeTypeIterator" %>
<%@ page import="javax.jcr.query.Query" %>
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
    <title>Installed Definitions Browser</title>
</head>

<form id="navigateForm" action="#" method="post">
    <input type="hidden" id="action" name="action"/>
    <input type="hidden" id="module" name="module"/>
    <input type="hidden" id="nodetype" name="nodetype"/>
    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
</form>

<%!
    private void deleteNodeTypes(final java.util.Iterator it, boolean unregister, final JspWriter out) throws java.io.IOException, RepositoryException {
        while (it.hasNext()) {
            final ExtendedNodeType nodeType = (ExtendedNodeType) it.next();

            JCRCallback<Object> callback = new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                    JCRNodeWrapper root = jcrSessionWrapper.getNode("/");
                    NodeIterator nodes = jcrSessionWrapper.getProviderSession(root.getProvider()).getWorkspace().getQueryManager().createQuery("select * from ['" + nodeType.getName() + "']", Query.JCR_SQL2).execute().getNodes();
                    int count = 0;
                    while (nodes.hasNext()) {
                        Node next = (Node) nodes.next();
                        if (nodeType.isMixin() && !next.getPrimaryNodeType().isNodeType(nodeType.getName())) {
                            System.out.println("removed mixin " + nodeType.getName() + " for " + next.getName());
                            next.removeMixin(nodeType.getName());
                        } else {
                            System.out.println("removed node " + nodeType.getName() + " for " + next.getName());
                            next.remove();
                        }
                        if ((++count % 100) == 0) {
                            jcrSessionWrapper.save();
                        }
                    }
                    jcrSessionWrapper.save();
                    return null;
                }
            };
            out.println("delete " + nodeType + "...");
            try {
                out.println("delete nodes for " + nodeType + "...");
                JCRTemplate.getInstance().doExecuteWithSystemSession(null, "default", callback);
                JCRTemplate.getInstance().doExecuteWithSystemSession(null, "live", callback);
                if (unregister) {
                    out.println("unregister " + nodeType + "...");
                    NodeTypeRegistry.getInstance().unregisterNodeType(nodeType.getName());
                }
            } catch (Exception e) {
                out.print("<b>" + e.getMessage() + "</b>");
            }
            out.println("</br>");
        }
    }

%>
<%
    final NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
    List<String> systemIds = nodeTypeRegistry.getSystemIds();
    Collections.sort(systemIds);
    pageContext.setAttribute("systemIds", systemIds);

    if ("deleteModule".equals(request.getParameter("action"))) {
        final String moduleName = request.getParameter("module");
        java.util.Iterator it = NodeTypeRegistry.getInstance().getNodeTypes(moduleName);

        deleteNodeTypes(it, false, out);

        try {
            out.println("unregister all nodetypes for " + moduleName);

            NodeTypeRegistry.getInstance().unregisterNodeTypes(moduleName);
            JCRStoreService.getInstance().undeployDefinitions(moduleName);
        } catch (java.lang.Exception exception) {
            exception.printStackTrace();
        }
    } else if ("deleteNodeType".equals(request.getParameter("action"))) {
        final String moduleName = request.getParameter("module");

        final List<ExtendedNodeType> nodeType = new java.util.ArrayList<ExtendedNodeType>();
        nodeType.add(NodeTypeRegistry.getInstance().getNodeType(request.getParameter("nodetype")));
        deleteNodeTypes(nodeType.iterator(), true, out);

        JCRStoreService.getInstance().deployDefinitions(moduleName);
        // todo : uregister node type from jackrabbit
    }
%>
<body id="dt_example">
<%@ include file="logout.jspf" %>
<%@ include file="gotoIndex.jspf" %>
<div class="container-fluid">
    <table id="moduleTable" class="table table-striped compact" data-table="dataTableDefinitionsBrowser">
        <thead>
        <tr>
            <th>N°</th>
            <th>System ID</th>
            <th>Types</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${systemIds}" var="pkg" varStatus="pstatus">
            <%
                NodeTypeIterator nodeTypes = nodeTypeRegistry.getNodeTypes((String) pageContext.getAttribute("pkg"));
                List ntList = IteratorUtils.toList(nodeTypes);
                Collections.sort(ntList, JCRContentUtils.NODE_TYPE_NAME_COMPARATOR);
                pageContext.setAttribute("nodeTypes", ntList);
            %>
            <tr>
                <td align="center">${pstatus.count}</td>
                <td><a name="${pkg}" href="modulesBrowser.jsp?#${pkg}">${pkg}</a>
                    &nbsp;<a href="#delete"
                             class="delete-definitions"
                             title="Delete">
                        <img src="<c:url value='/icons/delete.png'/>" height="16" width="16"
                                                 title="Delete" border="0" style="vertical-align: middle;" data-package="${pkg}"/></a>
                </td>
                <td>
                    <ol>
                        <c:forEach items="${nodeTypes}" var="dep">
                            <c:set var="defFileName" value="${fn:replace(dep.name,':','_')}"/>
                            <li><a href="#${defFileName}" class="defFileLink" data-src="#${defFileName}"
                                   data-fancybox>${dep.name}</a>
                                &nbsp;<a href="#delete"
                                         class="delete-nodetype"
                                         title="Delete">
                                    <img src="<c:url value='/icons/delete.png'/>" height="16"
                                                             width="16"
                                                             title="Delete" border="0" style="vertical-align: middle;"
                                                             data-package="${pkg}"
                                                             data-nodetype=${dep.name}
                                    /></a>
                            </li>
                            <div style="display:none;">
                                <div id="${defFileName}">
                                    <h3>${dep.name}</h3>
                                    <c:if test="${functions:length(dep.declaredSupertypes) > 0}">
                                        <p>
                                            Supertypes:
                                        </p>
                                        <ul>
                                            <c:forEach items="${dep.declaredSupertypes}" var="supertype">
                                                <li><a href="#${fn:replace(supertype.name,':','_')}"
                                                       data-src="#${fn:replace(supertype.name,':','_')}" data-fancybox
                                                       class="defFileLink">${supertype.name}</a></li>
                                            </c:forEach>
                                        </ul>
                                    </c:if>
                                    <c:if test="${functions:length(dep.propertyDefinitions)>0}">
                                        <p>
                                            Properties:
                                        </p>
                                        <ul>
                                            <c:forEach items="${dep.propertyDefinitions}" var="propDef">
                                                <c:if test="${propDef.declaringNodeType.name eq dep.name}">
                                                    <c:set var="propType" value="${propDef.requiredType}"/>
                                                    <% pageContext.setAttribute("propTypeName", javax.jcr.PropertyType.nameFromValue((Integer) pageContext.getAttribute("propType"))); %>
                                                    <li>${propDef.name} (${propTypeName})</li>
                                                </c:if>
                                            </c:forEach>
                                        </ul>
                                    </c:if>
                                    <c:if test="${functions:length(dep.subtypes)>0}">
                                        <p>
                                            Sub types:
                                        </p>
                                        <ul>
                                            <c:forEach items="${dep.subtypes}" var="supertype">
                                                <li><a href="#${fn:replace(supertype.name,':','_')}"
                                                       href="#${fn:replace(supertype.name,':','_')}"
                                                       data-src="#${fn:replace(supertype.name,':','_')}" data-fancybox
                                                       class="defFileLink">${supertype.name}</a></li>
                                            </c:forEach>
                                        </ul>
                                    </c:if>
                                    <c:if test="${functions:length(dep.childNodeDefinitions)>0}">
                                        <p>
                                            Children:
                                        </p>
                                        <ul>
                                            <c:forEach items="${dep.childNodeDefinitions}" var="supertype">
                                                <li><a href="#${fn:replace(supertype.name,':','_')}"
                                                       data-src="#${fn:replace(supertype.name,':','_')}" data-fancybox
                                                       class="defFileLink">${supertype.name}</a></li>
                                            </c:forEach>
                                        </ul>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </ol>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/fancybox.tools.bundle.js'/>"></script>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/datatable.tools.bundle.js'/>"></script>
<script  type="module" src="<c:url value='/modules/tools/javascript/apps/definitions.tools.bundle.js'/>"></script>
</body>
</html>
