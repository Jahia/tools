package org.jahia.modules.tools.gql.admin.jcr;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;
import org.jahia.modules.tools.gql.admin.jcr.types.GqlJcrNode;
import org.jahia.services.content.JCRSessionFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/**
 * GraphQL mutations for JCR Browser operations
 */
@GraphQLName("JcrBrowserMutation")
@GraphQLDescription("JCR Browser mutation operations")
public class JcrBrowserMutations {

    /**
     * Lock a node
     */
    @GraphQLField
    @GraphQLDescription("Lock a node")
    public GqlJcrNode lockNode(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            if (node.isLocked()) {
                throw new GqlJcrWrongInputException("Node is already locked");
            }

            // Lock the node (deep=false, session-scoped=true)
            node.getSession().getWorkspace().getLockManager().lock(
                node.getPath(),
                false,  // isDeep
                true,   // isSessionScoped
                Long.MAX_VALUE, // timeoutHint
                null    // ownerInfo
            );

            session.save();
            return new GqlJcrNode(node, workspace);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Unlock a node
     */
    @GraphQLField
    @GraphQLDescription("Unlock a node")
    public GqlJcrNode unlockNode(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            if (!node.isLocked()) {
                throw new GqlJcrWrongInputException("Node is not locked");
            }

            // Unlock the node
            node.getSession().getWorkspace().getLockManager().unlock(node.getPath());

            session.save();
            return new GqlJcrNode(node, workspace);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Delete a node
     */
    @GraphQLField
    @GraphQLDescription("Delete a node")
    public Boolean deleteNode(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            // Don't allow deleting root node
            if (node.getDepth() == 0) {
                throw new GqlJcrWrongInputException("Cannot delete root node");
            }

            // Check if node is locked
            if (node.isLocked()) {
                throw new LockException("Cannot delete locked node");
            }

            node.remove();
            session.save();
            return true;
        } catch (Exception e) {
            if (session != null) {
                session.refresh(false); // Rollback
            }
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Rename a node
     */
    @GraphQLField
    @GraphQLDescription("Rename a node")
    public GqlJcrNode renameNode(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("newName") @GraphQLDescription("New node name") String newName,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            // Validate new name
            if (newName == null || newName.trim().isEmpty()) {
                throw new GqlJcrWrongInputException("New name cannot be empty");
            }

            // Don't allow renaming root node
            if (node.getDepth() == 0) {
                throw new GqlJcrWrongInputException("Cannot rename root node");
            }

            // Check if node is locked
            if (node.isLocked()) {
                throw new LockException("Cannot rename locked node");
            }

            // Check if sibling with same name exists
            Node parent = node.getParent();
            if (parent.hasNode(newName)) {
                throw new GqlJcrWrongInputException("A node with name '" + newName + "' already exists");
            }

            // Rename by moving to new path
            String parentPath = node.getParent().getPath();
            String newPath = parentPath.equals("/") ? "/" + newName : parentPath + "/" + newName;
            session.move(node.getPath(), newPath);

            session.save();

            // Get the renamed node
            Node renamedNode = session.getNode(newPath);
            return new GqlJcrNode(renamedNode, workspace);
        } catch (Exception e) {
            if (session != null) {
                session.refresh(false); // Rollback
            }
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Add a mixin type to a node
     */
    @GraphQLField
    @GraphQLDescription("Add a mixin type to a node")
    public GqlJcrNode addMixin(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("mixinType") @GraphQLDescription("Mixin type name") String mixinType,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            // Check if node is locked
            if (node.isLocked()) {
                throw new LockException("Cannot modify locked node");
            }

            // Check if mixin already exists
            if (node.isNodeType(mixinType)) {
                throw new GqlJcrWrongInputException("Node already has mixin type: " + mixinType);
            }

            node.addMixin(mixinType);
            session.save();

            return new GqlJcrNode(node, workspace);
        } catch (Exception e) {
            if (session != null) {
                session.refresh(false); // Rollback
            }
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Remove a mixin type from a node
     */
    @GraphQLField
    @GraphQLDescription("Remove a mixin type from a node")
    public GqlJcrNode removeMixin(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("mixinType") @GraphQLDescription("Mixin type name") String mixinType,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            // Check if node is locked
            if (node.isLocked()) {
                throw new LockException("Cannot modify locked node");
            }

            // Check if mixin exists
            if (!node.isNodeType(mixinType)) {
                throw new GqlJcrWrongInputException("Node does not have mixin type: " + mixinType);
            }

            node.removeMixin(mixinType);
            session.save();

            return new GqlJcrNode(node, workspace);
        } catch (Exception e) {
            if (session != null) {
                session.refresh(false); // Rollback
            }
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Set a property value on a node
     */
    @GraphQLField
    @GraphQLDescription("Set a property value on a node")
    public GqlJcrNode setProperty(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("propertyName") @GraphQLDescription("Property name") String propertyName,
            @GraphQLName("propertyValue") @GraphQLDescription("Property value") String propertyValue,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            // Check if node is locked
            if (node.isLocked()) {
                throw new LockException("Cannot modify locked node");
            }

            node.setProperty(propertyName, propertyValue);
            session.save();

            return new GqlJcrNode(node, workspace);
        } catch (Exception e) {
            if (session != null) {
                session.refresh(false); // Rollback
            }
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Remove a property from a node
     */
    @GraphQLField
    @GraphQLDescription("Remove a property from a node")
    public GqlJcrNode removeProperty(
            @GraphQLName("uuid") @GraphQLDescription("Node UUID") String uuid,
            @GraphQLName("propertyName") @GraphQLDescription("Property name") String propertyName,
            @GraphQLName("workspace") @GraphQLDescription("Workspace name") String workspace
    ) throws RepositoryException {
        Session session = null;
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession(workspace);
            Node node = session.getNodeByIdentifier(uuid);

            // Check if node is locked
            if (node.isLocked()) {
                throw new LockException("Cannot modify locked node");
            }

            // Check if property exists
            if (!node.hasProperty(propertyName)) {
                throw new GqlJcrWrongInputException("Property does not exist: " + propertyName);
            }

            node.getProperty(propertyName).remove();
            session.save();

            return new GqlJcrNode(node, workspace);
        } catch (Exception e) {
            if (session != null) {
                session.refresh(false); // Rollback
            }
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
}
