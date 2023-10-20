<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="net.sf.ehcache.Ehcache" %>
<%@ page import="net.sf.ehcache.Element" %>
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.jahia.services.cache.CacheEntry" %>
<%@ page import="org.jahia.services.cache.ehcache.EhCacheStatisticsWrapper" %>
<%@ page import="org.jahia.services.render.filter.cache.AclCacheKeyPartGenerator" %>
<%@ page import="org.jahia.services.render.filter.cache.ModuleCacheProvider" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%--
  Output cache monitoring JSP.
  User: rincevent
  Date: 28 mai 2008
  Time: 16:59:07
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<c:if test="${not empty param.flushkey}">
    <%
        System.out.println(request.getParameter("flushkey"));
        boolean removed = ModuleCacheProvider.getInstance().getCache().remove(request.getParameter("flushkey"));
        pageContext.setAttribute("removed", removed);
    %>
</c:if>
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
        <title>Display content of module output cache</title>
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
            ModuleCacheProvider.getInstance().flushNonCacheableFragments();
        }
        List keys = cache.getKeys();
        pageContext.setAttribute("keys", keys);
        pageContext.setAttribute("cache", cache);
        pageContext.setAttribute("stats", new EhCacheStatisticsWrapper(cache.getStatistics()));
    %>
    <body id="dt_example" class="container-fluid">
    <a href="../index.jsp" title="back to the overview of caches">overview</a>&nbsp;
    <a href="?refresh&toolAccessToken=${toolAccessToken}">refresh</a>&nbsp;
    <a href="?flush=true&toolAccessToken=${toolAccessToken}"
       onclick="return confirm('This will flush the content of the cache. Would you like to continue?')"
       title="flush the content of the module output cache">flush</a>&nbsp;
    <a href="?viewContent=${param.viewContent ? 'false' : 'true'}&toolAccessToken=${toolAccessToken}">${param.viewContent ? 'hide content preview' : 'preview content'}</a>
    <c:if test="${not empty removed and removed}">
        <p>Key (${requestScope.flushkey}) has been flushed</p>
    </c:if>
    <div id="statistics">
        <span>Cache Hits: ${stats.cacheHitCount} (Cache hits in memory : ${stats.localHeapHitCount}; Cache hits on disk : ${stats.localDiskHitCount})</span><br/>
        <span>Cache Miss: ${stats.cacheMissCount}</span><br/>
        <span>Object counts: ${stats.size}</span><br/>
        <span>Memory size: ${cache.memoryStoreSize}</span><br/>
        <span>Disk size: ${cache.diskStoreSize}</span><br/>
        <span>Cache entries size = <span id="cacheSize"></span></span><br/>
        <span>Dependencies cache entries size = <span id="depsCacheSize"></span></span><br/>
    </div>
    <div id="keys">
        <table id="cacheTable" class="table table-striped compact" data-table="dataTable">
            <thead>
            <tr>
                <th>Key</th>
                <th>Expiration</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <% long cacheSize = 0; %>
            <% long globalDepsCacheSize = 0; %>
            <c:forEach items="${keys}" var="key" varStatus="i">

                <tr class="gradeA">
                    <td>${key}</td>
                    <% String attribute = (String) pageContext.getAttribute("key");
                        final Element element = cache.getQuiet(attribute);
                        if (element != null && element.getObjectValue() != null) {
                    %>

                    <td><%=SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(
                            element.getExpirationTime()))%>
                    </td>
                    <% String content = (String) ((CacheEntry) element.getObjectValue()).getObject();
                        cacheSize += content != null ? content.length() : 0;
                    %>
                    <td>
                        <c:if test="${param.viewContent}" var="viewContent">
                            <%= content %>
                        </c:if>
                        <c:if test="${not viewContent}">
                            <div style="text-align: center;">
                                <c:url var="detailsUrl" value="ehcache_cj.jsp">
                                    <c:param name="key" value="${key}"/>
                                    <c:param name="toolAccessToken" value="${toolAccessToken}"/>
                                </c:url>
                                <c:url var="flushUrl" value="ehcache_cj.jsp">
                                    <c:param name="flushkey" value="${key}"/>
                                    <c:param name="toolAccessToken" value="${toolAccessToken}"/>
                                </c:url>
                                <a href="${detailsUrl}" target="_blank">view</a>
                                <a href="${flushUrl}">flush</a>
                                <br/>[<%= FileUtils.byteCountToDisplaySize(content.length()).replace(" ", "&nbsp;") %>
                                ]<br/>
                            </div>
                        </c:if>
                    </td>
                    <%} else { %>
                    <td>empty</td>
                    <td>empty</td>
                    <%}%>
                </tr>
            </c:forEach>
            <script type="text/javascript">
                document.addEventListener('DOMContentLoaded', () => {
                    document.getElementById("cacheSize").innerText = "<%= FileUtils.byteCountToDisplaySize(cacheSize) %>";
                    document.getElementById("depsCacheSize").innerText = "<%= FileUtils.byteCountToDisplaySize(globalDepsCacheSize) %>";
                });
            </script>
            </tbody>
        </table>
    </div>
    <script type="module" src="<c:url value='/modules/tools/javascript/apps/datatable.tools.bundle.js'/>"></script>
    </body>
    </html>
</c:if>
