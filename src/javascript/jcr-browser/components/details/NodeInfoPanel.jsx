import React from 'react';
import PropTypes from 'prop-types';
import {
    Card,
    CardHeader,
    CardContent,
    Grid,
    Typography,
    Chip,
    Box
} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import LockIcon from '@mui/icons-material/Lock';
import HistoryIcon from '@mui/icons-material/History';

const NodeInfoPanel = ({node}) => {
    return (
        <Card sx={{mb: 3}}>
            <CardHeader
                avatar={<InfoIcon/>}
                title="Node Information"
                titleTypographyProps={{variant: 'h6'}}
            />
            <CardContent>
                <Grid container spacing={3}>
                    <Grid item xs={12} md={6}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Name
                        </Typography>
                        <Typography variant="body1" fontWeight="bold">
                            {node.name || '<root>'}
                        </Typography>
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            UUID
                        </Typography>
                        <Typography variant="body2" fontFamily="monospace">
                            {node.uuid}
                        </Typography>
                    </Grid>
                </Grid>

                <Box sx={{mt: 3}}>
                    <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                        Path
                    </Typography>
                    <Typography variant="body2" fontFamily="monospace">
                        {node.path}
                    </Typography>
                </Box>

                <Grid container spacing={3} sx={{mt: 1}}>
                    <Grid item xs={12} md={6}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Primary Type
                        </Typography>
                        <Chip label={node.primaryNodeType} color="primary" size="small"/>
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Workspace
                        </Typography>
                        <Chip
                            label={node.workspace === 'default' ? 'Edit' : 'Live'}
                            color="secondary"
                            size="small"
                        />
                    </Grid>
                </Grid>

                {node.mixinNodeTypes && node.mixinNodeTypes.length > 0 && (
                    <Box sx={{mt: 3}}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Mixin Types
                        </Typography>
                        <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1}}>
                            {node.mixinNodeTypes.map(mixin => (
                                <Chip key={mixin} label={mixin} color="info" size="small"/>
                            ))}
                        </Box>
                    </Box>
                )}

                <Grid container spacing={3} sx={{mt: 1}}>
                    <Grid item xs={4}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Depth
                        </Typography>
                        <Typography variant="body2">{node.depth}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Children
                        </Typography>
                        <Typography variant="body2">{node.childrenCount}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Status
                        </Typography>
                        <Box sx={{display: 'flex', gap: 0.5, flexWrap: 'wrap'}}>
                            {node.locked && (
                                <Chip
                                    icon={<LockIcon/>}
                                    label="Locked"
                                    color="warning"
                                    size="small"
                                />
                            )}
                            {node.versionable && (
                                <Chip
                                    icon={<HistoryIcon/>}
                                    label="Versionable"
                                    color="success"
                                    size="small"
                                />
                            )}
                            {!node.locked && !node.versionable && (
                                <Typography variant="body2" color="text.secondary">
                                    Normal
                                </Typography>
                            )}
                        </Box>
                    </Grid>
                </Grid>
            </CardContent>
        </Card>
    );
};

NodeInfoPanel.propTypes = {
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
        versionable: PropTypes.bool.isRequired
    }).isRequired
};

export default NodeInfoPanel;
