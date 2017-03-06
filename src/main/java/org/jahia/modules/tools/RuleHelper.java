/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.drools.core.base.EnabledBoolean;
import org.drools.core.rule.Package;
import org.drools.core.rule.Rule;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.rules.RulesListener;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

/**
 * Utility class for the business rule overview page.
 * 
 * @author Sergiy Shyrkov
 */
public class RuleHelper {

    /**
     * Rule package data object.
     */
    public class PackageData {

        private String content;

        private String origin;

        private Package pkg;

        /**
         * Initializes an instance of this class.
         * 
         * @param pkg the rule package object
         * @param origin the module, this package comes form
         * @param content the raw content of the rule file
         */
        public PackageData(Package pkg, String origin, String content) {
            super();
            this.pkg = pkg;
            this.origin = origin;
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public String getName() {
            return pkg.getName();
        }

        public String getOrigin() {
            return origin;
        }

        public Rule[] getRules() {
            return pkg.getRules();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RuleHelper.class);

    private static final String ORIGIN_CORE = "DX Core";

    private Map<RulesListener, List<PackageData>> rules;

    private PackageData createPackageData(Package pkg, RulesListener listener) {
        JahiaTemplatesPackage originModule = getPackageOrigin(pkg.getName(), listener);
        String origin = originModule != null ? originModule.getId() + '/' + originModule.getVersion() : ORIGIN_CORE;
        String content = null;
        try {
            content = getPackageContent(pkg.getName(), originModule, listener);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return new PackageData(pkg, origin, content);
    }

    /**
     * Get the rule base data.
     * 
     * @return the rule base data
     */
    public Map<RulesListener, List<PackageData>> getData() {
        if (null == rules) {
            rules = prepareRuleData();
        }
        return rules;
    }

    private String getPackageContent(String packageName, JahiaTemplatesPackage module, RulesListener listener)
            throws IOException {
        String content = null;
        if (module != null) {
            for (String file : module.getRulesFiles()) {
                if (listener.getFilesAccepted().contains(StringUtils.substringAfterLast(file, "/"))) {
                    return FileUtils.getContent(module.getResource(file));
                }
            }
        } else {
            // dealing with core rule
            // TODO any other way to get the content without "knowing" the file names?
            String fileName = null;
            if ("org.jahia.services.content.rules.extraction".equals(packageName)) {
                fileName = "text-extraction-rules.drl";
            } else if ("org.jahia.services.content.rules".equals(packageName)) {
                fileName = "live".equals(listener.getWorkspace()) ? "repository-live-rules.drl"
                        : "repository-rules.drl";
            }
            if (fileName != null) {
                return FileUtils.getContent(new FileSystemResource(
                        new File(SettingsBean.getInstance().getJahiaEtcDiskPath() + "/repository/rules/" + fileName)));
            }
        }

        return content;
    }

    private JahiaTemplatesPackage getPackageOrigin(String packageName, RulesListener listener) {
        for (Map.Entry<String, String> entry : listener.getModulePackageNameMap().entrySet()) {
            if (packageName.equals(entry.getValue())) {
                JahiaTemplatesPackage module = ServicesRegistry.getInstance().getJahiaTemplateManagerService()
                        .getTemplatePackage(entry.getKey());
                if (module != null) {
                    return module;
                }
            }
        }
        return null;
    }

    private Map<RulesListener, List<PackageData>> prepareRuleData() {
        Map<RulesListener, List<PackageData>> data = new LinkedHashMap<>();

        for (RulesListener listener : RulesListener.getInstances()) {
            List<PackageData> packages = data.get(listener);
            if (packages == null) {
                packages = new LinkedList<>();
                data.put(listener, packages);
            }
            for (Package pkg : listener.getRuleBase().getPackages()) {
                packages.add(createPackageData(pkg, listener));
            }
        }

        return data;
    }

    /**
     * Update the state of the specified rule.
     * 
     * @param listenerId the listener ID
     * @param packageId the package name
     * @param ruleId the rule name
     * @param enable should the rule be enabled or disabled?
     * @return <code>true</code> if the rule state was updated; <code>false</code> if the rule cannot be found
     */
    public boolean updateRuleState(String listenerId, String packageId, String ruleId, boolean enable) {
        for (RulesListener listener : RulesListener.getInstances()) {
            if (!listener.toString().equals(listenerId)) {
                continue;
            }
            Package pkg = listener.getRuleBase().getPackage(packageId);
            if (pkg != null) {
                Rule rule = pkg.getRule(ruleId);
                if (rule != null) {
                    rule.setEnabled(enable ? EnabledBoolean.ENABLED_TRUE : EnabledBoolean.ENABLED_FALSE);
                    return true;
                }
            }
        }
        return false;
    }

}
