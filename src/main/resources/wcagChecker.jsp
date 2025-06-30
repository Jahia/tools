<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="WCAG Validator Service"/>
<head>
<%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<form id="wcag" action="${pageContext.request.contextPath}/cms/wcag/validate" method="post">
<p>Paste here the HTML text to be validated:</p>
<p><textarea rows="5" cols="80" id="text" name="text"></textarea></p>
<p><input type="submit" value="Validate" /></p>
</form>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
