package org.jahia.test.consumer.stopped;

import org.jahia.test.core.CoreService;

/**
 * This bundle will be kept in INSTALLED state (not started)
 * so it should have no active wires and should be excluded
 * from the {@link org.jahia.modules.tools.gql.admin.osgi.OSGiAnalyzer#getPackageWires(java.util.Map, boolean)} results
 */
public class StoppedConsumer {

    private CoreService coreService;

    public StoppedConsumer() {
        this.coreService = new CoreService();
    }

    public String getCoreServiceName() {
        return coreService.getName();
    }

}
