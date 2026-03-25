package org.jahia.test.consumer.optional;

import org.jahia.test.core.CoreService;

public class OptionalConsumer {

    private CoreService coreService;

    public OptionalConsumer() {
        this.coreService = new CoreService();
    }

    public String getCoreServiceName() {
        return coreService.getName();
    }

    /**
     * This module imports org.jahia.test.nonexistent as optional,
     * so it should start even though that package doesn't exist
     */
}

