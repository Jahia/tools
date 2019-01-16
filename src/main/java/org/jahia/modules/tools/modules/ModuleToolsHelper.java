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
