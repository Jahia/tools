package org.jahia.modules.tools.userpermissions;

/**
 * Immutable value object representing a single resolved permission entry in the permission tree.
 *
 * <p>Permissions are stored as paths relative to the {@code /permissions} JCR node, for example
 * {@code /jContentActions/pageTreeActions/deletePageAction}. Simple permissions that could not be
 * resolved in the tree are stored as their raw name (e.g. {@code api-access}).
 *
 * <p>The helper methods {@link #getLocalName()}, {@link #getDepth()}, and {@link #getIndentPx()}
 * are designed for direct use in JSP/JSTL view templates.
 */
public class PermissionEntry {

    /** Path relative to {@code /permissions}, e.g. {@code /jContentActions/pageTreeActions/deletePageAction}. */
    private final String path;

    /**
     * Creates a new entry for the given permission path.
     *
     * @param path path relative to {@code /permissions}, or a raw permission name if the node could
     *             not be found in the permission tree
     */
    public PermissionEntry(String path) {
        this.path = path;
    }

    /**
     * Returns the full permission path as stored in this entry.
     *
     * @return permission path, never {@code null}
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns {@code true} when the path contains at least one {@code /}, indicating that the
     * permission was resolved in the {@code /permissions} tree and has a known ancestry.
     *
     * @return {@code true} for tree-resolved permissions, {@code false} for raw names
     */
    public boolean isHierarchical() {
        return path != null && path.contains("/");
    }

    /**
     * Returns the last segment of the path (the permission's own name), suitable for compact
     * display in a UI.  For non-hierarchical entries the full path is returned unchanged.
     *
     * @return local permission name, e.g. {@code deletePageAction}
     */
    public String getLocalName() {
        return isHierarchical() ? path.substring(path.lastIndexOf('/') + 1) : path;
    }

    /**
     * Returns the nesting depth of this permission within the tree, computed as the number of
     * {@code /} separators minus one.  Depth 0 means the permission sits directly under
     * {@code /permissions} (or is a raw non-hierarchical name).
     *
     * @return nesting depth, always &ge; 0
     */
    public int getDepth() {
        if (!isHierarchical()) {
            return 0;
        }
        return Math.max(0, path.length() - path.replace("/", "").length() - 1);
    }

    /**
     * Returns the CSS left-indent in pixels to apply when rendering nested permissions in a list,
     * calculated as {@code depth * 14}.
     *
     * @return indent in pixels, always &ge; 0
     */
    public int getIndentPx() {
        return getDepth() * 14;
    }
}
