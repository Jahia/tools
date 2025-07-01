<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@page import="org.jahia.services.content.JCRSessionFactory" %>
<%@page import="org.jahia.services.content.JCRSessionWrapper" %>
<%@page import="org.jahia.services.usermanager.JahiaUser" %>
<%@page import="org.jahia.services.usermanager.JahiaUserManagerService" %>
<%@ page import="org.jahia.services.workflow.Workflow" %>
<%@ page import="org.jahia.services.workflow.WorkflowService" %>
<%@ page import="org.jahia.services.workflow.WorkflowTask" %>
<%@ page import="javax.jcr.ItemNotFoundException" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<c:set var="title" value="Workflows monitor"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<c:set var="headerActions">
    <li><a href="workflows.jsp?abortGhost=all&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">
cancel_presentation
</span>Abort all ghost workflows</a>
    </li>
</c:set>
<%@ include file="commons/header.jspf" %>
<%

    final JahiaUser user = JahiaUserManagerService.getInstance().lookupRootUser().getJahiaUser();
    JCRSessionFactory.getInstance().setCurrentUser(user);
    JCRSessionWrapper jcrSession = JCRSessionFactory.getInstance().getCurrentUserSession();
%>

<c:if test="${not empty param['abortProcess']}">
    <%
        WorkflowService.getInstance().abortProcess(pageContext.getRequest().getParameter("abortProcess"), pageContext.getRequest().getParameter("provider"));
    %>
</c:if>
<c:if test="${not empty param['abortGhost']}">
    <%
        List<WorkflowTask> tasks = WorkflowService.getInstance().getTasksForUser(user, Locale.ENGLISH);
        for (WorkflowTask task : tasks) {
            Workflow w = WorkflowService.getInstance().getWorkflow(task.getProvider(), task.getProcessId(), Locale.ENGLISH);
            List<String> nodeIds = (List<String>) w.getVariables().get("nodeIds");
            if (nodeIds != null) {
                boolean ok = false;
                for (String nodeId : nodeIds) {
                    try {
                        jcrSession.getNodeByIdentifier(nodeId);
                        ok = true;
                        break;
                    } catch (ItemNotFoundException e) {
                    }
                }
                if (!ok) {
                    WorkflowService.getInstance().abortProcess(w.getId(), w.getProvider());
                }
            }
        }
    %>
</c:if>

<%
    List<WorkflowTask> tasks = WorkflowService.getInstance().getTasksForUser(user, Locale.ENGLISH);
    pageContext.setAttribute("tasks", tasks);
%>

<table border="1">
    <tr>
        <th>Taskid</th>
        <th>Processid</th>
        <th>MainNodeId</th>
        <th>Valid nodes count</th>
        <th>Invalid nodes count</th>
        <th>Operations</th>
    </tr>
    <c:forEach items="${tasks}" var="task">
        <tr>

            <td> ${task.id} (${task.name})</td>
            <%
                WorkflowTask task = (WorkflowTask) pageContext.getAttribute("task");
                Workflow w = WorkflowService.getInstance().getWorkflow(task.getProvider(), task.getProcessId(), Locale.ENGLISH);
                pageContext.setAttribute("workflow", w);
            %>
            <td> ${workflow.id} (${workflow.variables['jcr:title'][0].value})</td>

            <jcr:node var="node" uuid="${workflow.variables['nodeId']}"/>
            <td>
                <c:if test="${empty node}">
                    ${workflow.variables['nodeId']}
                </c:if>
                <c:if test="${not empty node}">
                    <a href="jcrBrowser.jsp?uuid=${workflow.variables['nodeId']}&toolAccessToken=${toolAccessToken}">${workflow.variables['nodeId']}</a>
                </c:if>
            </td>

            <c:set var="emptyNodes" value="0"/>
            <c:set var="nonEmptyNodes" value="0"/>
            <c:forEach items="${workflow.variables['nodeIds']}" var="nodeId">
                <jcr:node var="node" uuid="${nodeId}"/>
                <c:if test="${empty node}">
                    <c:set var="emptyNodes" value="${emptyNodes + 1}"/>
                </c:if>
                <c:if test="${not empty node}">
                    <c:set var="nonEmptyNodes" value="${nonEmptyNodes + 1}"/>
                </c:if>
            </c:forEach>
            <td>
                    ${nonEmptyNodes}
            </td>
            <td>
                    ${emptyNodes}
            </td>
            <td>
                <a href="workflows.jsp?abortProcess=${task.processId}&provider=${task.provider}&toolAccessToken=${toolAccessToken}">abort workflow</a>
            </td>
        </tr>
    </c:forEach>
</table>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
