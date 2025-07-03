<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@page import="org.jahia.services.SpringContextSingleton" %>
<%@page import="org.jahia.services.transform.DocumentConverterService" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="title" value="Jahia Document Conversion Service"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<%
    DocumentConverterService service = (DocumentConverterService) SpringContextSingleton.getBean("DocumentConverterService");
    pageContext.setAttribute("serviceEnabled", service != null && service.isEnabled());
%>
<c:if test="${serviceEnabled}">
    <form id="conversion" action="${pageContext.request.contextPath}/cms/convert" enctype="multipart/form-data"
          method="post">
        <p>
            <label for="file">Choose a file to upload:&nbsp;</label><input name="file" id="file" type="file"/>
        </p>
        <p>
            <label for="mimeType">Target document type:&nbsp;</label>
            <select id="mimeType" name="mimeType">
                <option value="application/pdf">Adobe PDF</option>
                <option value="application/msword">Microsoft Word Document</option>
                <option value="application/vnd.ms-excel">Microsoft Excel Sheet</option>
                <option value="application/vnd.ms-powerpoint">Microsoft Powerpoint Presentation</option>
                <option value="application/vnd.oasis.opendocument.text">OpenDocument Text</option>
                <option value="application/vnd.oasis.opendocument.spreadsheet">OpenDocument Spreadsheet</option>
                <option value="application/vnd.oasis.opendocument.presentation">OpenDocument Presentation</option>
                <option value="application/x-shockwave-flash">Flash</option>
                <option value="text/plain">Text</option>
            </select>
        </p>
        <p><input type="submit" value="Convert file"/></p>
    </form>
</c:if>
<c:if test="${!serviceEnabled}">
    <p>Conversion service is not enabled.</p>
</c:if>
<%@ include file="commons/footer.jspf" %>
</body>
</html>
