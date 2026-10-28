import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate

JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
    Object doInJCR(JCRSessionWrapper session) {
        session.getNode("PARENT_PATH").addNode("FOLDER_NAME", "jnt:contentFolder")
        session.save()
        return null
    }
})
setResult("added")
