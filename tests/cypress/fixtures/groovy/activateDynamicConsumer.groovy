import org.jahia.osgi.BundleUtils

// Activate the service by using it
var service = BundleUtils.getOsgiService("org.jahia.test.consumer.dynamic.DynamicConsumer")
setResult(service.getCoreServiceName())
