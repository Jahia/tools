import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate

JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
    Object doInJCR(JCRSessionWrapper session) {
        def parent = session.getNode("PARENT_PATH")
        def target = parent.addNode("target", "jnt:text")
        def reference = parent.addNode("reference", "jnt:contentReference")
        session.save()
        reference.setProperty("j:node", target)
        session.save()
        target.remove()
        session.save()
        return null
    }
})
setResult("added")
