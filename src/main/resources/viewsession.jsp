<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib uri="http://www.jahia.org/tags/utilityLib" prefix="utility" %>
<c:set var="title" value="Session Viewer JSP"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
    <style type="text/css">
        <!--

        ol.attribute {
            /* border: 1px solid #CFD9E1; */
            display: block;
            padding: 2px;
            clear: both;
        }

        ol.attribute li {
            background: #CFD9E1;
            display: block;
            width: 100%;
        }

        div.map ol.entry {
            background: #CFD9E1;
            display: block;
            padding: 0;
            width: 100%;
            clear: both;
        }

        div.map ol.entry li {
            background: #CFD9E1;
            display: inline;
            float: left;
        }

        div.map ol.entry li.key {
            width: 12%;
        }

        div.map ol.entry li.key-type {
            width: 10%;
        }

        div.map ol.entry li.value-type {
            width: 20%;
        }

        div.map ol.entry li.value {
            width: 55%;
        }

        -->
    </style>
</head>
<body>
<%@ include file="commons/header.jspf" %>
<utility:sessionViewer/>
</body>
</html>
