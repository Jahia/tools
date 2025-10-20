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
package org.jahia.modules.tools.benchmark;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jahia.services.SpringContextSingleton;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.DatabaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.*;

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

    private static final List<String> BENCHMARK_QUERIES = Arrays.asList("ping",
            "select count(*) from jahia_db_test");

    private static void appendStatValue(String label, StatValue v, StringBuilder out) {
        out.append("\t\t- ").append(label).append(": ").append(v.getMillis()).append(" ms (").append(v.getNanos())
                .append(" ns)\n");
    }

    @SuppressWarnings("unchecked")
    private static List<String> getBenchmarkQueries() {
        try {
            return (List<String>) SpringContextSingleton.getBean("jahiaToolsBenchmarkDatabaseQueries");
        } catch (Exception e) {
            // ignore, no such bean probably
            return BENCHMARK_QUERIES;
        }
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
