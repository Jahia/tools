import React, {useState, useCallback} from 'react';
import PropTypes from 'prop-types';
import {useQuery} from '@apollo/client/react';
import {
    Container,
    Box,
    Grid,
    Typography,
    Card,
    CardHeader,
    CardContent,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    Chip
} from '@mui/material';
import {GET_NODE} from '../graphql/queries';
import JcrTree from './tree/JcrTree';
import NodeInfoPanel from './details/NodeInfoPanel';
import PropertiesPanel from './details/PropertiesPanel';
import ActionsPanel from './details/ActionsPanel';
import Breadcrumb from './common/Breadcrumb';
import LoadingSpinner from './common/LoadingSpinner';
import ErrorMessage from './common/ErrorMessage';

const JcrBrowserApp = ({initialWorkspace, initialUuid}) => {
    const [currentWorkspace, setCurrentWorkspace] = useState(initialWorkspace);
    const [currentUuid, setCurrentUuid] = useState(initialUuid);

    const {loading, error, data} = useQuery(GET_NODE, {
        variables: {
            uuid: currentUuid,
            workspace: currentWorkspace
        }
    });

    const handleBreadcrumbNavigate = useCallback(path => {
        // Navigate to a node by path
        setCurrentUuid(null); // Will be set from path query
        // In a real implementation, you'd query by path
        // For now, just log
        console.log('Navigate to path:', path);
    }, []);

    const handleNodeDeleted = useCallback(deletedNode => {
        // Navigate to parent node after deletion
        console.log('Node deleted:', deletedNode);
        // In a real implementation, navigate to parent
        // For now, refresh the current view
        window.location.reload();
    }, []);

    const handleNodeRenamed = useCallback(renamedNode => {
        // Update the current UUID to reflect the renamed node
        console.log('Node renamed:', renamedNode);
        setCurrentUuid(renamedNode.uuid);
    }, []);

    if (loading) {
        return <LoadingSpinner/>;
    }

    if (error) {
        return <ErrorMessage error={error}/>;
    }

    const rootNode = data?.admin?.tools?.jcrBrowser?.node;

    return (
        <Box sx={{display: 'flex', flexDirection: 'column'}}>
            {/* Header Section - Fixed */}
            <Container maxWidth="xl" sx={{py: 3, flexShrink: 0}}>
                <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2}}>
                    <Typography variant="h4" component="h1">
                        JCR Browser
                    </Typography>
                    <Box sx={{display: 'flex', alignItems: 'center', gap: 2}}>
                        <Chip
                            label={currentWorkspace === 'default' ? 'Edit' : 'Live'}
                            color="secondary"
                            size="small"
                        />
                        <FormControl size="small" sx={{minWidth: 120}}>
                            <InputLabel id="workspace-select-label">Workspace</InputLabel>
                            <Select
                                labelId="workspace-select-label"
                                id="workspace-select"
                                value={currentWorkspace}
                                label="Workspace"
                                onChange={e => setCurrentWorkspace(e.target.value)}
                            >
                                <MenuItem value="default">Default</MenuItem>
                                <MenuItem value="live">Live</MenuItem>
                            </Select>
                        </FormControl>
                    </Box>
                </Box>
                {rootNode && (
                    <Breadcrumb
                        path={rootNode.path}
                        onNavigate={handleBreadcrumbNavigate}
                    />
                )}
            </Container>

            {/* Content Section - Flexible */}
            <Container maxWidth="xl">
                <Grid container spacing={3}>
                    {/* Left Column: Actions + Tree */}
                    <Grid item size={3}>
                        {rootNode && (
                            <>
                                {/* Actions Panel */}
                                <Box>
                                    <ActionsPanel
                                        node={rootNode}
                                        onNodeDeleted={handleNodeDeleted}
                                        onNodeRenamed={handleNodeRenamed}
                                    />
                                </Box>

                                {/* Tree */}
                                <Card sx={{flexGrow: 1, display: 'flex', flexDirection: 'column', minHeight: 0}}>
                                    <CardHeader title="Node Tree" titleTypographyProps={{variant: 'h6'}}/>
                                    <CardContent sx={{flexGrow: 1, overflow: 'auto', pt: 0, minHeight: 0}}>
                                        <JcrTree
                                            rootNode={rootNode}
                                            workspace={currentWorkspace}
                                            onNodeSelect={setCurrentUuid}
                                        />
                                    </CardContent>
                                </Card>
                            </>
                        )}
                    </Grid>

                    {/* Right Column: Node Info + Properties */}
                    <Grid item size="grow">
                        {rootNode ? (
                            <>
                                {/* Node Info Panel - Fixed height */}
                                <Box sx={{mb: 2, flexShrink: 0}}>
                                    <NodeInfoPanel node={rootNode}/>
                                </Box>

                                {/* Properties Panel - Takes remaining space */}
                                <Box sx={{flexGrow: 1, minHeight: 0}}>
                                    <PropertiesPanel properties={rootNode.properties || []}/>
                                </Box>
                            </>
                        ) : (
                            <Box sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center'
                            }}
                            >
                                <Typography variant="body1" color="text.secondary">
                                    Select a node to view details
                                </Typography>
                            </Box>
                        )}
                    </Grid>
                </Grid>
            </Container>
        </Box>
    );
};

JcrBrowserApp.propTypes = {
    initialWorkspace: PropTypes.string.isRequired,
    initialUuid: PropTypes.string.isRequired
};

export default JcrBrowserApp;
