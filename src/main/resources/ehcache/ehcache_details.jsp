<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="net.sf.ehcache.Ehcache" %>
<%@ page import="net.sf.ehcache.Element" %>
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.jahia.services.cache.CacheHelper" %>
<%@ page import="org.jahia.services.cache.ehcache.EhCacheStatisticsWrapper" %>
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
        Ehcache cache = CacheHelper.getCacheManager(pageContext.getRequest().getParameter("name")).getEhcache(pageContext.getRequest().getParameter("cache"));
        boolean removed = cache.remove(request.getParameter("flushkey"));
        pageContext.setAttribute("removed", removed);
    %>
</c:if>
<c:set var="title" value="Cache details"/>
<head>
    <%@ include file="../commons/html_header.jspf" %>
<%

    Ehcache cache = CacheHelper.getCacheManager(pageContext.getRequest().getParameter("name")).getEhcache(pageContext.getRequest().getParameter("cache"));
    if (pageContext.getRequest().getParameter("flush") != null) {
        System.out.println("Flushing cache content");
        cache.flush();
        cache.removeAll();
    }
    List keys = cache.getKeys();
    pageContext.setAttribute("keys", keys);
    pageContext.setAttribute("cache", cache);
    EhCacheStatisticsWrapper ehCacheStatisticsWrapper = new EhCacheStatisticsWrapper(cache.getStatistics());
    pageContext.setAttribute("stats", ehCacheStatisticsWrapper);
%>
</head>
<body id="dt_example" class="container-fluid">
<a href="../index.jsp" title="back to the overview of caches">overview</a>&nbsp;
<a href="?refresh&name=${param.name}&cache=${param.cache}&toolAccessToken=${toolAccessToken}">refresh</a>&nbsp;
<div id="statistics">
    <span>Cache Hits: ${stats.cacheHitCount} (Cache hits in memory : ${stats.localHeapHitCount}; Cache hits on disk : ${stats.localDiskHitCount})</span><br/>
    <span>Cache Miss: ${stats.cacheMissCount}</span><br/>
    <span>Object counts: ${stats.size}</span><br/>
    <span>Memory size: <%=FileUtils.byteCountToDisplaySize(ehCacheStatisticsWrapper.getLocalHeapSizeInBytes())%></span><br/>
    <span>Disk size: <%=FileUtils.byteCountToDisplaySize(ehCacheStatisticsWrapper.getLocalDiskSizeInBytes())%></span><br/>
    <span>Cache entries size = <span id="cacheSize"></span></span><br/>
    <span><%=ehCacheStatisticsWrapper%></span><br/>
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
        <c:forEach items="${keys}" var="key" varStatus="i">

            <tr class="gradeA">
                <td>${key}</td>
                <%
                    final Element element1 = cache.getQuiet(pageContext.getAttribute("key"));
                    final long expirationTime = element1 != null ? element1.getExpirationTime() : 0;
                %>

                <td><%= element1 != null
                        ? (expirationTime == Long.MAX_VALUE
                        ? "Never expires"
                        : SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(expirationTime))
                )
                        : "Element not found in cache or just expired" %>
                </td>
                <%
                    if (element1 != null) {
                        cacheSize += element1.getSerializedSize();
                    }
                %>
                <td>
                    <div style="text-align: center;">
                        <c:url var="flushUrl" value="ehcache_details.jsp?">
                            <c:param name="flushkey" value="${key}"/>
                            <c:param name="name" value="${param.name}"/>
                            <c:param name="cache" value="${param.cache}"/>
                            <c:param name="toolAccessToken" value="${toolAccessToken}"/>
                        </c:url>
                        <a href="${flushUrl}">flush</a>
                        <br/>[<%= element1 != null ? FileUtils.byteCountToDisplaySize(element1.getSerializedSize()).replace(" ", "&nbsp;") : "-" %>
                        ]
                    </div>
                </td>
            </tr>
        </c:forEach>
        <script type="text/javascript">
            document.addEventListener('DOMContentLoaded', () => {
                document.getElementById("cacheSize").innerText = "<%= FileUtils.byteCountToDisplaySize(cacheSize) %>";
            });
        </script>
        </tbody>
    </table>
</div>

<%@ include file="commons/footer.jspf" %>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/datatable.tools.bundle.js'/>"></script>
</body>
</html>
