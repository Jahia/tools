<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<c:set var="title" value="JCR Browser (New)"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<c:set var="description">
    <p>Modern JCR Browser with React - Browse the Java Content Repository with an enhanced interface</p>
</c:set>
<%@ include file="commons/header.jspf" %>

<div id="jcr-browser-root"
     data-graphql-endpoint="<c:url value='/modules/graphql'/>"
     data-csrf-token="${toolAccessToken}"
     data-initial-workspace="${functions:default(fn:escapeXml(param.workspace), 'default')}"
     data-initial-uuid="${functions:default(fn:escapeXml(param.uuid), 'cafebabe-cafe-babe-cafe-babecafebabe')}">
    <div style="display: flex; justify-content: center; align-items: center; min-height: 50vh;">
        <p>Loading JCR Browser...</p>
    </div>
</div>

<%@ include file="commons/footer.jspf" %>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/jcrBrowser.tools.bundle.js'/>"></script>
</body>
</html>
