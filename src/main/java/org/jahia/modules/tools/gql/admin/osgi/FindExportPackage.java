package org.jahia.modules.tools.gql.admin.osgi;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.ArrayList;
import java.util.List;

@GraphQLName("ImportPackageInspectorResultParent")
@GraphQLDescription("Result of the export package inspector operation.")
public class FindExportPackage {
    int totalCount = 0;

    List<ExportPackages> results = new ArrayList<>();

    public void add(ExportPackages result) {
        results.add(result);
        totalCount += result.getMatchingPackages().size();
    }

    @GraphQLField
    @GraphQLName("totalCount")
    @GraphQLDescription("Total number of matching export packages.")
    public int getTotalCount() {
        return totalCount;
    }

    @GraphQLField
    @GraphQLName("packages")
    @GraphQLDescription("List of matching packages")
    public List<ExportPackages> getExportPackages() {
        return results;
    }
}
