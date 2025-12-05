import {useState, useCallback, useEffect} from 'react';
import {useLazyQuery} from '@apollo/client/react';
import {GET_NODE_CHILDREN} from '../graphql/queries';

/**
 * Custom hook to manage tree data with lazy loading
 */
export const useTreeData = (rootNode, workspace) => {
    const [treeData, setTreeData] = useState([]);
    const [fetchChildren] = useLazyQuery(GET_NODE_CHILDREN);

    // Initialize with root node
    useEffect(() => {
        const initialData = transformNodeToTreeData(rootNode);
        setTreeData([initialData]);
    }, [rootNode]);

    // Load children for a node
    const loadChildren = useCallback(async nodeId => {
        try {
            const {data} = await fetchChildren({
                variables: {
                    uuid: nodeId,
                    workspace: workspace,
                    limit: 1000
                }
            });

            const children = data?.admin?.tools?.jcrBrowser?.node?.children || [];

            // Update tree data with loaded children
            setTreeData(prevData => updateTreeDataWithChildren(prevData, nodeId, children));

            return children.map(transformNodeToTreeData);
        } catch (error) {
            console.error('Error loading children:', error);
            return [];
        }
    }, [fetchChildren, workspace]);

    return {treeData, loadChildren};
};

// Transform a JCR node to tree data format
function transformNodeToTreeData(node) {
    return {
        id: node.uuid,
        name: node.name || '<root>',
        children: node.hasChildren ? [] : undefined,
        data: node
    };
}

// Update tree data by adding children to a specific node
function updateTreeDataWithChildren(treeData, parentId, children) {
    return treeData.map(node => {
        if (node.id === parentId) {
            return {
                ...node,
                children: children.map(transformNodeToTreeData)
            };
        }

        if (node.children && node.children.length > 0) {
            return {
                ...node,
                children: updateTreeDataWithChildren(node.children, parentId, children)
            };
        }

        return node;
    });
}
