/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.utils.manifest.Clause;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.Set;

@GraphQLName("BundleDependency")
@GraphQLDescription("Result of the dependency inspector operation.")
public class Dependency {

    public static final Set<Status> RESTRICTIVES_DEPENDENCY_STATUS = Set.of(Status.SINGLE_VERSION_RANGE, Status.RESTRICTIVE_RANGE, Status.STRICT_NO_RANGE, Status.UNKNOWN);

    private final Type type;
    private final String name;
    private final boolean optional;
    private final VersionRange versionRange;
    private final Status status;
    private final String error;

    public Dependency(Type type, String name, VersionRange versionRange, boolean optional) {
        this.type = type;
        this.name = name;
        this.versionRange = versionRange;
        this.optional = optional;
        this.error = "";
        if (this.hasVersion()) {
            if (type.equals(Type.IMPORT_PACKAGE) && !this.isRange()) {
                this.status = Status.STRICT_NO_RANGE;
            } else if (versionRange.isExact()) {
                this.status = Status.SINGLE_VERSION_RANGE;
            } else if (this.canBumpMinorVersion()) {
                this.status = Status.OPEN_RANGE;
            } else if (!this.rangeIncludesNextMajorVersion()) {
                this.status = Status.RESTRICTIVE_RANGE;
            } else if (this.canBumpMinorVersion()) {
                this.status = Status.OPEN_RANGE;
            } else {
                this.status = Status.UNKNOWN;
            }
        } else {
            this.status = Status.EMPTY;
        }
    }

    public Dependency(Type type, String name, boolean optional, String error) {
        this.type = type;
        this.name = name;
        this.versionRange = null;
        this.optional = optional;
        this.status = Status.UNKNOWN;
        this.error = error;
    }

    @GraphQLField
    @GraphQLName("type")
    @GraphQLDescription("The type of the dependency.")
    public Type getType() {
        return type;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("The name of the dependency")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("version")
    @GraphQLDescription("The version of the dependency (can be a range, a number or empty)")
    public String getVersion() {
        return (versionRange!=null)?versionRange.toString():"";
    }

    public VersionRange getVersionRange() {
        return versionRange;
    }

    @GraphQLField
    @GraphQLName("optional")
    @GraphQLDescription("The dependency is optional")
    public boolean isOptional() {
        return optional;
    }

    @GraphQLField
    @GraphQLName("error")
    @GraphQLDescription("An error occurred during parsing dependency")
    public String getError() {
        return error;
    }

    public boolean hasVersion() {
        return versionRange != null;
    }

    public boolean isRange() {
        return versionRange.getRight() != null;
    }

    /*
    * If we can bump the minor version to 20 (aka minor version up range is 99 or something like that we consider that the range is
    * open for upgrade
    */
    public boolean canBumpMinorVersion() {
        if (versionRange == null || versionRange.isExact()) {
            return false;
        }
        Version bumped = new Version(versionRange.getLeft().getMajor(), versionRange.getLeft().getMinor() + 20, 0);
        return versionRange.includes(bumped);
    }

    /*
     * If the up range is greater than the major version we consider that the range is open for upgrade
     */
    public boolean rangeIncludesNextMajorVersion() {
        if (versionRange == null || versionRange.isExact()) {
            return false;
        }
        if (type.equals(Type.JAHIA_DEPENDS) && versionRange.getRight() == null) {
            return true;
        }
        return versionRange.getRight() != null && versionRange.getRight().getMajor() > versionRange.getLeft().getMajor();
    }

    public boolean isStrictDependency() {
        return RESTRICTIVES_DEPENDENCY_STATUS.contains(status);
    }

    public static Dependency parse(String dependency) {
        String cleanedDep = dependency.replace(";optional", "");
        cleanedDep = cleanedDep.replace("optional", "");
        boolean optional = !cleanedDep.equals(dependency);

        if (cleanedDep.endsWith("=")) cleanedDep = cleanedDep.substring(0, cleanedDep.length() - 1);
        String[] parts = cleanedDep.split("=");
        Dependency instance;
        if (parts.length == 2) {
            try {
                instance = new Dependency(Type.JAHIA_DEPENDS, parts[0], VersionRange.valueOf(parts[1]), optional);
            } catch (IllegalArgumentException e) {
                instance = new Dependency(Type.JAHIA_DEPENDS, parts[0], optional, "Error parsing version range: " + e.getMessage());
            }
        } else {
            instance = new Dependency(Type.JAHIA_DEPENDS, cleanedDep, null, optional);
        }
        return instance;
    }

    public static Dependency parse(Clause importedPackageClause) {
        String version = importedPackageClause.getAttribute(Constants.VERSION_ATTRIBUTE);
        boolean optional = (importedPackageClause.getAttribute(Constants.RESOLUTION_OPTIONAL) != null);
        Dependency instance;
        if (version != null) {
            try {
                instance = new Dependency(Type.IMPORT_PACKAGE, importedPackageClause.getName(), VersionRange.valueOf(version), optional);
            } catch (IllegalArgumentException e) {
                instance = new Dependency(Type.IMPORT_PACKAGE, importedPackageClause.getName(), optional, "Error parsing version range: " + e.getMessage());
            }
        } else {
            instance = new Dependency(Type.IMPORT_PACKAGE, importedPackageClause.getName(), null, optional);
        }
        return instance;
    }

    @GraphQLField
    @GraphQLName("summary")
    @GraphQLDescription("The summary of dependency inspection")
    @Override
    public String toString() {
        return "type='" + type.getMessage() + '\'' + ", name='" + name + '\'' + ", version=" + versionRange + ", status=" + status +
                ((StringUtils.isNotEmpty(error))?", error=" + error:"");
    }

    public enum Type {
        JAHIA_DEPENDS("Jahia-Depends"),
        IMPORT_PACKAGE("Import-Package");

        private final String message;

        Type(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public enum Status {
        EMPTY("No version dependency"),
        STRICT_NO_RANGE("Strict version dependency"),
        SINGLE_VERSION_RANGE("Single version range dependency"),
        RESTRICTIVE_RANGE("Version range is too restrictive to ensure safe minor upgrades"),
        OPEN_RANGE("Version is open to minor upgrade"),
        UNKNOWN("Unable to get version status");

        private final String message;

        Status(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
