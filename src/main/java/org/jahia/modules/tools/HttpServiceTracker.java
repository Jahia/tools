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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service tracker for the HttpService to register the {@link JspPrecompileServlet}.
 * 
 * @author Sergiy Shyrkov
 */
public class HttpServiceTracker extends ServiceTracker<HttpService, HttpService> {

    private static final Logger logger = LoggerFactory.getLogger(HttpServiceTracker.class);

    public HttpServiceTracker(BundleContext context) {
        super(context, HttpService.class.getName(), null);
    }

    @Override
    public HttpService addingService(ServiceReference<HttpService> reference) {
        HttpService httpService = super.addingService(reference);
        if (httpService == null)
            return null;

        try {
            httpService.registerServlet("/tools/precompileServlet", new JspPrecompileServlet(), null, null);
        } catch (Exception e) {
            logger.error("Cannot register Servlet",e);
        }

        return httpService;
    }

    @Override
    public void removedService(ServiceReference<HttpService> reference, HttpService service) {
        service.unregister("/tools/precompileServlet");

        super.removedService(reference, service);
    }

}
