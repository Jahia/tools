<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="net.sf.ehcache.Ehcache" %>
<%@ page import="net.sf.ehcache.Element" %>
<%@ page import="org.apache.commons.lang3.StringEscapeUtils" %>
<%@ page import="org.jahia.services.cache.CacheEntry" %>
<%@ page import="org.jahia.services.cache.ehcache.EhCacheStatisticsWrapper" %>
<%@ page import="org.jahia.services.content.JCRSessionFactory" %>
<%@ page import="org.jahia.services.render.filter.cache.ModuleCacheProvider" %>
<%@ page import="javax.jcr.RepositoryException" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>

        <%@ page contentType="text/xml;charset=UTF-8" language="java" %>
    <%
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        ModuleCacheProvider cacheProvider = ModuleCacheProvider.getInstance();
        Ehcache cache = cacheProvider.getCache();
        Ehcache depCache = cacheProvider.getDependenciesCache();
        List keys = cache.getKeys();
        pageContext.setAttribute("keys", keys);
        pageContext.setAttribute("cache", cache);
        pageContext.setAttribute("stats", new EhCacheStatisticsWrapper(cache.getStatistics()));
    %>
<cache>
            <c:forEach items="${keys}" var="key" varStatus="i">
    <entry key="<%= StringEscapeUtils.escapeXml((String)pageContext.getAttribute("key"))%>"><%
        String attribute = (String) pageContext.getAttribute("key");
            final Element element = cache.getQuiet(attribute);
            if (element != null && element.getObjectValue() != null) {
                pageContext.setAttribute("properties",((CacheEntry) element.getObjectValue()).getExtendedProperties());
        %>
        <creation><%=dateFormat.format(new Date(element.getCreationTime()))%></creation>
        <expiration><%=dateFormat.format(new Date(element.getExpirationTime()))%></expiration>
        <lastAccess><%=dateFormat.format(new Date(element.getLastAccessTime()))%></lastAccess>
        <properties><c:forEach items="${properties}" var="prop" varStatus="i">
            <property><%
                    Map.Entry entry = (Map.Entry) pageContext.getAttribute("prop");
            %>
                <key>${prop.key}</key>
                <value><%= StringEscapeUtils.escapeXml(entry.getValue().toString())%></value><%
                try {
                    if (entry.getKey().equals("areaResource")) {
                        String areaResourcePath = JCRSessionFactory.getInstance().getCurrentSystemSession("live", null,null).getNodeByIdentifier((String)entry.getValue()).getPath();
                %>
                <path><%=areaResourcePath%></path><%
                    }
                } catch (RepositoryException e) {
                 //
                }
            %>
            </property></c:forEach>
        </properties>
        <content><%
            String content = (String) ((CacheEntry) element.getObjectValue()).getObject();
        %><%=StringEscapeUtils.escapeXml(content) %></content><%
            }
        %>
    </entry></c:forEach>
</cache>
