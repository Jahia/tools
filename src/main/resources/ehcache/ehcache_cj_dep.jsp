<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="net.sf.ehcache.Ehcache" %>
<%@ page import="net.sf.ehcache.Element" %>
<%@ page import="org.jahia.services.cache.CacheEntry" %>
<%@ page import="org.jahia.services.render.filter.cache.AclCacheKeyPartGenerator" %>
<%@ page import="org.jahia.services.render.filter.cache.ModuleCacheProvider" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%--
  Output cache monitoring JSP.
  User: rincevent
  Date: 28 mai 2008
  Time: 16:59:07
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<c:if test="${not empty param.key}">
    <html>
    <body>
    <%
        System.out.println(request.getParameter("key"));
        Element elem = ModuleCacheProvider.getInstance().getCache().getQuiet(request.getParameter("key"));
        Object obj = elem != null ? ((CacheEntry) elem.getValue()).getObject() : null;
    %><%= obj %>
    </body>
    </html>
</c:if>
<c:if test="${empty param.key}">
    <html>
    <head>
        <title>Display content of module cache dependencies</title>
    </head>
    <%
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        Ehcache cache = cacheProvider.getCache();
        Ehcache depCache = cacheProvider.getDependenciesCache();
        if (pageContext.getRequest().getParameter("flush") != null) {
            System.out.println("Flushing cache content");
            cache.flush();
            cache.removeAll();
            depCache.flush();
            depCache.removeAll();
            ((AclCacheKeyPartGenerator) cacheProvider.getKeyGenerator().getPartGenerator("acls")).flushUsersGroupsKey();
        }
        List keys = depCache.getKeys();
        pageContext.setAttribute("keys", keys);
        pageContext.setAttribute("cache", cache);
    %>
    <body id="dt_example" class="container-fluid">
    <a href="../index.jsp" title="back to the overview of caches">overview</a>&nbsp;
    <a href="?flush=true&toolAccessToken=${toolAccessToken}"
       onclick="return confirm('This will flush the content of the cache. Would you like to continue?')"
       title="flush the content of the module output cache">flush</a>&nbsp;
    <div id="keys">
        <table id="cacheTable" class="table table-striped compact" data-table="dataTable">
            <thead>
            <tr>
                <th>Key</th>
                <th>Dependencies</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${keys}" var="key" varStatus="i">
                <% String attribute = (String) pageContext.getAttribute("key");
                    final Element element = depCache.getQuiet(attribute);
                    if (element != null) {
                %>
                <tr class="gradeA">

                    <td>${key}</td>
                    <td><%
                        Set<String> deps = (Set<String>) element.getValue();
                        if (deps.size() > 10) {
                            out.print("Number of dependencies : " + deps.size());
                        } else {
                            for (String dep : deps) {
                                out.print(dep + "<br/>");
                            }
                        }%>
                        <br/>
                    </td>
                </tr>
                <%}%>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <script type="module" src="<c:url value='/modules/tools/javascript/apps/datatable.tools.bundle.js'/>"></script>
    </body>
    </html>
</c:if>
