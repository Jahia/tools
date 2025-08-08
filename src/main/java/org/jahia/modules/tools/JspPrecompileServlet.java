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

import org.jahia.bin.listeners.JahiaContextLoaderListener;
import org.jahia.modules.tools.csrf.ToolsAccessTokenFilter;
import org.jahia.osgi.BundleUtils;
import org.jahia.osgi.FrameworkService;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.NoOutputResponseWrapper;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;

/**
 * Helps to precompile JSPs of a WebApp. The Servlet performs 3 actions depending on the passed params: - if jsp_name param is passed, the
 * servlet tries to forward to the JSP with the passed name - if compile_type=all is passed, the servlet tries to forward to all found JSPs
 * and generates a report HTML output - if compile_type=modules is passed, the servlet tries to forward to all found module JSPs and
 * generates a report HTML output - if compile_type=site is passed, the servlet tries to forward to all found module JSPs of a site and
 * generates a report HTML output - if no special param is passed, the servlet generates a page with links for the above described purposes
 */
public class JspPrecompileServlet extends HttpServlet {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(JspPrecompileServlet.class);
    private static final long serialVersionUID = 7291760429380775493L;
    private static final String JSP_NAME_PARAM = "jsp_name";
    private static final String COMPILE_TYPE_PARAM = "compile_type";

    private static final String MAGIC_TOMCAT_PARAM = "jsp_precompile=true";

    public void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse)
            throws IOException {
        doWork(aRequest, aResponse);
    }

    public void doPost(HttpServletRequest aRequest,
                       HttpServletResponse aResponse) throws IOException {
        doWork(aRequest, aResponse);
    }

    /**
     * Performs depending on the passed request params the actions mentioned in the class description.
     */
    private void doWork(HttpServletRequest aRequest,
                        HttpServletResponse aResponse) throws IOException {
        aRequest.getSession(true);

        String jspName = aRequest.getParameter(JSP_NAME_PARAM);
        String compileType = aRequest.getParameter(COMPILE_TYPE_PARAM);
        if (jspName != null) {
            // precompile single JSP
            precompileJsps(Collections.singletonList(jspName), aRequest, aResponse);
        } else if ("all".equals(compileType)) {
            // precompile all JSPs and generate report
            precompileJsps(searchForAllJsps(), aRequest, aResponse);
        } else if ("modules".equals(compileType)) {
            // precompile all JSPs and generate report
            precompileJsps(searchForBundleJsps(), aRequest, aResponse);
        } else if ("module".equals(compileType)) {
            // precompile JSPs of a module
            precompileJsps(
                    searchForBundleJsps(FrameworkService.getBundleContext().getBundle(
                            Long.parseLong(aRequest.getParameter("id")))), aRequest, aResponse);
        } else if ("non-modules".equals(compileType)) {
            // precompile all JSPs not in modules and generate report
            precompileJsps(searchForJsps(), aRequest, aResponse);
        } else {
            // generate output with links for compile all and all JSPs
            aResponse.setContentType("text/html;charset=UTF-8");
            PrintWriter out = aResponse.getWriter();
            List<String> foundJsps = searchForAllJsps();
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
            out.print("<link rel=\"stylesheet\" href=\"");
            out.print(aResponse.encodeURL(aRequest.getContextPath() + "/tools/css/google_material.css"));
            out.println("\" type=\"text/css\">");
            out.print("<link rel=\"stylesheet\" href=\"");
            out.print(aResponse.encodeURL(aRequest.getContextPath() + "/tools/css/reset.css"));
            out.println("\" type=\"text/css\">");
            out.print("<link rel=\"stylesheet\" href=\"");
            out.print(aResponse.encodeURL(aRequest.getContextPath() + "/tools/css/tools.css"));
            out.println("\" type=\"text/css\">");
            out.println("<title>JSP compilation</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<header class=\"page-header\">");
            out.println("    <ul class=\"page-header_bar\">");
            out.println("        <li><a href=\"" + aRequest.getContextPath() + "/tools/index.jsp\"><span class=\"material-symbols-outlined\">home</span>Home</a></li>");
            out.println("        <li>");
            out.println("            <a href='" + aRequest.getContextPath() + "/cms/logout?redirect=" + aRequest.getContextPath() + "/start'><span class=\"material-symbols-outlined\">logout</span>Logout</a>");
            out.println("        </li>");
            out.println("    </ul>");
            out.println("    <hgroup>");
            out.println("        <h1>JSP Compilation</h1>");
            if (!foundJsps.isEmpty()) {
                out.print("<span>Found <strong>");
                out.print(foundJsps.size());
                out.println("</strong> JSPs</span>");
            }
            out.println("<span><strong>" + SettingsBean.getInstance().getString("jahia.environment", "") + "</strong></span>");
            out.println("    </hgroup>");
            out.println("</header>");

            out.println("<h2>Pre-compile:</h2>");
            out.println("<ul>");
            out.print("<li><a id=\"allJsp\" target=\"_blank\" href=\"");

            long now = System.currentTimeMillis();

            String url = aResponse.encodeURL(aRequest.getContextPath()
                    + aRequest.getServletPath() + "?" + COMPILE_TYPE_PARAM
                    + "=all&timestamp=" + now + "&" + MAGIC_TOMCAT_PARAM + "&" + getTokenParam(aRequest));

            out.print(url);
            out.println("\">all</a></li>");

            out.print("<li><a target=\"_blank\" href=\"");

            url = aResponse.encodeURL(aRequest.getContextPath()
                    + aRequest.getServletPath() + "?" + COMPILE_TYPE_PARAM
                    + "=non-modules&timestamp=" + now + "&" + MAGIC_TOMCAT_PARAM + "&" + getTokenParam(aRequest));

            out.print(url);
            out.println("\">non-modules</a></li>");

            out.print("<li><a target=\"_blank\" href=\"");

            url = aResponse.encodeURL(aRequest.getContextPath()
                    + aRequest.getServletPath() + "?" + COMPILE_TYPE_PARAM
                    + "=modules&timestamp=" + now + "&" + MAGIC_TOMCAT_PARAM + "&" + getTokenParam(aRequest));

            out.print(url);
            out.print("\">all modules</a></li>");

            out.println("</ul>");
            out.println("<h2>Modules:</h2>");

            out.println("<ul>");
            Map<String, Long> moduleBundles = new TreeMap<>();
            for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
                if (BundleUtils.isJahiaModuleBundle(bundle) && bundle.getState() == Bundle.ACTIVE) {
                    Enumeration<?> en = bundle.findEntries("/", "*.jsp", true);
                    if (en != null && en.hasMoreElements()) {
                        moduleBundles.put(bundle.getSymbolicName(), bundle.getBundleId());
                    }
                }
            }
            for (Map.Entry<String, Long> entry : moduleBundles.entrySet()) {
                out.print("<li><a target=\"_blank\" href=\"");

                url = aResponse.encodeURL(aRequest.getContextPath() + aRequest.getServletPath() + "?"
                        + COMPILE_TYPE_PARAM + "=module&id=" + entry.getValue() + "&timestamp=" + now + "&"
                        + MAGIC_TOMCAT_PARAM + "&" + getTokenParam(aRequest));

                out.print(url);
                out.print("\">" + entry.getKey() + "</a></li>");
            }

            out.println("</ul><br>");

            out.println("<h2>All JSPs:</h2>");
            listFiles(out, aRequest, foundJsps, aResponse, now);
            out.println("<footer class=\"page-footer\">");
            out.println("    <ul class=\"page-footer_bar\">");
            out.println("        <li><a href=\"" + aRequest.getContextPath() + "/tools/index.jsp\"><span class=\"material-symbols-outlined\">home</span>Home</a></li>");
            out.println("        <li>");
            out.println("            <a href='" + aRequest.getContextPath() + "/cms/logout?redirect=" + aRequest.getContextPath() + "/start'><span class=\"material-symbols-outlined\">logout</span>Logout</a>");
            out.println("        </li>");
            out.println("    </ul>");
            out.println("</footer>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Searches for Files with extension JSP in the whole web app directory.
     *
     * @return List of context relative JSP names (Strings)
     */
    private List<String> searchForAllJsps() {
        List<String> list = searchForJsps();
        list.addAll(searchForBundleJsps());
        return list;
    }

    /**
     * Searches for Files with extension JSP in the whole web app directory.
     *
     * @return List of context relative JSP names (Strings)
     */
    private List<String> searchForJsps() {
        String webModulePath = JahiaContextLoaderListener.getServletContext().getRealPath("/");
        File jspsDir = new File(webModulePath);
        List<String> foundJsps = new ArrayList<>();
        searchForJsps(webModulePath, jspsDir, foundJsps);
        return foundJsps;
    }

    private List<String> searchForBundleJsps() {
        List<String> foundJsps = new ArrayList<>();
        for (Bundle bundle : FrameworkService.getBundleContext().getBundles()) {
            if (BundleUtils.isJahiaModuleBundle(bundle) && bundle.getState() == Bundle.ACTIVE) {
                foundJsps.addAll(searchForBundleJsps(bundle));
            }
        }
        return foundJsps;
    }

    private List<String> searchForBundleJsps(Bundle bundle) {
        List<String> foundJsps = new ArrayList<>();
        Enumeration<?> en = bundle.findEntries("/", "*.jsp", true);
        if (en != null) {
            while (en.hasMoreElements()) {
                URL url = (URL) en.nextElement();
                foundJsps.add("modules/" + bundle.getSymbolicName() + url.getPath());
            }
        }
        return foundJsps;
    }

    /**
     * Fills passed List with context relative URLs of found JSPs. If passed dir contains subdirs, the method is called recursive for this
     * subdirs.
     */
    private void searchForJsps(String aWebModulePath, File aDir, List<String> aFoundJsps) {
        File[] files = aDir.listFiles();
        if (files == null) {
            // Nothing to do
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                // subdir found
                searchForJsps(aWebModulePath, file, aFoundJsps);
            } else {
                int extIdx = file.getName().lastIndexOf('.');
                if (extIdx != -1
                        && file.getName().length() == extIdx + 4 // ... + ".jsp"
                        && file.getName().regionMatches(true, extIdx + 1,
                        "jsp", 0, 3)) {
                    // JSP found!
                    String jspPath = file.getPath();
                    jspPath = jspPath.substring(aWebModulePath.length());
                    jspPath = jspPath.replace('\\', '/');
                    aFoundJsps.add(jspPath);
                }
            }
        }
    }

    /**
     * Loops through list of all JSP URLs, "includes" each JSP and generates a report HTML response. Progress information is printed to
     * stdout.
     */
    private void precompileJsps(List<String> foundJsps, HttpServletRequest aRequest,
                                HttpServletResponse aResponse) throws IOException {
        System.out.println("Precompile started...");

        List<String> buggyJsps = new ArrayList<>();
        int i = 1;
        for (final String jspPath : foundJsps) {
            try {
                NoOutputResponseWrapper responseWrapper = new NoOutputResponseWrapper(aResponse);
                compile(jspPath, aRequest, responseWrapper, i);
                System.out.println(" OK.");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                buggyJsps.add(jspPath);
            }
            aResponse.resetBuffer();
            i++;
        }
        System.out.println("Precompile ended!");
        PrintWriter out = aResponse.getWriter();
        aResponse.setContentType("text/html;charset=ISO-8859-1");
        out.print("<html>" + "<head>"
                + "<META http-equiv=\"expires\" content=\"0\">"
                + "<title>JSP precompile result</title>" + "</head>\r\n"
                + "<body>\r\n" + "<b>");
        out.print(foundJsps.size());
        out.print(" JSPs processed.</b><br>\r\n");
        if (buggyJsps.isEmpty()) {
            out.print("No problems found!\r\n");
        } else {
            out.print("Precompile failed for following <strong>" + buggyJsps.size()
                    + "</strong> JSPs:<br>\r\n");
            listFiles(out, aRequest, buggyJsps, aResponse, System.currentTimeMillis());
        }
        out.println("</body>" + "</html>");
    }

    private void compile(final String jspPath, final HttpServletRequest aRequest, HttpServletResponse aResponse, int i) throws Exception {
        RequestDispatcher rd = aRequest.getRequestDispatcher("/" + jspPath);
        System.out.print("Compiling (" + i + ") " + jspPath + "...");
        rd.include(aRequest, aResponse);
    }

    /**
     * Adds a hyperlinks for each JSP to the output. Each link contains the JSP name. If the JSP is located somewhere below WEB-INF dir, it
     * can not be reached from outside, therefore a link to the servlet is created with a jsp_name param. Tomcat specific jsp_precompile
     * param is also added to each link. Also current timestamp is added to help the browser marking visited links.
     */
    private void listFiles(PrintWriter anOut, HttpServletRequest request, List<String> aFoundJsps,
                           HttpServletResponse aResponse, long now) {
        for (String jspPath : aFoundJsps) {
            anOut.print("<a target=\"_blank\" href=\"");

            // create link to JspPrecompileServlet with jsp_name param
            String url = request.getContextPath() + request.getServletPath() + "?" + JSP_NAME_PARAM + "="
                    + jspPath + "&" + MAGIC_TOMCAT_PARAM + "&" + getTokenParam(request);

            url = url + "&now=" + now;
            anOut.print(aResponse.encodeURL(url));

            anOut.print("\">");
            anOut.print(jspPath);
            anOut.println("</a><br>");
        }
    }

    private String getTokenParam(ServletRequest request) {
        return ToolsAccessTokenFilter.CSRF_TOKEN_ATTR + "=" + request.getAttribute(ToolsAccessTokenFilter.CSRF_TOKEN_ATTR);
    }
}
