package org.jahia.modules.tools.userpermissions;

import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;

/**
 * Main analysis engine for the User Permissions Inspector tool.
 *
 * <p>Given a Jahia username, a JCR node path, and a workspace name, this class resolves every
 * effective role that applies to the user on the target node and returns them as a list of
 * {@link RoleResult} objects ready for display in the JSP view.
 *
 * <h3>ACL resolution algorithm</h3>
 * <p>Jahia's {@code JCRNodeWrapper.getAclEntries()} returns a flat map of
 * {@code principal → List<String[]>} where each {@code String[]} is
 * {@code [grantedOnPath, grantType, roleName]}.  Three ACE types are possible:
 * <ul>
 *   <li><b>GRANT / DENY</b> — standard ACE from a {@code jnt:ace} node on a content node.</li>
 *   <li><b>EXTERNAL</b> — mirror ACE created on the site node by Jahia's {@code AclListener}
 *       when a role that has {@code jnt:externalPermissions} children is granted on a content
 *       node.  The role name in this case has the form {@code baseRole/extPermName}, e.g.
 *       {@code translator-en/currentSite-access}.</li>
 * </ul>
 *
 * <p>Analysis is done in two passes:
 * <ol>
 *   <li><b>Pass 1 — GRANT / DENY</b>: Collect direct role grants for the user (and their
 *       groups).  For each role only the deepest (most specific) grant path is kept.</li>
 *   <li><b>Pass 2 — EXTERNAL</b>: For each orphan EXTERNAL entry (role not yet in the result
 *       map from Pass 1), navigate the {@code jnt:externalAce} node on the site's {@code j:acl}
 *       and iterate the multi-valued {@code j:sourceAce} REFERENCE property to recover the
 *       original content node(s) where the role was actually granted.  Each distinct source
 *       node produces a separate result entry, disambiguated with a {@code §N} suffix on the
 *       map key.</li>
 * </ol>
 *
 * <h3>Permission check</h3>
 * <p>When {@code checkPermission} is supplied, the analyzer opens a user session (temporarily
 * switching {@code JCRSessionFactory}'s current user to the target user) and calls
 * {@code JCRNodeWrapper.hasPermission()} for an authoritative answer.  It then scans the
 * resolved role list to identify which role and which permission entry provides the queried
 * permission, using both exact name matching and aggregate-privilege expansion.
 */
public class UserPermissionsAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(UserPermissionsAnalyzer.class);

    /**
     * Separator used to make map keys unique when the same role is granted via multiple content
     * nodes (e.g. through a group that has the role on several pages).
     * Value: {@code §} (U+00A7 SECTION SIGN).
     */
    private static final String ROLE_DUPLICATE_SEPARATOR = "\u00a7";

    /** Workspace name used to resolve role definition nodes under {@code /roles}. */
    private static final String EDIT_WORKSPACE = "default";

    /** JCR node type name for Jahia role nodes. */
    private static final String ROLE_NODE_TYPE = "jnt:role";

    /**
     * Immutable container for all data produced by one {@link UserPermissionsAnalyzer#analyze} call.
     * Instances are created by the analyzer and consumed directly by the JSP view.
     */
    public static class Result {

        /** All effective roles resolved for the user on the target node. */
        private final List<RoleResult> roleResults;

        /**
         * Result of {@code JCRNodeWrapper.hasPermission()} for the queried permission, or
         * {@code null} when no permission was queried.
         */
        private final Boolean permGranted;

        /**
         * Error message when the {@code hasPermission()} call could not be completed (e.g. user
         * not found), or {@code null} on success.
         */
        private final String permCheckError;

        /**
         * Roles identified as providing the queried permission, populated when
         * {@code checkPermission} is supplied and at least one match is found.
         */
        private final List<PermCheckMatch> permCheckMatches;

        /**
         * Full permission paths that matched the queried permission, used to highlight entries
         * in the permission lists.
         */
        private final Set<String> highlightedPerms;

        /**
         * Map view of {@link #highlightedPerms} keyed by path for efficient EL-based lookup in
         * JSTL (e.g. {@code ${result.highlightedPermMap[perm.path]}}).
         */
        private final Map<String, Boolean> highlightedPermMap;

        public Result(List<RoleResult> roleResults, Boolean permGranted, String permCheckError,
                List<PermCheckMatch> permCheckMatches, Set<String> highlightedPerms) {
            this.roleResults = Collections.unmodifiableList(new ArrayList<>(roleResults));
            this.permGranted = permGranted;
            this.permCheckError = permCheckError;
            this.permCheckMatches = Collections.unmodifiableList(new ArrayList<>(permCheckMatches));
            this.highlightedPerms = Collections.unmodifiableSet(new LinkedHashSet<>(highlightedPerms));
            // Build the map eagerly so JSTL can do O(1) lookups via ${result.highlightedPermMap[perm.path]}.
            Map<String, Boolean> highlightMap = new LinkedHashMap<>();
            for (String highlightedPerm : this.highlightedPerms) {
                highlightMap.put(highlightedPerm, Boolean.TRUE);
            }
            this.highlightedPermMap = Collections.unmodifiableMap(highlightMap);
        }

        /** @return immutable list of all effective roles for the inspected user */
        public List<RoleResult> getRoleResults() {
            return roleResults;
        }

        /**
         * Returns the result of {@code JCRNodeWrapper.hasPermission()} for the queried permission,
         * or {@code null} when no permission was queried.
         *
         * @return {@code Boolean.TRUE} if granted, {@code Boolean.FALSE} if denied, {@code null} if not checked
         */
        public Boolean getPermGranted() {
            return permGranted;
        }

        /**
         * Returns an error message when the permission check could not be performed (e.g. user
         * not found), or {@code null} when the check succeeded or was not requested.
         *
         * @return error message, or {@code null}
         */
        public String getPermCheckError() {
            return permCheckError;
        }

        /** @return immutable list of roles that provide the queried permission */
        public List<PermCheckMatch> getPermCheckMatches() {
            return permCheckMatches;
        }

        /**
         * Returns a {@code Map<String, Boolean>} view of the highlighted permission set suitable
         * for O(1) EL lookups in JSTL: {@code ${result.highlightedPermMap[perm.path]}}.
         *
         * @return immutable map; a {@code true} value means the path is highlighted
         */
        public Map<String, Boolean> getHighlightedPermMap() {
            return highlightedPermMap;
        }
    }

    /**
     * Performs a complete ACL analysis for the given user on the given node.
     *
     * <p>Opens two JCR sessions internally:
     * <ul>
     *   <li>A <em>system session</em> in the requested workspace to read ACL entries and resolve
     *       {@code jnt:externalAce} nodes.</li>
     *   <li>A <em>system session in the {@code default} workspace</em> to resolve role definition
     *       nodes under {@code /roles} and read their permission declarations.</li>
     * </ul>
     * Both sessions are closed in a {@code finally} block.
     *
     * @param username        Jahia username or group principal (prefix {@code g:}) to inspect
     * @param nodePath        absolute JCR path of the node to analyse
     * @param workspace       workspace to use: {@code "default"} or {@code "live"}
     * @param checkPermission permission name to quick-check via {@code hasPermission()} (may be
     *                        {@code null} or empty to skip; skipped automatically for group principals)
     * @param userSiteKey     site key used for user and group membership resolution; may be
     *                        {@code null} or empty to resolve against the global (non-site) context
     * @param request         current HTTP request, used to build the server base URL for editor
     *                        deep-links; may be {@code null} (editor links will be omitted)
     * @return analysis result containing all resolved roles and optional permission-check data
     * @throws javax.jcr.PathNotFoundException if {@code nodePath} does not exist in the workspace
     * @throws RepositoryException             on any other JCR error
     */
    public Result analyze(String username, String nodePath, String workspace, String checkPermission,
            String userSiteKey, HttpServletRequest request) throws RepositoryException {
        List<RoleResult> roleResults = new ArrayList<>();
        Boolean permGranted = null;
        String permCheckError = null;
        List<PermCheckMatch> permCheckMatches = new ArrayList<>();
        Set<String> highlightedPerms = new LinkedHashSet<>();
        // nodeSiteKey is used only for content-editor URL building (derived from the node path).
        // userSiteKey is the explicitly chosen site for user/group membership resolution.
        String nodeSiteKey = extractSiteKey(nodePath);
        String resolvedUserSiteKey = (userSiteKey != null && !userSiteKey.trim().isEmpty()) ? userSiteKey : nodeSiteKey;
        boolean isGroupPrincipal = username != null && username.startsWith("g:");
        String serverBase = buildServerBase(request);
        String language = "en";

        JCRSessionWrapper systemSession = null;
        JCRSessionWrapper editSession = null;
        try {
            systemSession = JCRSessionFactory.getInstance().getCurrentSystemSession(workspace, null, null);
            editSession = JCRSessionFactory.getInstance().getCurrentSystemSession(EDIT_WORKSPACE, null, null);

            JCRNodeWrapper node = systemSession.getNode(nodePath);
            JahiaGroupManagerService groupMgr = ServicesRegistry.getInstance().getJahiaGroupManagerService();
            Map<String, List<String[]>> aclEntries = node.getAclEntries();
            Map<String, String[]> effectiveRoles = new LinkedHashMap<>();
            List<String[]> orphanExternals = new ArrayList<>();

            for (Map.Entry<String, List<String[]>> aclEntry : aclEntries.entrySet()) {
                String principal = aclEntry.getKey();
                if (!matchesPrincipal(username, resolvedUserSiteKey, principal, groupMgr)) {
                    continue;
                }

                for (String[] ace : aclEntry.getValue()) {
                    String grantedOnPath = ace[0];
                    String grantType = ace[1];
                    String roleName = ace[2];
                    if ("EXTERNAL".equals(grantType)) {
                        orphanExternals.add(new String[] {principal, grantedOnPath, roleName});
                        continue;
                    }

                    // Find an existing entry for the same (role, principal) pair so that a
                    // deeper path replaces a shallower one for the same principal, while a
                    // different principal always gets its own separate card.
                    String existingKey = findKeyForRoleAndPrincipal(effectiveRoles, roleName, principal);
                    if (existingKey == null) {
                        effectiveRoles.put(findUniqueKey(effectiveRoles, roleName),
                                new String[] {grantedOnPath, grantType, principal});
                    } else if (grantedOnPath.length() > effectiveRoles.get(existingKey)[0].length()) {
                        effectiveRoles.put(existingKey, new String[] {grantedOnPath, grantType, principal});
                    }
                }
            }

            for (String[] orphanExternal : orphanExternals) {
                String extPrincipal = orphanExternal[0];
                String extGrantPath = orphanExternal[1];
                String extRoleName = orphanExternal[2];
                String extBase = extRoleName.contains("/")
                        ? extRoleName.substring(0, extRoleName.indexOf('/')) : extRoleName;

                // Always resolve j:sourceAce — even when a direct GRANT was already found in Pass 1,
                // there may be additional content nodes (group granted same role on multiple nodes).
                List<String> contentPaths = resolveExternalContentPaths(systemSession, extPrincipal,
                        extGrantPath, extBase);

                if (contentPaths.isEmpty() && !isAlreadyCovered(effectiveRoles, extBase)) {
                    // Fallback only when the role is truly absent: use the site/ancestor path.
                    contentPaths.add(extGrantPath);
                }

                for (String contentPath : contentPaths) {
                    if (!isPathCoveredForRole(effectiveRoles, extBase, contentPath)) {
                        effectiveRoles.put(findUniqueKey(effectiveRoles, extBase),
                                new String[] {contentPath, "GRANT", extPrincipal});
                    }
                }
            }

            for (Map.Entry<String, String[]> roleEntry : effectiveRoles.entrySet()) {
                String roleName = stripDuplicateSuffix(roleEntry.getKey());
                String[] roleGrant = roleEntry.getValue();
                String grantedOnPath = roleGrant[0];
                String grantType = roleGrant[1];
                String matchedPrincipal = roleGrant[2];
                Node roleNode = resolveRoleNode(editSession, roleName);
                String roleNodePath = roleNode != null ? roleNode.getPath() : null;
                List<PermissionEntry> nodePerms = new ArrayList<>();
                List<SitePermGroup> sitePerms = new ArrayList<>();
                if (roleNode != null) {
                    collectRolePermissions(roleNode, editSession, nodePerms, sitePerms);
                }

                String grantNodeUuid = null;
                try {
                    if (systemSession.nodeExists(grantedOnPath)) {
                        grantNodeUuid = systemSession.getNode(grantedOnPath).getIdentifier();
                    }
                } catch (RepositoryException e) {
                    logger.debug("Unable to resolve grant node UUID for {}", grantedOnPath, e);
                }

                roleResults.add(new RoleResult(roleName, grantType, matchedPrincipal, grantedOnPath, roleNodePath,
                        nodePerms, sitePerms,
                        ContentEditorUrlBuilder.build(serverBase, nodeSiteKey, language, grantedOnPath, grantNodeUuid)));
            }

            if (checkPermission != null && !checkPermission.trim().isEmpty()) {
                if (isGroupPrincipal) {
                    // hasPermission() requires a user session; it cannot be used for group principals.
                    permCheckError = "Permission check via hasPermission() is not available for group principals.";
                } else {
                    permGranted = Boolean.FALSE;
                    JahiaUser previousUser = JCRSessionFactory.getInstance().getCurrentUser();
                    JCRSessionWrapper userSession = null;
                    try {
                        JCRUserNode targetUserNode = JahiaUserManagerService.getInstance().lookupUser(username);
                        if (targetUserNode == null && resolvedUserSiteKey != null) {
                            targetUserNode = JahiaUserManagerService.getInstance().lookupUser(username, resolvedUserSiteKey);
                        }
                        if (targetUserNode == null) {
                            permCheckError = "User \"" + username
                                    + "\" not found — cannot open a user session for hasPermission() check.";
                        } else {
                            JahiaUser targetUser = targetUserNode.getJahiaUser();
                            JCRSessionFactory.getInstance().setCurrentUser(targetUser);
                            userSession = JCRSessionFactory.getInstance().getCurrentUserSession(workspace, null, null);
                            permGranted = userSession.getNode(nodePath).hasPermission(checkPermission);
                        }
                    } catch (RepositoryException e) {
                        permCheckError = "Permission check failed: " + e.getMessage();
                        logger.debug("Unable to check permission {} for user {} on {}", checkPermission, username,
                                nodePath, e);
                    } finally {
                        logoutSession(userSession);
                        JCRSessionFactory.getInstance().setCurrentUser(previousUser);
                    }
                }

                Map<String, String> grantingPrivToVia = buildGrantingPrivilegeMap(systemSession, checkPermission);
                for (RoleResult roleResult : roleResults) {
                    addPermCheckMatches(roleResult, grantingPrivToVia, highlightedPerms, permCheckMatches);
                }
            }

            return new Result(roleResults, permGranted, permCheckError, permCheckMatches, highlightedPerms);
        }  finally {
            logoutSession(editSession);
            logoutSession(systemSession);
        }
    }

    /**
     * Populates {@code nodePerms} and {@code sitePerms} from the given role node and its parent
     * roles (traversed recursively to inherit permissions from parent {@code jnt:role} nodes).
     *
     * <p>Permissions can be stored in two ways on a role node:
     * <ul>
     *   <li>{@code j:permissionNames} (STRING multi-value) — preferred; permission names are
     *       expanded via {@link PermissionExpander}.</li>
     *   <li>{@code j:permissions} (REFERENCE multi-value) — legacy; UUIDs referencing
     *       {@code jnt:permission} nodes; the tree is walked directly.</li>
     * </ul>
     *
     * <p>Child nodes of type {@code jnt:externalPermissions} are collected into
     * {@code sitePerms}, one {@link SitePermGroup} per child.  If the same group name appears in
     * a parent role and the child role, the child's version replaces the parent's (more specific
     * definition wins).
     *
     * @param roleNode  the role node to collect permissions from
     * @param session   JCR session for resolving permission nodes (edit workspace system session)
     * @param nodePerms accumulator for node-level permissions
     * @param sitePerms accumulator for site-level permission groups
     */
    private void collectRolePermissions(Node roleNode, Session session, List<PermissionEntry> nodePerms,
            List<SitePermGroup> sitePerms) throws RepositoryException {
        Node parent = roleNode.getParent();
        if (parent.isNodeType(ROLE_NODE_TYPE)) {
            collectRolePermissions(parent, session, nodePerms, sitePerms);
        }

        if (roleNode.hasProperty("j:permissionNames")) {
            for (Value value : roleNode.getProperty("j:permissionNames").getValues()) {
                addPermissionEntries(nodePerms, PermissionExpander.expand(value.getString(), session));
            }
        } else if (roleNode.hasProperty("j:permissions")) {
            for (Value value : roleNode.getProperty("j:permissions").getValues()) {
                try {
                    Node permissionNode = session.getNodeByIdentifier(value.getString());
                    addPermissionTree(permissionNode, nodePerms);
                } catch (RepositoryException e) {
                    logger.debug("Unable to resolve node-level permission {} for role {}", value.getString(),
                            roleNode.getPath(), e);
                }
            }
        }

        NodeIterator children = roleNode.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (!child.isNodeType("jnt:externalPermissions")) {
                continue;
            }

            String groupName = child.getName();
            String targetPath = child.hasProperty("j:path") ? child.getProperty("j:path").getString() : "(unknown)";
            List<PermissionEntry> permissions = new ArrayList<>();
            if (child.hasProperty("j:permissionNames")) {
                for (Value value : child.getProperty("j:permissionNames").getValues()) {
                    addPermissionEntries(permissions, PermissionExpander.expand(value.getString(), session));
                }
            } else if (child.hasProperty("j:permissions")) {
                for (Value value : child.getProperty("j:permissions").getValues()) {
                    try {
                        Node permissionNode = session.getNodeByIdentifier(value.getString());
                        addPermissionTree(permissionNode, permissions);
                    } catch (RepositoryException e) {
                        logger.debug("Unable to resolve site permission {} for role {}", value.getString(),
                                roleNode.getPath(), e);
                    }
                }
            }

            SitePermGroup replacement = new SitePermGroup(groupName, targetPath, permissions);
            boolean replaced = false;
            for (int i = 0; i < sitePerms.size(); i++) {
                if (groupName.equals(sitePerms.get(i).getName())) {
                    sitePerms.set(i, replacement);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                sitePerms.add(replacement);
            }
        }
    }

    /**
     * Looks up {@code perm} in the map of privileges that grant the queried permission, trying
     * three forms in order:
     * <ol>
     *   <li>Exact match (e.g. {@code jcr:read_default}).</li>
     *   <li>After stripping the namespace prefix (e.g. {@code read_default}).</li>
     *   <li>After taking only the last path segment (e.g. {@code deletePageAction} from a
     *       hierarchical path like {@code /jContentActions/pageTreeActions/deletePageAction}).</li>
     * </ol>
     *
     * @param grantingPrivToVia map from privilege name to an explanatory "via" string; an empty
     *                          string means the permission matched directly
     * @param perm              the permission path or name to look up
     * @return the "via" explanation string (possibly empty) if a match is found, or {@code null}
     */
    private String lookupPerm(Map<String, String> grantingPrivToVia, String perm) {
        String via = grantingPrivToVia.get(perm);
        if (via != null) {
            return via;
        }
        if (perm.contains(":")) {
            via = grantingPrivToVia.get(perm.substring(perm.indexOf(':') + 1));
            if (via != null) {
                return via;
            }
        }
        if (perm.contains("/")) {
            via = grantingPrivToVia.get(perm.substring(perm.lastIndexOf('/') + 1));
            return via;
        }
        return null;
    }

    /**
     * Checks whether the given ACL principal string matches the inspected user or group.
     *
     * <p>Matching rules:
     * <ul>
     *   <li>If {@code usernameOrGroup} starts with {@code g:}, match only when the ACL
     *       {@code principal} refers to the same group (using flexible {@link #principalsMatch}).</li>
     *   <li>{@code u:username} — matches when the username is equal.</li>
     *   <li>{@code g:guests} — always matches (every visitor is a guest).</li>
     *   <li>{@code g:users} / {@code g:site-users} — matches every non-guest user.</li>
     *   <li>Any other group — delegates to
     *       {@link JahiaGroupManagerService#isMember(String, String, String, String)}, checking
     *       both the site group and the global group (site key {@code null}).</li>
     * </ul>
     *
     * @param usernameOrGroup username or group principal (prefixed {@code g:}) being inspected
     * @param siteKey         site key used for group membership resolution, may be {@code null}
     * @param principal       principal string from the ACL entry (prefixed {@code u:} or {@code g:})
     * @param groupMgr        group manager service instance
     * @return {@code true} if the principal applies to the given user or group
     */
    private boolean matchesPrincipal(String usernameOrGroup, String siteKey, String principal,
            JahiaGroupManagerService groupMgr) {
        // When the input itself is a group principal, only match the exact same group.
        if (usernameOrGroup.startsWith("g:")) {
            return principalsMatch(usernameOrGroup, principal);
        }
        if (principal.startsWith("u:")) {
            return usernameOrGroup.equals(principal.substring(2));
        }
        if (!principal.startsWith("g:")) {
            return false;
        }

        String groupName = principal.substring(2);
        if (JahiaGroupManagerService.GUEST_GROUPNAME.equals(groupName)) {
            return true;
        }
        if (JahiaGroupManagerService.USERS_GROUPNAME.equals(groupName)
                || JahiaGroupManagerService.SITE_USERS_GROUPNAME.equals(groupName)) {
            return !"guest".equals(usernameOrGroup);
        }

        try {
            return groupMgr.isMember(usernameOrGroup, null, groupName, siteKey)
                    || groupMgr.isMember(usernameOrGroup, null, groupName, null);
        } catch (Exception e) {
            logger.debug("Unable to resolve group membership for {} and {}", usernameOrGroup, principal, e);
            return false;
        }
    }

    /**
     * Returns {@code true} when {@code effectiveRoles} already contains an entry whose clean role
     * name equals {@code extBase} or starts with {@code extBase/} (child role).  Used as a
     * lightweight guard to avoid re-adding a role that was already captured by a direct GRANT in
     * Pass 1 — but note that Pass 2 still processes the {@code j:sourceAce} to discover any
     * additional content nodes not already represented.
     *
     * @param effectiveRoles current effective-role map (may contain {@code §N} suffixed keys)
     * @param extBase        base role name (without any {@code /extPermName} suffix)
     * @return {@code true} if the role is already present in the map
     */
    private boolean isAlreadyCovered(Map<String, String[]> effectiveRoles, String extBase) {
        for (String key : effectiveRoles.keySet()) {
            String cleanKey = stripDuplicateSuffix(key);
            if (cleanKey.equals(extBase) || cleanKey.startsWith(extBase + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the content node(s) where the role was originally granted by iterating the
     * {@code j:sourceAce} multi-valued REFERENCE property on the matching {@code jnt:externalAce}
     * at the site node's {@code j:acl}.
     *
     * <p>Each {@code j:sourceAce} value is a REFERENCE or WEAKREFERENCE UUID pointing to an ACE
     * node ({@code jnt:ace}) on the content node's {@code j:acl}.  Navigating
     * {@code sourceAce → parent (j:acl) → parent} yields the content node path.
     *
     * <p>Matching strategy for the {@code jnt:externalAce}:
     * <ol>
     *   <li>Node type must be {@code jnt:externalAce}.</li>
     *   <li>{@code j:principal} must match {@code extPrincipal} via {@link #principalsMatch}
     *       (flexible to handle site-qualified group names).</li>
     *   <li>{@code j:roles} (NAME multi-value) must contain the base role name {@code extBase}.</li>
     * </ol>
     *
     * <p>The method does NOT add a fallback path; callers are responsible for handling an empty
     * result.
     *
     * @param systemSession JCR system session (workspace being analysed)
     * @param extPrincipal  principal from the EXTERNAL ACL entry (e.g. {@code g:sample-group})
     * @param extGrantPath  path of the site node where the {@code jnt:externalAce} lives
     * @param extBase       base role name (e.g. {@code translator-en})
     * @return distinct content node paths resolved from {@code j:sourceAce}; empty if none found
     */
    private List<String> resolveExternalContentPaths(JCRSessionWrapper systemSession,
            String extPrincipal, String extGrantPath, String extBase) {
        List<String> contentPaths = new ArrayList<>();
        try {
            if (systemSession.nodeExists(extGrantPath)) {
                JCRNodeWrapper siteNode = systemSession.getNode(extGrantPath);
                if (siteNode.hasNode("j:acl")) {
                    NodeIterator aclChildren = siteNode.getNode("j:acl").getNodes();
                    while (aclChildren.hasNext()) {
                        Node aceNode = aclChildren.nextNode();
                        if (!aceNode.isNodeType("jnt:externalAce")) {
                            continue;
                        }
                        String acePrincipal = aceNode.hasProperty("j:principal")
                                ? aceNode.getProperty("j:principal").getString() : null;
                        if (!principalsMatch(extPrincipal, acePrincipal)) {
                            continue;
                        }
                        if (aceNode.hasProperty("j:roles") && !matchesExternalRole(extBase, aceNode)) {
                            continue;
                        }
                        if (!aceNode.hasProperty("j:sourceAce")) {
                            continue;
                        }
                        for (Value sourceAceValue : aceNode.getProperty("j:sourceAce").getValues()) {
                            try {
                                Node sourceAce = sourceAceValue.getType() == PropertyType.REFERENCE
                                        || sourceAceValue.getType() == PropertyType.WEAKREFERENCE
                                                ? systemSession.getNodeByIdentifier(sourceAceValue.getString())
                                                : systemSession.getNode(sourceAceValue.getString());
                                addIfMissing(contentPaths, sourceAce.getParent().getParent().getPath());
                            } catch (RepositoryException e) {
                                logger.debug("Unable to resolve source ACE {} for {}", sourceAceValue,
                                        extGrantPath, e);
                            }
                        }
                        // No break — a site j:acl could theoretically have multiple matching
                        // jnt:externalAce nodes; collect all their source paths.
                    }
                }
            }
        } catch (RepositoryException e) {
            logger.debug("Unable to resolve external ACE sources for {}", extGrantPath, e);
        }
        return contentPaths;
    }

    /**
     * Compares two principal strings for equality, with a fallback that compares only the local
     * part (after the last {@code :}) to accommodate site-qualified group names.
     *
     * <p>Example: {@code "g:digitall:sample-group"} and {@code "g:sample-group"} are considered
     * equal because both have the local name {@code sample-group}.
     *
     * @param p1 first principal; {@code null} returns {@code false}
     * @param p2 second principal; {@code null} returns {@code false}
     * @return {@code true} if the principals refer to the same user or group
     */
    private boolean principalsMatch(String p1, String p2) {
        if (p1 == null || p2 == null) {
            return false;
        }
        if (p1.equals(p2)) {
            return true;
        }
        String local1 = p1.substring(p1.lastIndexOf(':') + 1);
        String local2 = p2.substring(p2.lastIndexOf(':') + 1);
        return local1.equals(local2);
    }

    /**
     * Returns {@code true} when {@code effectiveRoles} already contains an entry for role
     * {@code extBase} (or a child of it) with exactly {@code contentPath} as the grant path.
     * Used to prevent adding duplicate {@link RoleResult} entries when the same content path is
     * discovered both via a direct GRANT (Pass 1) and via {@code j:sourceAce} (Pass 2).
     *
     * @param effectiveRoles current effective-role map
     * @param extBase        base role name to check
     * @param contentPath    content node path to check for
     * @return {@code true} if the (role, path) combination is already present
     */
    private boolean isPathCoveredForRole(Map<String, String[]> effectiveRoles, String extBase, String contentPath) {
        for (Map.Entry<String, String[]> entry : effectiveRoles.entrySet()) {
            String cleanKey = stripDuplicateSuffix(entry.getKey());
            if ((cleanKey.equals(extBase) || cleanKey.startsWith(extBase + "/"))
                    && contentPath.equals(entry.getValue()[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the next available map key for {@code base} in {@code effectiveRoles}.
     * Returns {@code base} itself when that key is absent; otherwise returns
     * {@code base§1}, {@code base§2}, etc., incrementing until a free slot is found.
     *
     * @param effectiveRoles map to check for key collisions
     * @param base           base role name
     * @return unique key to use for a new entry
     */
    private String findUniqueKey(Map<String, String[]> effectiveRoles, String base) {
        if (!effectiveRoles.containsKey(base)) {
            return base;
        }
        int n = 1;
        while (effectiveRoles.containsKey(base + ROLE_DUPLICATE_SEPARATOR + n)) {
            n++;
        }
        return base + ROLE_DUPLICATE_SEPARATOR + n;
    }

    /**
     * Finds the existing map key in {@code effectiveRoles} whose base role name (after stripping
     * the {@link #ROLE_DUPLICATE_SEPARATOR} suffix) equals {@code roleName} AND whose stored
     * principal (index 2 of the value array) matches {@code principal}.
     *
     * <p>This allows Pass 1 to treat each (role, principal) pair independently: a deeper grant
     * for the same principal updates the existing entry, while a grant from a different principal
     * always gets its own card.
     *
     * @param effectiveRoles current effective-role accumulator
     * @param roleName       base role name to look for (without suffix)
     * @param principal      principal string to match exactly
     * @return the matching key, or {@code null} if none exists yet
     */
    private String findKeyForRoleAndPrincipal(Map<String, String[]> effectiveRoles,
            String roleName, String principal) {
        for (Map.Entry<String, String[]> entry : effectiveRoles.entrySet()) {
            if (stripDuplicateSuffix(entry.getKey()).equals(roleName)
                    && principal.equals(entry.getValue()[2])) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Checks whether the {@code j:roles} NAME multi-value property of a {@code jnt:externalAce}
     * node contains {@code extBase} (exact match or local-name match after stripping any path
     * prefix).
     *
     * @param extBase base role name to look for (e.g. {@code translator-en})
     * @param aceNode {@code jnt:externalAce} node whose {@code j:roles} property is read
     * @return {@code true} if the role is listed on the external ACE
     */
    private boolean matchesExternalRole(String extBase, Node aceNode) throws RepositoryException {
        for (Value value : aceNode.getProperty("j:roles").getValues()) {
            String roleValue = value.getString();
            String localValue = roleValue.contains("/") ? roleValue.substring(roleValue.lastIndexOf('/') + 1)
                    : roleValue;
            if (extBase.equals(roleValue) || extBase.equals(localValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the {@code jnt:role} definition node for the given role name using the
     * {@code default} workspace system session.
     *
     * <p>Lookup strategy:
     * <ol>
     *   <li>Try {@code /roles/{roleName}} directly (handles nested roles like
     *       {@code translator/translator-en}).</li>
     *   <li>Fall back to a JCR-SQL2 query on {@code localname() = localPart} under
     *       {@code /roles} (handles roles whose path is unknown but whose local name is unique).
     *       </li>
     * </ol>
     *
     * @param editSession JCR system session in the {@code default} workspace
     * @param roleName    role name as returned by the ACL entry, e.g. {@code translator-en} or
     *                    {@code translator/translator-en}
     * @return the role node, or {@code null} if it cannot be found
     */
    private Node resolveRoleNode(Session editSession, String roleName) {
        try {
            String fullRolePath = "/roles/" + roleName;
            if (editSession.nodeExists(fullRolePath)) {
                return editSession.getNode(fullRolePath);
            }

            String localName = roleName.contains("/") ? roleName.substring(roleName.lastIndexOf('/') + 1) : roleName;
            NodeIterator roleNodes = editSession.getWorkspace().getQueryManager().createQuery(
                    "select * from [" + ROLE_NODE_TYPE + "] as r where localname()='"
                            + JCRContentUtils.sqlEncode(localName)
                            + "' and isdescendantnode(r,['/roles'])",
                    Query.JCR_SQL2).execute().getNodes();
            if (roleNodes.hasNext()) {
                return roleNodes.nextNode();
            }
        } catch (RepositoryException e) {
            logger.debug("Unable to resolve role node for {}", roleName, e);
        }
        return null;
    }

    /**
     * Builds a map of privilege names (and their local variants) that would grant the queried
     * permission, keyed by privilege name with the value being a human-readable "via" explanation.
     *
     * <p>The map is pre-seeded with the queried permission and its local (namespace-stripped)
     * form.  It is then expanded by iterating all registered Jahia privileges and checking
     * whether any of them aggregate the target privilege — if so, the aggregate privilege is
     * also added with a {@code "via aggregate privilege: …"} note.
     *
     * <p>This expansion is necessary because a role may declare a broad aggregate privilege
     * (e.g. {@code jcr:write}) without explicitly listing the narrower permission being queried
     * (e.g. {@code jcr:modifyProperties}).
     *
     * @param systemSession JCR system session used to obtain the {@code AccessControlManager}
     * @param checkPermission permission name to expand
     * @return map from privilege name / local name to explanation; never {@code null}
     */
    private Map<String, String> buildGrantingPrivilegeMap(JCRSessionWrapper systemSession, String checkPermission) {
        Map<String, String> grantingPrivToVia = new LinkedHashMap<>();
        grantingPrivToVia.put(checkPermission, "");
        String checkPermLocal = checkPermission.contains(":")
                ? checkPermission.substring(checkPermission.indexOf(':') + 1)
                : checkPermission;
        if (!checkPermLocal.equals(checkPermission)) {
            grantingPrivToVia.put(checkPermLocal, "");
        }

        try {
            AccessControlManager accessControlManager = systemSession.getAccessControlManager();
            Privilege targetPrivilege = accessControlManager.privilegeFromName(checkPermission);
            grantingPrivToVia.put(targetPrivilege.getName(), "");

            for (String registeredName : JahiaPrivilegeRegistry.getRegisteredPrivilegeNames()) {
                try {
                    Privilege registeredPrivilege = accessControlManager.privilegeFromName(registeredName);
                    if (grantingPrivToVia.containsKey(registeredPrivilege.getName())) {
                        continue;
                    }
                    for (Privilege aggregatePrivilege : registeredPrivilege.getAggregatePrivileges()) {
                        if (aggregatePrivilege.equals(targetPrivilege)) {
                            String via = "via aggregate privilege: " + registeredName;
                            grantingPrivToVia.put(registeredPrivilege.getName(), via);
                            grantingPrivToVia.put(registeredName, via);
                            String aggregateLocal = registeredName.contains(":")
                                    ? registeredName.substring(registeredName.indexOf(':') + 1)
                                    : registeredName;
                            grantingPrivToVia.put(aggregateLocal, via);
                            break;
                        }
                    }
                } catch (RepositoryException e) {
                    logger.debug("Unable to inspect aggregate privilege {}", registeredName, e);
                }
            }
        } catch (RepositoryException e) {
            logger.debug("Unable to expand aggregate privileges for {}", checkPermission, e);
        }
        return grantingPrivToVia;
    }

    /**
     * Scans the node-level and site-level permissions of {@code roleResult} for entries that
     * appear in {@code grantingPrivToVia}.  For each match, one {@link PermCheckMatch} is added
     * to {@code permCheckMatches} and the matching permission path is recorded in
     * {@code highlightedPerms} for visual emphasis in the view.
     *
     * <p>Only the first matching permission per category (node vs site) is recorded to avoid
     * duplicating matches when both a parent and a child permission appear in the list.
     *
     * @param roleResult          role whose permissions are scanned
     * @param grantingPrivToVia   map built by {@link #buildGrantingPrivilegeMap}
     * @param highlightedPerms    set to accumulate highlighted permission paths
     * @param permCheckMatches    list to accumulate match records
     */
    private void addPermCheckMatches(RoleResult roleResult, Map<String, String> grantingPrivToVia,
            Set<String> highlightedPerms, List<PermCheckMatch> permCheckMatches) {
        for (PermissionEntry permission : roleResult.getNodePermissions()) {
            String via = lookupPerm(grantingPrivToVia, permission.getPath());
            if (via != null) {
                highlightedPerms.add(permission.getPath());
                permCheckMatches.add(new PermCheckMatch(roleResult.getRoleName(), roleResult.getContentGrantPath(),
                        roleResult.getGrantType(), "node", via, roleResult.getContentEditorUrl()));
                break;
            }
        }

        for (SitePermGroup sitePermGroup : roleResult.getSitePermissions()) {
            for (PermissionEntry permission : sitePermGroup.getPermissions()) {
                String via = lookupPerm(grantingPrivToVia, permission.getPath());
                if (via != null) {
                    highlightedPerms.add(permission.getPath());
                    String detail = sitePermGroup.getName() + " on " + sitePermGroup.getTargetPath()
                            + (via.isEmpty() ? "" : " (" + via + ")");
                    permCheckMatches.add(new PermCheckMatch(roleResult.getRoleName(), roleResult.getContentGrantPath(),
                            roleResult.getGrantType(), "site", detail, roleResult.getContentEditorUrl()));
                    break;
                }
            }
        }
    }

    /** Adds all entries from {@code additions} to {@code target}, skipping duplicates by path. */
    private void addPermissionEntries(List<PermissionEntry> target, List<PermissionEntry> additions) {
        for (PermissionEntry addition : additions) {
            addIfMissing(target, addition);
        }
    }

    /**
     * Convenience wrapper that strips the {@code /permissions} prefix from a permission node's
     * path and delegates to {@link #collectPermissionTree} for recursive collection.
     */
    private void addPermissionTree(Node permissionNode, List<PermissionEntry> target) {
        try {
            String relativePath = permissionNode.getPath().startsWith("/permissions")
                    ? permissionNode.getPath().substring("/permissions".length())
                    : permissionNode.getPath();
            collectPermissionTree(permissionNode, relativePath, target);
        } catch (RepositoryException e) {
            logger.debug("Unable to collect permission tree for {}", permissionNode, e);
        }
    }

    /**
     * Recursively walks the {@code jnt:permission} subtree rooted at {@code permissionNode},
     * adding a {@link PermissionEntry} for each node (including the root) to {@code target}.
     */
    private void collectPermissionTree(Node permissionNode, String relativePath, List<PermissionEntry> target)
            throws RepositoryException {
        addIfMissing(target, new PermissionEntry(relativePath));
        NodeIterator children = permissionNode.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (child.isNodeType("jnt:permission")) {
                collectPermissionTree(child, relativePath + "/" + child.getName(), target);
            }
        }
    }

    /**
     * Strips the {@code §N} duplicate suffix from a role map key, returning the clean base name.
     * E.g. {@code "translator-en§2"} → {@code "translator-en"}.
     */
    private String stripDuplicateSuffix(String value) {
        int separatorIndex = value.indexOf(ROLE_DUPLICATE_SEPARATOR);
        return separatorIndex >= 0 ? value.substring(0, separatorIndex) : value;
    }

    /**
     * Extracts the site key from a node path of the form {@code /sites/{siteKey}/…}.
     *
     * @param nodePath absolute JCR path
     * @return site key, or {@code null} if the path does not start with {@code /sites/}
     */
    private String extractSiteKey(String nodePath) {
        if (nodePath == null || !nodePath.startsWith("/sites/")) {
            return null;
        }
        String remaining = nodePath.substring("/sites/".length());
        int separatorIndex = remaining.indexOf('/');
        return separatorIndex >= 0 ? remaining.substring(0, separatorIndex) : remaining;
    }

    /**
     * Builds the server base URL ({@code scheme://host[:port]}) from the current HTTP request,
     * omitting the port suffix for the standard ports 80 and 443.
     *
     * @param request current HTTP request; {@code null} returns {@code null}
     * @return server base URL, or {@code null}
     */
    private String buildServerBase(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        int port = request.getServerPort();
        String portSuffix = port == 80 || port == 443 ? "" : ":" + port;
        return request.getScheme() + "://" + request.getServerName() + portSuffix;
    }

    /** Adds {@code value} to {@code target} only if the list does not already contain it. */
    private void addIfMissing(List<String> target, String value) {
        if (!target.contains(value)) {
            target.add(value);
        }
    }

    /** Adds {@code value} to {@code target} only if no entry with the same path already exists. */
    private void addIfMissing(List<PermissionEntry> target, PermissionEntry value) {
        for (PermissionEntry entry : target) {
            if (entry.getPath().equals(value.getPath())) {
                return;
            }
        }
        target.add(value);
    }

    /** Safely logs out a JCR session, ignoring {@code null} and already-closed sessions. */
    private void logoutSession(Session session) {
        if (session == null) {
            return;
        }
        try {
            if (session.isLive()) {
                session.logout();
            }
        } catch (Exception e) {
            logger.debug("Unable to close JCR session", e);
        }
    }
}
