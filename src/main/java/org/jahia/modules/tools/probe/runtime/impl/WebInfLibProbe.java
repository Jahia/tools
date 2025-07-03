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
package org.jahia.modules.tools.probe.runtime.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.utils.StringOutputStream;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.io.File;
import java.io.PrintStream;

/**
 * Reports the list of JAR files under WEB-INF/lib.
 *
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + WebInfLibProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + WebInfLibProbe.NAME, Probe.CATEGORY + "=" + WebInfLibProbe.CATEGORY,
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + WebInfLibProbe.CATEGORY + ",name="
                + WebInfLibProbe.KEY })
public class WebInfLibProbe implements ProbeMBean {

    static final String CATEGORY = "runtime";

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");

    static final String KEY = "web-inf-lib";

    static final String NAME = "List of JARs under WEB-INF/lib";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        File dir = new File(System.getProperty("jahiaWebAppRoot"), "WEB-INF/lib");
        if (!dir.isDirectory()) {
            return StringUtils.EMPTY;
        }
        ShellTable table = new ShellTable();
        table.column("#");
        table.column("Name");
        table.column("Size").alignRight();
        table.column("Last modified").alignRight();

        File[] jars = dir.listFiles();
        if (jars != null) {
            int count = 0;
            for (File jar : jars) {
                table.addRow().addContent(++count, jar.getName(), jar.isFile() ? jar.length() : 0,
                        DATE_FORMAT.format(jar.lastModified()));
            }
        }

        StringOutputStream out = new StringOutputStream();

        table.print(new PrintStream(out));

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
