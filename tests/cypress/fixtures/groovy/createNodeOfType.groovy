import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate

// Try to create a node of NODE_TYPE directly through a JCR session (as the Groovy
// console does), i.e. at the Jackrabbit level, bypassing the higher-level APIs that
// validate against the in-memory NodeTypeRegistry. This tells us whether a node type
// that was deleted from the definitions browser can still be instantiated.
// See https://github.com/Jahia/tools/issues/233
// The node is added transiently (not saved) so nothing is persisted.
String outcome
try {
    JCRTemplate.getInstance().doExecuteWithSystemSession({ JCRSessionWrapper session ->
        session.getNode('/sites/SITE_KEY').addNode('NODE_NAME', 'NODE_TYPE')
        return null
    } as JCRCallback)
    outcome = 'CREATED'
} catch (Throwable e) {
    Throwable root = e
    while (root.getCause() != null) {
        root = root.getCause()
    }
    outcome = root.getClass().getName()
}
setResult(outcome)
