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
