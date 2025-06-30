<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.services.content.nodetypes.initializers.ChoiceListInitializer" %>
<%@ page import="org.jahia.services.content.nodetypes.initializers.ChoiceListInitializerService" %>
<%@ page import="org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer" %>
<%@ page import="org.jahia.services.content.nodetypes.renderer.ChoiceListRenderer" %>
<%@ page import="org.jahia.services.content.nodetypes.renderer.ChoiceListRendererService" %>
<%@ page import="org.jahia.services.content.nodetypes.renderer.ModuleChoiceListRenderer" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<c:set var="title" value="Choicelist initializers &amp; renderers"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<%
    final Map<String,ChoiceListInitializer> initializers = ChoiceListInitializerService.getInstance().getInitializers();
    final int nbInitializers = initializers == null ? 0 : initializers.size();

    final Map<String,ChoiceListRenderer> renderers = ChoiceListRendererService.getInstance().getRenderers();
    final int nbRenderers = renderers == null ? 0 : renderers.size();
    int count = 0;
%>
<body>
<%@ include file="commons/header.jspf" %>
<div style="float:left;width:40%">
    <h2>Choicelist initializers (<%=nbInitializers%> found)</h2>

    <% if (nbInitializers > 0) { %>
    <table border="1" cellspacing="0" cellpadding="5">
        <thead>
            <tr>
                <th>#</th>
                <th>Name</th>
                <th>Is defined by a module</th>
            </tr>
        </thead>
        <tbody>
        <% for (String key : initializers.keySet()) {
                final ChoiceListInitializer initializer = initializers.get(key);
        %>
            <tr>
                <td><%=++count%></td>
                <td title="<%=initializer.getClass().getName()%>"><%=key%></td>
                <td align="center"><%=initializer instanceof ModuleChoiceListInitializer%></td>
            </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>

<div style="float:left;width:40%">
    <h2>Choicelist renderers (<%=nbRenderers%> found)</h2>

    <% if (nbRenderers > 0) { %>
    <table border="1" cellspacing="0" cellpadding="5">
        <thead>
        <tr>
            <th>#</th>
            <th>Name</th>
            <th>Is defined by a module</th>
        </tr>
        </thead>
        <tbody>
        <%
            count = 0;
            for (String key : renderers.keySet()) {
            final ChoiceListRenderer renderer = renderers.get(key);
        %>
        <tr>
            <td><%=++count%></td>
            <td title="<%=renderer.getClass().getName()%>"><%=key%></td>
            <td align="center"><%=renderer instanceof ModuleChoiceListRenderer%></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>

<div style="clear:both"></div>

<%@ include file="commons/footer.jspf" %>
</body>
</html>

