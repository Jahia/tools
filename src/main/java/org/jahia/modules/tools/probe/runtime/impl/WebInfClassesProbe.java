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
