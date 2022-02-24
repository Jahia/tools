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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.utils.StringOutputStream;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * Reports the list of class files under WEB-INF/classes.
 * 
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + WebInfClassesProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + WebInfClassesProbe.NAME,
        Probe.CATEGORY + "=" + WebInfClassesProbe.CATEGORY, Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + WebInfClassesProbe.CATEGORY + ",name="
                + WebInfClassesProbe.KEY })
public class WebInfClassesProbe implements ProbeMBean {

    static final String CATEGORY = "runtime";

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");

    static final String KEY = "web-inf-classes";

    static final String NAME = "List of classes under WEB-INF/classes";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        File dir = new File(System.getProperty("jahiaWebAppRoot"), "WEB-INF/classes");
        if (!dir.isDirectory()) {
            return StringUtils.EMPTY;
        }
        StringOutputStream out = null;
        Collection<File> classes = FileUtils.listFiles(dir, new String[] { "class" }, true);

        if (!classes.isEmpty()) {
            ShellTable table = new ShellTable();
            table.column("#");
            table.column("Class");
            table.column("Size").alignRight();
            table.column("Last modified").alignRight();

            try {
                String base = dir.getCanonicalPath();
                int count = 0;
                for (File f : classes) {
                    table.addRow().addContent(++count, f.getPath().substring(base.length() + 1).replace('\\', '/'),
                            f.isFile() ? f.length() : 0, DATE_FORMAT.format(f.lastModified()));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }

            out = new StringOutputStream();
            table.print(new PrintStream(out));
        }

        return out != null ? out.toString() : StringUtils.EMPTY;

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
