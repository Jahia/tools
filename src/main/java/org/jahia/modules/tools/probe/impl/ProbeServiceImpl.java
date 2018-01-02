/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools.probe.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.UnmodifiableMap;
import org.codehaus.plexus.util.StringUtils;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Service that retrieves all registered probes.
 * 
 * @author Sergiy Shyrkov
 */
@Component(name = "org.jahia.tools.probe.service", service = ProbeService.class, property = {
        Constants.SERVICE_PID + "=org.jahia.tools.probe.service",
        Constants.SERVICE_DESCRIPTION + "=Service that retrieves all registered probes",
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME })
public class ProbeServiceImpl implements ProbeService {

    private Map<String, List<Probe>> probes = new HashMap<>();

    @Reference(service = Probe.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "unbind")
    protected void bind(Probe probe) {
        List<Probe> probesByCategory = probes.get(probe.getCategory());
        if (probesByCategory == null) {
            probesByCategory = new LinkedList<>();
            probes.put(probe.getCategory(), probesByCategory);
        }
        probesByCategory.add(probe);
    }

    @Override
    public List<Probe> getAllProbes() {
        List<Probe> allProbes = new LinkedList<>();
        for (List<Probe> probe : probes.values()) {
            allProbes.addAll(probe);
        }

        return allProbes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, List<Probe>> getProbesByCategory() {
        return UnmodifiableMap.decorate(probes);
    }

    protected void unbind(Probe probe) {
        List<Probe> probesByCategory = probes.get(probe.getCategory());
        if (probesByCategory != null) {
            for (Iterator< Probe>iterator = probesByCategory.iterator(); iterator.hasNext();) {
                Probe p = iterator.next();
                if (StringUtils.equals(p.getKey(), probe.getKey())) {
                    iterator.remove();
                }
            }
        }
    }
}
