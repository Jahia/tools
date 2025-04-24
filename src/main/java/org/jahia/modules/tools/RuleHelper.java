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

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.drools.core.base.EnabledBoolean;
import org.drools.core.rule.Package;
import org.drools.core.rule.Rule;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.rules.RulesListener;
import org.jahia.services.templates.ModuleVersion;
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
        for (Map.Entry<String, Collection<String>> entry : listener.getModulePackageNameMap().entrySet()) {
            if (entry.getValue().contains(packageName)) {
                try {
                    // entry key are like this: location/3.2.0
                    String[] entryKeySplit = entry.getKey().split("/");
                    if (entryKeySplit.length == 2){
                        JahiaTemplatesPackage module = ServicesRegistry.getInstance().getJahiaTemplateManagerService()
                                .getTemplatePackageRegistry().lookupByIdAndVersion(entryKeySplit[0], new ModuleVersion(entryKeySplit[1]));
                        if (module != null) {
                            return module;
                        }
                    }
                } catch (Exception e) {
                    // failed silently ... was not able to locate module
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
