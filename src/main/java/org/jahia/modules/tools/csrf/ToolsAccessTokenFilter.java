/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.settings.SettingsBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ToolsAccessTokenFilter extends AbstractServletFilter {
    private static final String CSRF_TOKENS_ATTR = "toolAccessTokens";
    public static final String CSRF_TOKEN_ATTR = "toolAccessToken";
    private static final int MAX_TOKENS = 5000;
    private int tokenExpiration = 20;

    private static final Pattern TOOLS_REGEXP = Pattern.compile("^(/[^/]+|)/tools/.*");
    private static final String TOKEN_URI = "/token";
    private static final String TOKEN_METHOD = "POST";
    private static final String TOKEN_CONTENT_TYPE = "application/json";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (request.getPathInfo() != null && TOOLS_REGEXP.matcher(request.getPathInfo()).matches()) {
            if (servletRequest.getParameterMap().size() > 0) {
                validateToken(request);
            } else {
                String token = generateAndStoreToken(request);

                if (request.getMethod().equals(TOKEN_METHOD) && request.getRequestURI().endsWith(TOKEN_URI)) {
                    HttpServletResponse response = (HttpServletResponse) servletResponse;
                    PrintWriter out = response.getWriter();
                    response.setContentType(TOKEN_CONTENT_TYPE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    out.print("{\"token\":\"" + token + "\"}");
                    out.flush();
                }
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

        if (token == null || getCache(httpReq).get(token) == null || getCache(httpReq).get(token) < (System.currentTimeMillis() - tokenExpiration * 60L * 1000L)) {
            throw new ServletException("Missing token: " + httpReq.getRequestURL() + (StringUtils.isNotEmpty(httpReq.getQueryString()) ? ("?" + httpReq.getQueryString()) : ""));
        }

        // keep same token
        httpReq.setAttribute(CSRF_TOKEN_ATTR, token);
    }


    private String generateAndStoreToken(HttpServletRequest httpReq) {
        // generate and store token
        String token = UUID.randomUUID().toString();
        HashMap<String, Long> tokens = getCache(httpReq);
        tokens.put(token, System.currentTimeMillis());

        if (tokens.size() > MAX_TOKENS) {
            tokens.remove(tokens.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow(ArrayIndexOutOfBoundsException::new).getKey());
        }

        httpReq.getSession().setAttribute(CSRF_TOKENS_ATTR, tokens);

        // send token in current request
        httpReq.setAttribute(CSRF_TOKEN_ATTR, token);

        return token;
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Long> getCache(HttpServletRequest httpReq) {
        HashMap<String, Long> tokensCache = (HashMap<String, Long>) httpReq.getSession().getAttribute(CSRF_TOKENS_ATTR);

        if (tokensCache == null){
            tokensCache = new HashMap<>();
            httpReq.getSession().setAttribute(CSRF_TOKENS_ATTR, tokensCache);
        }

        return tokensCache;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing to init
    }

    @Override
    public void destroy() {
        // Nothing to destroy
    }

    public void setTokenExpiration(int tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }
}
