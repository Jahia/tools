/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.tools.csrf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.settings.SettingsBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ToolsAccessTokenFilter extends AbstractServletFilter {
    private static final String CSRF_TOKENS_ATTR = "toolAccessTokens";
    public static final String CSRF_TOKEN_ATTR = "toolAccessToken";
    private static final int MAX_TOKENS = 5000;
    private static final int MAX_DURATION = Integer.parseInt(SettingsBean.getInstance().getString("toolsTokenExpiration","20"));

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
        if (SettingsBean.getInstance().isDevelopmentMode()) {
            return;
        }
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
