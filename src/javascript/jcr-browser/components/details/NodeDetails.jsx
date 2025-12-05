import React from 'react';
import PropTypes from 'prop-types';
import {Alert, Box} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import NodeInfoPanel from './NodeInfoPanel';
import PropertiesPanel from './PropertiesPanel';
import ActionsPanel from './ActionsPanel';

const NodeDetails = ({node, onNodeDeleted, onNodeRenamed}) => {
    if (!node) {
        return (
            <Box sx={{p: 2}}>
                <Alert severity="info" icon={<InfoIcon/>}>
                    Select a node to view details
                </Alert>
            </Box>
        );
    }

    return (
        <Box>
            <NodeInfoPanel node={node}/>
            <ActionsPanel
                node={node}
                onNodeDeleted={onNodeDeleted}
                onNodeRenamed={onNodeRenamed}
            />
            <PropertiesPanel properties={node.properties || []}/>
        </Box>
    );
};

NodeDetails.propTypes = {
    node: PropTypes.shape({
        uuid: PropTypes.string.isRequired,
        name: PropTypes.string,
        path: PropTypes.string.isRequired,
        primaryNodeType: PropTypes.string.isRequired,
        mixinNodeTypes: PropTypes.arrayOf(PropTypes.string),
        workspace: PropTypes.string.isRequired,
        depth: PropTypes.number.isRequired,
        childrenCount: PropTypes.number.isRequired,
        locked: PropTypes.bool.isRequired,
        versionable: PropTypes.bool.isRequired,
        properties: PropTypes.array
    }),
    onNodeDeleted: PropTypes.func,
    onNodeRenamed: PropTypes.func
};

export default NodeDetails;
