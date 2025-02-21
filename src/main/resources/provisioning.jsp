<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.jahia.services.provisioning.ProvisioningManager" %>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="css.jspf" %>
    <title>Run provisioning script</title>
</head>
<body>
<%@ include file="logout.jspf" %>
<h1>Run provisioning script</h1>
<a target="_blank" href="https://academy.jahia.com/documentation/jahia-cms/jahia-8.2/dev-ops/provisioning/creating-a-provisioning-script">Link to academy</a>
<p>Paste here the provisioning script you would like to execute against Jahia:</p>
<form method="post" id="provisioningForm">
    <textarea name="script" id="provisioning" style="width: 100%; height: 500px;" required></textarea>
    <fieldset id="errorField" hidden>
        <legend style="color: red">Error</legend>
        <p style="color: red" id="yamlMessage"></p>
    </fieldset>
    <button id="submitYaml" type="button" disabled> Run provisioning script </button>
</form>
<%@ include file="gotoIndex.jspf" %>
<h2 hidden="true" id="provisioningMessage"><strong>Request sent, waiting for provisioning API response</strong></h2>
<strong><h2 hidden=true id="provisioningResult"></h2></strong>
<c:if test="${not empty param.script}">
    <%
        ProvisioningManager provisioningManager = (ProvisioningManager) BundleUtils.getOsgiService("org.jahia.services.provisioning.ProvisioningManager", null);
        provisioningManager.executeScript(request.getParameter("script"), "yaml");
    %>
</c:if>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/provisioning.tools.bundle.js'/>"></script>
</body>