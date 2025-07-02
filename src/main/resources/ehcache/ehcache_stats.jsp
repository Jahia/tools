<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="net.sf.ehcache.Ehcache" %>
<%@ page import="net.sf.ehcache.Element" %>
<%@ page import="org.jahia.services.cache.ehcache.EhCacheStatisticsWrapper" %>
<%@ page import="org.jahia.services.render.filter.cache.AclCacheKeyPartGenerator" %>
<%@ page import="org.jahia.services.render.filter.cache.ModuleCacheProvider" %>
<%--
  Output cache monitoring JSP.
  User: rincevent
  Date: 28 mai 2008
  Time: 16:59:07
--%>
<c:if test="${not empty param.flushkey}">
        <%
        System.out.println(request.getParameter("flushkey"));
        boolean removed = ModuleCacheProvider.getInstance().getCache().remove(request.getParameter("flushkey"));
        pageContext.setAttribute("removed", removed);
    %>
</c:if>
<c:set var="title" value="Display content of module output cache"/>
<head>
    <%@ include file="../commons/html_header.jspf" %>
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
    pageContext.setAttribute("cache", cache);
    pageContext.setAttribute("stats", new EhCacheStatisticsWrapper(cache.getStatistics()));
    pageContext.setAttribute("depstats", new EhCacheStatisticsWrapper(depCache.getStatistics()));
%>
<body id="dt_example" class="container-fluid">

<c:set var="headerActions">
    <li><a href="?refresh&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">refresh</span>Refresh</a>&nbsp;</li>
    <li><a href="?flush=true&toolAccessToken=${toolAccessToken}"
       onclick="return confirm('This will flush the content of the cache. Would you like to continue?')"
       title="flush the content of the module output cache"><span class="material-symbols-outlined">recycling</span>Flush</a>&nbsp;</li>
    <li><a href="?viewContent=${param.viewContent ? 'false' : 'true'}&toolAccessToken=${toolAccessToken}"><span class="material-symbols-outlined">preview</span>${param.viewContent ? 'Hide content preview' : 'Preview content'}</a></li>
</c:set>
<%@ include file="../commons/header.jspf" %>
<c:if test="${not empty removed and removed}">
    <p>Key (${requestScope.flushkey}) has been flushed</p>
</c:if>
<div id="statistics">
    <p>Module statistics</p>
    <span>Cache Hits: ${stats.cacheHitCount} (Cache hits in memory : ${stats.localHeapHitCount}; Cache hits on disk : ${stats.localDiskHitCount})</span><br/>
    <span>Cache Miss: ${stats.cacheMissCount}</span><br/>
    <span>Object counts: ${stats.size}</span><br/>
    <span>Memory size: ${cache.memoryStoreSize}</span><br/>
    <span>Disk size: ${cache.diskStoreSize}</span><br/>

    <p>Dependencies statistics</p>
    <span>Cache Hits: ${depstats.cacheHitCount} (Cache hits in memory : ${depstats.localHeapHitCount}; Cache hits on disk : ${depstats.localDiskHitCount})</span><br/>
    <span>Cache Miss: ${depstats.cacheMissCount}</span><br/>
    <span>Object counts: ${depstats.size}</span><br/>
</div>

<%@ include file="commons/footer.jspf" %>
</body>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/datatable.tools.bundle.js'/>"></script>
</html>

