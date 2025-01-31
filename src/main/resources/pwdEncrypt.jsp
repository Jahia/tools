<%@ page contentType="text/html; charset=UTF-8" language="java"
%>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ page import="org.jahia.services.pwd.PasswordService" %>
<%@ page import="org.jahia.utils.EncryptionUtils" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <%@ include file="css.jspf" %>
    <title>Password encryption</title>
</head>
<body>
<%@ include file="logout.jspf" %>
<h1>Password encryption</h1>
<c:if test="${not empty param.pwd}">
    <c:choose>
        <c:when test="${param.digest == 'legacy'}">
            <% pageContext.setAttribute("digest", EncryptionUtils.sha1DigestLegacy(request.getParameter("pwd"))); %>
        </c:when>
        <c:when test="${param.digest == 'strong'}">
            <% pageContext.setAttribute("digest", PasswordService.getInstance().digest(request.getParameter("pwd"), true)); %>
        </c:when>
        <c:otherwise>
            <% pageContext.setAttribute("digest", PasswordService.getInstance().digest(request.getParameter("pwd"))); %>
        </c:otherwise>
    </c:choose>
    <p style="color: blue">Encrypted password for <strong>${fn:escapeXml(param.pwd)}</strong> is:<br/><span
            class="copy-to-clipboard" title="Copy to clipboard">${digest}</span></p>
    <button class="empty-clipboard" title="Empty clipboard">Empty clipboard</button>
</c:if>
<form id="pwdEncrypt" action="?" method="get">
    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
    <p>
        <label for="pwd">Provide a password you would like to digest:</label><br/>
        <input type="text" id="pwd" name="pwd" value="${fn:escapeXml(param.pwd)}" size="30"/>
        <input type="submit" value="Encrypt"/>
        <br/>
        <input type="radio" name="digest" id="digest-default"
               value="default" ${empty param.digest || param.digest == 'default' ? 'checked="checked"' : ''}/><label
            for="digest-default">Default digest</label><br/>
        <input type="radio" name="digest" id="digest-strong"
               value="strong" ${param.digest == 'strong' ? 'checked="checked"' : ''}/><label for="digest-strong">Strong
        digest (e.g. for root and tools user)</label><br/>
        <input type="radio" name="digest" id="digest-legacy"
               value="legacy" ${param.digest == 'legacy' ? 'checked="checked"' : ''}/><label for="digest-legacy">Legacy
        digest</label><br/>
    </p>
</form>
<%@ include file="gotoIndex.jspf" %>
<c:if test="${not empty param.pwd}">
    <script type="text/javascript">
        document.addEventListener('DOMContentLoaded', () => {
            document.querySelector('.copy-to-clipboard').addEventListener('click', (event) => {
                if (!navigator.clipboard) {
                    return
                }
                const digest = event.target.innerText;
                console.log('Copying to clipboard', digest);
                navigator.clipboard.writeText(digest);
            });
            document.querySelector('.empty-clipboard').addEventListener('click', (event) => {
                if (!navigator.clipboard) {
                    return
                }
                console.log('Emptying clipboard');
                navigator.clipboard.writeText('');
            });
        });
    </script>
</c:if>
</body>
</html>
