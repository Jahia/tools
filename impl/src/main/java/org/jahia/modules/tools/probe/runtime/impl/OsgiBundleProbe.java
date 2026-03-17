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

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.bin.Jahia;
import org.jahia.modules.tools.probe.Probe;
import org.jahia.modules.tools.probe.ProbeMBean;
import org.jahia.osgi.FrameworkService;
import org.jahia.utils.StringOutputStream;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Reports the state of OSGI bundles.
 *
 * @author Sergiy Shyrkov
 */
@Component(service = Probe.class, property = { Probe.KEY + "=" + OsgiBundleProbe.KEY,
        Constants.SERVICE_DESCRIPTION + "=" + OsgiBundleProbe.NAME, Probe.CATEGORY + "=" + OsgiBundleProbe.CATEGORY,
        Constants.SERVICE_VENDOR + "=" + Jahia.VENDOR_NAME,
        "jmx.objectname=org.jahia.server:type=tools,subtype=probe,category=" + OsgiBundleProbe.CATEGORY + ",name="
                + OsgiBundleProbe.KEY })
public class OsgiBundleProbe implements ProbeMBean {

    static final String CATEGORY = "runtime";

    static final String KEY = "osgi-bundles";

    static final String NAME = "List of OSGi bundles";

    private BundleService bundleService;

    private void appendInfo(Bundle b, ShellTable table) {
        BundleInfo info = this.bundleService.getInfo(b);
        String version = info.getVersion();
        ArrayList<Object> rowData = new ArrayList<>();
        rowData.add(info.getBundleId());
        rowData.add(getStateString(info.getState()));
        rowData.add(info.getStartLevel());
        rowData.add(version);
        rowData.add(info.getSymbolicName() == null ? "<no symbolic name>" : info.getSymbolicName());

        String bundleName = (info.getName() == null) ? info.getSymbolicName() : info.getName();
        bundleName = (bundleName == null) ? info.getUpdateLocation() : bundleName;
        String name = bundleName + printFragments(info) + printHosts(info);
        rowData.add(name);

        rowData.add(info.getUpdateLocation());

        rowData.add(info.getUpdateLocation());

        rowData.add(info.getRevisions());

        Row row = table.addRow();
        row.addContent(rowData);
    }

    @Reference(service = BundleService.class)
    protected void bindBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getData() {
        ShellTable table = new ShellTable();
        table.column("ID").alignRight();
        table.column("State");
        table.column("Lvl").alignRight();
        table.column("Version");
        table.column("Symbolic name");
        table.column("Name");

        table.column(new Col("Location") {
            @Override
            protected String cut(String value, int size) {
                if (value.length() > size) {
                    String[] parts = value.split("/");
                    StringBuilder cut = new StringBuilder();
                    for (int idx = parts.length - 1; idx > 0; idx--) {
                        if (cut.length() + parts[0].length() + 4 + parts[idx].length() + 1 < size) {
                            cut.append("/").append(parts[idx]).append(cut);
                        } else {
                            break;
                        }
                    }
                    cut.append(parts[0]).append("/...").append(cut);
                    return cut.toString();
                } else {
                    return super.cut(value, size);
                }
            }
        });
        table.column("Update location");
        table.column("Revisions");

        for (Bundle b : FrameworkService.getBundleContext().getBundles()) {
            appendInfo(b, table);
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

    private String getStateString(BundleState state) {
        return (state == null) ? "" : state.toString();
    }

    private String printFragments(BundleInfo info) {
        if (info.getFragments().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(", Fragments: ");
        boolean first = true;
        for (Bundle host : info.getFragments()) {
            builder.append((first ? "" : ", ") + host.getBundleId());
            first = false;
        }
        return builder.toString();
    }

    private String printHosts(BundleInfo info) {
        if (info.getFragmentHosts().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(", Hosts: ");
        boolean first = true;
        for (Bundle host : info.getFragmentHosts()) {
            builder.append((first ? "" : ", ") + host.getBundleId());
            first = false;
        }
        return builder.toString();
    }
}
