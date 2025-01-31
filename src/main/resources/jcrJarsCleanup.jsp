<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.slf4j.Logger"%>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.jahia.services.SpringContextSingleton" %>
<%@ page import="org.jahia.services.content.JCRTemplate" %>
<%@ page import="org.jahia.services.modulemanager.persistence.BundlePersister" %>
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ page import="javax.jcr.query.Query" %>
<%@ page import="javax.jcr.query.QueryResult" %>
<%@ page import="javax.jcr.NodeIterator" %>
<%@ page import="org.jahia.services.content.JCRNodeWrapper" %>
<%@ page import="org.jahia.services.content.JCRCallback" %>
<%@ page import="org.jahia.services.content.JCRSessionWrapper" %>
<%@ page import="javax.jcr.RepositoryException" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>

<%!
    private List<String> listBundles(final boolean doCleanup) throws RepositoryException {
        final Logger logger = LoggerFactory.getLogger("org.jahia.tools.jcrBundlesCleanup");

        final BundlePersister persister = (BundlePersister) SpringContextSingleton.getBean("org.jahia.services.modulemanager.persistence.BundlePersister");

        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<List<String>>() {
            @Override
            public List<String> doInJCR(JCRSessionWrapper jcrSession) throws RepositoryException {
                List<String> l = new ArrayList<>();

                Query query = jcrSession.getWorkspace().getQueryManager().createQuery("SELECT * FROM [jnt:moduleManagementBundle]", Query.JCR_SQL2);
                QueryResult result = query.execute();
                NodeIterator ni = result.getNodes();
                while (ni.hasNext()) {
                    JCRNodeWrapper node = (JCRNodeWrapper) ni.nextNode();
                    try {
                        if (BundleUtils.getBundle(node.getProperty("j:symbolicName").getString(), node.getProperty("j:version").getString()) == null) {
                            String bundleKey = node.getProperty("j:groupId").getString() + "/" + node.getProperty("j:symbolicName").getString() + "/" + node.getProperty("j:version").getString();
                            l.add(bundleKey);
                            if (doCleanup) {
                                if (persister.delete(bundleKey)) {
                                    logger.info("Remove " + bundleKey);
                                } else {
                                    logger.warn("Unable to remove bundle entry " + bundleKey + " for path " + node.getPath() + " - please remove the node manually");
                                }
                            }
                        } else {
                            logger.info(String.format("Module %s/%s found", node.getProperty("j:symbolicName").getString(), node.getProperty("j:version").getString()));
                        }
                    } catch (Exception e) {
                        logger.error("Unable to remove bundle entry for path " + node.getPath() + " - please remove the node manually", e);
                    }
                }

                return l;
            }
        });
    }
%>

<head>
    <title>JCR Bundles storage</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <%@ include file="css.jspf" %>
</head>

<%
    boolean b = Boolean.parseBoolean(request.getParameter("doCleanup"));
    List<String> result= listBundles(b);
    pageContext.setAttribute("result", result);
%>
<body>
<%@ include file="logout.jspf" %>
<div>
    <fieldset>
        <legend><strong>JCR bundles cleanup</strong></legend>

        <c:if test="${functions:length(result) == 0}">
            No unused bundle have been found.
        </c:if>

        <c:if test="${functions:length(result) > 0}">
            <c:if test="${param.doCleanup}">
                These bundles have been removed :
            </c:if>
            <c:if test="${!param.doCleanup}">
                These bundles can be removed :
            </c:if>

            <ul>
                <c:forEach items="${result}" var="item">
                    <li>${item}</li>
                </c:forEach>
            </ul>
            <c:if test="${!param.doCleanup}">
                <hr/>
                <form method="post">
				    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                    <button type="submit" name="doCleanup" value="true" >Do it</button>
                </form>
            </c:if>
        </c:if>
    </fieldset>
</div>
</body>
</html>
