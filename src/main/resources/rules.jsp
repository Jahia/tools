<%@page import="org.drools.core.base.EnabledBoolean"%>
<%@page import="org.drools.core.spi.Enabled"%>
<%@page import="org.drools.core.rule.Rule"%>
<%@page import="java.lang.reflect.Field"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"
%><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="org.jahia.services.content.rules.RulesListener"%>
<%@ page import="java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Business Rules</title>
    <%@ include file="css.jspf" %>
</head>
<body>
    <%@ include file="logout.jspf" %>
    <h1>Business Rules</h1>
    <p>
        Here is a list of all business rules, currently registered in the system.
        <a href="?refresh=true&toolAccessToken=${toolAccessToken}" title="Refresh"><img src="<c:url value='/icons/refresh.png'/>" alt="refresh" title="Refresh" height="16" width="16"/></a>
    </p>
    <p>
        Note, please, the enable/disable actions here are not persistent, meaning they influence the state of a rule only till the next server restart and only on this current cluster node.<br/>
        If you would like to <strong>permanently disable some rules</strong>, please use the configuration approach:
        <a class="popupLink" title="How to permanently disable rules?" href="#disablingRules" data-src="#disablingRules" data-fancybox><img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="?" title="How to permanently disable rules?"/></a>
    </p>
    <div style="display: none;">
        <div id="disablingRules">
            <h3>How to permanently disable rules?</h3>
            <p>
            In order to disable some rules permanently a configuration entry can be added into <code>jahia.properties</code> or <code>jahia.custom.properties</code> file in the following format:
            <pre>
            jahia.rules.disabledConfig=["&lt;workspace&gt;".]"&lt;package-name-1&gt;"."&lt;rule-name-1&gt;",["&lt;workspace&gt;".]"&lt;package-name-2&gt;"."&lt;rule-name-2&gt;"...
            </pre>
            The workspace part is optional. For example the following configuration:
            <pre>
            jahia.rules.disabledConfig="org.jahia.modules.rules"."Image update","live"."org.jahia.modules.dm.thumbnails"."Automatically generate thumbnail for the document"
            </pre>
            will disable rule <code>"Image update"</code> (from package <code>org.jahia.modules.rules</code>) in rules for all workspaces (live and
            default)<br/> and the rule <code>"Automatically generate thumbnail for the document"</code> (package
            <code>org.jahia.modules.dm.thumbnails</code>) will be disabled in rules for live workspace only.
            </p>
        </div>
    </div>

    <jsp:useBean id="ruleHelper" class="org.jahia.modules.tools.RuleHelper"/>
	<c:if test="${('enable' == param.action or 'disable' == param.action) && not empty param.listener && not empty param.pkg && not empty param.rule}">
	    <% if (ruleHelper.updateRuleState(request.getParameter("listener"), request.getParameter("pkg"), request.getParameter("rule"), "enable".equals(request.getParameter("action")))) {%>
	        <p style="color: blue">The rule <strong>${fn:escapeXml(param.rule)}</strong> has been temporary <strong>${param.action}d</strong>.</p>
	    <% } %>
	</c:if>
	<c:forEach var="listener" items="${ruleHelper.data}" varStatus="listenerStatus">
	<h2>Rules for workspace: ${listener.key.workspace}${listener.key.availableDuringPublish ? ' (also available during publication)' : ''}</h2>
	<ul>
	<c:forEach var="pkg" items="${listener.value}" varStatus="pkgStatus">
	    <li>
	        <c:set var="contentId" value="packageContent-${listenerStatus.index}-${pkgStatus.index}"/>
	        <c:set var="packageName" value="${pkg.name} [${pkg.origin}]"/>
	        <a class="popupLink" title="View package content" data-src="#${contentId}" data-fancybox href="#${contentId}"><img src="<c:url value='/icons/filePreview.png'/>" width="16" height="16" alt="?" title="View package content"/></a>
	        <strong>${fn:escapeXml(packageName)}</strong>
	        <ul>
	        <c:forEach var="rule" items="${pkg.rules}">
	            <c:set var="ruleEnabled" value="${rule.enabled == 'true'}"/>
	            <c:url value="/icons/publication/${!ruleEnabled ? 'not' : ''}published.png" var="iconUrl"/>
	            <c:url value="/icons/${ruleEnabled ? 'cancel' : 'accept'}.png" var="actionIconUrl"/>
	            <li><img src="${iconUrl}" alt="${ruleEnabled ? 'on' : 'off'}" height="12" width="12"/> ${rule.name} [<a href="?listener=${fn:escapeXml(listener.key)}&amp;pkg=${fn:escapeXml(pkg.name)}&amp;rule=${fn:escapeXml(rule.name)}&amp;action=${ruleEnabled ? 'disable' : 'enable'}&toolAccessToken=${toolAccessToken}" title="Temporary ${ruleEnabled ? 'disable' : 'enable'} this rule"><img src="${actionIconUrl}" alt="${ruleEnabled ? 'disable' : 'enable'}" title="Temporary ${ruleEnabled ? 'disable' : 'enable'} this rule" height="16" width="16"/></a>]</li>
	        </c:forEach>
	        </ul>
	        <div style="display: none;">
	            <div id="${contentId}">
	                <h3>${fn:escapeXml(packageName)}</h3>
	                <pre>${fn:escapeXml(pkg.content)}</pre>
	            </div>
	        </div>
	    </li>
	</c:forEach>
	</ul>
	</c:forEach>

<%@ include file="gotoIndex.jspf" %>
    <script type="module" src="<c:url value='/modules/tools/javascript/apps/fancybox.tools.bundle.js'/>"></script>
</body>
</html>
