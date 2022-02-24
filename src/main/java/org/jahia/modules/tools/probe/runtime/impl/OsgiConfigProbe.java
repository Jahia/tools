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
package org.jahia.modules.tools.probe.runtime.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.config.core.ConfigRepository;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.utils.StringOutputStream;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Lists configurations in OSGI
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + OsgiConfigProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + OsgiConfigProbe.NAME, Probe.CATEGORY + "=" + OsgiConfigProbe.CATEGORY,
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + OsgiConfigProbe.CATEGORY + ",name="
                + OsgiConfigProbe.KEY })
public class OsgiConfigProbe implements ProbeMBean {

    static final String CATEGORY = "runtime";

    static final String KEY = "osgi-config";

    static final String NAME = "OSGi configurations";

    private ConfigRepository configRepository;

    @Reference(service = ConfigRepository.class)
    protected void bindConfigRepository(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        Configuration[] configs;
        try {
            configs = configRepository.getConfigAdmin().listConfigurations(null);
        } catch (IOException | InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        String data = null;
        StringOutputStream os = new StringOutputStream();
        try (PrintStream out = new PrintStream(os)) {
            if (configs != null) {
                Map<String, Configuration> sortedConfigs = new TreeMap<>();
                for (Configuration config : configs) {
                    sortedConfigs.put(config.getPid(), config);
                }
                for (Configuration config : sortedConfigs.values()) {
                    writeConfigToStream(config, out);
                }
            }

            out.flush();

            data = os.toString();
        }
        return data;
    }
    
    private void writeConfigToStream(Configuration config, PrintStream out) {
        out.println("----------------------------------------------------------------");
        out.println("Pid:            " + config.getPid());
        if (config.getFactoryPid() != null) {
            out.println("FactoryPid:     " + config.getFactoryPid());
        }
        out.println("BundleLocation: " + config.getBundleLocation());
        if (config.getProperties() != null) {
            out.println("Properties:");
            Dictionary<String, Object> props = config.getProperties();
            Map<String, Object> sortedProps = new TreeMap<>();
            for (Enumeration< String>e = props.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                sortedProps.put(key, props.get(key));
            }
            for (Map.Entry<String, Object> entry : sortedProps.entrySet()) {
                out.println("   " + entry.getKey() + " = " + entry.getValue());
            }
        }
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
