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
package org.jahia.modules.tools;

import org.jahia.modules.tools.csrf.ToolsAccessTokenFilter;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servlet to track assets usage across all Jahia bundles.
 * Scans all text-based files in all bundles for references to assets (e.g., /assets/, /modules/assets/)
 * and displays statistics grouped by module.
 */
public class AssetsUsageServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AssetsUsageServlet.class);
    private static final long serialVersionUID = 1L;
    
    private static final String SEARCH_PATTERN_PARAM = "searchPattern";
    private static final String TARGET_BUNDLE_PARAM = "targetBundle";
    private static final String DEFAULT_SEARCH_PATTERN = "/assets/";
    private static final String DEFAULT_TARGET_BUNDLE = "assets";
    
    private static final Pattern ADD_RESOURCES_PATTERN = Pattern.compile("<template:addResources[^>]*>.*?</template:addResources>|<template:addResources[^>]*/>");
    private static final Pattern RESOURCES_ATTR_PATTERN = Pattern.compile("resources\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final Pattern TYPE_ATTR_PATTERN = Pattern.compile("type\\s*=\\s*[\"']([^\"']*)[\"']");
    
    private static class ResourceUsage {
        String resourcePath;
        String type;
        List<String> usedInFiles = new ArrayList<>();
        Set<String> usedInModules = new TreeSet<>();
        boolean existsInTarget = false;
        
        ResourceUsage(String resourcePath, String type) {
            this.resourcePath = resourcePath;
            this.type = type;
        }
        
        void addUsage(String filePath, String moduleName) {
            if (!usedInFiles.contains(filePath)) {
                usedInFiles.add(filePath);
            }
            if (moduleName != null) {
                usedInModules.add(moduleName);
            }
        }
        
        int getUsageCount() {
            return usedInFiles.size();
        }
        
        int getModuleCount() {
            return usedInModules.size();
        }
        
        String getModuleNames() {
            return String.join(", ", usedInModules);
        }
        
        void setExistsInTarget(boolean exists) {
            this.existsInTarget = exists;
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doWork(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doWork(request, response);
    }
    
    private void doWork(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession(true);
        
        String searchPattern = request.getParameter(SEARCH_PATTERN_PARAM);
        if (searchPattern == null || searchPattern.trim().isEmpty()) {
            searchPattern = DEFAULT_SEARCH_PATTERN;
        }
        
        String targetBundleName = request.getParameter(TARGET_BUNDLE_PARAM);
        if (targetBundleName == null || targetBundleName.trim().isEmpty()) {
            targetBundleName = DEFAULT_TARGET_BUNDLE;
        }
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        generateHtmlOutput(out, request, response, searchPattern, targetBundleName);
    }
    
    private void generateHtmlOutput(PrintWriter out, HttpServletRequest request, 
                                    HttpServletResponse response, String searchPattern, 
                                    String targetBundleName) {
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        out.print("<link rel=\"stylesheet\" href=\"");
        out.print(response.encodeURL(request.getContextPath() + "/tools/css/google_material.css"));
        out.println("\" type=\"text/css\">");
        out.print("<link rel=\"stylesheet\" href=\"");
        out.print(response.encodeURL(request.getContextPath() + "/tools/css/reset.css"));
        out.println("\" type=\"text/css\">");
        out.print("<link rel=\"stylesheet\" href=\"");
        out.print(response.encodeURL(request.getContextPath() + "/tools/css/tools.css"));
        out.println("\" type=\"text/css\">");
        out.println("<title>Assets Usage Tracker</title>");
        out.println("<style>");
        out.println(".module-section { margin: 20px 0; padding: 15px; background: #f9f9f9; border-left: 4px solid #0066cc; }");
        out.println(".module-name { font-size: 1.2em; font-weight: bold; color: #333; margin-bottom: 10px; }");
        out.println(".resource-table { width: 100%; border-collapse: collapse; margin: 10px 0; }");
        out.println(".resource-table th { background: #0066cc; color: white; padding: 10px; text-align: left; }");
        out.println(".resource-table td { padding: 8px; border-bottom: 1px solid #ddd; }");
        out.println(".resource-table tr:hover { background: #f5f5f5; }");
        out.println(".resource-path { font-family: monospace; }");
        out.println(".usage-count { font-weight: bold; color: #0066cc; }");
        out.println(".assets-badge { background: #28a745; color: white; padding: 2px 6px; border-radius: 3px; font-size: 0.8em; margin-left: 10px; }");
        out.println(".module-list { color: #666; font-size: 0.9em; font-style: italic; }");
        out.println(".config-form { background: #f0f0f0; padding: 15px; margin: 20px 0; border-radius: 5px; }");
        out.println(".config-form input { padding: 5px; margin: 5px; }");
        out.println(".config-form button { background: #0066cc; color: white; padding: 8px 15px; border: none; border-radius: 3px; cursor: pointer; }");
        out.println(".config-form button:hover { background: #0052a3; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<header class=\"page-header\">");
        out.println("    <ul class=\"page-header_bar\">");
        out.println("        <li><a href=\"" + request.getContextPath() + "/tools/index.jsp\"><span class=\"material-symbols-outlined\">home</span>Home</a></li>");
        out.println("        <li><a href='" + request.getContextPath() + "/cms/logout?redirect=" + request.getContextPath() + "/start'><span class=\"material-symbols-outlined\">logout</span>Logout</a></li>");
        out.println("    </ul>");
        out.println("    <hgroup>");
        out.println("        <h1>Assets Usage Tracker</h1>");
        out.println("    <ul class=\"page-header_toolbar\">");
        String jahiaEnv = SettingsBean.getInstance().getString("jahia.environment", "");
        if (!jahiaEnv.isEmpty()) {
            out.println("<li>jahia.environment: <strong>" + jahiaEnv + "</strong></li>");
        }
        out.println("    </ul>");
        out.println("    </hgroup>");
        out.println("</header>");
        
        // Configuration form
        out.println("<div class=\"config-form\">");
        out.println("<form method=\"GET\">");
        out.println("<input type=\"hidden\" name=\"" + ToolsAccessTokenFilter.CSRF_TOKEN_ATTR + "\" value=\"" + request.getAttribute(ToolsAccessTokenFilter.CSRF_TOKEN_ATTR) + "\"/>");
        out.println("<label>Search Pattern: <input type=\"text\" name=\"" + SEARCH_PATTERN_PARAM + "\" value=\"" + escapeHtml(searchPattern) + "\" placeholder=\"/assets/\"/></label>");
        out.println("<label>Target Bundle: <input type=\"text\" name=\"" + TARGET_BUNDLE_PARAM + "\" value=\"" + escapeHtml(targetBundleName) + "\" placeholder=\"assets\"/></label>");
        out.println("<button type=\"submit\">Scan</button>");
        out.println("</form>");
        out.println("<p><strong>Current Settings:</strong> Searching for \"" + escapeHtml(searchPattern) + "\" in target bundle \"" + escapeHtml(targetBundleName) + "\"</p>");
        out.println("</div>");
        
        displayResourceUsageStatistics(out, searchPattern, targetBundleName);
        
        out.println("<footer class=\"page-footer\">");
        out.println("    <ul class=\"page-footer_bar\">");
        out.println("        <li><a href=\"" + request.getContextPath() + "/tools/index.jsp\"><span class=\"material-symbols-outlined\">home</span>Home</a></li>");
        out.println("        <li><a href='" + request.getContextPath() + "/cms/logout?redirect=" + request.getContextPath() + "/start'><span class=\"material-symbols-outlined\">logout</span>Logout</a></li>");
        out.println("    </ul>");
        out.println("</footer>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private void displayResourceUsageStatistics(PrintWriter out, String searchPattern, String targetBundleName) {
        Map<String, Map<String, ResourceUsage>> moduleResources = new TreeMap<>();
        Map<String, ResourceUsage> globalResources = new TreeMap<>();
        
        // Create pattern for searching - escape special regex chars but make it flexible
        String patternStr = "[\"'`]([^\"'`]*" + Pattern.quote(searchPattern) + "[^\"'`]*)[\"'`]";
        Pattern assetsReferencePattern = Pattern.compile(patternStr);
        
        // Find the target bundle
        Bundle targetBundle = null;
        for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
            if (targetBundleName.equals(bundle.getSymbolicName()) && bundle.getState() == Bundle.ACTIVE) {
                targetBundle = bundle;
                break;
            }
        }
        
        out.println("<h2>Scanning bundles for pattern: \"" + escapeHtml(searchPattern) + "\"</h2>");
        
        // Scan all bundles for the search pattern
        for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
            if (BundleUtils.isJahiaModuleBundle(bundle) && bundle.getState() == Bundle.ACTIVE) {
                scanBundleResourcesForPattern(bundle, bundle.getSymbolicName(), 
                                             moduleResources, globalResources, 
                                             assetsReferencePattern, searchPattern);
                // Also scan for template:addResources tags
                scanBundleForTemplateResources(bundle, bundle.getSymbolicName(), 
                                              moduleResources, globalResources);
            }
        }
        
        // Remove modules without resources
        moduleResources.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        if (globalResources.isEmpty()) {
            out.println("<p>No resources found matching pattern \"" + escapeHtml(searchPattern) + "\".</p>");
            return;
        }
        
        // Check which resources exist in target bundle - keep only those
        if (targetBundle != null) {
            Map<String, ResourceUsage> filteredGlobal = new TreeMap<>();
            Map<String, Map<String, ResourceUsage>> filteredModule = new TreeMap<>();
            
            for (Map.Entry<String, ResourceUsage> entry : globalResources.entrySet()) {
                ResourceUsage resource = entry.getValue();
                String[] pathsToCheck = {
                    "/css/" + resource.resourcePath,
                    "/javascript/" + resource.resourcePath,
                    "/js/" + resource.resourcePath,
                    "/" + resource.resourcePath,
                    "/img/" + resource.resourcePath,
                    "/images/" + resource.resourcePath,
                    "/icons/" + resource.resourcePath
                };
                
                for (String path : pathsToCheck) {
                    if (targetBundle.getEntry(path) != null) {
                        resource.setExistsInTarget(true);
                        filteredGlobal.put(entry.getKey(), resource);
                        
                        // Keep module entries for this resource
                        for (Map.Entry<String, Map<String, ResourceUsage>> moduleEntry : moduleResources.entrySet()) {
                            if (moduleEntry.getValue().containsKey(entry.getKey())) {
                                filteredModule.putIfAbsent(moduleEntry.getKey(), new TreeMap<>());
                                filteredModule.get(moduleEntry.getKey()).put(entry.getKey(), moduleEntry.getValue().get(entry.getKey()));
                            }
                        }
                        break;
                    }
                }
            }
            
            globalResources = filteredGlobal;
            moduleResources = filteredModule;
        }
        
        if (globalResources.isEmpty()) {
            out.println("<p>No resources found in target bundle \"" + escapeHtml(targetBundleName) + "\".</p>");
            return;
        }
        
        int totalResources = globalResources.size();
        int totalUsages = globalResources.values().stream().mapToInt(ResourceUsage::getUsageCount).sum();
        
        out.println("<p>Found <strong>" + totalResources + "</strong> unique resources in \"" + escapeHtml(targetBundleName) + "\" bundle with <strong>" + totalUsages + "</strong> total usages across <strong>" + moduleResources.size() + "</strong> modules</p>");
        
        // Global summary
        out.println("<h2>Global Resources Summary (Bundle: " + escapeHtml(targetBundleName) + ")</h2>");
        out.println("<div class=\"module-section\">");
        out.println("<table class=\"resource-table\">");
        out.println("<thead>");
        out.println("<tr><th>Type</th><th>Resource Path</th><th>Total Usage Count</th><th>Modules Using</th></tr>");
        out.println("</thead>");
        out.println("<tbody>");
        
        List<ResourceUsage> sortedGlobal = new ArrayList<>(globalResources.values());
        sortedGlobal.sort((a, b) -> Integer.compare(b.getUsageCount(), a.getUsageCount()));
        
        for (ResourceUsage resource : sortedGlobal) {
            out.println("<tr>");
            out.println("<td>" + resource.type + "</td>");
            out.println("<td class=\"resource-path\">" + escapeHtml(resource.resourcePath) + "<span class=\"assets-badge\">✓ Found</span></td>");
            out.println("<td class=\"usage-count\">" + resource.getUsageCount() + "</td>");
            out.println("<td class=\"module-list\">" + resource.getModuleCount() + " modules: " + escapeHtml(resource.getModuleNames()) + "</td>");
            out.println("</tr>");
        }
        
        out.println("</tbody></table></div>");
        
        // Per-module breakdown
        out.println("<h2>Per-Module Breakdown</h2>");
        
        for (Map.Entry<String, Map<String, ResourceUsage>> moduleEntry : moduleResources.entrySet()) {
            String moduleName = moduleEntry.getKey();
            Map<String, ResourceUsage> resources = moduleEntry.getValue();
            
            out.println("<div class=\"module-section\">");
            out.println("<div class=\"module-name\">" + moduleName + "</div>");
            out.println("<table class=\"resource-table\">");
            out.println("<thead>");
            out.println("<tr><th>Type</th><th>Resource Path</th><th>Usage Count</th><th>Used in Files</th></tr>");
            out.println("</thead>");
            out.println("<tbody>");
            
            List<ResourceUsage> sortedResources = new ArrayList<>(resources.values());
            sortedResources.sort((a, b) -> Integer.compare(b.getUsageCount(), a.getUsageCount()));
            
            for (ResourceUsage resource : sortedResources) {
                out.println("<tr>");
                out.println("<td>" + resource.type + "</td>");
                out.println("<td class=\"resource-path\">" + escapeHtml(resource.resourcePath) + "</td>");
                out.println("<td class=\"usage-count\">" + resource.getUsageCount() + "</td>");
                out.println("<td>");
                for (int i = 0; i < resource.usedInFiles.size(); i++) {
                    if (i > 0) out.print(", ");
                    out.print("<span class=\"module-list\">" + escapeHtml(resource.usedInFiles.get(i)) + "</span>");
                }
                out.println("</td>");
                out.println("</tr>");
            }
            
            out.println("</tbody></table></div>");
        }
    }
    
    private void scanBundleResourcesForPattern(Bundle bundle, String moduleName,
                                              Map<String, Map<String, ResourceUsage>> moduleResources,
                                              Map<String, ResourceUsage> globalResources,
                                              Pattern searchPattern, String patternStr) {
        Enumeration<?> entries = bundle.findEntries("/", "*", true);
        if (entries == null) {
            return;
        }
        
        while (entries.hasMoreElements()) {
            URL url = (URL) entries.nextElement();
            String path = url.getPath();
            
            // Skip directories and binary files
            if (path.endsWith("/") || path.matches(".*\\.(class|jar|zip|png|jpg|jpeg|gif|ico|webp|woff|woff2|ttf|eot|pdf|mp4|mp3)$")) {
                continue;
            }
            
            // Read file and look for pattern
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    Matcher matcher = searchPattern.matcher(line);
                    while (matcher.find()) {
                        String assetUrl = matcher.group(1);
                        
                        // Extract the resource path after the search pattern
                        String resourcePath = assetUrl;
                        if (resourcePath.contains("/modules" + patternStr)) {
                            resourcePath = resourcePath.substring(resourcePath.indexOf("/modules" + patternStr) + 9 + patternStr.length());
                        } else if (resourcePath.contains(patternStr)) {
                            resourcePath = resourcePath.substring(resourcePath.indexOf(patternStr) + patternStr.length());
                        } else {
                            continue;
                        }
                        
                        // Clean up query params and fragments
                        int queryIdx = resourcePath.indexOf('?');
                        if (queryIdx != -1) resourcePath = resourcePath.substring(0, queryIdx);
                        int hashIdx = resourcePath.indexOf('#');
                        if (hashIdx != -1) resourcePath = resourcePath.substring(0, hashIdx);
                        
                        if (resourcePath.isEmpty()) continue;
                        
                        // Determine type from extension
                        String type = "asset";
                        if (resourcePath.endsWith(".css")) type = "css";
                        else if (resourcePath.endsWith(".js")) type = "javascript";
                        else if (resourcePath.matches(".*\\.(png|jpg|jpeg|gif|svg|ico|webp)$")) type = "image";
                        
                        String key = type + ":" + resourcePath;
                        String filePath = path + ":" + lineNum;
                        
                        // Module-specific tracking
                        moduleResources.putIfAbsent(moduleName, new TreeMap<>());
                        moduleResources.get(moduleName).putIfAbsent(key, new ResourceUsage(resourcePath, type));
                        moduleResources.get(moduleName).get(key).addUsage(filePath, moduleName);
                        
                        // Global tracking
                        globalResources.putIfAbsent(key, new ResourceUsage(resourcePath, type));
                        globalResources.get(key).addUsage(filePath, moduleName);
                    }
                }
            } catch (Exception e) {
                // Skip files that can't be read
                logger.debug("Could not read file for pattern scan: " + path, e);
            }
        }
    }
    
    private void scanBundleForTemplateResources(Bundle bundle, String moduleName,
                                               Map<String, Map<String, ResourceUsage>> moduleResources,
                                               Map<String, ResourceUsage> globalResources) {
        Enumeration<?> entries = bundle.findEntries("/", "*.jsp", true);
        if (entries == null) {
            return;
        }
        
        while (entries.hasMoreElements()) {
            URL url = (URL) entries.nextElement();
            String path = url.getPath();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    content.append(line).append("\n");
                }
                
                String contentStr = content.toString();
                Matcher tagMatcher = ADD_RESOURCES_PATTERN.matcher(contentStr);
                
                while (tagMatcher.find()) {
                    String tag = tagMatcher.group();
                    
                    // Extract type attribute
                    Matcher typeMatcher = TYPE_ATTR_PATTERN.matcher(tag);
                    String type = typeMatcher.find() ? typeMatcher.group(1) : "unknown";
                    
                    // Extract resources attribute
                    Matcher resourcesMatcher = RESOURCES_ATTR_PATTERN.matcher(tag);
                    if (resourcesMatcher.find()) {
                        String resourcesAttr = resourcesMatcher.group(1);
                        String[] resourceList = resourcesAttr.split("[,;]");
                        
                        for (String resource : resourceList) {
                            resource = resource.trim();
                            if (!resource.isEmpty()) {
                                String key = type + ":" + resource;
                                String filePath = path + ":" + lineNum;
                                
                                // Module-specific tracking
                                moduleResources.putIfAbsent(moduleName, new TreeMap<>());
                                moduleResources.get(moduleName).putIfAbsent(key, new ResourceUsage(resource, type));
                                moduleResources.get(moduleName).get(key).addUsage(filePath, moduleName);
                                
                                // Global tracking
                                globalResources.putIfAbsent(key, new ResourceUsage(resource, type));
                                globalResources.get(key).addUsage(filePath, moduleName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not read JSP for template scan: " + path, e);
            }
        }
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
