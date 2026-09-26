package org.jahia.modules.tools.userpermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of the site-level permissions that a role grants via one of its
 * {@code jnt:externalPermissions} child nodes.
 *
 * <p>In Jahia, a role can carry <em>external permissions</em>: when the role is granted on a
 * content node, Jahia's {@code AclListener} automatically creates a mirror ACE on the site node
 * so that the listed permissions are checked against the site context rather than the content
 * node itself.  Each {@code jnt:externalPermissions} child gives a distinct group of such
 * permissions; this class models one such group.
 *
 * <p>Example: the {@code translator-en} role has a {@code currentSite-access} external-permissions
 * child with {@code j:path=currentSite} — granting it implicitly grants {@code jContentAccess}
 * (and siblings) on the site.
 */
public class SitePermGroup {

    /** Name of the {@code jnt:externalPermissions} child node, e.g. {@code currentSite-access}. */
    private final String name;

    /**
     * Value of the {@code j:path} property on the {@code jnt:externalPermissions} node,
     * indicating where the permissions are evaluated (e.g. {@code currentSite}).
     */
    private final String targetPath;

    /** Resolved permissions provided by this external-permissions group. */
    private final List<PermissionEntry> permissions;

    /**
     * Creates a new site permission group.
     *
     * @param name        name of the {@code jnt:externalPermissions} child node
     * @param targetPath  value of {@code j:path} — where the permissions are checked
     * @param permissions resolved permissions belonging to this group
     */
    public SitePermGroup(String name, String targetPath, List<PermissionEntry> permissions) {
        this.name = name;
        this.targetPath = targetPath;
        this.permissions = Collections.unmodifiableList(new ArrayList<PermissionEntry>(permissions));
    }

    /**
     * Returns the name of the {@code jnt:externalPermissions} child node.
     *
     * @return group name, e.g. {@code currentSite-access}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the path expression indicating where these permissions are evaluated,
     * as declared in the {@code j:path} property of the {@code jnt:externalPermissions} node.
     *
     * @return target path expression, e.g. {@code currentSite}
     */
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * Returns the immutable list of resolved permissions belonging to this group.
     *
     * @return permission entries, never {@code null}
     */
    public List<PermissionEntry> getPermissions() {
        return permissions;
    }
}
