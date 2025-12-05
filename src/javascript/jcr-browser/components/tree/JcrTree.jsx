import React, {useState, useCallback} from 'react';
import PropTypes from 'prop-types';
import {useQuery} from '@apollo/client/react';
import {SimpleTreeView} from '@mui/x-tree-view/SimpleTreeView';
import {TreeItem} from '@mui/x-tree-view/TreeItem';
import {Box, Typography, CircularProgress} from '@mui/material';
import FolderIcon from '@mui/icons-material/Folder';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import DescriptionIcon from '@mui/icons-material/Description';
import LockIcon from '@mui/icons-material/Lock';
import {GET_NODE_CHILDREN} from '../../graphql/queries';

const JcrTree = ({rootNode, workspace, onNodeSelect}) => {
    const [expandedItems, setExpandedItems] = useState([rootNode.uuid]);
    const [selectedItem, setSelectedItem] = useState(rootNode.uuid);

    const handleExpandedItemsChange = useCallback((event, itemIds) => {
        setExpandedItems(itemIds);
    }, []);

    const handleSelectedItemsChange = useCallback((event, itemId) => {
        if (itemId) {
            setSelectedItem(itemId);
            onNodeSelect(itemId);
        }
    }, [onNodeSelect]);

    return (
        <SimpleTreeView
            expandedItems={expandedItems}
            selectedItems={selectedItem}
            sx={{
                height: '100%',
                width: '100%',
                '& .MuiTreeItem-content': {
                    padding: '4px 8px',
                    borderRadius: 1,
                    '&:hover': {
                        backgroundColor: 'action.hover'
                    },
                    '&.Mui-selected': {
                        backgroundColor: 'primary.light',
                        '&:hover': {
                            backgroundColor: 'primary.light'
                        },
                        '&.Mui-focused': {
                            backgroundColor: 'primary.main',
                            color: 'primary.contrastText'
                        }
                    }
                }
            }}
            onExpandedItemsChange={handleExpandedItemsChange}
            onSelectedItemsChange={handleSelectedItemsChange}
        >
            <JcrTreeNode
                node={rootNode}
                workspace={workspace}
                expandedItems={expandedItems}
            />
        </SimpleTreeView>
    );
};

JcrTree.propTypes = {
    rootNode: PropTypes.shape({
        uuid: PropTypes.string.isRequired,
        name: PropTypes.string,
        path: PropTypes.string.isRequired,
        primaryNodeType: PropTypes.string.isRequired,
        hasChildren: PropTypes.bool.isRequired,
        locked: PropTypes.bool
    }).isRequired,
    workspace: PropTypes.string.isRequired,
    onNodeSelect: PropTypes.func.isRequired
};

const JcrTreeNode = ({node, workspace, expandedItems}) => {
    const isExpanded = expandedItems.includes(node.uuid);
    const hasChildren = node.hasChildren;

    const {data, loading} = useQuery(GET_NODE_CHILDREN, {
        variables: {
            uuid: node.uuid,
            workspace: workspace,
            limit: 100
        },
        skip: !isExpanded || !hasChildren
    });

    const children = data?.admin?.tools?.jcrBrowser?.node?.children || [];

    // Determine icon
    const getIcon = () => {
        if (!hasChildren) {
            return <DescriptionIcon sx={{fontSize: 18}}/>;
        }

        return isExpanded ? <FolderOpenIcon sx={{fontSize: 18}}/> : <FolderIcon sx={{fontSize: 18}}/>;
    };

    // Build label with node info
    const label = (
        <Box sx={{display: 'flex', alignItems: 'center', gap: 1, py: 0.5}}>
            {getIcon()}
            {node.locked && <LockIcon sx={{fontSize: 16, color: 'warning.main'}}/>}
            <Typography variant="body2" sx={{flexGrow: 1}}>
                {node.name || '<root>'}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{fontFamily: 'monospace'}}>
                {node.primaryNodeType}
            </Typography>
        </Box>
    );

    return (
        <TreeItem
            itemId={node.uuid}
            label={label}
        >
            {hasChildren && isExpanded && (
                <>
                    {loading && (
                        <Box sx={{display: 'flex', alignItems: 'center', gap: 1, p: 1, pl: 4}}>
                            <CircularProgress size={16}/>
                            <Typography variant="caption" color="text.secondary">
                                Loading...
                            </Typography>
                        </Box>
                    )}
                    {!loading && children.map(child => (
                        <JcrTreeNode
                            key={child.uuid}
                            node={child}
                            workspace={workspace}
                            expandedItems={expandedItems}
                        />
                    ))}
                </>
            )}
        </TreeItem>
    );
};

JcrTreeNode.propTypes = {
    node: PropTypes.shape({
        uuid: PropTypes.string.isRequired,
        name: PropTypes.string,
        path: PropTypes.string.isRequired,
        primaryNodeType: PropTypes.string.isRequired,
        locked: PropTypes.bool,
        hasChildren: PropTypes.bool.isRequired
    }).isRequired,
    workspace: PropTypes.string.isRequired,
    expandedItems: PropTypes.arrayOf(PropTypes.string).isRequired
};

export default JcrTree;
