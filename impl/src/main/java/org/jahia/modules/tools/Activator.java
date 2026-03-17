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
package org.jahia.modules.tools;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for this OSGi bundle that registers the JSP pre-compile servlet.
 * 
 * @author Sergiy Shyrkov
 */
public class Activator implements BundleActivator {
    
    private HttpServiceTracker httpTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        httpTracker = new HttpServiceTracker(context);
        httpTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        httpTracker.close();
    }

}
