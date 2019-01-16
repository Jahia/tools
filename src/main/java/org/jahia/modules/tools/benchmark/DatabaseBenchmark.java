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
package org.jahia.modules.tools.benchmark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jahia.services.SpringContextSingleton;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.DatabaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for benchmarking database connection latency. When a benchmark is performed by the {@link #perform()} method, it reads the list
 * of test queries from the Spring bean configuration and executes tests for each of them for a configured number of executions (100 by
 * default). The execution times are collected and a standard metrics are calculated, like min/max/mean, percentiles etc. By default, there
 * are two test queries configured:
 * <ul>
 * <li><code>ping</code> - a special "query" that executes {@link Connection#isValid(int)} method to do a "ping"</li>
 * <li><code>select count(*) from jahia_db_test</code> - is a fast count query on an empty DB test table</li>
 * </ul>
 * 
 * @author Sergiy Shyrkov
 */
public final class DatabaseBenchmark {

    /**
     * Holds the query execution statistics.
     */
    public static class StatValue {
        private double value;

        StatValue(double value) {
            this.value = value;
        }

        public String getMillis() {
            return MILLIS_FORMATTER.format(value / 1000000d);
        }

        public String getNanos() {
            return NANOS_FORMATTER.format(value);
        }

        public double getValue() {
            return value;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBenchmark.class);

    private static DecimalFormat MILLIS_FORMATTER = new DecimalFormat("#.###");

    private static DecimalFormat NANOS_FORMATTER = new DecimalFormat("#.##");

    private static void appendStatValue(String label, StatValue v, StringBuilder out) {
        out.append("\t\t- ").append(label).append(": ").append(v.getMillis()).append(" ms (").append(v.getNanos())
                .append(" ns)\n");
    }

    @SuppressWarnings("unchecked")
    private static List<String> getBenchmarkQueries() {
        return (List<String>) SpringContextSingleton.getBean("jahiaToolsBenchmarkDatabaseQueries");
    }

    private static int getQueryExecutionCount() {
        return Integer.valueOf(SettingsBean.getInstance().getPropertiesFile()
                .getProperty("jahiaTools.benchmarkDatabase.queryExecutionCount", "100"));
    }

    /**
     * Performs the database connection tests and returns the results with the timings per configured test query.
     * 
     * @return the results with the timings per configured test query
     */
    public static Map<String, Map<String, Object>> perform() {
        List<String> queries = getBenchmarkQueries();
        Map<String, Map<String, Object>> results = new LinkedHashMap<>(queries.size());
        int count = getQueryExecutionCount();
        Connection conn = null;
        try {
            conn = DatabaseUtils.getDatasource().getConnection();
            for (String query : queries) {
                DescriptiveStatistics stats = new DescriptiveStatistics();

                PreparedStatement stmt = null;
                try {
                    boolean isPingQuery = "ping".equalsIgnoreCase(query);
                    stmt = !isPingQuery ? conn.prepareStatement(query) : null;

                    for (int i = 0; i < count; i++) {
                        long startTime = System.nanoTime();

                        if (isPingQuery) {
                            // in case of a special "ping" query we execute the fast isValid() call on the connection
                            conn.isValid(20);
                        } else {
                            stmt.executeQuery();
                        }

                        stats.addValue(System.nanoTime() - startTime);
                    }
                } catch (Exception e) {
                    logger.error("Error executing database query " + query + ". Cause: " + e.getMessage(), e);
                } finally {
                    DatabaseUtils.closeQuietly(stmt);
                }

                results.put(query, wrapResult(stats));
            }
        } catch (Exception e) {
            logger.error("Error executing database connection speed benchmark. Cause: " + e.getMessage(), e);
        } finally {
            DatabaseUtils.closeQuietly(conn);
        }

        return results;
    }

    /**
     * Returns a formatted string representation of the database connection stats.
     * 
     * @param stats the collected statistics
     * @return the formatted string representation of the database connection stats
     */
    public static String statsToString(Map<String, Map<String, Object>> stats) {
        StringBuilder out = new StringBuilder(512);
        out.append("Database connection speed:").append("\n");
        try {
            for (Map.Entry<String, Map<String, Object>> stat : stats.entrySet()) {
                out.append("\t* Query: ").append(stat.getKey()).append("\n");
                Map<String, Object> value = stat.getValue();

                @SuppressWarnings("unchecked")
                Map<Double, StatValue> percentiles = (Map<Double, StatValue>) value.get("percentiles");

                appendStatValue("50% line", percentiles.get(50d), out);
                appendStatValue("90% line", percentiles.get(90d), out);
                appendStatValue("99% line", percentiles.get(99d), out);

                appendStatValue("min", (StatValue) value.get("min"), out);
                appendStatValue("average", (StatValue) value.get("mean"), out);
                appendStatValue("max", (StatValue) value.get("max"), out);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> wrapResult(DescriptiveStatistics stats) {
        return LazyMap.decorate(new HashMap<>(), new Transformer() {
            @Override
            public Object transform(Object input) {
                String key = String.valueOf(input);
                switch (key) {
                    case "count":
                        return stats.getN();

                    case "max":
                        return new StatValue(stats.getMax());

                    case "mean":
                        return new StatValue(stats.getMean());

                    case "min":
                        return new StatValue(stats.getMin());

                    case "percentiles":
                        return LazyMap.decorate(new HashMap<>(), new Transformer() {
                            @Override
                            public Object transform(Object input) {
                                double p = (input instanceof Number) ? ((Number) input).doubleValue()
                                        : Double.parseDouble(String.valueOf(input));
                                return new StatValue(stats.getPercentile(p));
                            }
                        });

                    case "standardDeviation":
                        return new StatValue(stats.getStandardDeviation());

                    case "sum":
                        return new StatValue(stats.getSum());

                    default:
                        throw new IllegalArgumentException("Key " + input + " is not supported by this data object");
                }
            }
        });
    }

    private DatabaseBenchmark() {
        super();
    }
}
