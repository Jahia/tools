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
package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.osgi.framework.Bundle;

import java.util.HashSet;
import java.util.Set;

@GraphQLName("BundleWithImportPackages")
@GraphQLDescription("Result of the import package inspector operation.")
public class BundleWithImportPackages extends BundleResultEntry{
    private final Set<String> matchingImportedPackage = new HashSet<>();

    public BundleWithImportPackages(Bundle bundle) {
        super(bundle);
    }

    @GraphQLField
    @GraphQLName("matchingImportPackages")
    @GraphQLDescription("List of matching imported packages.")
    public Set<String> getMatchingImportedPackage() {
        return matchingImportedPackage;
    }

    public void addMatchingImportedPackage(String importedPackage) {
        matchingImportedPackage.add(importedPackage);
    }
}
