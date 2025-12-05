import React, {useState, useCallback, useMemo} from 'react';
import PropTypes from 'prop-types';
import {Tree} from 'react-arborist';

const JcrTreeArborist = ({rootNode, onNodeSelect}) => {
    const [selectedId, setSelectedId] = useState(rootNode.uuid);

    // Transform root node to tree data format
    const data = useMemo(() => {
        return [{
            id: rootNode.uuid,
            name: rootNode.name || '<root>',
            children: rootNode.hasChildren ? null : undefined, // Null means not loaded, undefined means no children
            data: rootNode
        }];
    }, [rootNode]);

    const handleSelect = useCallback(nodes => {
        if (nodes && nodes.length > 0) {
            const node = nodes[0];
            setSelectedId(node.id);
            onNodeSelect(node.id);
        }
    }, [onNodeSelect]);

    const handleMove = useCallback(() => {
        // Disable drag and drop for now

    }, []);

    return (
        <div className="jcr-tree-arborist" style={{height: '600px'}}>
            <Tree
                disableDrop
                disableDrag
                data={data}
                openByDefault={false}
                width="100%"
                height={600}
                indent={24}
                rowHeight={32}
                overscanCount={10}
                selection={selectedId}
                onSelect={handleSelect}
                onMove={handleMove}
            >
                {Node}
            </Tree>
        </div>
    );
};

JcrTreeArborist.propTypes = {
    rootNode: PropTypes.shape({
        uuid: PropTypes.string.isRequired,
        name: PropTypes.string,
        path: PropTypes.string.isRequired,
        hasChildren: PropTypes.bool.isRequired
    }).isRequired,
    onNodeSelect: PropTypes.func.isRequired
};

// Custom Node renderer
const Node = ({node, style, dragHandle}) => {
    const nodeData = node.data.data;
    const hasChildren = nodeData.hasChildren;

    return (
        <div
            ref={dragHandle}
            style={style}
            className={`tree-node-item ${node.state.isSelected ? 'selected' : ''}`}
        >
            <div className="tree-node-content d-flex align-items-center">
                {hasChildren && (
                    <button
                        type="button"
                        className="btn btn-link btn-sm p-0 me-2 tree-toggle"
                        aria-label={node.isOpen ? 'Collapse' : 'Expand'}
                        onClick={e => {
                            e.stopPropagation();
                            node.toggle();
                        }}
                    >
                        {node.isOpen ? '▼' : '▶'}
                    </button>
                )}
                {!hasChildren && (
                    <span className="me-2" style={{width: '20px', display: 'inline-block'}}/>
                )}
                <span className="tree-node-label">
                    {nodeData.locked && '🔒 '}
                    {node.data.name}
                    <span className="text-muted small ms-2">({nodeData.primaryNodeType})</span>
                </span>
            </div>
        </div>
    );
};

Node.propTypes = {
    node: PropTypes.object.isRequired,
    style: PropTypes.object.isRequired,
    dragHandle: PropTypes.func
};

export default JcrTreeArborist;
