<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ page import="org.apache.karaf.jaas.boot.principal.RolePrincipal" %>
<%@ page import="org.jahia.modules.tools.karaf.KarafCommand" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<c:set var="title" value="Karaf command line"/>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>

<fieldset>
    <legend>Command line</legend>
    <form id="command" action="?" method="get">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <input name="commandInput" id="commandInput" onkeyup="if ((event || window.event).keyCode == 13 && (event || window.event).ctrlKey) document.getElementById('command').submit();" autofocus size="70" value="${fn:escapeXml(param.commandInput)}"/>
        <span>

        <input type="submit" name="action" value="Execute ([Ctrl+Enter])"  title="Execute" />
        </span>
    </form>

    Examples: bundle:list , bundle:restart [bundleid] , bundle:tree-show [bundleid] , jahia:modules , shell:tail -n 100 ../logs/jahia.log , dump-create , ...
</fieldset>


<c:if test="${not empty param.commandInput}">
    <c:catch var="error">
        <%
            KarafCommand c = (KarafCommand) BundleUtils.getOsgiService("org.jahia.modules.tools.karaf.KarafCommand", null);
            String output = c.executeCommand(request.getParameter("commandInput"), 10000L, false, request.getUserPrincipal(), new
                    RolePrincipal("manager"), new RolePrincipal("admin"), new RolePrincipal("systembundles"));
            pageContext.setAttribute("output", output);
        %>
        <pre>${fn:escapeXml(output)}</pre>
    </c:catch>

    <c:if test="${not empty error}">
        <fieldset style="color: red">
            <legend><strong>Error</strong></legend>
            <pre>${fn:escapeXml(error)}</pre>
        </fieldset>
    </c:if>
</c:if>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
