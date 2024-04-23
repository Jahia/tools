/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.stream.Collectors;

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
            if (!servletRequest.getParameterMap().isEmpty()) {
                try {
                    validateToken(request);
                } catch (MissingTokenException e) {
                    throw new ServletException(e.getMessage());
                }
            } else {
                String token = generateAndStoreToken(request);
                //Token generation is performed if the request is a POST on the specific path, in that case filter chain is not called
                //a better approach would be to use a dedicated endpoint for token generation
                if (request.getMethod().equals(TOKEN_METHOD) && request.getRequestURI().endsWith(TOKEN_URI)) {
                    HttpServletResponse response = (HttpServletResponse) servletResponse;
                    String body = "{\"token\":\"" + token + "\"}";
                    PrintWriter out = response.getWriter();
                    response.setContentType(TOKEN_CONTENT_TYPE);
                    response.setContentLength(body.length());
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print(body);
                    out.flush();
                    //return here to avoid calling filter chain and cause an exception because writing in an already flushed response writer
                    return;
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void validateToken(HttpServletRequest httpReq) throws MissingTokenException {
        if (SettingsBean.getInstance().isDevelopmentMode()) {
            return;
        }
        String token = httpReq.getParameter(CSRF_TOKEN_ATTR);

        if (token == null || getCache(httpReq).get(token) == null || getCache(httpReq).get(token) < (System.currentTimeMillis() - tokenExpiration * 60L * 1000L)) {
            throw new MissingTokenException("Missing token: " + httpReq.getRequestURL() + (StringUtils.isNotEmpty(httpReq.getQueryString()) ?
                    ("?" + httpReq.getQueryString()) : ""));
        }

        // keep same token
        httpReq.setAttribute(CSRF_TOKEN_ATTR, token);
    }


    private String generateAndStoreToken(HttpServletRequest httpReq) {
        // generate and store token
        String token = UUID.randomUUID().toString();
        Map<String, Long> tokens = getCache(httpReq);
        synchronized (this){
            tokens.put(token, System.currentTimeMillis());

            //Purge stale tokens
            tokens = tokens.entrySet().stream().filter(e -> e.getValue() > (System.currentTimeMillis() - tokenExpiration * 60L * 1000L))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
        }

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
