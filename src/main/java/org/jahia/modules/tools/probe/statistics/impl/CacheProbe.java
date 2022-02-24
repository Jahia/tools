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

import java.io.PrintWriter;

import org.jahia.bin.Jahia;
import org.jahia.bin.errors.ErrorFileDumper;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.properties.impl.BaseSysInfoProbe;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Reports current DX cache status.
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + CacheProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + CacheProbe.NAME, Probe.CATEGORY + "=" + CacheProbe.CATEGORY,
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + CacheProbe.CATEGORY + ",name="
                + CacheProbe.KEY })
public class CacheProbe extends BaseSysInfoProbe {

    static final String CATEGORY = "statistics";

    static final String KEY = "cache";

    static final String NAME = "Cache status";

    @Override
    public void generateInfo(PrintWriter pw) {
        ErrorFileDumper.outputSystemInfo(pw, false, false, false, false, true, false, false, false);
    }

    @Override
    public String getCategory() {
        return CATEGORY;
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
