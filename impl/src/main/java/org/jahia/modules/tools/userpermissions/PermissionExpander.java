package org.jahia.modules.tools.userpermissions;

import org.jahia.services.content.JCRContentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * Utility class that resolves a Jahia permission name to its full set of
 * {@link PermissionEntry} objects by walking the {@code /permissions} JCR tree.
 *
 * <p>Jahia organises permissions hierarchically under {@code /permissions}.  When a role grants
 * a parent permission (e.g. {@code jContentActions}), all of its {@code jnt:permission}
 * descendants are implicitly granted as well.  This class expands a single permission name into
 * the complete set of paths so the UI can display the full hierarchy.
 *
 * <p>Usage:
 * <pre>
 *     List&lt;PermissionEntry&gt; entries = PermissionExpander.expand("jContentActions", editSession);
 *     // entries contains /jContentActions, /jContentActions/pageTreeActions, ...
 * </pre>
 */
public final class PermissionExpander {

    private static final Logger logger = LoggerFactory.getLogger(PermissionExpander.class);

    private PermissionExpander() {
        // static utility class — no instances
    }

    /**
     * Resolves {@code permName} to its entry in the {@code /permissions} tree and returns a list
     * containing the permission's own path followed by all descendant {@code jnt:permission}
     * paths (depth-first order).
     *
     * <p>Lookup strategy:
     * <ol>
     *   <li>Strip any JCR namespace prefix (e.g. {@code jcr:read_default} → {@code read_default})
     *       since workspace-scoped names may include a prefix not present in node names.</li>
     *   <li>Query {@code /permissions} using {@code localname() = localName} to find the node.</li>
     *   <li>Recurse into all {@code jnt:permission} children via {@link #collect}.</li>
     *   <li>If the node is not found in the tree, fall back to returning the raw {@code permName}
     *       as a single non-hierarchical entry.</li>
     * </ol>
     *
     * @param permName the permission name to expand, e.g. {@code jContentActions} or
     *                 {@code jcr:read_default}; {@code null} or blank values are silently ignored
     * @param session  a JCR session with read access to {@code /permissions} (typically the
     *                 system session in the {@code default} workspace)
     * @return non-null list of resolved entries; contains at least one element unless
     *         {@code permName} is blank
     */
    public static List<PermissionEntry> expand(String permName, Session session) {
        List<PermissionEntry> permissions = new ArrayList<PermissionEntry>();
        if (permName == null || permName.trim().isEmpty() || session == null) {
            return permissions;
        }

        // Strip namespace prefix so workspace-scoped names like "jcr:read_default" resolve correctly.
        String localName = permName.contains(":") ? permName.substring(permName.indexOf(':') + 1) : permName;
        try {
            NodeIterator nodes = session.getWorkspace().getQueryManager().createQuery(
                    "SELECT * FROM [jnt:permission] WHERE localname()='" + JCRContentUtils.sqlEncode(localName)
                            + "' AND ISDESCENDANTNODE('/permissions')",
                    Query.JCR_SQL2).execute().getNodes();
            if (nodes.hasNext()) {
                Node permissionNode = nodes.nextNode();
                // Build the path relative to /permissions (strip the root prefix).
                String relativePath = permissionNode.getPath().startsWith("/permissions")
                        ? permissionNode.getPath().substring("/permissions".length())
                        : permissionNode.getPath();
                collect(permissionNode, relativePath, permissions);
                return permissions;
            }
        } catch (RepositoryException e) {
            logger.debug("Unable to expand permission {}", permName, e);
        }

        // Fallback: permission node not found — return the raw name as-is.
        addIfMissing(permissions, permName);
        return permissions;
    }

    /**
     * Recursively adds the given node's path and all of its {@code jnt:permission} descendant
     * paths to {@code target} in depth-first order.
     *
     * @param node    current permission node
     * @param relPath path of {@code node} relative to {@code /permissions}
     * @param target  accumulator for the collected entries
     */
    private static void collect(Node node, String relPath, List<PermissionEntry> target) {
        try {
            addIfMissing(target, relPath);
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (child.isNodeType("jnt:permission")) {
                    collect(child, relPath + "/" + child.getName(), target);
                }
            }
        } catch (RepositoryException e) {
            logger.debug("Unable to collect permission descendants for {}", relPath, e);
        }
    }

    /** Adds a new {@link PermissionEntry} for {@code path} only if no entry with that path exists yet. */
    private static void addIfMissing(List<PermissionEntry> target, String path) {
        for (PermissionEntry entry : target) {
            if (entry.getPath().equals(path)) {
                return;
            }
        }
        target.add(new PermissionEntry(path));
    }
}
