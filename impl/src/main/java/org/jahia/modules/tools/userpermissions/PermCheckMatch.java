package org.jahia.modules.tools.userpermissions;

/**
 * Immutable record of one role that was found to provide a specific queried permission to the
 * inspected user, produced by the quick <em>permission check</em> feature of
 * {@link UserPermissionsAnalyzer}.
 *
 * <p>When the user submits a permission name to check (e.g. {@code jcr:read}), the analyzer
 * calls {@code JCRNodeWrapper.hasPermission()} to obtain the authoritative yes/no answer and
 * then scans the resolved {@link RoleResult} list to identify which role(s) actually provide
 * that permission — producing one {@code PermCheckMatch} per matching role.
 */
public class PermCheckMatch {

    /** Name of the role that provides the queried permission. */
    private final String roleName;

    /** Absolute JCR path of the content node on whose {@code j:acl} the granting ACE was found. */
    private final String grantPath;

    /** ACE type: {@code "GRANT"} or {@code "DENY"}. */
    private final String grantType;

    /**
     * Scope at which the permission was found: {@code "node"} for a node-level permission or
     * {@code "site"} for a permission from a {@code jnt:externalPermissions} group.
     */
    private final String level;

    /**
     * Human-readable explanation of why this match was found, e.g.
     * {@code "via aggregate privilege: jcr:write"} or the external-permissions group name.
     * May be an empty string when the permission name matched directly.
     */
    private final String detail;

    /**
     * Pre-computed jContent deep-link URL for opening the granting content node in the editor,
     * or {@code null} if the URL could not be built.
     */
    private final String contentEditorUrl;

    /**
     * Constructs a permission-check match record.
     *
     * @param roleName         name of the matching role
     * @param grantPath        path of the content node carrying the granting ACE
     * @param grantType        {@code "GRANT"} or {@code "DENY"}
     * @param level            {@code "node"} or {@code "site"}
     * @param detail           explanation string (may be empty, never {@code null})
     * @param contentEditorUrl pre-computed editor URL, or {@code null}
     */
    public PermCheckMatch(String roleName, String grantPath, String grantType, String level, String detail,
            String contentEditorUrl) {
        this.roleName = roleName;
        this.grantPath = grantPath;
        this.grantType = grantType;
        this.level = level;
        this.detail = detail;
        this.contentEditorUrl = contentEditorUrl;
    }

    /** @return name of the role that provides the queried permission */
    public String getRoleName() {
        return roleName;
    }

    /** @return path of the content node carrying the granting ACE */
    public String getGrantPath() {
        return grantPath;
    }

    /** @return {@code "GRANT"} or {@code "DENY"} */
    public String getGrantType() {
        return grantType;
    }

    /** @return {@code "node"} for a node-level permission, {@code "site"} for a site-level one */
    public String getLevel() {
        return level;
    }

    /**
     * Returns an explanation of the match, e.g. {@code "via aggregate privilege: jcr:write"}.
     * Empty when the permission name matched directly.
     *
     * @return detail string, never {@code null}
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Returns the pre-computed jContent deep-link URL for the granting content node, or
     * {@code null} if it could not be constructed.
     *
     * @return content editor URL, or {@code null}
     */
    public String getContentEditorUrl() {
        return contentEditorUrl;
    }
}
