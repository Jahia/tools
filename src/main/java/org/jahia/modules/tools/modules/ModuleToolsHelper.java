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
package org.jahia.modules.tools.modules;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;

@Component(immediate = true)
public class ModuleToolsHelper implements SynchronousBundleListener {
    private BundleContext bundleContext;

    private static ModuleToolsHelper instance;

    public static ModuleToolsHelper getInstance() {
        return instance;
    }

    private Map<String, List<Tool>> tools = new TreeMap<>();

    @Activate
    public void activate(BundleContext bundleContext) {
        bundleContext.addBundleListener(this);
        this.bundleContext = bundleContext;
        Bundle[] bundles = bundleContext.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            if (bundle.getState() == Bundle.ACTIVE) {
                parseBundle(bundle);
            }
        }
        instance = this;
    }

    @Deactivate
    public void deactivate(BundleContext bundleContext) {
        bundleContext.removeBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED) {
            parseBundle(event.getBundle());
        } else if (event.getType() == BundleEvent.STOPPED) {
            tools.remove(event.getBundle().getSymbolicName());
        }
    }

    private void parseBundle(Bundle bundle) {
        Enumeration<String> list = bundle.getEntryPaths("/tools");
        if (list != null) {
            ArrayList<Tool> tools = new ArrayList<>();
            this.tools.put(bundle.getSymbolicName(), tools);
            while (list.hasMoreElements()) {
                String filePath = list.nextElement();
                if (filePath.endsWith(".jsp")) {
                    tools.add(new Tool("/modules/" + bundle.getSymbolicName() + "/" + filePath, getName(bundle, filePath)));
                }
            }
        }
    }

    private String getName(Bundle bundle, String filePath) {
        String fileName = StringUtils.substringBefore(StringUtils.substringAfterLast(filePath, "/"), ".");
        return bundle.getHeaders().get("Bundle-Name") + " : " + fileName;

    }

    public List<Tool> getTools() {
        List<Tool> r = new ArrayList<Tool>();
        for (List<Tool> list : tools.values()) {
            r.addAll(list);
        }
        return r;
    }

    public class Tool {
        public String path;
        public String name;

        public Tool(String path, String name) {
            this.path = path;
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }
    }
}
