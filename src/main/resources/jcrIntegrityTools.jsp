<%@ page contentType="text/html; charset=UTF-8" language="java"
%>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page import="org.apache.commons.io.IOUtils" %>
<%@ page import="org.jahia.ajax.gwt.helper.CacheHelper" %>
<%@ page import="org.jahia.api.Constants" %>
<%@ page import="org.jahia.registries.ServicesRegistry" %>
<%@ page import="org.jahia.services.SpringContextSingleton" %>
<%@ page import="org.jahia.services.content.*" %>
<%@ page import="org.jahia.utils.DatabaseUtils" %>
<%@ page import="org.jahia.utils.RequestLoadAverage" %>
<%@ page import="org.quartz.JobDetail" %>
<%@ page import="org.quartz.SchedulerException" %>
<%@ page import="javax.jcr.*" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<c:set var="workspace" value="${functions:default(param.workspace, 'default')}"/>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <%@ include file="css.jspf" %>
    <title>JCR Integrity Tool</title>
    <script type="text/javascript">
        function toggleLayer(whichLayer) {
            var elem, vis;
            if (document.getElementById) // this is the way the standards work
                elem = document.getElementById(whichLayer);
            else if (document.all) // this is the way old msie versions work
                elem = document.all[whichLayer];
            else if (document.layers) // this is the way nn4 works
                elem = document.layers[whichLayer];
            vis = elem.style;
            // if the style.display value is blank we try to figure it out here
            if (vis.display === '' && elem.offsetWidth !== undefined && elem.offsetHeight !== undefined)
                vis.display = (elem.offsetWidth !== 0 && elem.offsetHeight !== 0) ? 'block' : 'none';
            vis.display = (vis.display === '' || vis.display === 'block') ? 'none' : 'block';
        }
    </script>
    <style type="text/css">
        div.hiddenDetails {
            margin: 0 20px 0 20px;
            display: none;
        }

        .error {
            color: #FF0000;
        }

        .warning {
            color: brown;
        }
    </style>
</head>
<body>
<%@ include file="logout.jspf" %>
<h1>JCR Integrity Tools</h1>

<p>
    This tool will perform some integrity checks on the JCR repository, and also implements some fixes.
</p>

<h2>Integrity checks</h2>
<%@ include file="functions.jspf" %>

<%!
    private static SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private void runJCRTest(final JspWriter out, HttpServletRequest request, JspContext pageContext, final boolean fix) throws IOException {

        if (running) {
            println(out, "ABORTING: check or fix already running, please wait for it to complete !");
            return;
        }
        running = true;
        mustStop = false;
        if (fix) {
            if (RequestLoadAverage.getInstance().getOneMinuteLoad() > 1) {
                println(out, "ABORTING: request load is above 1, users are using the platform and we cannot run a fix while this is the case !");
                running = false;
                return;
            }
            try {
                List<JobDetail> activeJobs = ServicesRegistry.getInstance().getSchedulerService().getAllActiveJobs();
                if (activeJobs.size() > 0) {
                    println(out, "ABORTING: background jobs are executing, cannot run fix while background jobs are present !");
                    running = false;
                    return;
                }
            } catch (SchedulerException se) {
                println(out, "ABORTING: error accessing scheduler service", se, false);
                running = false;
                return;
            }
        }

        long startTime;
        long bytesRead;
        long totalTime;
        final boolean referencesCheck = fix || !isParameterActive(request, "option", "noReferencesCheck");
        final boolean missingReferencesCheck = isParameterActive(request, "option", "missingReferencesCheck");
        final boolean binaryCheck = !isParameterActive(request, "option", "noBinariesCheck");
        final String integrityID = integrityID(request, "uuid");
        if (fix) {
            printTestName(out, "JCR Integrity Fix");
        } else {
            StringBuilder exclusions = new StringBuilder();
            if (!binaryCheck) {
                exclusions.append("no binaries");
            }
            if (!referencesCheck) {
                if (exclusions.length() > 0) {
                    exclusions.append(", ");
                }
                exclusions.append("no references");
            }

            printTestName(out, "JCR Integrity Check " + (exclusions.length() > 0 ? "(" + exclusions.toString() + ")" : ""));
        }

        try {

            String chosenWorkspace = request.getParameter("workspace");
            final Map<String, Long> results = new HashMap<String, Long>();
            results.put("bytesRead", 0L);
            results.put("nodesRead", 0L);
            bytesRead = 0;
            startTime = System.currentTimeMillis();

            for (String workspaceName : workspaces) {
                if (chosenWorkspace == null || chosenWorkspace.isEmpty() || chosenWorkspace.equals(workspaceName)) {

                    JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspaceName, null, new JCRCallback<Object>() {

                        @Override
                        public Object doInJCR(JCRSessionWrapper sessionWrapper) throws RepositoryException {

                            JCRNodeWrapper jahiaNode;
                            if (integrityID == "root") {
                                jahiaNode = sessionWrapper.getRootNode();
                            } else {
                                jahiaNode = sessionWrapper.getNodeByUUID(integrityID);
                            }

                            try {
                                println(out, "Running integrity check from path: " + jahiaNode.getPath());
                            } catch (IOException e) {
                                throw new RepositoryException("IOException while running", e);
                            }
                            Node jcrNode = jahiaNode.getRealNode();
                            Session jcrSession = jcrNode.getSession();

                            Workspace workspace = jcrSession.getWorkspace();
                            try {
                                println(out, "Traversing " + workspace.getName() + " workspace ...");
                                processNode(out, sessionWrapper, jcrNode, results, fix, referencesCheck, binaryCheck, missingReferencesCheck);
                            } catch (IOException e) {
                                throw new RepositoryException("IOException while running", e);
                            }
                            return null;
                        }
                    });
                }
            }

            if (fix) {
                CacheHelper cacheHelper = (CacheHelper) SpringContextSingleton.getInstance().getContext().getBean("CacheHelper");
                if (cacheHelper != null) {
                    println(out, "Flushing all caches...");
                    cacheHelper.flushAll();
                } else {
                    println(out, "Couldn't find cache helper, please flush all caches manually.");
                }
            }

            bytesRead = results.get("bytesRead");
            long nodesRead = results.get("nodesRead");
            totalTime = System.currentTimeMillis() - startTime;
            println(out, "Total time to process all JCR " + nodesRead + " nodes data" + (bytesRead == 0 ? "" : " (" + bytesRead + " bytes)") + " : " + totalTime + "ms");
            if (bytesRead > 0) {
                double jcrReadSpeed = bytesRead / (1024.0 * 1024.0) / (totalTime / 1000.0);
                println(out, "JCR processing speed = " + jcrReadSpeed + "MB/sec");
            }
        } catch (Throwable t) {
            println(out, "Error reading JCR ", t, false);
        } finally {
            running = false;
            mustStop = false;
        }
    }

    protected boolean processPropertyValue(JspWriter out, Session session, Node node, Property property, Value propertyValue, Map<String, Long> results, boolean fix, boolean referencesCheck, boolean binaryCheck) throws RepositoryException, IOException {
        int propertyType = propertyValue.getType();
        switch (propertyType) {
            case PropertyType.BINARY:
                if (!binaryCheck) {
                    break;
                }
                Binary binaryValue = null;
                //binaryValue.getSize();
                long bytesRead = results.get("bytesRead");
                InputStream binaryInputStream = null;
                try {
                    binaryValue = propertyValue.getBinary();
                    binaryInputStream = binaryValue.getStream();
                    while (binaryInputStream.read() != -1) {
                        bytesRead++;
                    }
                } finally {
                    IOUtils.closeQuietly(binaryInputStream);
                    if (binaryValue != null) {
                        binaryValue.dispose();
                    }
                }
                results.put("bytesRead", bytesRead);
                break;
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                if (!referencesCheck) {
                    break;
                }
                String uuid = propertyValue.getString();
                try {
                    Node referencedNode = session.getNodeByIdentifier(uuid);
                } catch (ItemNotFoundException infe) {
                    if (Constants.LIVE_WORKSPACE.equals(node.getSession().getWorkspace().getName()) && isNodeUnderSiteTemplates(node.getPath())) {
                        try {
                            JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null).getNodeByIdentifier(uuid);
                            // node found in default -> ignore this case
                            break;
                        } catch (ItemNotFoundException infeDefault) {
                            // node not found also in default
                        }
                    }
                    Connection conn = null;
                    PreparedStatement statement = null;
                    ResultSet resultSet = null;
                    try {
                        conn = DatabaseUtils.getDatasource().getConnection();
                        statement = conn.prepareStatement("select * from jahia_external_mapping where internalUuid=?");
                        statement.setString(1, uuid);
                        resultSet = statement.executeQuery();
                        if(resultSet.next()) {
                            println(out, "Mapping found towards "+resultSet.getString("externalId") + ", this reference is not available at this time (referenced from property " + property.getPath()+"), please check your mount points and/or external providers.", null, true);
                            if(fix) {
                                println(out, "It will not be fixed automatically.", null, true);
                            }
                            break;
                        }
                    } catch (SQLException throwables) {
                        //uuid is not an external reference
                    } finally {
                        DatabaseUtils.closeQuietly(resultSet);
                        DatabaseUtils.closeQuietly(statement);
                        DatabaseUtils.closeQuietly(conn);
                    }
                    println(out, "Couldn't find referenced node with UUID " + uuid + " referenced from property " + property.getPath(), null, true);
                    if (fix) {
                        if (mustRemoveParentNode(node)) {
                            println(out, "Fixing invalid reference by removing node " + node.getPath() + " from repository...");
                            Node parentNode = node.getParent();
                            Calendar originalLastModificationDate = null;
                            try {
                                originalLastModificationDate = parentNode.getProperty(Constants.JCR_LASTMODIFIED).getDate();
                            } catch (PathNotFoundException pnfe) {
                                originalLastModificationDate = null;
                            }
                            Session nodeSession = node.getSession();
                            if (!parentNode.isCheckedOut()) {
                                nodeSession.getWorkspace().getVersionManager().checkout(parentNode.getPath());
                            }
                            node.remove();
                            nodeSession.save();
                            // let's reload the node to make sure we don't have any cache issues.
                            parentNode = nodeSession.getNodeByIdentifier(parentNode.getIdentifier());
                            Calendar newLastModificationDate = null;
                            try {
                                newLastModificationDate = parentNode.getProperty(Constants.JCR_LASTMODIFIED).getDate();
                            } catch (PathNotFoundException pnfe) {
                                newLastModificationDate = null;
                            }
                            if (newLastModificationDate == null && originalLastModificationDate == null) {
                                // do nothing, they are equal
                            } else if (((newLastModificationDate != null) && (originalLastModificationDate == null)) ||
                                    ((newLastModificationDate == null) && (originalLastModificationDate != null)) ||
                                    (!newLastModificationDate.equals(originalLastModificationDate))) {
                                println(out, "Last modification date (" + originalLastModificationDate.getTime().toString() + ") was changed by save operation (to " + newLastModificationDate.getTime().toString() + "), must reset to old value !");
                                parentNode.setProperty(Constants.JCR_LASTMODIFIED, originalLastModificationDate);
                                nodeSession.save();
                            }
                            return false;
                        } else {
                            println(out, "Fixing invalid reference by setting reference property " + property.getPath() + " to null...");
                            Calendar originalLastModificationDate = null;
                            try {
                                originalLastModificationDate = node.getProperty(Constants.JCR_LASTMODIFIED).getDate();
                            } catch (PathNotFoundException pnfe) {
                                originalLastModificationDate = null;
                            }
                            if (property.isMultiple()) {
                                Value[] oldValues = property.getValues();
                                List<Value> newValues = new LinkedList<Value>();
                                for (Value oldValue : oldValues) {
                                    if (!oldValue.getString().equals(uuid)) {
                                        newValues.add(oldValue);
                                    }
                                }
                                property.setValue(newValues.toArray(new Value[]{}));
                            } else {
                                property.setValue((Value) null);
                            }
                            Session nodeSession = node.getSession();
                            nodeSession.save();
                            // let's reload the node to make sure we don't have any cache issues.
                            node = nodeSession.getNodeByIdentifier(node.getIdentifier());
                            Calendar newLastModificationDate = null;
                            try {
                                newLastModificationDate = node.getProperty(Constants.JCR_LASTMODIFIED).getDate();
                            } catch (PathNotFoundException pnfe) {
                                newLastModificationDate = null;
                            }
                            if (newLastModificationDate == null && originalLastModificationDate == null) {
                                // do nothing, they are equal
                            } else if (((newLastModificationDate != null) && (originalLastModificationDate == null)) ||
                                    ((newLastModificationDate == null) && (originalLastModificationDate != null)) ||
                                    (!newLastModificationDate.equals(originalLastModificationDate))) {
                                println(out, "Last modification date (" + originalLastModificationDate.getTime().toString() + ") was changed by save operation (to " + newLastModificationDate.getTime().toString() + "), must reset to old value !");
                                node.setProperty(Constants.JCR_LASTMODIFIED, originalLastModificationDate);
                                nodeSession.save();
                            }
                        }
                    }
                }
                break;
            default:
        }
        return true;
    }

    private boolean isNodeUnderSiteTemplates(String pathToCheck) {
        boolean isUnder = false;
        if (pathToCheck.startsWith("/sites/") && pathToCheck.length() > "/sites/".length()) {
            pathToCheck = pathToCheck.substring("/sites/".length());
            int ix = pathToCheck.indexOf("/templates/");
            isUnder = ix != -1 && ix == pathToCheck.indexOf('/');
        }
        return isUnder;
    }

    private boolean mustRemoveParentNode(Node node) throws RepositoryException {
        for (String nodeTypeToTest : invalidReferenceNodeTypesToRemove) {
            if (node.isNodeType(nodeTypeToTest)) {
                return true;
            }
        }
        return false;
    }

    protected void processNode(JspWriter out, Session session, Node node, Map<String, Long> results, boolean fix, boolean referencesCheck, boolean binaryCheck, boolean missingReferencesCheck) throws IOException, RepositoryException {
        long nodesRead = results.get("nodesRead");
        // first let's try to read all the properties
        try {
            // node let's recurse into subnodes.
            nodesRead++;
            if (nodesRead % 1000 == 0) {
                println(out, "Processed " + nodesRead + " nodes...");
            }
            results.put("nodesRead", nodesRead);

            if (missingReferencesCheck) {
                // Report missing nodes which refer to this node
                PropertyIterator weekRefs = node.getWeakReferences();
                PropertyIterator refs = node.getReferences();
                PropertyIterator[] iterators = new PropertyIterator[]{refs, weekRefs};

                for (PropertyIterator it : iterators) {
                    while (it.hasNext()) {
                        Property p = it.nextProperty();
                        try {
                            session.getNodeByIdentifier(p.getParent().getIdentifier());
                        } catch (Exception e) {
                            println(out, "Referencing node " + p.getParent().getPath() + " no longer exists for referenced node: " + node.getPath(), null, true);
                        }
                    }
                }
            }

            if (fix || referencesCheck || binaryCheck) {
                PropertyIterator propertyIterator = node.getProperties();
                while (propertyIterator.hasNext() && !mustStop) {
                    Property property = propertyIterator.nextProperty();
                    try {
                        property.getDefinition();
                        int propertyType = property.getType();
                        if (property.isMultiple()) {
                            Value[] values = property.getValues();
                            for (int i = 0; i < values.length; i++) {
                                if (!processPropertyValue(out, session, node, property, values[i], results, fix, referencesCheck, binaryCheck)) {
                                    return;
                                }
                            }
                        } else {
                            if (!processPropertyValue(out, session, node, property, property.getValue(), results, fix, referencesCheck, binaryCheck)) {
                                return;
                            }
                        }
                    } catch (javax.jcr.nodetype.ConstraintViolationException x) {
                        //Definition was changed, property is missing
                        println(out, "Warning: Property definition for node " + node.getPath() + " is missing:" + x.getMessage());
                    }
                }
            }
            NodeIterator childNodeIterator = node.getNodes();
            while (childNodeIterator.hasNext() && !mustStop) {
                Node childNode = childNodeIterator.nextNode();
                if (childNode.getName().equals("jcr:system")) {
                    println(out, "Ignoring jcr:system node and it's child objects");
                } else {
                    processNode(out, session, childNode, results, fix, referencesCheck, binaryCheck, missingReferencesCheck);
                }
            }
        } catch (ValueFormatException vfe) {
            println(out, "ValueFormatException while processing node " + node.getPath(), vfe, false);
        } catch (RepositoryException re) {
            println(out, "RepositoryException while processing node " + node.getPath(), re, false);
        }
    }

    // by default in the case of an invalid reference we will simply reset it to null, but if the node type is listed
    // in the following list, we will remove the parent node completely. This can be the case for group members, where
    // if the reference doesn't exist we want to remove the parent node completely. It is recommended that this list
    // be minimal as it does delete the node !
    static String[] invalidReferenceNodeTypesToRemove = new String[]{
            "nt:linkedFile",
            "jnt:member",
            "jnt:reference"
    };

    static boolean running = false;
    static boolean mustStop = false;
%>

<%
    if (request.getParameterMap().size() > 0) {
        long timer = System.currentTimeMillis();

        if (isParameterActive(request, "operation", "runJCRTest")) {
            runJCRTest(out, request, pageContext, false);
        }

        if (isParameterActive(request, "operation", "fixJCR")) {
            runJCRTest(out, request, pageContext, true);
        }

        if (isParameterActive(request, "operation", "stop")) {
            mustStop = true;
        }

        out.println("<h2>Test completed in " + (System.currentTimeMillis() - timer) + " ms.</h2>");
    } else {
        if (!running) {
            out.println("<form><input type=\"hidden\" name=\"toolAccessToken\" value=\"" + request.getAttribute("toolAccessToken") + "\"/>");
            renderWorkspaceSelector(out);
            renderNodeInput(out, "", "UUID of node to index from (will use root node if empty): ");
            out.println("(you can lookup UUID in JCR Browser: " + "<a title=\"Lookup UUID in JCR Browser\" href=\"jcrBrowser.jsp\" target=\"_blank\"><img src=\"/icons/search.png\" width=\"16\"height=\"16\" alt=\"lookup\" title=\"Lookup UUID in JCR Browser\"></a>)<br/>");
            renderRadio(out, "runJCRTest", "Run Java Content Repository integrity check", true);
            renderCheckbox(out, "noReferencesCheck", "Do not check reference properties", false);
            renderCheckbox(out, "missingReferencesCheck", "Check for missing references (node still keeps track of no longer existing nodes which referenced it)", false);
            renderCheckbox(out, "noBinariesCheck", "Do not check binary properties", true);
            renderRadio(out, "fixJCR", "Fix full Java Content Repository integrity (also performs check). DO NOT RUN IF PLATFORM IS ACTIVE (USERS, BACKGROUND JOBS ARE RUNNING !). Also this operation WILL DELETE node with invalid references so please backup your data before running this fix!", false);
            out.println("<input type=\"submit\" name=\"submit\" value=\"Submit\">");
            out.println("</form>");
        } else {
            out.println("<form><input type=\"hidden\" name=\"toolAccessToken\" value=\"" + request.getAttribute("toolAccessToken") + "\"/>");
            renderRadio(out, "stop", "Stop currently running check/fix", true);
            out.println("<input type=\"submit\" name=\"submit\" value=\"Submit\">");
            out.println("</form>");
        }
    }
%>

<%@ include file="gotoIndex.jspf" %>
</body>
</html>
