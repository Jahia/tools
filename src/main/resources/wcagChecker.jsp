<%@ page contentType="text/html; charset=UTF-8" language="java"
%><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%@ include file="css.jspf" %>
<title>WCAG Validator Service</title>
</head>
<body>
<%@ include file="logout.jspf" %>
<h1>WCAG Validator Service</h1>
<form id="wcag" action="${pageContext.request.contextPath}/cms/wcag/validate" method="post">
<p>Paste here the HTML text to be validated:</p>
<p><textarea rows="5" cols="80" id="text" name="text"></textarea></p>
<p><input type="submit" value="Validate" /></p>
</form>
<%@ include file="gotoIndex.jspf" %>
</body>
</html>