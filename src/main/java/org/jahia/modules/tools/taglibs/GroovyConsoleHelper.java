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
package org.jahia.modules.tools.taglibs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.osgi.BundleResource;
import org.jahia.registries.ServicesRegistry;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;

/**
 * Utility class for the Groovy Console.
 */
public class GroovyConsoleHelper {

    private static final Logger logger = LoggerFactory.getLogger(GroovyConsoleHelper.class);

    public static final String WARN_MSG = "WARNING: You are about to execute a script, which can manipulate the repository data or execute services in DX. Are you sure, you want to continue?";

    private static void generateCbFormElement(String paramName, StringBuilder sb, Properties confs,
            HttpServletRequest request) {
        sb.append("<p><label for=\"scriptParam_").append(paramName).append("\">");
        sb.append(confs.getProperty(String.format("script.param.%s.label", paramName), paramName)).append("</label> ");
        sb.append("<input type=\"checkbox\" name=\"scriptParam_").append(paramName).append("\" id=\"scriptParam_")
                .append(paramName);
        final String paramVal;
        if ("true".equals(request.getParameter("runScript"))) {
            paramVal = request.getParameter("scriptParam_" + paramName);
        } else {
            paramVal = confs.getProperty(String.format("script.param.%s.default", paramName), "").trim();
        }
        if (StringUtils.isNotBlank(paramVal)
                && ("on".equalsIgnoreCase(paramVal.trim()) || "true".equalsIgnoreCase(paramVal.trim())))
            sb.append("\" checked=\"true");
        sb.append("\" /></p>");
    }

    private static void generateFormElement(String paramName, StringBuilder sb, Properties confs,
            HttpServletRequest request) {
        final String paramType = confs.getProperty(String.format("script.param.%s.type", paramName), "checkbox").trim();
        if ("checkbox".equals(paramType)) {
            generateCbFormElement(paramName, sb, confs, request);
        } else if ("text".equals(paramType)) {
            generateTextFormElement(paramName, sb, confs, request);
        } else {
            logger.error(
                    String.format("Unsupported form element type form the parameter %s: %s", paramName, paramType));
        }
    }

    public static StringBuilder generateScriptSkeleton() {

        final StringBuilder code = new StringBuilder(2048);
        code.append("import com.google.common.collect.*\n");
        code.append("import com.google.common.io.*\n");
        code.append("import com.sun.enterprise.web.connector.grizzly.comet.*\n");
        code.append("import com.sun.grizzly.comet.*\n");
        code.append("import com.sun.grizzly.tcp.*\n");
        code.append("import com.sun.grizzly.websockets.*\n");
        code.append("import com.sun.image.codec.jpeg.*\n");
        code.append("import com.sun.medialib.mlib.*\n");
        code.append("import com.sun.net.httpserver.*\n");
        code.append("import com.sun.syndication.feed.synd.*\n");
        code.append("import com.sun.syndication.fetcher.*\n");
        code.append("import com.sun.syndication.fetcher.impl.*\n");
        code.append("import com.sun.syndication.io.*\n");
        code.append("import eu.infomas.annotation.*\n");
        code.append("import groovy.lang.*\n");
        code.append("import groovy.util.*\n");
        code.append("import groovy.util.slurpersupport.*\n");
        code.append("import groovy.xml.*\n");
        code.append("import javax.annotation.security.*\n");
        code.append("import javax.ejb.*\n");
        code.append("import javax.enterprise.context.*\n");
        code.append("import javax.enterprise.context.spi.*\n");
        code.append("import javax.enterprise.event.*\n");
        code.append("import javax.enterprise.inject.*\n");
        code.append("import javax.enterprise.inject.spi.*\n");
        code.append("import javax.enterprise.util.*\n");
        code.append("import javax.inject.*\n");
        code.append("import javax.interceptor.*\n");
        code.append("import javax.jcr.*\n");
        code.append("import javax.jcr.nodetype.*\n");
        code.append("import javax.jcr.observation.*\n");
        code.append("import javax.jcr.query.*\n");
        code.append("import javax.jcr.query.qom.*\n");
        code.append("import javax.jcr.version.*\n");
        code.append("import javax.mail.*\n");
        code.append("import javax.mail.internet.*\n");
        code.append("import javax.mail.util.*\n");
        code.append("import javax.persistence.*\n");
        code.append("import javax.servlet.*\n");
        code.append("import javax.servlet.annotation.*\n");
        code.append("import javax.servlet.http.*\n");
        code.append("import javax.servlet.resources.*\n");
        code.append("import javax.validation.*\n");
        code.append("import name.fraser.neil.plaintext.*\n");
        code.append("import net.htmlparser.jericho.*\n");
        code.append("import nu.xom.*\n");
        code.append("import oauth.signpost.*\n");
        code.append("import oauth.signpost.basic.*\n");
        code.append("import oauth.signpost.commonshttp.*\n");
        code.append("import oauth.signpost.exception.*\n");
        code.append("import oauth.signpost.http.*\n");
        code.append("import oracle.xml.parser.*\n");
        code.append("import oracle.xml.parser.v2.*\n");
        code.append("import org.aopalliance.aop.*\n");
        code.append("import org.aopalliance.intercept.*\n");
        code.append("import org.apache.camel.*\n");
        code.append("import org.apache.camel.builder.*\n");
        code.append("import org.apache.camel.component.mail.*\n");
        code.append("import org.apache.camel.impl.*\n");
        code.append("import org.apache.camel.model.*\n");
        code.append("import org.apache.camel.spring.*\n");
        code.append("import org.apache.camel.util.*\n");
        code.append("import org.apache.catalina.connector.*\n");
        code.append("import org.apache.catalina.util.*\n");
        code.append("import org.apache.catalina.websocket.*\n");
        code.append("import org.apache.commons.beanutils.*\n");
        code.append("import org.apache.commons.codec.binary.*\n");
        code.append("import org.apache.commons.codec.digest.*\n");
        code.append("import org.apache.commons.collections.*\n");
        code.append("import org.apache.commons.collections.iterators.*\n");
        code.append("import org.apache.commons.collections.keyvalue.*\n");
        code.append("import org.apache.commons.collections.list.*\n");
        code.append("import org.apache.commons.collections.map.*\n");
        code.append("import org.apache.commons.httpclient.*\n");
        code.append("import org.apache.commons.httpclient.auth.*\n");
        code.append("import org.apache.commons.httpclient.methods.*\n");
        code.append("import org.apache.commons.httpclient.methods.multipart.*\n");
        code.append("import org.apache.commons.httpclient.params.*\n");
        code.append("import org.apache.commons.httpclient.protocol.*\n");
        code.append("import org.apache.commons.id.*\n");
        code.append("import org.apache.commons.lang.*\n");
        code.append("import org.apache.commons.lang.builder.*\n");
        code.append("import org.apache.commons.lang.exception.*\n");
        code.append("import org.apache.commons.lang.math.*\n");
        code.append("import org.apache.commons.lang.time.*\n");
        code.append("import org.apache.commons.logging.*\n");
        code.append("import org.apache.coyote.http11.upgrade.*\n");
        code.append("import org.apache.jackrabbit.commons.query.*\n");
        code.append("import org.apache.jackrabbit.util.*\n");
        code.append("import org.apache.jackrabbit.value.*\n");
        code.append("import org.apache.log4j.*\n");
        code.append("import org.apache.oro.text.regex.*\n");
        code.append("import org.apache.pdfbox.pdmodel.*\n");
        code.append("import org.apache.pluto.container.*\n");
        code.append("import org.apache.regexp.*\n");
        code.append("import org.apache.solr.client.solrj.response.*\n");
        code.append("import org.apache.tika.io.*\n");
        code.append("import org.apache.tomcat.util.http.mapper.*\n");
        code.append("import org.apache.tools.ant.*\n");
        code.append("import org.apache.velocity.tools.generic.*\n");
        code.append("import org.apache.xerces.dom.*\n");
        code.append("import org.apache.xerces.jaxp.*\n");
        code.append("import org.apache.xerces.parsers.*\n");
        code.append("import org.artofsolving.jodconverter.document.*\n");
        code.append("import org.artofsolving.jodconverter.office.*\n");
        code.append("import org.codehaus.groovy.runtime.*\n");
        code.append("import org.codehaus.groovy.runtime.typehandling.*\n");
        code.append("import org.cyberneko.html.parsers.*\n");
        code.append("import org.dom4j.*\n");
        code.append("import org.dom4j.io.*\n");
        code.append("import org.dom4j.tree.*\n");
        code.append("import org.drools.*\n");
        code.append("import org.drools.spi.*\n");
        code.append("import org.drools.util.*\n");
        code.append("import org.eclipse.jetty.continuation.*\n");
        code.append("import org.eclipse.jetty.websocket.*\n");
        code.append("import org.glassfish.grizzly.*\n");
        code.append("import org.glassfish.grizzly.comet.*\n");
        code.append("import org.glassfish.grizzly.filterchain.*\n");
        code.append("import org.glassfish.grizzly.http.*\n");
        code.append("import org.glassfish.grizzly.http.server.*\n");
        code.append("import org.glassfish.grizzly.http.server.util.*\n");
        code.append("import org.glassfish.grizzly.http.util.*\n");
        code.append("import org.glassfish.grizzly.servlet.*\n");
        code.append("import org.glassfish.grizzly.utils.*\n");
        code.append("import org.glassfish.grizzly.websockets.*\n");
        code.append("import org.hibernate.*\n");
        code.append("import org.hibernate.cfg.*\n");
        code.append("import org.hibernate.classic.*\n");
        code.append("import org.hibernate.criterion.*\n");
        code.append("import org.jahia.admin.*\n");
        code.append("import org.jahia.admin.sites.*\n");
        code.append("import org.jahia.ajax.gwt.client.widget.contentengine.*\n");
        code.append("import org.jahia.ajax.gwt.client.widget.edit.sidepanel.*\n");
        code.append("import org.jahia.ajax.gwt.client.widget.publication.*\n");
        code.append("import org.jahia.ajax.gwt.client.widget.subscription.*\n");
        code.append("import org.jahia.ajax.gwt.client.widget.toolbar.action.*\n");
        code.append("import org.jahia.ajax.gwt.helper.*\n");
        code.append("import org.jahia.ajax.gwt.utils.*\n");
        code.append("import org.jahia.api.*\n");
        code.append("import org.jahia.bin.*\n");
        code.append("import org.jahia.bin.errors.*\n");
        code.append("import org.jahia.data.*\n");
        code.append("import org.jahia.data.applications.*\n");
        code.append("import org.jahia.data.beans.portlets.*\n");
        code.append("import org.jahia.data.templates.*\n");
        code.append("import org.jahia.data.viewhelper.principal.*\n");
        code.append("import org.jahia.defaults.config.spring.*\n");
        code.append("import org.jahia.engines.*\n");
        code.append("import org.jahia.exceptions.*\n");
        code.append("import org.jahia.modules.visibility.rules.*\n");
        code.append("import org.jahia.params.*\n");
        code.append("import org.jahia.params.valves.*\n");
        code.append("import org.jahia.pipelines.*\n");
        code.append("import org.jahia.pipelines.valves.*\n");
        code.append("import org.jahia.registries.*\n");
        code.append("import org.jahia.security.license.*\n");
        code.append("import org.jahia.services.*\n");
        code.append("import org.jahia.services.applications.*\n");
        code.append("import org.jahia.services.atmosphere.*\n");
        code.append("import org.jahia.services.cache.*\n");
        code.append("import org.jahia.services.channels.*\n");
        code.append("import org.jahia.services.channels.providers.*\n");
        code.append("import org.jahia.services.content.*\n");
        code.append("import org.jahia.services.content.decorator.*\n");
        code.append("import org.jahia.services.content.nodetypes.*\n");
        code.append("import org.jahia.services.content.nodetypes.initializers.*\n");
        code.append("import org.jahia.services.content.nodetypes.renderer.*\n");
        code.append("import org.jahia.services.content.rules.*\n");
        code.append("import org.jahia.services.image.*\n");
        code.append("import org.jahia.services.importexport.*\n");
        code.append("import org.jahia.services.logging.*\n");
        code.append("import org.jahia.services.mail.*\n");
        code.append("import org.jahia.services.notification.*\n");
        code.append("import org.jahia.services.preferences.user.*\n");
        code.append("import org.jahia.services.pwdpolicy.*\n");
        code.append("import org.jahia.services.query.*\n");
        code.append("import org.jahia.services.render.*\n");
        code.append("import org.jahia.services.render.filter.*\n");
        code.append("import org.jahia.services.render.filter.cache.*\n");
        code.append("import org.jahia.services.render.scripting.*\n");
        code.append("import org.jahia.services.scheduler.*\n");
        code.append("import org.jahia.services.search.*\n");
        code.append("import org.jahia.services.seo.*\n");
        code.append("import org.jahia.services.seo.jcr.*\n");
        code.append("import org.jahia.services.seo.urlrewrite.*\n");
        code.append("import org.jahia.services.sites.*\n");
        code.append("import org.jahia.services.tags.*\n");
        code.append("import org.jahia.services.tasks.*\n");
        code.append("import org.jahia.services.templates.*\n");
        code.append("import org.jahia.services.transform.*\n");
        code.append("import org.jahia.services.translation.*\n");
        code.append("import org.jahia.services.uicomponents.bean.*\n");
        code.append("import org.jahia.services.uicomponents.bean.contentmanager.*\n");
        code.append("import org.jahia.services.uicomponents.bean.editmode.*\n");
        code.append("import org.jahia.services.uicomponents.bean.toolbar.*\n");
        code.append("import org.jahia.services.usermanager.*\n");
        code.append("import org.jahia.services.usermanager.jcr.*\n");
        code.append("import org.jahia.services.visibility.*\n");
        code.append("import org.jahia.services.workflow.*\n");
        code.append("import org.jahia.settings.*\n");
        code.append("import org.jahia.tools.files.*\n");
        code.append("import org.jahia.tools.jvm.*\n");
        code.append("import org.jahia.utils.*\n");
        code.append("import org.jahia.utils.comparator.*\n");
        code.append("import org.jahia.utils.i18n.*\n");
        code.append("import org.jahia.utils.zip.*\n");
        code.append("import org.jaxen.*\n");
        code.append("import org.jaxen.jdom.*\n");
        code.append("import org.jbpm.api.activity.*\n");
        code.append("import org.jbpm.api.model.*\n");
        code.append("import org.jbpm.api.task.*\n");
        code.append("import org.joda.time.*\n");
        code.append("import org.joda.time.format.*\n");
        code.append("import org.mortbay.util.ajax.*\n");
        code.append("import org.quartz.*\n");
        code.append("import org.springframework.aop.*\n");
        code.append("import org.springframework.aop.framework.*\n");
        code.append("import org.springframework.aop.support.*\n");
        code.append("import org.springframework.beans.*\n");
        code.append("import org.springframework.beans.factory.*\n");
        code.append("import org.springframework.beans.factory.annotation.*\n");
        code.append("import org.springframework.beans.factory.config.*\n");
        code.append("import org.springframework.beans.factory.support.*\n");
        code.append("import org.springframework.beans.factory.xml.*\n");
        code.append("import org.springframework.beans.propertyeditors.*\n");
        code.append("import org.springframework.context.*\n");
        code.append("import org.springframework.context.event.*\n");
        code.append("import org.springframework.context.support.*\n");
        code.append("import org.springframework.core.*\n");
        code.append("import org.springframework.core.enums.*\n");
        code.append("import org.springframework.core.io.*\n");
        code.append("import org.springframework.core.io.support.*\n");
        code.append("import org.springframework.dao.*\n");
        code.append("import org.springframework.jdbc.core.*\n");
        code.append("import org.springframework.orm.*\n");
        code.append("import org.springframework.orm.hibernate3.*\n");
        code.append("import org.springframework.orm.hibernate3.annotation.*\n");
        code.append("import org.springframework.orm.hibernate3.support.*\n");
        code.append("import org.springframework.scheduling.quartz.*\n");
        code.append("import org.springframework.ui.context.*\n");
        code.append("import org.springframework.ui.context.support.*\n");
        code.append("import org.springframework.util.*\n");
        code.append("import org.springframework.util.xml.*\n");
        code.append("import org.springframework.web.context.*\n");
        code.append("import org.springframework.web.context.support.*\n");
        code.append("import org.springframework.web.servlet.*\n");
        code.append("import org.springframework.web.servlet.mvc.*\n");
        code.append("import org.springframework.webflow.core.collection.*\n");
        code.append("import sun.awt.image.*\n");
        code.append("import sun.awt.image.codec.*\n");
        code.append("import sun.security.action.*\n");
        code.append("import ucar.nc2.util.net.*\n");
        code.append("\n");

        return code;
    }

    private static void generateTextFormElement(String paramName, StringBuilder sb, Properties confs,
            HttpServletRequest request) {
        sb.append("<p><label for=\"scriptParam_").append(paramName).append("\">");
        sb.append(confs.getProperty(String.format("script.param.%s.label", paramName), paramName)).append("</label> ");
        sb.append("<input type=\"text\" name=\"scriptParam_").append(paramName).append("\" id=\"scriptParam_")
                .append(paramName);
        final String paramVal;
        if ("true".equals(request.getParameter("runScript"))) {
            paramVal = request.getParameter("scriptParam_" + paramName);
        } else {
            paramVal = confs.getProperty(String.format("script.param.%s.default", paramName), "");
        }
        if (StringUtils.isNotBlank(paramVal))
            sb.append("\" value=\"").append(paramVal);
        sb.append("\" /></p>");
    }

    /**
     * Returns a collection of BundleResource, representing scripts, which are found in all active module bundles.
     * 
     * @return a collection of BundleResource, representing scripts, which are found in all active module bundles
     */
    public static Collection<BundleResource> getGroovyConsoleScripts() {
        final List<BundleResource> scripts = new ArrayList<>();
        for (final JahiaTemplatesPackage aPackage : ServicesRegistry.getInstance().getJahiaTemplateManagerService()
                .getAvailableTemplatePackages()) {
            final Bundle bundle = aPackage.getBundle();
            if (bundle != null) {
                final Enumeration<URL> resourceEnum = bundle.findEntries("META-INF/groovyConsole", "*.groovy", false);
                if (resourceEnum == null)
                    continue;
                while (resourceEnum.hasMoreElements()) {
                    final BundleResource bundleResource = new BundleResource(resourceEnum.nextElement(), bundle);
                    scripts.add(bundleResource);
                }
            }
        }
        return scripts;
    }

    /**
     * Returns a generated HTML with form elements for the script parameters.
     * 
     * @param scriptURI
     * @param request
     * @return
     */
    public static String getScriptCustomFormElements(String scriptURI, HttpServletRequest request) {
        if (StringUtils.isBlank(scriptURI)) {
            return StringUtils.EMPTY;
        }
        final StringBuilder sb = new StringBuilder();
        try {
            final UrlResource resource = new UrlResource(
                    StringUtils.substringBeforeLast(scriptURI, ".groovy") + ".properties");
            if (resource.exists()) {
                final Properties confs = new Properties();
                confs.load(resource.getInputStream());
                final String[] paramNames = StringUtils
                        .split(confs.getProperty("script.parameters.names", "").replaceAll("\\s", ""), ",");
                for (String paramName : paramNames) {
                    generateFormElement(paramName.trim(), sb, confs, request);
                }

            }
        } catch (IOException e) {
            logger.error("An error occured while reading the configurations for the script " + scriptURI, e);
            return StringUtils.EMPTY;
        }
        final String formElements = sb.toString();
        if (StringUtils.isBlank(formElements)) {
            return StringUtils.EMPTY;
        }
        return sb.delete(0, sb.length()).append("<fieldset><legend>Script configuration</legend>").append(formElements)
                .append("</fieldset>").toString();
    }

    /**
     * Returns an array of parameter names for the specified script or <code>null</code> if the script has no parameters.
     * 
     * @param scriptURI the script URI to get parameter names for
     * @return an array of parameter names for the specified script or <code>null</code> if the script has no parameters
     */
    public static String[] getScriptParamNames(String scriptURI) {
        try {
            final UrlResource resource = new UrlResource(
                    StringUtils.substringBeforeLast(scriptURI, ".groovy") + ".properties");
            if (resource.exists()) {
                final Properties confs = new Properties();
                confs.load(resource.getInputStream());
                return StringUtils.split(confs.getProperty("script.parameters.names", "").replaceAll("\\s", ""), ",");
            }
        } catch (IOException e) {
            logger.error("An error occured while reading the configurations for the script " + scriptURI, e);
        }
        return null;
    }

}