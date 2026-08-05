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

import org.jahia.osgi.BundleResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the Groovy console only ever loads a script that an active module bundle registers.
 * <p>
 * The console executes the content it loads, so accepting an arbitrary {@code scriptURI} would turn the parameter into
 * an SSRF, an arbitrary file read and a remote code execution vector at once. The registry of packaged scripts is the
 * allowlist, and it is stubbed here since populating it needs a running OSGi container.
 */
public class GroovyConsoleHelperTest {

    private static final String REGISTERED_URI = "bundle://157.0:1/META-INF/groovyConsole/registered.groovy";

    private MockedStatic<GroovyConsoleHelper> helper;
    private BundleResource registeredScript;

    @Before
    public void setUp() throws Exception {
        registeredScript = mock(BundleResource.class);
        when(registeredScript.getURI()).thenReturn(URI.create(REGISTERED_URI));
        helper = Mockito.mockStatic(GroovyConsoleHelper.class, Mockito.CALLS_REAL_METHODS);
        helper.when(GroovyConsoleHelper::getGroovyConsoleScripts)
                .thenReturn(Arrays.asList(registeredScript));
    }

    @After
    public void tearDown() {
        helper.close();
    }

    @Test
    public void resolvesAScriptPackagedInAnActiveModuleBundle() {
        assertSame(registeredScript, GroovyConsoleHelper.resolveScript(REGISTERED_URI));
    }

    @Test
    public void rejectsAnyURITheRegistryDoesNotOffer() throws Exception {
        final String[] forgedURIs = {
                "file:///etc/passwd",
                "file:///tmp/evil.groovy",
                "http://attacker.example:9999/evil.groovy",
                "https://attacker.example/evil.groovy",
                "ftp://attacker.example/evil.groovy",
                "classpath:org/jahia/evil.groovy",
                "bundle://157.0:1/META-INF/groovyConsole/unregistered.groovy",
                REGISTERED_URI + "/../../../evil.groovy"
        };
        for (final String forgedURI : forgedURIs) {
            assertNull("must not resolve " + forgedURI, GroovyConsoleHelper.resolveScript(forgedURI));
        }
        // A rejected URI is never opened: no request is issued and no file is read while deciding.
        verify(registeredScript, never()).getInputStream();
        verify(registeredScript, never()).getURL();
    }

    @Test
    public void treatsBlankAndCustomAsTheTextareaScript() {
        for (final String customURI : new String[] { null, "", "  ", GroovyConsoleHelper.CUSTOM_SCRIPT }) {
            assertTrue("must be the custom script: " + customURI, GroovyConsoleHelper.isCustomScript(customURI));
            assertNull(GroovyConsoleHelper.resolveScript(customURI));
        }
        assertFalse(GroovyConsoleHelper.isCustomScript(REGISTERED_URI));
    }

    @Test
    public void readsNoConfigurationForAURITheRegistryDoesNotOffer() {
        // The sibling .properties file is a second read derived from the same parameter, so it needs the same gate.
        assertEquals("", GroovyConsoleHelper.getScriptCustomFormElements("file:///etc/passwd",
                mock(HttpServletRequest.class)));
        assertNull(GroovyConsoleHelper.getScriptParamNames(null));
    }
}
