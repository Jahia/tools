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
package org.jahia.modules.tools.probe.properties.impl;

import java.io.PrintWriter;

import org.apache.commons.io.output.StringBuilderWriter;
import org.jahia.modules.tools.probe.ProbeMBean;

/**
 * Base class for the DX info probes.
 * 
 * @author Sergiy Shyrkov
 */
public abstract class BaseSysInfoProbe implements ProbeMBean {

    private static void trimLeadingEmptyLine(StringBuilder data) {
        if (data.indexOf(System.lineSeparator()) == 0) {
            // trim first empty line
            data.delete(0, System.lineSeparator().length());
        }
    }

    protected abstract void generateInfo(PrintWriter pw);

    @Override
    public final String getData() {
        StringBuilderWriter data = new StringBuilderWriter();
        generateInfo(new PrintWriter(data));
        trimLeadingEmptyLine(data.getBuilder());

        return data.toString();
    }
}
