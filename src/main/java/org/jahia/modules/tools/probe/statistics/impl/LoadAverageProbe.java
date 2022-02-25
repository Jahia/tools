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
package org.jahia.modules.tools.probe.statistics.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.utils.JCRSessionLoadAverage;
import org.jahia.utils.LoadAverage;
import org.jahia.utils.RequestLoadAverage;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Reports current load averages.
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + LoadAverageProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + LoadAverageProbe.NAME,
        Probe.CATEGORY + "=" + LoadAverageProbe.CATEGORY, Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + LoadAverageProbe.CATEGORY + ",name="
                + LoadAverageProbe.KEY })
public class LoadAverageProbe implements ProbeMBean {

    static final String CATEGORY = "statistics";

    static final String KEY = "loads";

    static final String NAME = "Load average";

    private static void appendInfo(String name, LoadAverage loadAverage, StringBuilder out) {
        if (out.length() > 0) {
            out.append("\n");
        }
        out.append(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), ' '));
        out.append(": ");
        if (loadAverage != null) {
            out.append("Over one minute=" + loadAverage.getOneMinuteLoad() + " Over five minutes="
                    + loadAverage.getFiveMinuteLoad() + " Over fifteen minutes=" + loadAverage.getFifteenMinuteLoad());
        } else {
            out.append("not available");
        }
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        StringBuilder out = new StringBuilder(512);
        appendInfo(RequestLoadAverage.class.getSimpleName(), RequestLoadAverage.getInstance(), out);
        appendInfo(JCRSessionLoadAverage.class.getSimpleName(), JCRSessionLoadAverage.getInstance(), out);

        return out.toString();
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
