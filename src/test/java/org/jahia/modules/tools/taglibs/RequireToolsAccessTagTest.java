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
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the denial contract of {@link RequireToolsAccessTag}: the tag is the guard's only enforcement point once a
 * request reaches a JSP, so it must send {@code 403} and abort the page for an unauthorized user, and let an authorized
 * user through untouched. The authorization decision itself ({@link ToolsAccessGuard#isGranted()}) is stubbed here; its
 * own logic is out of scope for this unit.
 */
public class RequireToolsAccessTagTest {

    private RequireToolsAccessTag tag;
    private PageContext pageContext;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() {
        tag = new RequireToolsAccessTag();
        pageContext = mock(PageContext.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(pageContext.getRequest()).thenReturn(request);
        when(pageContext.getResponse()).thenReturn(response);
        tag.setJspContext(pageContext);
    }

    @Test
    public void deniesUnauthorizedWith403AndAbortsPage() throws Exception {
        try (MockedStatic<ToolsAccessGuard> guard = Mockito.mockStatic(ToolsAccessGuard.class)) {
            guard.when(ToolsAccessGuard::isGranted).thenReturn(false);
            when(response.isCommitted()).thenReturn(false);

            assertThrows(SkipPageException.class, () -> tag.doTag());

            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Test
    public void allowsAuthorizedUserWithoutTouchingTheResponse() throws Exception {
        try (MockedStatic<ToolsAccessGuard> guard = Mockito.mockStatic(ToolsAccessGuard.class)) {
            guard.when(ToolsAccessGuard::isGranted).thenReturn(true);

            tag.doTag(); // must return normally, letting the page render

            verify(response, never()).sendError(Mockito.anyInt());
        }
    }

    @Test
    public void abortsWithoutWritingWhenResponseAlreadyCommitted() throws Exception {
        try (MockedStatic<ToolsAccessGuard> guard = Mockito.mockStatic(ToolsAccessGuard.class)) {
            guard.when(ToolsAccessGuard::isGranted).thenReturn(false);
            when(response.isCommitted()).thenReturn(true);

            // The page must still be aborted, but we cannot send an error once the response is committed.
            assertThrows(SkipPageException.class, () -> tag.doTag());

            verify(response, never()).sendError(Mockito.anyInt());
        }
    }
}
