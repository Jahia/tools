<%@ page contentType="text/html;charset=UTF-8" language="java"
        %><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="javax.jcr.nodetype.PropertyDefinition"%>
<%@page import="javax.jcr.version.Version" %>
<%@page import="org.apache.commons.collections.IteratorUtils"%>
<%@page import="org.apache.commons.lang3.StringUtils" %>
<%@page import="org.apache.jackrabbit.core.JahiaRepositoryImpl"%>
<%@page import="javax.jcr.version.VersionIterator" %>
<%@page import="java.util.*" %>
<%@ page import="org.jahia.api.Constants" %>
<%@ page import="org.jahia.services.content.*" %>
<%@ page import="org.jahia.services.content.nodetypes.NodeTypeRegistry" %>
<%@ page import="javax.jcr.nodetype.NodeType" %>
<%@ page import="javax.jcr.nodetype.NodeTypeIterator" %>
<%@ page import="javax.jcr.*" %>
<%@ page import="org.jahia.services.usermanager.JahiaUserManagerService" %>
<%@ page import="org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<%@taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr"%>
<%@taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<utility:useConstants var="jcrPropertyTypes" className="org.jahia.services.content.nodetypes.ExtendedPropertyType" scope="application"/>
<c:set var="showProperties" value="${functions:default(fn:escapeXml(param.showProperties), 'false')}"/>
<c:set var="showReferences" value="${functions:default(fn:escapeXml(param.showReferences), 'false')}"/>
<c:set var="showNodes" value="${functions:default(fn:escapeXml(param.showNodes), 'true')}"/>
<c:set var="sortNodes" value="${functions:default(fn:escapeXml(param.sortNodes), 'false')}"/>
<c:set var="showActions" value="${functions:default(fn:escapeXml(param.showActions), 'false')}"/>
<c:set var="showVersions" value="${functions:default(param.showVersions, 'false')}"/>
<c:set var="workspace" value="${functions:default(fn:escapeXml(param.workspace), 'default')}"/>
<c:set var="nodeId" value="${not empty param.uuid ? fn:trim(fn:escapeXml(param.uuid)) : 'cafebabe-cafe-babe-cafe-babecafebabe'}"/>
<c:set var="showJCRNodes" value="${not empty param.showJCRNodes ? fn:trim(fn:escapeXml(param.showJCRNodes)) : 'false'}"/>
<%!
	private static Comparator<Item> BY_NAME_COMPARATOR = new Comparator<Item>() {
	    public int compare(Item o1, Item o2) {
	        try {
	        	return o1.getName().compareTo(o2.getName());
	        } catch (Exception e) {
	            // ignore
	        }
	        return 0;
	    }
	};

    private void traceVersionTree(Version v, List<StringBuffer> lines, Map<String, int[]> m, int currentLine, int col) throws Exception {
        m.put(v.getName(), new int[]{currentLine,col});
        while (lines.size() <= currentLine) {
            lines.add(new StringBuffer());
        }

        String preds = "";
        for (Version pred : v.getPredecessors()) {
            preds += pred.getName() + ",";
        }

        String[] s = v.getContainingHistory().getVersionLabels(v);

        StringBuffer stringBuffer = lines.get(currentLine);
        String str = (stringBuffer.length()==0 ? "  " : "  -> ") + v.getName() + " "+(s.length == 0 ? "": Arrays.asList(s));

        for (Version version : v.getSuccessors()) {
            if (m.containsKey(version.getName())) {
                int[] c = m.get(version.getName());
                if (c[0] == 0) {
                    col = c[1];
                    StringBuffer l = lines.get(currentLine - 1);
                    if (l.length() < col + 3) {
                        l.append(StringUtils.repeat(" ", col + 3 - l.length()));
                    }
                    l.append("-");
                } else {
                    str += ("(‚-"+version.getName());
                }
            }
        }

        if (stringBuffer.length() < col+str.length()) {
            stringBuffer.append(StringUtils.repeat((stringBuffer.length()==0 ? " " : "-"), col+str.length() - stringBuffer.length()));
        }
        stringBuffer.replace(col, col + str.length(), str);

        int nextCol = stringBuffer.length();
        int lineNumber = currentLine;
        for (Version version : v.getSuccessors()) {
            if (!m.containsKey(version.getName())) {
                traceVersionTree(version, lines, m ,lineNumber, nextCol);
                lineNumber +=2;
            }
        }
    }

    private List<String> getNodeTypes(Node node) throws RepositoryException {
        List<String> results = new ArrayList<String>();
        if (NodeTypeRegistry.getInstance().hasNodeType(node.getPrimaryNodeType().getName())) {
            results.add(node.getPrimaryNodeType().getName());
        } else {
            results.add("nt:base");
        }
        NodeType[] mixin = node.getMixinNodeTypes();
        for (int i = 0; i < mixin.length; i++) {
            NodeType mixinType = mixin[i];
            if (NodeTypeRegistry.getInstance().hasNodeType(mixinType.getName())) {
                results.add(mixinType.getName());
            }
        }

        return results;
    }
%>
<%
    long timer = System.currentTimeMillis();
    JCRSessionFactory.getInstance().setCurrentUser(JahiaUserManagerService.getInstance().lookupRootUser().getJahiaUser());

    Session jcrSession = JCRSessionFactory.getInstance().getCurrentUserSession((String) pageContext.getAttribute("workspace"));

    boolean showJCRNodes = Boolean.parseBoolean((String) pageContext.getAttribute("showJCRNodes"));
    if (showJCRNodes) {
        jcrSession = ((JCRSessionWrapper) jcrSession).getProviderSession(((JCRSessionWrapper) jcrSession).getNode("/").getProvider());
    }

    try {
        Node node = null;
        if (request.getParameter("path") != null && request.getParameter("path").length() > 0) {
            node = jcrSession.getNode(JCRContentUtils.escapeNodePath(request.getParameter("path")));
            pageContext.setAttribute("nodeId", node.getIdentifier());
        } else {
            node = jcrSession.getNodeByIdentifier((String) pageContext.getAttribute("nodeId"));
        }
        pageContext.setAttribute("node", node);
        pageContext.setAttribute("currentNode", node);
        if (jcrSession instanceof JCRSessionWrapper && node instanceof JCRNodeWrapper && ((JCRNodeWrapper) node).getProvider().isVersioningAvailable() && node.isNodeType(Constants.MIX_VERSIONABLE)) {
            VersionIterator versionIterator = jcrSession.getWorkspace().getVersionManager().getVersionHistory(node.getPath()).getAllLinearVersions();
            pageContext.setAttribute("versionIterator", versionIterator);

            Version v = jcrSession.getWorkspace().getVersionManager().getVersionHistory(node.getPath()).getRootVersion();
            List<StringBuffer> lines = new ArrayList<StringBuffer>();
            traceVersionTree(v, lines, new HashMap<String,int[]>(),0,0);
            pageContext.setAttribute("versionGraph", lines);
        }
%>
<head>
    <title>JCR Browser</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="css.jspf" %>
    <script type="text/javascript">
        function go(id1, value1, id2, value2, id3, value3) {
            document.getElementById(id1).value=value1;
            if ('path' === id1) {
                document.getElementById('uuid').value='';
            }
            if (id2 != null) {
                document.getElementById(id2).value=value2;
            }
            if (id3 != null) {
                document.getElementById(id3).value=value3;
            }

            // If switching workspace reset target and action to avoid invoking functions
            if (id1 === 'workspace') {
                document.getElementById('target').value = '';
                document.getElementById('action').value = '';
            }

            document.getElementById('navigateForm').submit();
        }
    </script>
</head>
<body>
    <%@ include file="logout.jspf" %>
<c:url var="mgrUrl" value="/engines/manager.jsp">
    <c:param name="selectedPaths" value="${currentNode.path}"/>
    <c:param name="workspace" value="${workspace}"/>
    <c:param name="jahia.ui.theme" value="default"/>
</c:url>
<div class="${workspace}">
<fieldset>
    <form id="navigateForm" action="?" method="get">
        <input type="hidden" id="showProperties" name="showProperties" value="${showProperties}"/>
        <input type="hidden" id="showReferences" name="showReferences" value="${showReferences}"/>
        <input type="hidden" id="showNodes" name="showNodes" value="${showNodes}"/>
        <input type="hidden" id="sortNodes" name="sortNodes" value="${sortNodes}"/>
        <input type="hidden" id="showActions" name="showActions" value="${showActions}"/>
        <input type="hidden" id="showVersions" name="showVersions" value="${showVersions}"/>
        <input type="hidden" id="workspace" name="workspace" value="${workspace}"/>
        <input type="hidden" id="path" name="path" value=""/>
        <input type="hidden" id="uuid" name="uuid" value="${nodeId}"/>
        <input type="hidden" id="value" name="value" value=""/>
        <input type="hidden" id="action" name="action" value=""/>
        <input type="hidden" id="target" name="target" value=""/>
        <input type="hidden" id="showJCRNodes" name="showJCRNodes" value="${showJCRNodes}"/>
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
    </form>
    <input type="text" id="goToPath" name="goToPath" value="${fn:escapeXml(node.path)}"
           onkeypress="if ((event || window.event).keyCode == 13) go('path', this.value);" />
    &nbsp;<a href="#go"
             onclick='var path=document.getElementById("goToPath").value; if (path.length > 0) { go("path", path); } return false;' title="Go to the node with path"
        ><img src="<c:url value='/icons/refresh.png'/>" height="16" width="16" title="Go to the node with path" border="0" style="vertical-align: middle;"/></a>
    <label for="goToUuid">UUID: </label>
    <input type="text" id="goToUuid" name="goToUuid" value=""
           onkeypress="if ((event || window.event).keyCode == 13) go('uuid', this.value);" />
    &nbsp;<a href="#go" onclick='var uuid=document.getElementById("goToUuid").value; if (uuid.length > 0) { go("uuid", uuid); } return false;' title="Go to the node with UUID"><img src="<c:url value='/icons/search.png'/>" height="16" width="16" title="Go to the node with UUID" border="0" style="vertical-align: middle;"/></a>
</fieldset>

<fieldset>
<c:url value="/icons/${workspace == 'default' ? 'editMode' : 'live'}.png" var="iconWorkspace"/>
<c:url value="/icons/${workspace == 'default' ? 'live' : 'editMode'}.png" var="iconSwitchWorkspace"/>
<c:url value="/icons/${showActions ? 'preview' : 'editContent'}.png" var="iconActions"/>
<c:set var="anotherWorkspace" value="${workspace == 'default' ? 'live' : 'default'}"/>
    <legend><strong>This is the ${workspace} workspace</strong>&nbsp;(<a href="#switchWorkspace" onclick="go('workspace', '${anotherWorkspace}'); return false;">switch to ${anotherWorkspace}</a>)
    &nbsp; browse as ${showJCRNodes?"<span style='color:red;font-weight:bold'>jcr</span>":"jahia"} session (<a href="#showJCRNodes" onclick="go('showJCRNodes','${showJCRNodes?"false":"true"}'); return false;">switch to ${!showJCRNodes?"jcr":"jahia"}</a>) )
    &nbsp;
    <a href="${mgrUrl}" target="_blank"><img src="<c:url value='/icons/fileManager.png'/>" width="16" height="16" alt=" " role="presentation" style="position:relative; top: 4px; margin-right:2px;"/>repository explorer</a>
</legend>

<fieldset style="position: absolute; right: 20px;">
    <legend><strong>Settings</strong></legend>
    <p>
        <input id="cbActions" type="checkbox" ${showActions ? 'checked="checked"' : ''}
               onchange="go('showActions', '${!showActions}')"/>&nbsp;<label for="cbActions">Show actions</label><br/>
        <input id="cbProperties" type="checkbox" ${showProperties ? 'checked="checked"' : ''}
               onchange="go('showProperties', '${!showProperties}')"/>&nbsp;<label for="cbProperties">Show properties</label><br/>
        <input id="cbNodes" type="checkbox" ${showNodes ? 'checked="checked"' : ''}
               onchange="go('showNodes', '${!showNodes}')"/>&nbsp;<label for="cbNodes">Show child nodes</label><br/>
        <input id="cbSortNodes" type="checkbox" ${sortNodes ? 'checked="checked"' : ''}
               onchange="go('sortNodes', '${!sortNodes}')"/>&nbsp;<label for="cbSortNodes">Sort child nodes</label><br/>
        <input id="cbReferences" type="checkbox" ${showReferences ? 'checked="checked"' : ''}
               onchange="go('showReferences', '${!showReferences}')"/>&nbsp;<label for="cbReferences">Show references</label><br/>
        <input id="cbVersions" type="checkbox" ${showVersions ? 'checked="checked"' : ''}
               onchange="go('showVersions', '${!showVersions}')"/>&nbsp;<label for="cbVersions">Show versioning</label><br/>
    </p>
</fieldset>

<c:if test="${not empty param.action}">
    <c:choose>
        <c:when test="${param.action == 'delete' && not empty param.target}">
            <% Node target = jcrSession.getNodeByIdentifier(request.getParameter("target"));
                pageContext.setAttribute("target", target);
                if (!jcrSession.getWorkspace().getVersionManager().isCheckedOut(target.getParent().getPath()))  {
                    jcrSession.getWorkspace().getVersionManager().checkout(target.getParent().getPath());
                }
                pageContext.setAttribute("deletedTargetPath",target.getPath());
                target.remove();
                jcrSession.save();
            %>
            <p style="color: blue">Node <strong>${fn:escapeXml(deletedTargetPath)}</strong> deleted successfully</p>
        </c:when>
        <c:when test="${param.action == 'rename' && not empty param.target && not empty param.value}">
            <% Node target = jcrSession.getNodeByIdentifier(request.getParameter("target"));
                pageContext.setAttribute("target", target);
                if (!jcrSession.getWorkspace().getVersionManager().isCheckedOut(target.getParent().getPath()))  {
                    jcrSession.getWorkspace().getVersionManager().checkout(target.getParent().getPath());
                }
                pageContext.setAttribute("deletedTargetPath",target.getPath());
                jcrSession.move(target.getPath(), target.getParent().getPath() + "/" + JCRContentUtils.findAvailableNodeName(target.getParent(), request.getParameter("value")));
                jcrSession.save();
            %>
            <p style="color: blue">Node <strong>${fn:escapeXml(deletedTargetPath)}</strong> renamed successfully</p>
        </c:when>
        <c:when test="${param.action == 'removeMixin' && not empty param.value}">
            <%
                if (!jcrSession.getWorkspace().getVersionManager().isCheckedOut(node.getPath()))  {
                    jcrSession.getWorkspace().getVersionManager().checkout(node.getPath());
                }
                node.removeMixin(request.getParameter("value"));
                jcrSession.save();
            %>
            <p style="color: blue">Mixin ${param.value} successfully removed from the node <strong>${fn:escapeXml(node.path)}</strong></p>
        </c:when>
        <c:when test="${param.action == 'removeProperty' && not empty param.value}">
            <%
                if (node.hasProperty(request.getParameter("value"))) {
                    if (!jcrSession.getWorkspace().getVersionManager().isCheckedOut(node.getPath()))  {
                        jcrSession.getWorkspace().getVersionManager().checkout(node.getPath());
                    }
                    node.getProperty(request.getParameter("value")).remove();
                    jcrSession.save();
            %>
            <p style="color: blue">Property ${param.value} successfully removed from the node <strong>${fn:escapeXml(node.path)}</strong></p>
            <% } else { %>
            <p style="color: red">Cannot find property ${param.value} on the node <strong>${fn:escapeXml(node.path)}</strong></p>
            <% } %>
        </c:when>
        <c:when test="${param.action == 'setProperty' && not empty param.value}">
            <%
                PropertyDefinition def = JCRContentUtils.getPropertyDefinition(node.getPrimaryNodeType().getName(), request.getParameter("value"));
                if (def != null) {
                    if (!jcrSession.getWorkspace().getVersionManager().isCheckedOut(node.getPath()))  {
                        jcrSession.getWorkspace().getVersionManager().checkout(node.getPath());
                    }
                    if (def.isMultiple()) {
                        String[] newValues = request.getParameterValues("propertyValue");
                        if (newValues != null) {
                            Value[] vals = new Value[newValues.length];
                            for (int i = 0; i < newValues.length; i++) {
                                vals[i] = jcrSession.getValueFactory().createValue(newValues[i]);
                            }
                            node.setProperty(request.getParameter("value"), vals);
                        } else {
                            node.setProperty(request.getParameter("value"), (Value[]) null);
                        }
                    } else {
                        node.setProperty(request.getParameter("value"), request.getParameter("propertyValue"));
                    }
                    jcrSession.save();
            %>
            <p style="color: blue">Property ${param.value} successfully set on the node <strong>${fn:escapeXml(node.path)}</strong></p>
            <% } else { %>
            <p style="color: red">Cannot find definition for property ${param.value} on the node <strong>${fn:escapeXml(node.path)} [${node.primaryNodeTypeName}]</strong></p>
            <% } %>
        </c:when>
        <c:when test="${param.action == 'addMixin' && not empty param.value}">
            <%
                if (!jcrSession.getWorkspace().getVersionManager().isCheckedOut(node.getPath()))  {
                    jcrSession.getWorkspace().getVersionManager().checkout(node.getPath());
                }
                node.addMixin(request.getParameter("value"));
                jcrSession.save();
            %>
            <p style="color: blue">Mixin ${param.value} successfully added to the node <strong>${fn:escapeXml(node.path)}</strong></p>
        </c:when>
        <c:when test="${param.action == 'lock'}">
            <%
                if (node instanceof JCRNodeWrapper && jcrSession instanceof JCRSessionWrapper) {
                    JCRSessionWrapper jahiaSession = (JCRSessionWrapper) jcrSession;
                    JCRNodeWrapper jahiaNode = (JCRNodeWrapper) node;
                    jahiaNode.lockAndStoreToken("user");
                    jcrSession.save();
            %>
            <p style="color: blue">Node <strong>${fn:escapeXml(node.path)}</strong> locked</p>
            <%
                }
            %>
        </c:when>
        <c:when test="${param.action == 'unlock'}">
            <%
                if (node instanceof JCRNodeWrapper && jcrSession instanceof JCRSessionWrapper) {
                    JCRSessionWrapper jahiaSession = (JCRSessionWrapper) jcrSession;
                    JCRNodeWrapper jahiaNode = (JCRNodeWrapper) node;
                    JCRContentUtils.clearAllLocks(node.getPath(), false, jahiaSession.getWorkspace().getName());
                    jcrSession.save();
            %>
            <p style="color: blue">Locks cleared for node <strong>${fn:escapeXml(node.path)}</strong></p>
            <%
                }
            %>
        </c:when>
        <c:when test="${param.action == 'unlockTree'}">
            <%
                if (node instanceof JCRNodeWrapper && jcrSession instanceof JCRSessionWrapper) {
                    JCRSessionWrapper jahiaSession = (JCRSessionWrapper) jcrSession;
                    JCRNodeWrapper jahiaNode = (JCRNodeWrapper) node;
                    JCRContentUtils.clearAllLocks(node.getPath(), true, jahiaSession.getWorkspace().getName());
                    jcrSession.save();
            %>
            <p style="color: blue">Locks cleared for node <strong>${fn:escapeXml(node.path)}</strong> and its children</p>
            <%
                }
            %>
        </c:when>

        <c:when test="${param.action == 'reindex-tree'}">
            <%
            if (node instanceof JCRNodeWrapper && ((JCRNodeWrapper) node).getProvider().isDefault()) {
                long treeReindexStartTime = System.currentTimeMillis();
                ((JahiaRepositoryImpl)((SpringJackrabbitRepository) ((JCRNodeWrapper) node).getProvider().getRepository()).getRepository()).reindexTree(node.getIdentifier(), jcrSession.getWorkspace().getName());
                %>
                <p style="color: blue">Re-indexing tree in workspace <strong>${workspace}</strong>, starting from node <strong>${nodeId}</strong> has been completed in <strong><%= System.currentTimeMillis() - treeReindexStartTime %></strong> ms</p>
                <%
            }
            %>
        </c:when>
    </c:choose>
</c:if>

<c:if test="${node.path != '/'}">
    <a href="#parent" onclick="go('uuid', '${node.parent.identifier}'); return false;">[..]</a>
    <c:if test="${fn:contains(fn:substringAfter(node.path, '/'), '/')}">
        <a href="#root" onclick="go('uuid', 'cafebabe-cafe-babe-cafe-babecafebabe'); return false;">[/]</a>
    </c:if>
    <c:set var="breadcrumbs" value=""/>
    <c:forTokens items="${node.path}" delims="/" var="pathItem"
                 varStatus="loop"><c:set var="breadcrumbs" value="${breadcrumbs}/${pathItem}"
            />/<c:if test="${!loop.last}"><a href="#breadcrumbs" onclick="go('path', '${breadcrumbs}'); return false;">${fn:escapeXml(pathItem)}</a
            ></c:if><c:if test="${loop.last}">${fn:escapeXml(pathItem)}</c:if></c:forTokens>
</c:if>
<p>
    <c:if test="${showActions}">
<p>
    <c:if test="${!node.locked}">
        <img src="<c:url value='/icons/lock.png'/>" height="16" width="16" border="0" style="vertical-align: middle;" alt=" "/>&nbsp;<a href="#lock" onclick="if (confirm('You are about to put a lock on this node. Continue?')) {go('action', 'lock');} return false;" title="Put a lock on this node">lock node</a>
    </c:if>
    <c:if test="${node.locked}">
        <img src="<c:url value='/icons/unlock.png'/>" height="16" width="16" border="0" style="vertical-align: middle;" alt=" "/>&nbsp;<a href="#unlock" onclick="if (confirm('You are about to remove all locks on this node. Continue?')) {go('action', 'unlock');} return false;" title="Clean all locks on this node">unlock node</a>
        <img src="<c:url value='/icons/unlock.png'/>" height="16" width="16" border="0" style="vertical-align: middle;" alr=" "/>&nbsp;<a href="#unlockTree" onclick="if (confirm('You are about to remove all locks on this node and its children. Continue?')) {go('action', 'unlockTree');} return false;" title="Clean all locks on this node and its children">unlock tree</a>
    </c:if>
    <% if (node instanceof JCRNodeWrapper && ((JCRNodeWrapper) node).getProvider().isDefault()) { %>
    &nbsp;
    <img src="<c:url value='/icons/reversePublish.png'/>" height="16" width="16" border="0" style="vertical-align: middle;" alt=" "/>&nbsp;<a href="#reindex-tree" onclick="if (confirm('This will execute (synchronously) a re-indexing of the JCR sub-tree in the ${workspace} workspace, starting with the this node. Would you like to continue?')) {go('action', 'reindex-tree');} return false;" title="Re-index the node and its whole sub-tree in ${workspace} workspace">re-index node and sub-tree</a>
    <% } %>
</p>
</c:if>
<strong>Name:&nbsp;</strong>${fn:escapeXml(not empty node.name ? node.name : '<root>')}<br/>
<strong>Path:&nbsp;</strong>${fn:escapeXml(node.path)}<br/>
<strong>ID:&nbsp;</strong>${fn:escapeXml(node.identifier)}<br/>
<strong>Type:&nbsp;</strong>${fn:escapeXml(node.primaryNodeTypeName)}<br/>
<strong>Mixins:&nbsp;</strong>[<c:forEach items="${node.mixinNodeTypes}" var="mixin" varStatus="status">${status.index > 0 ? ", " : ""}${mixin.name}<c:if test="${showActions}">&nbsp;<a href="#remove" onclick="if (confirm('You are about to remove mixin ${mixin.name} from the node. Continue?')) {go('action', 'removeMixin', 'value', '${mixin.name}');} return false;"><img src="<c:url value='/icons/delete.png'/>" height="16" width="16" title="Delete mixin" border="0" style="vertical-align: middle;"/></a></c:if></c:forEach>]<br/>
<c:if test="${showActions}">
    <%

        Set<String> mixins = new TreeSet<String>();
        Set<String> existingMixins = new HashSet<String>(getNodeTypes(node));
        existingMixins.add("mix:shareable"); // we will skip this one

        NodeTypeIterator allMixins = node.getSession().getWorkspace().getNodeTypeManager()
                .getMixinNodeTypes();
        while (allMixins.hasNext()) {
            String nt = allMixins.nextNodeType().getName();
            if (!existingMixins.contains(nt) && node.canAddMixin(nt)) {
                mixins.add(nt);
            }
        }
        pageContext.setAttribute("mixins", mixins); %>
    <select id="mixins" name="mixins">
        <c:forEach items="${mixins}" var="mixin">
            <option value="${mixin}">${mixin}</option>
        </c:forEach>
    </select>
    <button onclick="var newMixin=document.getElementById('mixins').value; if (confirm('You are about to add mixin ' + newMixin + ' to the node. Continue?')) {go('action', 'addMixin', 'value', newMixin);} return false;">add</button>
</c:if>
<%
    if (pageContext.getAttribute("node") instanceof JCRNodeWrapper) {
%>
<c:if test="${jcr:isNodeType(node, 'nt:file')}">
    <br/><strong>File:&nbsp;</strong><a target="_blank" href="<c:url value='${node.url}' context='/'/>" title="download"><img src="<c:url value='/icons/download.png'/>" height="16" width="16" title="download" border="0" style="vertical-align: middle;"/></a>
</c:if>
<%
    }
%>
</p>
<p><strong>Properties:&nbsp;</strong><a href="#properties" onclick="go('showProperties', ${showProperties ? 'false' : 'true'}); return false;">${showProperties ? 'hide' : 'show'}</a></p>
<c:if test="${showProperties}">
    <ul>
        <c:set var="properties" value="${node.properties}"/>
        <c:set var="propCount" value="${fn:length(node.properties)}"/>
        <c:if test="${propCount == 0}"><li>No properties present</li></c:if>
        <c:if test="${propCount > 0}">
        	<%
        	// sort properties
        	List sortedProperties = IteratorUtils.toList((Iterator) pageContext.getAttribute("properties"));
        	Collections.sort(sortedProperties, BY_NAME_COMPARATOR);
        	pageContext.setAttribute("properties", sortedProperties);
        	%>
            <c:forEach items="${properties}" var="property">
                <li>
                    <strong>${fn:escapeXml(property.name)}:&nbsp;</strong>
                    <c:if test="${property.multiple}" var="multiple">
                        <ul>
                            <c:if test="${empty property.values}">
                                <li>[]</li>
                            </c:if>
                            <c:forEach items="${property.values}" var="value">
                                <li><%@include file="value.jspf" %></li>
                            </c:forEach>
                        </ul>
                    </c:if>
                    <c:if test="${!multiple}">
                        <c:set var="value" value="${property.value}"/>
                        <%@include file="value.jspf" %>
                    </c:if>
                </li>
            </c:forEach>
        </c:if>
    </ul>
</c:if>

<p><strong>References:&nbsp;</strong><a href="#references" onclick="go('showReferences', ${showReferences ? 'false' : 'true'}); return false;">${showReferences ? 'hide' : 'show'}</a></p>
<c:if test="${showReferences}">
    <ul>
      <%try {%>
        <c:set var="refsCount" value="${functions:length(node.references) + functions:length(node.weakReferences)}"/>
        <c:if test="${refsCount == 0}"><li>No references found</li></c:if>
        <c:if test="${refsCount > 0}">
            <c:forEach items="${node.references}" var="ref">
                <li>
                    <c:if test="${not empty ref}">
                        <c:set var="refTarget" value="${ref.parent}"/>
                        <a href="#reference" onclick="go('uuid', '${refTarget.identifier}'); return false;">${fn:escapeXml(refTarget.name)}&nbsp;(${refTarget.identifier}) / ${ref.name}</a>
                    </c:if>
                </li>
            </c:forEach>
            <c:forEach items="${node.weakReferences}" var="ref">
                <li>
                    <c:if test="${not empty ref}">
                        <c:set var="refTarget" value="${ref.parent}"/>
                        <a href="#reference" onclick="go('uuid', '${refTarget.identifier}'); return false;">${fn:escapeXml(refTarget.name)}&nbsp;(${refTarget.identifier}) / ${ref.name} - weak</a>
                    </c:if>
                </li>
            </c:forEach>
        </c:if>
      <%} catch (Exception ex) {%>
          <p style="color:red;">Error retrieving references<br/>Cause: <%=(ex.getCause() != null ? ex.getCause().toString() : ex.toString())%></p>
      <%} %>
    </ul>
</c:if>

<p><strong>Child nodes:&nbsp;</strong><a href="#nodes" onclick="go('showNodes', '${!showNodes}'); return false;">${showNodes ? 'hide' : 'show'}</a>
    <c:if test="${showNodes}">
    / <a href="#sortNodes" onclick="go('sortNodes', '${!sortNodes}'); return false;">${sortNodes ? 'no sort' : 'sort'}</a>
    <c:set var="nodes" value="${node.nodes}"/>
    <c:set var="childrenCount" value="${functions:length(nodes)}"/>
    <c:if test="${childrenCount > 0}">- ${childrenCount} nodes found</c:if>
</p>
<ul>
    <c:if test="${not empty parentUrl}">
        <li><a href="${parentUrl}">[..]</a></li>
    </c:if>
    <c:if test="${childrenCount == 0}"><li>No child nodes present</li></c:if>
    <c:if test="${childrenCount > 0}">
    	<c:if test="${sortNodes && childrenCount > 1}">
        	<%
        	// sort properties
        	List sortedNodes = IteratorUtils.toList((Iterator) pageContext.getAttribute("nodes"));
        	Collections.sort(sortedNodes, BY_NAME_COMPARATOR);
        	pageContext.setAttribute("sortedNodes", sortedNodes);
        	%>
        </c:if>
        <c:forEach items="${not empty sortedNodes ? sortedNodes : nodes}" var="child">
            <%
                pageContext.setAttribute("childNodeTypes",getNodeTypes((Node) pageContext.getAttribute("child")));
            %>
            <li>
                <a href="#child" onclick="go('uuid', '${child.identifier}'); return false;">${fn:escapeXml(child.name)}</a>&nbsp;(${childNodeTypes})
                <c:if test="${showActions}">
                    &nbsp;|
                    <%
                        if (pageContext.getAttribute("child") instanceof JCRNodeWrapper) {
                    %>
                    <c:if test="${jcr:isNodeType(child, 'nt:file')}">
                        &nbsp;<a target="_blank" href="<c:url value='${child.url}' context='/'/>" title="download"><img src="<c:url value='/icons/download.png'/>" height="16" width="16" title="download" border="0" style="vertical-align: middle;"/></a>
                    </c:if>
                    <%
                        }
                    %>
                    <c:url value="/cms/export/${workspace}${child.path}.xml?cleanup=simple" var="urlExportXml"/>
                    <c:url value="/cms/export/${workspace}${child.path}.zip?cleanup=simple" var="urlExportZip"/>
                    &nbsp;<a target="_blank" href="${fn:escapeXml(urlExportXml)}" title="Export as XML"><img src="<c:url value='/icons/import.png'/>" height="16" width="16" title="Export as XML" border="0" style="vertical-align: middle;"/></a>
                    &nbsp;<a target="_blank" href="${fn:escapeXml(urlExportZip)}" title="Export as ZIP"><img src="<c:url value='/icons/zip.png'/>" height="16" width="16" title="Export as ZIP" border="0" style="vertical-align: middle;"/></a>
                    |&nbsp;
                    <c:set var="childNameEscaped" value="${fn:escapeXml(functions:escapeJavaScript(child.name))}"/>
                    &nbsp;<a href="#rename" onclick="var name=prompt('Please provide a new name for the node:', '${childNameEscaped}'); if (name != null & name != '${childNameEscaped}') { go('action', 'rename', 'target', '${child.identifier}', 'value', name);} return false;" title="Rename"><img src="<c:url value='/icons/editContent.png'/>" height="16" width="16" title="Rename" border="0" style="vertical-align: middle;"/></a>
                    &nbsp;<a href="#delete" onclick="var nodeName='${childNameEscaped}'; if (!confirm('You are about to delete the node ' + nodeName + ' with all child nodes. Continue?')) return false; go('action', 'delete', 'target', '${child.identifier}'); return false;" title="Delete"><img src="<c:url value='/icons/delete.png'/>" height="16" width="16" title="Delete" border="0" style="vertical-align: middle;"/></a>
                </c:if>
            </li>
        </c:forEach>
    </c:if>
</ul>
</c:if>
<c:if test="${empty showNodes || not showNodes}">
    </p>
</c:if>

<c:if test="${showVersions}">
    <strong>Linear history:&nbsp;</strong>[<c:forEach items="${versionIterator}" var="version" varStatus="status">${status.index > 0 ? ", " : ""}<a href="#version" onclick="go('uuid', '${version.identifier}'); return false;">${version.name}</a></c:forEach>]<br>
    <strong>Full version graph:&nbsp;</strong>
    <pre>
<c:forEach items="${versionGraph}" var="version" varStatus="status">${version}&nbsp;
</c:forEach>
    </pre><br>
</c:if>
<div style="position: absolute; right: 20px; top: 10px; font-size: 80%">rendered in <%= System.currentTimeMillis() - timer %> ms</div>
</fieldset>
</body>
<%} catch (javax.jcr.ItemNotFoundException e) {
%>
<c:url var="switchWorkspaceUrl" value="?">
    <c:param name="uuid" value="${node.identifier}"/>
    <c:param name="showProperties" value="${showProperties}"/>
    <c:param name="showReferences" value="${showReferences}"/>
    <c:param name="showNodes" value="${showNodes}"/>
    <c:param name="showActions" value="${showActions}"/>
    <c:param name="workspace" value="${workspace == 'default' ? 'live' : 'default'}"/>
    <c:param name="toolAccessToken" value="${toolAccessToken}"/>
</c:url>
<body>
<p>Item with UUID <strong>${nodeId}</strong> does not exist in the '${workspace}' workspace</p>
<p>Actions:
    &nbsp;<a href="${switchWorkspaceUrl}">switch to ${workspace == 'default' ? 'live' : 'default'} workspace</a>
    &nbsp;<a href="javascript:history.back()">go back</a>
</p>
    <%} catch (javax.jcr.PathNotFoundException e) {
%>
<body>
<c:url var="switchWorkspaceUrl" value="?">
    <c:param name="path" value="${param.path}"/>
    <c:param name="showProperties" value="${showProperties}"/>
    <c:param name="showReferences" value="${showReferences}"/>
    <c:param name="showNodes" value="${showNodes}"/>
    <c:param name="showActions" value="${showActions}"/>
    <c:param name="workspace" value="${workspace == 'default' ? 'live' : 'default'}"/>
    <c:param name="toolAccessToken" value="${toolAccessToken}"/>
</c:url>
<p>Item with the path <strong>${param.path}</strong> does not exist in the '${workspace}' workspace</p>
<p>Actions:
    &nbsp;<a href="${switchWorkspaceUrl}">switch to ${workspace == 'default' ? 'live' : 'default'} workspace</a>
    &nbsp;<a href="javascript:history.back()">go back</a>
</p>
    <%} catch (Exception e) {
%>
<body>
<p style="color:red;"><strong>Error: </strong><%=e %><pre style="color:red;"><% e.printStackTrace(new java.io.PrintWriter(out)); %></pre></p>
<%} finally {
    JCRSessionFactory.getInstance().setCurrentUser(null);
}%>
</div>
<%@ include file="gotoIndex.jspf" %>
</body>
</html>
