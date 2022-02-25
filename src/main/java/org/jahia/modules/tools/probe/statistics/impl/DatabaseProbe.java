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