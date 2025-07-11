<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@page import="org.apache.jackrabbit.api.stats.QueryStat"%>
<%@page import="org.apache.jackrabbit.core.JahiaRepositoryImpl"%>
<%@page import="org.jahia.services.content.JCRSessionFactory"%>
<%@page import="org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="title" value="JCR Query statistics"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%
    QueryStat queryStat = ((JahiaRepositoryImpl)((SpringJackrabbitRepository)JCRSessionFactory.getInstance().getDefaultProvider().getRepository()).getRepository()).getContext().getStatManager().getQueryStat();
    pageContext.setAttribute("queryStat", queryStat);
%>

<c:if test="${not empty param.action}">
    <c:choose>
        <c:when test="${param.action == 'enable'}">
            <% queryStat.setEnabled("on".equals(request.getParameter("status"))); %>
            <c:set var="message">
                <p style="color: blue">Query statistics now ${param.status == 'on' ? 'enabled' : 'disabled'}.</p>
            </c:set>
        </c:when>
        <c:when test="${param.action == 'reset'}">
            <%
                queryStat.clearPopularQueriesQueue();
                queryStat.clearSlowQueriesQueue();
            %>
            <c:set var="message">
                <p style="color: blue">Query statistics was cleared.</p>
            </c:set>
        </c:when>
    </c:choose>
</c:if>


<c:set var="description">
<p>Query statistics when enabled provides information about slow queries and most popular queries.</p>
    <p>The JCR query statistics is currently <strong>${queryStat.enabled ? 'enabled' : 'disabled'}</strong>.</p>
    ${message}
</c:set>
<c:set var="headerActions">
    <li><a href="?refresh=true&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">refresh</span>Refresh</a></li>
    <li><a href="?action=reset&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">recycling</span>Reset statistics</a></li>
    <li><a href="?action=enable&amp;status=${queryStat.enabled ? 'off' : 'on'}&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">switch</span>${queryStat.enabled ? 'Disable stats' : 'Enable stats'}</a></li>
</c:set>
<%@ include file="commons/header.jspf" %>

<c:if test="${queryStat.enabled}">
<fieldset>
<legend>Slow queries</legend>
<c:if test="${not empty queryStat.slowQueries}" var="statsAvailable">
<ol>
	<c:forEach items="${queryStat.slowQueries}" var="q">
		<li>
			${fn:escapeXml(q.statement)}
			<br/>
			<c:url var="executeUrl" value="jcrQuery.jsp">
				<c:param name="lang" value="${q.language}"/>
				<c:param name="query" value="${q.statement}"/>
				<c:param name="toolAccessToken" value="${toolAccessToken}"/>
			</c:url>
			<a title="Execute in JCR Query Tool"
            	href="${executeUrl}"
                target="_blank"><img src="<c:url value='/icons/tab-search.png'/>" width="16" height="16" alt="run" title="Execute in JCR Query Tool">execute</a>
			<br/>
			duration: <strong>${q.duration} ms</strong><br/>
			language: <strong>${q.language}</strong><br/>
			created on: <strong>${q.creationTime}</strong><br/>
		</li>
	</c:forEach>
</ol>
</c:if>
<c:if test="${!statsAvailable}">
<p>There is no statistics collected so far</p>
</c:if>
</fieldset>

<fieldset>
<legend>Popular queries</legend>
<c:if test="${not empty queryStat.popularQueries}" var="statsAvailable">
<ol>
	<c:forEach items="${queryStat.popularQueries}" var="q">
		<li>
			${fn:escapeXml(q.statement)}
			<br/>
			<c:url var="executeUrl" value="jcrQuery.jsp">
				<c:param name="lang" value="${q.language}"/>
				<c:param name="query" value="${q.statement}"/>
				<c:param name="toolAccessToken" value="${toolAccessToken}"/>
			</c:url>
			<a title="Execute in JCR Query Tool"
            	href="${executeUrl}"
                target="_blank"><img src="<c:url value='/icons/tab-search.png'/>" width="16" height="16" alt="run" title="Execute in JCR Query Tool">execute</a>
			<br/>
			duration: <strong>${q.duration} ms</strong><br/>
			language: <strong>${q.language}</strong><br/>
			created on: <strong>${q.creationTime}</strong><br/>
		</li>
	</c:forEach>
</ol>
</c:if>
<c:if test="${!statsAvailable}">
<p>There is no statistics collected so far</p>
</c:if>
</fieldset>
</c:if>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
