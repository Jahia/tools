<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ page import="org.jahia.utils.DatabaseUtils" %>
<c:set var="title" value="Jahia DB Query Tool"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<c:set var="offset" value="${not empty param.offset ? param.offset : '0'}"/>
<fieldset>
    <legend>DB query</legend>
    <form id="queryForm" action="?" method="get">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <textarea rows="3" cols="75" name="query" id="query"
            onkeyup="if ((event || window.event).keyCode === 13 && (event || window.event).ctrlKey) document.getElementById('queryForm').submit();"
        >${not empty param.query ? param.query : 'SELECT * FROM jahia_db_test'}</textarea>
        <span>
        Max rows:
        <select name="maxRows" id="maxRows">
            <option value="50"${param.maxRows == '50' ? 'selected="selected"' : ''}>50</option>
            <option value="100"${param.maxRows == '100' ? 'selected="selected"' : ''}>100</option>
            <option value="500"${param.maxRows == '500' ? 'selected="selected"' : ''}>500</option>
            <option value="1000"${param.maxRows == '1000' ? 'selected="selected"' : ''}>1000</option>
            <option value="-1"${param.maxRows == '-1' ? 'selected="selected"' : ''}>all</option>
        </select>
        &nbsp;Offset:
        <input type="text" size="2" name="offset" id="offset" value="${offset}"/>
        <input type="submit" name="action" value="Execute ([Ctrl+Enter])"  title="Executes the provided statement" />
        </span>
        <%--
        <br/>
        <input type="submit" name="action" value="Execute update" title="Use this button to execute any DB data/structure modifications queries, i.e. INSERT, UPDATE, DELETE, CREATE, ALTER etc." />
        --%>
    </form>
</fieldset>

<c:if test="${not empty param.query}">
    <c:catch var="dbError">
        <%
        	long actionTime = System.currentTimeMillis();
        	pageContext.setAttribute("jahiaDS", DatabaseUtils.getDatasource());
        %>
        <c:choose>
            <c:when test="${param.action == 'Execute update' || !fn:startsWith(fn:trim(fn:toLowerCase(param.query)), 'select')}">
                <sql:update dataSource="${jahiaDS}" sql="${param.query}" var="affected"/>
                <% pageContext.setAttribute("took", Long.valueOf(System.currentTimeMillis() - actionTime));  %>
                <fieldset>
                    <legend>Update executed in ${took} ms</legend>
                    <p>Affected <strong>${affected}</strong> row${affected > 1 ? 's' : ''}</p>
                </fieldset>
            </c:when>
            <c:otherwise>
                <sql:query dataSource="${jahiaDS}" sql="${param.query}" var="results" maxRows="${not empty param.maxRows ? param.maxRows : '-1'}" startRow="${not empty param.offset ? param.offset : '0'}"/>
                <% pageContext.setAttribute("took", Long.valueOf(System.currentTimeMillis() - actionTime));  %>
                <fieldset>
                    <legend>Displaying <strong>${results.rowCount} rows</strong> (query took ${took} ms)</legend>
                    <table border="1" cellspacing="0" cellpadding="5">
                        <thead>
                            <tr>
                                <th>#</th>
                                <c:forEach var="col" items="${results.columnNames}">
                                <th>${fn:escapeXml(col)}</th>
                                </c:forEach>
                            </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="row" items="${results.rows}" varStatus="status">
                            <tr>
                                <td><strong>${offset + status.index}</strong></td>
                                <c:forEach var="col" items="${results.columnNames}">
                                <td>${fn:escapeXml(row[col])}</td>
                                </c:forEach>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </fieldset>
            </c:otherwise>
        </c:choose>
    </c:catch>
    <c:if test="${not empty dbError}">
        <fieldset style="color: red">
            <legend><strong>Error</strong></legend>
            <pre>${fn:escapeXml(dbError)}</pre>
        </fieldset>
    </c:if>
</c:if>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
