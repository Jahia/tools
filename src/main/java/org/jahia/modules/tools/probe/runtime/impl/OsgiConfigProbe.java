/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
        StringOutputStream os = new StringOutputStream();
        PrintStream out = new PrintStream(os);
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

        return os.toString();
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
