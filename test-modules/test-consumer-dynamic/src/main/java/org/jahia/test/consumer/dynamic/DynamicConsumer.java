package org.jahia.test.consumer.dynamic;

import org.jahia.test.core.CoreService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DynamicConsumer.class)
public class DynamicConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DynamicConsumer.class);

    private CoreService coreService;

    public DynamicConsumer() {
        this.coreService = new CoreService();
    }

    public String getCoreServiceName() {
        return coreService.getName();
    }

    @Activate
    public void activate() {
        try {
            // dynamic import to load CoreUtils and DateUtils
            // those 2 packages are declared as "DynamicImport-Package" in the MANIFEST
            Class.forName("org.jahia.test.core.util.CoreUtils");
            Class.forName("org.jahia.test.util.DateUtils");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Dynamic class not available: " + e.getMessage(), e);
        }
        logger.info("Dynamic consumer activated with dynamic imports");
    }

}

