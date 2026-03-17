<%@ page import="org.jahia.services.render.RenderInfo" %>
<%@ page import="java.lang.String" %>
<%
    response.setContentType("application/json");
    response.setHeader("Content-Disposition","attachment; filename=\"dump.json\"");
    String s = RenderInfo.dump();
%>
<%= s %>
