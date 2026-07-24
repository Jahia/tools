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
package org.jahia.modules.tools.taglibs;

import org.jahia.modules.tools.security.ToolsAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * Enforces, at the very top of every tool JSP, that the connected user is authorized to use the support tools. On
 * denial it returns {@code 403 Forbidden} and aborts the page, so none of the page's privileged operations run.
 *
 * @see ToolsAccessGuard
 */
public class RequireToolsAccessTag extends SimpleTagSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequireToolsAccessTag.class);

    @Override
    public void doTag() throws JspException, IOException {
        if (ToolsAccessGuard.isGranted()) {
            return;
        }
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Denying unauthorized access to tools resource {}", request.getRequestURI());
        }
        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        // Abort the rest of the page so no privileged work runs.
        throw new SkipPageException();
    }
}
