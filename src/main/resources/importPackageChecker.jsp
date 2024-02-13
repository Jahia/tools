<%@ page import="org.jahia.modules.tools.gql.admin.osgi.OSGIPackageHeaderChecker" %>
<%@ page import="org.jahia.modules.tools.gql.admin.osgi.FindImportPackage" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="css.jspf" %>
    <title>OSGI Import-Package checker</title>
</head>

<body>
<div>
    <h1>OSGI Import-Package checker</h1>

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
            FindImportPackage result = OSGIPackageHeaderChecker.findImportPackages(request.getParameter("regexp"), request.getParameter("version"), request.getParameter("matchVersionRangeMissing") != null);
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
                        <a href="<c:url value='/tools/osgi/console/bundles/${entry.bundleId}'/>" title="See details">
                            [${entry.bundleId}]
                        </a>
                        <strong>${entry.bundleDisplayName} [${entry.bundleId}]</strong>

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

<%@ include file="gotoIndex.jspf" %>

</body>
</html>
