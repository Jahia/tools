package org.jahia.modules.tools.userpermissions;

/**
 * Utility class that builds jContent deep-link URLs for opening a content node directly in the
 * Jahia content editor (jContent).
 *
 * <p>The generated URL format is:
 * <pre>
 * {serverBase}/jahia/jcontent/{siteKey}/{lang}/{treePrefix}/{relativePath}
 *     ?params=(sub:!f)
 *     #(contentEditor:!((formKey:modal_0,isFullscreen:!t,lang:{lang},mode:edit,uilang:{lang},uuid:{uuid})))
 * </pre>
 *
 * <p>The {@code treePrefix} is derived from the first path segment after the site root:
 * <ul>
 *   <li>{@code files} for nodes under {@code /sites/{siteKey}/files}</li>
 *   <li>{@code contents} for nodes under {@code /sites/{siteKey}/contents}</li>
 *   <li>{@code pages} for all other nodes (including {@code home})</li>
 * </ul>
 *
 * <p>The tree path used in the URL points to the <em>parent</em> of the target node so that the
 * jContent tree is positioned correctly while the editor modal opens on the node itself via its
 * UUID.
 */
public final class ContentEditorUrlBuilder {

    private ContentEditorUrlBuilder() {
        // static utility class — no instances
    }

    /**
     * Builds a jContent editor deep-link URL for the given content node.
     *
     * @param serverBase scheme + host + optional port, e.g. {@code http://localhost:8080}
     * @param siteKey    Jahia site key, e.g. {@code digitall}
     * @param lang       two-letter language code, e.g. {@code en}
     * @param nodePath   absolute JCR path of the node to open, e.g.
     *                   {@code /sites/digitall/home/about}
     * @param uuid       UUID of the node (used to open the editor modal)
     * @return fully formed jContent URL, or {@code null} if any required parameter is blank or
     *         the node path does not belong to the given site
     */
    public static String build(String serverBase, String siteKey, String lang, String nodePath, String uuid) {
        if (isBlank(serverBase) || isBlank(siteKey) || isBlank(lang) || isBlank(nodePath) || isBlank(uuid)) {
            return null;
        }

        String sitePrefix = "/sites/" + siteKey;
        if (!nodePath.startsWith(sitePrefix)) {
            return null;
        }

        // Use the parent of the node as the tree context path so jContent highlights the correct
        // folder in the navigation tree while the editor modal opens on the node itself.
        String treeNodePath = nodePath;
        int lastSlash = nodePath.lastIndexOf('/');
        if (lastSlash > sitePrefix.length()) {
            treeNodePath = nodePath.substring(0, lastSlash);
        }

        // Build the relative path (after stripping the site prefix).
        String relativePath;
        if (treeNodePath.equals(sitePrefix)) {
            relativePath = "home";
        } else if (treeNodePath.startsWith(sitePrefix + "/")) {
            relativePath = treeNodePath.substring((sitePrefix + "/").length());
        } else {
            return null;
        }

        if (isBlank(relativePath)) {
            return null;
        }

        // Determine the jContent tree prefix from the first path segment.
        String[] segments = relativePath.split("/", 2);
        String firstSegment = segments[0];
        String prefix;
        if ("files".equals(firstSegment)) {
            prefix = "files";
        } else if ("contents".equals(firstSegment)) {
            prefix = "contents";
        } else {
            prefix = "pages";
        }

        String treePath = prefix + "/" + relativePath;
        return serverBase + "/jahia/jcontent/" + siteKey + "/" + lang + "/" + treePath
                + "?params=(sub:!f)#(contentEditor:!((formKey:modal_0,isFullscreen:!t,lang:" + lang
                + ",mode:edit,uilang:" + lang + ",uuid:" + uuid + ")))";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
