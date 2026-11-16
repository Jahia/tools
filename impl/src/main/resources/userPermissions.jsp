<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page import="org.jahia.modules.tools.userpermissions.UserPermissionsAnalyzer,org.jahia.modules.tools.userpermissions.UserPermissionsAnalyzer.Result" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<!DOCTYPE html>
<html>
<%
    String selectedWorkspace = request.getParameter("workspace");
    if (selectedWorkspace == null || selectedWorkspace.trim().isEmpty()) {
        selectedWorkspace = "default";
    }
    pageContext.setAttribute("selectedWorkspace", selectedWorkspace);
    pageContext.setAttribute("checkPermissionParam", request.getParameter("checkPermission"));
    java.util.List<String> allPrivilegeNames = new java.util.ArrayList<>(
            org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry.getRegisteredPrivilegeNames());
    java.util.Collections.sort(allPrivilegeNames);
    pageContext.setAttribute("allPrivilegeNames", allPrivilegeNames);

    // Load available site keys for the site selector
    try {
        org.jahia.services.content.JCRSessionWrapper siteSession =
            org.jahia.services.content.JCRSessionFactory.getInstance().getCurrentSystemSession("default", null, null);
        javax.jcr.NodeIterator siteIt = siteSession.getNode("/sites").getNodes();
        java.util.List<String> siteKeys = new java.util.ArrayList<>();
        while (siteIt.hasNext()) {
            javax.jcr.Node sn = siteIt.nextNode();
            if (sn.isNodeType("jnt:virtualsite")) {
                siteKeys.add(sn.getName());
            }
        }
        java.util.Collections.sort(siteKeys);
        pageContext.setAttribute("siteKeys", siteKeys);
    } catch (Exception ignored) { /* site selector will be empty */ }

    if (request.getParameter("username") != null && !request.getParameter("username").trim().isEmpty()
            && request.getParameter("nodePath") != null && !request.getParameter("nodePath").trim().isEmpty()) {
        try {
            Result analysisResult = new UserPermissionsAnalyzer().analyze(request.getParameter("username"),
                    request.getParameter("nodePath"), selectedWorkspace, request.getParameter("checkPermission"),
                    request.getParameter("userSite"), request);
            pageContext.setAttribute("result", analysisResult);
        } catch (javax.jcr.PathNotFoundException e) {
            pageContext.setAttribute("errorMsg", "Node not found: " + request.getParameter("nodePath"));
        } catch (Exception e) {
            pageContext.setAttribute("errorMsg", e.getMessage());
        }
    }
%>
<c:set var="title" value="User Permissions Inspector"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
    <%@ include file="css.jspf" %>
    <link rel="stylesheet" href="<c:url value='/modules/tools/css/userPermissions.css'/>" type="text/css" />
</head>
<body>
<%@ include file="commons/header.jspf" %>

<div class="search-panel">
    <h3>Inspect user permissions on a node</h3>
    <form class="permissions-form" action="?" method="get">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <div class="form-grid">
            <div class="form-field form-field-username">
                <label for="username">Username or group</label>
                <input type="text" id="username" name="username" value="${fn:escapeXml(param.username)}"
                       placeholder="username or g:groupname"/>
            </div>
            <div class="form-field form-field-site">
                <label for="userSite">Site (user/group lookup)</label>
                <select id="userSite" name="userSite">
                    <option value="">(global)</option>
                    <c:forEach var="sk" items="${siteKeys}">
                        <option value="${fn:escapeXml(sk)}" <c:if test="${param.userSite == sk}">selected="selected"</c:if>>${fn:escapeXml(sk)}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-field form-field-wide">
                <label for="nodePath">Node path</label>
                <input type="text" id="nodePath" name="nodePath" value="${fn:escapeXml(param.nodePath)}"
                       placeholder="e.g. /sites/systemsite/home"/>
            </div>
            <div class="form-field form-field-workspace">
                <label for="workspace">Workspace</label>
                <select id="workspace" name="workspace">
                    <option value="default" <c:if test="${selectedWorkspace == 'default'}">selected="selected"</c:if>>default</option>
                    <option value="live" <c:if test="${selectedWorkspace == 'live'}">selected="selected"</c:if>>live</option>
                </select>
            </div>
            <c:if test="${not empty allPrivilegeNames}">
                <div class="form-field form-field-wide">
                    <label for="checkPermission">Check permission</label>
                    <select id="checkPermission" name="checkPermission">
                        <option value="">(none — show all roles only)</option>
                        <c:forEach var="privName" items="${allPrivilegeNames}">
                            <option value="${fn:escapeXml(privName)}" <c:if test="${checkPermissionParam == privName}">selected="selected"</c:if>>${fn:escapeXml(privName)}</option>
                        </c:forEach>
                    </select>
                </div>
            </c:if>
            <div class="form-field">
                <label>&nbsp;</label>
                <input type="submit" value="Inspect permissions"/>
            </div>
        </div>
    </form>
</div>

<c:if test="${not empty param.username and not empty param.nodePath}">
    <h2>Results for user <strong>${fn:escapeXml(param.username)}</strong> on node
        <code>${fn:escapeXml(param.nodePath)}</code>
        (workspace: <strong>${fn:escapeXml(selectedWorkspace)}</strong>)</h2>

    <c:if test="${not empty errorMsg}">
        <p class="error-msg" data-test-id="error-msg">${fn:escapeXml(errorMsg)}</p>
    </c:if>

    <c:if test="${not empty checkPermissionParam and empty errorMsg}">
        <div class="section-title">Quick permission check</div>
        <c:choose>
            <c:when test="${not empty result.permCheckError}">
                <div class="perm-check-result perm-check-denied">
                    <strong>&#9888; Check unavailable</strong> &mdash; ${fn:escapeXml(result.permCheckError)}
                </div>
            </c:when>
            <c:when test="${result.permGranted}">
                <div class="perm-check-result perm-check-granted">
                    <strong>&#10003; GRANTED</strong> &mdash;
                    <code>${fn:escapeXml(checkPermissionParam)}</code> is granted to
                    <strong>${fn:escapeXml(param.username)}</strong> on this node
                    (confirmed by <code>JCRNodeWrapper.hasPermission()</code>).
                    <c:if test="${not empty result.permCheckMatches}">
                        <div class="perm-check-detail">
                            <table>
                                <thead>
                                <tr>
                                    <th>Role</th>
                                    <th>Grant type</th>
                                    <th>Content node (where role is granted)</th>
                                    <th>Permission level</th>
                                    <th>Details</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="match" items="${result.permCheckMatches}">
                                    <tr>
                                        <td><strong>${fn:escapeXml(match.roleName)}</strong></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${match.grantType == 'GRANT'}"><span class="tag-grant">GRANT</span></c:when>
                                                <c:when test="${match.grantType == 'EXTERNAL'}"><span class="tag-external">EXTERNAL</span></c:when>
                                                <c:otherwise><span class="tag-deny">DENY</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="grant-node-row">
                                                <code>${fn:escapeXml(match.grantPath)}</code>
                                                <c:if test="${not empty match.contentEditorUrl}">
                                                    <a href="${match.contentEditorUrl}" target="_blank" class="edit-link">&#9998;&nbsp;edit</a>
                                                </c:if>
                                            </div>
                                        </td>
                                        <td>${fn:escapeXml(match.level)}</td>
                                        <td>${fn:escapeXml(empty match.detail ? '-' : match.detail)}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:if>
                    <c:if test="${empty result.permCheckMatches}">
                        <div class="perm-check-detail">
                            <em>The granting role could not be identified from the role analysis above
                            (may be via an aggregate privilege not expanded in the role scan).</em>
                        </div>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="perm-check-result perm-check-denied">
                    <strong>&#10007; NOT GRANTED</strong> &mdash;
                    <code>${fn:escapeXml(checkPermissionParam)}</code> is not granted to
                    <strong>${fn:escapeXml(param.username)}</strong> on this node
                    (confirmed by <code>JCRNodeWrapper.hasPermission()</code>).
                    <c:if test="${not empty result.permCheckMatches}">
                        <div class="perm-check-detail">
                            The following roles define this permission but it is effectively denied:
                            <table>
                                <thead>
                                <tr>
                                    <th>Role</th>
                                    <th>Grant type</th>
                                    <th>Content node (where role is granted)</th>
                                    <th>Permission level</th>
                                    <th>Details</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="match" items="${result.permCheckMatches}">
                                    <tr>
                                        <td><strong>${fn:escapeXml(match.roleName)}</strong></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${match.grantType == 'GRANT'}"><span class="tag-grant">GRANT</span></c:when>
                                                <c:when test="${match.grantType == 'EXTERNAL'}"><span class="tag-external">EXTERNAL</span></c:when>
                                                <c:otherwise><span class="tag-deny">DENY</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="grant-node-row">
                                                <code>${fn:escapeXml(match.grantPath)}</code>
                                                <c:if test="${not empty match.contentEditorUrl}">
                                                    <a href="${match.contentEditorUrl}" target="_blank" class="edit-link">&#9998;&nbsp;edit</a>
                                                </c:if>
                                            </div>
                                        </td>
                                        <td>${fn:escapeXml(match.level)}</td>
                                        <td>${fn:escapeXml(empty match.detail ? '-' : match.detail)}</td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <c:if test="${empty errorMsg}">
        <div class="section-title">All effective roles and permissions</div>
        <c:choose>
            <c:when test="${empty result.roleResults}">
                <p class="no-results" data-test-id="no-results">No permissions found for this user on this node.</p>
            </c:when>
            <c:otherwise>
                <div class="role-cards" data-test-id="role-cards">
                    <c:forEach var="roleResult" items="${result.roleResults}">
                        <div class="role-card" data-test-id="role-card" data-role-name="${fn:escapeXml(roleResult.roleName)}">
                            <div class="role-card-header">
                                <span class="role-name" data-test-id="role-name">${fn:escapeXml(roleResult.roleName)}</span>
                                <span class="role-principal <c:if test="${fn:startsWith(roleResult.matchedPrincipal, 'g:')}">role-principal-group</c:if>" data-test-id="role-principal">${fn:escapeXml(roleResult.matchedPrincipal)}</span>
                                <span class="role-tag">
                                <c:choose>
                                    <c:when test="${roleResult.grantType == 'GRANT'}"><span class="tag-grant">GRANT</span></c:when>
                                    <c:when test="${roleResult.grantType == 'DENY'}"><span class="tag-deny">DENY</span></c:when>
                                    <c:otherwise><span class="tag-external">${fn:escapeXml(roleResult.grantType)}</span></c:otherwise>
                                </c:choose>
                                </span>
                            </div>

                            <div class="role-card-meta">
                                <span class="meta-label">Content node</span>
                                <div class="grant-node-row">
                                    <code>${fn:escapeXml(roleResult.contentGrantPath)}</code>
                                    <c:if test="${not empty roleResult.contentEditorUrl}">
                                        <a href="${roleResult.contentEditorUrl}" target="_blank" class="edit-link">&#9998;&nbsp;edit</a>
                                    </c:if>
                                </div>
                                <span class="meta-label">Role</span>
                                <code>${fn:escapeXml(empty roleResult.roleNodePath ? '(not found)' : roleResult.roleNodePath)}</code>
                            </div>

                            <div class="role-card-perms">
                                <div class="perm-col" data-test-id="node-perms-col">
                                    <div class="perm-col-title">Node permissions</div>
                                    <c:choose>
                                        <c:when test="${empty roleResult.nodePermissions}">
                                            <span class="no-perm">(none)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <ul class="perm-list">
                                                <c:forEach var="perm" items="${roleResult.nodePermissions}">
                                                    <li style="padding-left:${perm.indentPx}px">
                                                        <c:choose>
                                                            <c:when test="${perm.hierarchical}">
                                                                <details class="perm-detail">
                                                                    <summary><code class="${result.highlightedPermMap[perm.path] ? 'highlight-perm' : ''}">${fn:escapeXml(perm.localName)}</code></summary>
                                                                    <span class="perm-full-path">${fn:escapeXml(perm.path)}</span>
                                                                </details>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <code class="${result.highlightedPermMap[perm.path] ? 'highlight-perm' : ''}">${fn:escapeXml(perm.path)}</code>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                        </c:otherwise>
                                    </c:choose>
                                </div>

                                <div class="perm-col" data-test-id="site-perms-col">
                                    <div class="perm-col-title">Site permissions</div>
                                    <c:choose>
                                        <c:when test="${empty roleResult.sitePermissions}">
                                            <span class="no-perm">(none)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <c:forEach var="sitePermGroup" items="${roleResult.sitePermissions}">
                                                <div class="perm-section">
                                                    <div class="perm-section-label">
                                                        <strong>${fn:escapeXml(sitePermGroup.name)}</strong>
                                                        &mdash; checked on <code>${fn:escapeXml(sitePermGroup.targetPath)}</code>
                                                    </div>
                                                    <c:choose>
                                                        <c:when test="${empty sitePermGroup.permissions}">
                                                            <span class="no-perm">(none)</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <ul class="perm-list">
                                                                <c:forEach var="perm" items="${sitePermGroup.permissions}">
                                                                    <li style="padding-left:${perm.indentPx}px">
                                                                        <c:choose>
                                                                            <c:when test="${perm.hierarchical}">
                                                                                <details class="perm-detail">
                                                                                    <summary><code class="${result.highlightedPermMap[perm.path] ? 'highlight-perm' : ''}">${fn:escapeXml(perm.localName)}</code></summary>
                                                                                    <span class="perm-full-path">${fn:escapeXml(perm.path)}</span>
                                                                                </details>
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                <code class="${result.highlightedPermMap[perm.path] ? 'highlight-perm' : ''}">${fn:escapeXml(perm.path)}</code>
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                    </li>
                                                                </c:forEach>
                                                            </ul>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </c:forEach>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>
</c:if>

<%@ include file="commons/footer.jspf" %>
</body>
</html>
