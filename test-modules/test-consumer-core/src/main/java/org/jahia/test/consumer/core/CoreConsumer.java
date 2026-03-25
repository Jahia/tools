package org.jahia.test.consumer.core;

import org.jahia.test.core.CoreService;
import org.jahia.test.core.api.CoreApi;

public class CoreConsumer implements CoreApi {

    private CoreService coreService;

    public CoreConsumer() {
        this.coreService = new CoreService();
    }

    @Override
    public String getVersion() {
        return "1.0.0 - " + coreService.getName();
    }
}

