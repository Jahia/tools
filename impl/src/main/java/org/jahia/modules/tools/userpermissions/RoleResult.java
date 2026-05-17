package org.jahia.modules.tools.userpermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of one effective role assignment for the inspected user on the target node.
 *
 * <p>Each instance corresponds to a single entry in the resolved ACL chain: the role
 * ({@link #getRoleName()}), how it was granted ({@link #getGrantType()}), which principal matched
 * ({@link #getMatchedPrincipal()}), and on which content node the grant was recorded
 * ({@link #getContentGrantPath()}).
 *
 * <p>The role's permissions are split into two categories:
 * <ul>
 *   <li><strong>Node permissions</strong> — permissions evaluated against the content node itself,
 *       collected from the {@code j:permissionNames} / {@code j:permissions} properties of the
 *       role's {@code jnt:role} node (and its parent roles).</li>
 *   <li><strong>Site permissions</strong> — permissions evaluated against the site node, one
 *       {@link SitePermGroup} per {@code jnt:externalPermissions} child of the role node.</li>
 * </ul>
 *
 * <p>The {@link #getContentEditorUrl()} field is pre-computed by {@link ContentEditorUrlBuilder}
 * and ready to embed directly in HTML as a deep-link into jContent's content editor.
 */
public class RoleResult {

    /** Local name of the role as it appears under {@code /roles}, e.g. {@code translator-en}. */
    private final String roleName;

    /**
     * ACE type: {@code "GRANT"} or {@code "DENY"}.
     * External ACEs are always surfaced as {@code "GRANT"} after source resolution.
     */
    private final String grantType;

    /**
     * The principal string that matched the inspected user, e.g. {@code u:mathias} for a direct
     * user grant or {@code g:sample-group} for a group-based grant.
     */
    private final String matchedPrincipal;

    /**
     * Absolute JCR path of the content node on whose {@code j:acl} the granting ACE lives.
     * For roles surfaced via a site-level {@code jnt:externalAce}, this is the path of the
     * original content node resolved through the {@code j:sourceAce} reference chain.
     */
    private final String contentGrantPath;

    /**
     * Absolute JCR path of the role definition node under {@code /roles}, or {@code null} if the
     * role could not be resolved in the repository.
     */
    private final String roleNodePath;

    /** Permissions evaluated against the content node (from the role's {@code j:permissionNames}). */
    private final List<PermissionEntry> nodePermissions;

    /** Site-level permission groups (one per {@code jnt:externalPermissions} child of the role). */
    private final List<SitePermGroup> sitePermissions;

    /**
     * Pre-computed jContent deep-link URL for opening the granting content node in the editor,
     * or {@code null} if the URL could not be built (e.g. node outside a site tree).
     */
    private final String contentEditorUrl;

    /**
     * Constructs a fully populated, immutable role result.
     *
     * @param roleName          local role name
     * @param grantType         {@code "GRANT"} or {@code "DENY"}
     * @param matchedPrincipal  principal that matched the inspected user (prefixed with {@code u:} or {@code g:})
     * @param contentGrantPath  path of the content node carrying the granting ACE
     * @param roleNodePath      path of the role definition node, or {@code null}
     * @param nodePermissions   permissions evaluated at node level
     * @param sitePermissions   permissions evaluated at site level, grouped by external-permissions name
     * @param contentEditorUrl  pre-computed jContent editor URL, or {@code null}
     */
    public RoleResult(String roleName, String grantType, String matchedPrincipal, String contentGrantPath,
            String roleNodePath, List<PermissionEntry> nodePermissions, List<SitePermGroup> sitePermissions,
            String contentEditorUrl) {
        this.roleName = roleName;
        this.grantType = grantType;
        this.matchedPrincipal = matchedPrincipal;
        this.contentGrantPath = contentGrantPath;
        this.roleNodePath = roleNodePath;
        this.nodePermissions = Collections.unmodifiableList(new ArrayList<PermissionEntry>(nodePermissions));
        this.sitePermissions = Collections.unmodifiableList(new ArrayList<SitePermGroup>(sitePermissions));
        this.contentEditorUrl = contentEditorUrl;
    }

    /** @return local role name, e.g. {@code translator-en} */
    public String getRoleName() {
        return roleName;
    }

    /** @return {@code "GRANT"} or {@code "DENY"} */
    public String getGrantType() {
        return grantType;
    }

    /**
     * Returns the principal that matched the inspected user, prefixed with {@code u:} for user
     * principals or {@code g:} for group principals.
     *
     * @return matched principal string
     */
    public String getMatchedPrincipal() {
        return matchedPrincipal;
    }

    /**
     * Returns the absolute path of the content node on whose {@code j:acl} the granting ACE was
     * found.  For roles surfaced via a site-level external ACE this is the source content node
     * resolved through {@code j:sourceAce}, not the site node itself.
     *
     * @return content node path
     */
    public String getContentGrantPath() {
        return contentGrantPath;
    }

    /**
     * Returns the absolute path of the role's definition node under {@code /roles}, or
     * {@code null} when the role could not be located.
     *
     * @return role node path, or {@code null}
     */
    public String getRoleNodePath() {
        return roleNodePath;
    }

    /** @return immutable list of node-level permissions */
    public List<PermissionEntry> getNodePermissions() {
        return nodePermissions;
    }

    /** @return immutable list of site-level permission groups */
    public List<SitePermGroup> getSitePermissions() {
        return sitePermissions;
    }

    /**
     * Returns the pre-computed jContent deep-link URL for editing the granting content node,
     * or {@code null} if it could not be constructed.
     *
     * @return content editor URL, or {@code null}
     */
    public String getContentEditorUrl() {
        return contentEditorUrl;
    }
}
