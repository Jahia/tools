<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.services.content.JCRSessionWrapper" buffer="16kb" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<c:set var="workspace" value="${functions:default(param.workspace, 'default')}"/>
<c:set var="title" value="JCR Sessions"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<p>There are currently <%= JCRSessionWrapper.getActiveSessionsObjects().size() %> open sessions.<br/>
There is <%= JCRSessionWrapper.getActiveSessions() %> non system and active sessions.</p>

<p>List of session not hold/created by this page/request:</p>
<ol>
    <%
        final PrintWriter s = new PrintWriter(pageContext.getOut());
        Set<Map.Entry<UUID, JCRSessionWrapper>> entries = JCRSessionWrapper.getActiveSessionsObjects().entrySet();
        ArrayList<Map.Entry<UUID, JCRSessionWrapper>> list = new ArrayList<Map.Entry<UUID, JCRSessionWrapper>>(entries);
        Collections.sort(list, new Comparator<Map.Entry<UUID, JCRSessionWrapper>>() {
            public int compare(Map.Entry<UUID, JCRSessionWrapper> o1, Map.Entry<UUID, JCRSessionWrapper> o2) {
                String message = o1.getValue().getSessionTrace().getMessage();
                String message2 = o2.getValue().getSessionTrace().getMessage();
                if(message.contains("created")){
                    return message.split("created")[1].compareTo(message2.split("created")[1]);
                }
                return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
        });
        for (Map.Entry<UUID, JCRSessionWrapper> entry : list) {
            Exception sessionTrace = entry.getValue().getSessionTrace();
            String sessionTraceMessage = sessionTrace.getMessage();
            if (!sessionTraceMessage.contains(Thread.currentThread().getName() + "_" + Thread.currentThread().getId())) {
    %>
    <li><a href="#" class="exception" data-session="<%=entry.getKey()%>"><%=sessionTraceMessage%></a>
<pre id="<%=entry.getKey()%>" style="display: none" class="exceptionCode"><%
    sessionTrace.printStackTrace(s);%>
        </pre>
    </li>
    <%
            }
            pageContext.getOut().flush();
        }
    %>

</ol>
<%@ include file="commons/footer.jspf" %>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/session.tools.bundle.js'/>"></script>
</body>
</html>
