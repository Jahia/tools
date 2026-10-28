package org.jahia.test.consumer.names;

import org.jahia.test.names.NamesService;

public class NamesConsumer {

    public String getMessage() {
        return new NamesService().getName();
    }
}
