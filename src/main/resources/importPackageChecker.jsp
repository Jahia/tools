<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.FindImportPackage" %>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.OSGIAnalyzer" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="title" value="OSGI Import-Package checker"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>

<body>
<%@ include file="commons/header.jspf" %>
<div>

    <fieldset>
        <legend>Perform Import-Package checker</legend>
        <form id="importPackageChecker" action="?" method="get">
            <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
            <label for="regexp">
                Return Import-Package clause where package name match regExp:
            </label><br>
            <input name="regexp" id="regexp" value="${not empty param.regexp ? param.regexp : ''}" size="100"/><br>
            <label for="regexp">
                (ex: <b>org\.springframework\..*</b> will match <u>org.springframework.jdbc.datasource; version="[3.2, 4)"</u> and <u>org.springframework.jdbc.datasource; "[5.3.29, 5.4)"</u> and <u>org.springframework.jdbc.datasource</u>
            </label>
            <br>
            <br>

            <label for="version">
                Return Import-Package with version limitations matching given version:
            </label><br>
            <input name="version" id="version" value="${not empty param.version ? param.version : ''}"/><br>
            <label for="version">
                (ex: <b>3.2.19</b> will only match <u>org.springframework.jdbc.datasource; version="[3.2, 4)"</u> and <u>org.springframework.jdbc.datasource</u>)
            </label>
            <br><br>

            <input name="matchVersionRangeMissing" id="matchVersionRangeMissing" type="checkbox" class="cbProbe category-db"
                ${not empty param.matchVersionRangeMissing ? 'checked="checked"' : ''}>
            <label for="matchVersionRangeMissing">
                Return Import-Package with no version limitation specified<br>
                (if checked will only match: <u>org.springframework.jdbc.datasource</u>)
            </label>
            <br><br>

            <input type="submit" name="do" value="Execute"  title="Execute" />
        </form>
    </fieldset>

    <c:if test="${not empty param.do}">
        <%
            FindImportPackage result = OSGIAnalyzer.findImportPackages(request.getParameter("regexp"), request.getParameter("version"), request.getParameter("matchVersionRangeMissing") != null);
            pageContext.setAttribute("result", result);
        %>

        <fieldset>
            <legend><strong>(${result.totalMatchCount})</strong> Matching Import-Package</legend>

            <c:choose>
                <c:when test="${empty result.bundles}">
                    <strong>No matching Import-Package found</strong>
                </c:when>
            </c:choose>

            <ul>
                <c:forEach items="${result.bundles}" var="entry">
                    <li>
                        <a href="<c:url value='/tools/osgi/console/bundles/${entry.id}'/>" title="See details">
                            [${entry.id}]
                        </a>
                        <strong>${entry.displayName} [${entry.id}]</strong>

                        <ul>
                            <c:forEach items="${entry.matchingImportedPackage}" var="importedPackage">
                                <li>${importedPackage}</li>
                            </c:forEach>
                        </ul>
                    </li>
                </c:forEach>
            </ul>
        </fieldset>
    </c:if>
</div>

<%@ include file="commons/footer.jspf" %>

</body>
</html>
