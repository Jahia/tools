/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.tools.gql.admin.jcr.types;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphQL type representing a JCR property
 */
@GraphQLName("JcrProperty")
@GraphQLDescription("JCR property representation")
public class GqlJcrProperty {
    private final Property property;

    public GqlJcrProperty(Property property) {
        this.property = property;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("Property name")
    public String getName() throws RepositoryException {
        return property.getName();
    }

    @GraphQLField
    @GraphQLName("type")
    @GraphQLDescription("Property type (STRING, LONG, DOUBLE, BOOLEAN, DATE, BINARY, etc.)")
    public String getType() throws RepositoryException {
        return PropertyType.nameFromValue(property.getType());
    }

    @GraphQLField
    @GraphQLName("multiple")
    @GraphQLDescription("Whether this property holds multiple values")
    public boolean isMultiple() throws RepositoryException {
        return property.isMultiple();
    }

    @GraphQLField
    @GraphQLName("value")
    @GraphQLDescription("Single property value (null if multiple)")
    public String getValue() throws RepositoryException {
        if (property.isMultiple()) {
            return null;
        }
        return formatValue(property.getValue());
    }

    @GraphQLField
    @GraphQLName("values")
    @GraphQLDescription("Multiple property values (empty if single)")
    public List<String> getValues() throws RepositoryException {
        List<String> result = new ArrayList<>();
        if (property.isMultiple()) {
            Value[] values = property.getValues();
            for (Value value : values) {
                result.add(formatValue(value));
            }
        }
        return result;
    }

    @GraphQLField
    @GraphQLName("path")
    @GraphQLDescription("Property path")
    public String getPath() throws RepositoryException {
        return property.getPath();
    }

    private String formatValue(Value value) throws RepositoryException {
        if (value == null) {
            return null;
        }

        switch (value.getType()) {
            case PropertyType.BINARY:
                return "[Binary data: " + value.getBinary().getSize() + " bytes]";
            case PropertyType.DATE:
                return value.getDate().getTime().toString();
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return value.getString();
            default:
                return value.getString();
        }
    }
}
