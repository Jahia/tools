package org.jahia.modules.tools.csrf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ToolsAccessTokenFilter extends AbstractServletFilter {
    private static final String CSRF_TOKENS_ATTR = "toolAccessTokens";
    private static final String CSRF_TOKEN_ATTR = "toolAccessToken";
    private static final int MAX_TOKENS = 5000;
    private static final int MAX_DURATION = 20;

    private static Pattern TOOLS_REGEXP = Pattern.compile("^(/[^/]+|)/tools/.*");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (request.getPathInfo() != null && TOOLS_REGEXP.matcher(request.getPathInfo()).matches()) {
            if (servletRequest.getParameterMap().size() > 0) {
                validateToken(request);
            } else {
                generateAndStoreToken(request);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @SuppressWarnings("unchecked")
    private void validateToken(HttpServletRequest httpReq) throws ServletException {

        String token = httpReq.getParameter(CSRF_TOKEN_ATTR);

        if (token == null || getCache(httpReq).getIfPresent(token) == null) {
            throw new ServletException("Missing token: " + httpReq.getRequestURL() + (StringUtils.isNotEmpty(httpReq.getQueryString()) ? ("?" + httpReq.getQueryString()) : ""));
        }

        // keep same token
        httpReq.setAttribute(CSRF_TOKEN_ATTR, token);
    }


    private void generateAndStoreToken(HttpServletRequest httpReq) {
        // generate and store token
        String token = UUID.randomUUID().toString();
        getCache(httpReq).put(token, Boolean.TRUE);

        // send token in current request
        httpReq.setAttribute(CSRF_TOKEN_ATTR, token);
    }

    @SuppressWarnings("unchecked")
    private Cache<String, Boolean> getCache(HttpServletRequest httpReq) {
        Cache<String, Boolean> tokensCache = (Cache<String, Boolean>)
                httpReq.getSession().getAttribute(CSRF_TOKENS_ATTR);

        if (tokensCache == null){
            tokensCache = CacheBuilder.newBuilder()
                    .maximumSize(MAX_TOKENS)
                    .expireAfterWrite(MAX_DURATION, TimeUnit.MINUTES)
                    .build();

            httpReq.getSession().setAttribute(CSRF_TOKENS_ATTR, tokensCache);
        }

        return tokensCache;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
