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

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.jahia.bin.Jahia;
import org.jahia.modules.tools.benchmark.DatabaseBenchmark;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.utils.DatabaseUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports database information and connection speed stats, collected by the execution of {@link DatabaseBenchmark} utility.
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + DatabaseProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + DatabaseProbe.NAME, Probe.CATEGORY + "=" + DatabaseProbe.CATEGORY,
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + DatabaseProbe.CATEGORY + ",name="
                + DatabaseProbe.KEY })
public class DatabaseProbe implements ProbeMBean {

    static final String CATEGORY = "statistics";

    static final String KEY = "database";

    private static final Logger logger = LoggerFactory.getLogger(DatabaseProbe.class);

    static final String NAME = "Database info";

    private void appendDbConnectionSpeedStats(StringBuilder out) {
        out.append(DatabaseBenchmark.statsToString(DatabaseBenchmark.perform()));
    }

    private void appendDbInfo(StringBuilder out) {
        Connection conn = null;

        try {
            conn = DatabaseUtils.getDatasource().getConnection();

            DatabaseMetaData meta = conn.getMetaData();
            out.append("Database: ").append(meta.getDatabaseProductName()).append(" ")
                    .append(meta.getDatabaseProductVersion()).append("\n");
            out.append("JDBC driver: ").append(meta.getDriverName()).append(" ").append(meta.getDriverVersion())
                    .append("\n");
            out.append("URL: ").append(meta.getURL()).append("\n");
        } catch (Exception e) {
            logger.error("Unable to get database information. Cause: " + e.getMessage(), e);
        } finally {
            DatabaseUtils.closeQuietly(conn);
        }
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        StringBuilder out = new StringBuilder(1024);

        appendDbInfo(out);
        out.append("\n");
        appendDbConnectionSpeedStats(out);

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