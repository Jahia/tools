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
