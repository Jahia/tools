package org.jahia.test.consumer.nomatches;

import org.jahia.test.other.OtherService;

public class NoMatchesConsumer {

    private OtherService otherService;

    public NoMatchesConsumer() {
        this.otherService = new OtherService();
    }

    public String getMessage() {
        return otherService.getMessage();
    }
}

