import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {useMutation} from '@apollo/client/react';
import {
    Card,
    CardHeader,
    CardContent,
    Button,
    ButtonGroup,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Typography,
    Chip,
    Box
} from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import EditIcon from '@mui/icons-material/Edit';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import CloseIcon from '@mui/icons-material/Close';
import {
    LOCK_NODE,
    UNLOCK_NODE,
    DELETE_NODE,
    RENAME_NODE,
    ADD_MIXIN,
    REMOVE_MIXIN
} from '../../graphql/mutations';
import {GET_NODE} from '../../graphql/queries';
import ConfirmDialog from '../common/ConfirmDialog';
import Notification from '../common/Notification';

// eslint-disable-next-line complexity
const ActionsPanel = ({node, onNodeDeleted, onNodeRenamed}) => {
    const [confirmDialog, setConfirmDialog] = useState({isOpen: false, action: null});
    const [renameDialog, setRenameDialog] = useState({isOpen: false, newName: ''});
    const [mixinDialog, setMixinDialog] = useState({isOpen: false, mixinType: '', action: 'add'});
    const [notification, setNotification] = useState(null);

    // Mutations
    const [lockNode, {loading: lockLoading}] = useMutation(LOCK_NODE, {
        refetchQueries: [{query: GET_NODE, variables: {uuid: node.uuid, workspace: node.workspace}}],
        onCompleted: () => showNotification('success', 'Node locked successfully'),
        onError: error => showNotification('error', `Failed to lock node: ${error.message}`)
    });

    const [unlockNode, {loading: unlockLoading}] = useMutation(UNLOCK_NODE, {
        refetchQueries: [{query: GET_NODE, variables: {uuid: node.uuid, workspace: node.workspace}}],
        onCompleted: () => showNotification('success', 'Node unlocked successfully'),
        onError: error => showNotification('error', `Failed to unlock node: ${error.message}`)
    });

    const [deleteNode, {loading: deleteLoading}] = useMutation(DELETE_NODE, {
        onCompleted: () => {
            showNotification('success', 'Node deleted successfully');
            if (onNodeDeleted) {
                onNodeDeleted(node);
            }
        },
        onError: error => showNotification('error', `Failed to delete node: ${error.message}`)
    });

    const [renameNode, {loading: renameLoading}] = useMutation(RENAME_NODE, {
        refetchQueries: [{query: GET_NODE, variables: {uuid: node.uuid, workspace: node.workspace}}],
        onCompleted: data => {
            const renamedNode = data.admin.tools.jcrBrowser.renameNode;
            showNotification('success', `Node renamed to "${renamedNode.name}"`);
            if (onNodeRenamed) {
                onNodeRenamed(renamedNode);
            }
        },
        onError: error => showNotification('error', `Failed to rename node: ${error.message}`)
    });

    const [addMixin, {loading: addMixinLoading}] = useMutation(ADD_MIXIN, {
        refetchQueries: [{query: GET_NODE, variables: {uuid: node.uuid, workspace: node.workspace}}],
        onCompleted: () => showNotification('success', 'Mixin type added successfully'),
        onError: error => showNotification('error', `Failed to add mixin: ${error.message}`)
    });

    const [removeMixin, {loading: removeMixinLoading}] = useMutation(REMOVE_MIXIN, {
        refetchQueries: [{query: GET_NODE, variables: {uuid: node.uuid, workspace: node.workspace}}],
        onCompleted: () => showNotification('success', 'Mixin type removed successfully'),
        onError: error => showNotification('error', `Failed to remove mixin: ${error.message}`)
    });

    const showNotification = (type, message) => {
        setNotification({type, message});
    };

    const handleLockClick = () => {
        if (node.locked) {
            setConfirmDialog({
                isOpen: true,
                action: 'unlock',
                title: 'Unlock Node',
                message: `Are you sure you want to unlock "${node.name || node.path}"?`,
                variant: 'primary'
            });
        } else {
            lockNode({variables: {uuid: node.uuid, workspace: node.workspace}});
        }
    };

    const handleDeleteClick = () => {
        setConfirmDialog({
            isOpen: true,
            action: 'delete',
            title: 'Delete Node',
            message: `Are you sure you want to delete "${node.name || node.path}"? This action cannot be undone.`,
            variant: 'danger'
        });
    };

    const handleRenameClick = () => {
        setRenameDialog({isOpen: true, newName: node.name || ''});
    };

    const handleMixinAddClick = () => {
        setMixinDialog({isOpen: true, mixinType: '', action: 'add'});
    };

    const handleMixinRemoveClick = mixinType => {
        setMixinDialog({isOpen: true, mixinType, action: 'remove'});
    };

    const handleConfirmAction = () => {
        const {action} = confirmDialog;
        setConfirmDialog({isOpen: false, action: null});

        if (action === 'unlock') {
            unlockNode({variables: {uuid: node.uuid, workspace: node.workspace}});
        } else if (action === 'delete') {
            deleteNode({variables: {uuid: node.uuid, workspace: node.workspace}});
        }
    };

    const handleRenameSubmit = e => {
        e.preventDefault();
        const newName = renameDialog.newName.trim();
        if (newName && newName !== node.name) {
            renameNode({
                variables: {
                    uuid: node.uuid,
                    newName,
                    workspace: node.workspace
                }
            });
        }

        setRenameDialog({isOpen: false, newName: ''});
    };

    const handleMixinSubmit = e => {
        e.preventDefault();
        const {mixinType, action} = mixinDialog;
        if (mixinType.trim()) {
            if (action === 'add') {
                addMixin({
                    variables: {
                        uuid: node.uuid,
                        mixinType,
                        workspace: node.workspace
                    }
                });
            } else {
                removeMixin({
                    variables: {
                        uuid: node.uuid,
                        mixinType,
                        workspace: node.workspace
                    }
                });
            }
        }

        setMixinDialog({isOpen: false, mixinType: '', action: 'add'});
    };

    const isLoading = lockLoading || unlockLoading || deleteLoading || renameLoading ||
        addMixinLoading || removeMixinLoading;

    // Don't allow actions on root node
    const isRootNode = node.depth === 0;

    return (
        <Card sx={{mb: 3}}>
            <CardHeader
                avatar={<SettingsIcon/>}
                title="Actions"
                titleTypographyProps={{variant: 'h6'}}
            />
            <CardContent>
                <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1}}>
                    <ButtonGroup variant="outlined" size="small">
                        <Button
                            startIcon={node.locked ? <LockOpenIcon/> : <LockIcon/>}
                            disabled={isLoading}
                            color={node.locked ? 'warning' : 'inherit'}
                            onClick={handleLockClick}
                        >
                            {node.locked ? 'Unlock' : 'Lock'}
                        </Button>
                        <Button
                            startIcon={<EditIcon/>}
                            disabled={isLoading || isRootNode || node.locked}
                            onClick={handleRenameClick}
                        >
                            Rename
                        </Button>
                    </ButtonGroup>
                    <Button
                        variant="outlined"
                        size="small"
                        startIcon={<AddIcon/>}
                        disabled={isLoading || node.locked}
                        color="primary"
                        onClick={handleMixinAddClick}
                    >
                        Add Mixin
                    </Button>
                    <Button
                        variant="outlined"
                        size="small"
                        startIcon={<DeleteIcon/>}
                        disabled={isLoading || isRootNode || node.locked}
                        color="error"
                        onClick={handleDeleteClick}
                    >
                        Delete
                    </Button>
                </Box>

                {node.mixinNodeTypes && node.mixinNodeTypes.length > 0 && (
                    <Box sx={{mt: 3}}>
                        <Typography gutterBottom variant="caption" color="text.secondary" display="block">
                            Current Mixins (click to remove)
                        </Typography>
                        <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1}}>
                            {node.mixinNodeTypes.map(mixin => (
                                <Chip
                                    key={mixin}
                                    label={mixin}
                                    deleteIcon={<CloseIcon/>}
                                    disabled={isLoading || node.locked}
                                    color="info"
                                    size="small"
                                    title="Click to remove this mixin"
                                    onDelete={() => handleMixinRemoveClick(mixin)}
                                />
                            ))}
                        </Box>
                    </Box>
                )}
            </CardContent>

            {/* Confirm Dialog */}
            <ConfirmDialog
                isOpen={confirmDialog.isOpen}
                title={confirmDialog.title}
                message={confirmDialog.message}
                variant={confirmDialog.variant}
                onConfirm={handleConfirmAction}
                onCancel={() => setConfirmDialog({isOpen: false, action: null})}
            />

            {/* Rename Dialog */}
            <Dialog
                fullWidth
                open={renameDialog.isOpen}
                maxWidth="sm"
                onClose={() => setRenameDialog({isOpen: false, newName: ''})}
            >
                <form onSubmit={handleRenameSubmit}>
                    <DialogTitle>Rename Node</DialogTitle>
                    <DialogContent>
                        <TextField
                            autoFocus
                            required
                            fullWidth
                            margin="dense"
                            label="New Name"
                            type="text"
                            value={renameDialog.newName}
                            onChange={e => setRenameDialog({...renameDialog, newName: e.target.value})}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setRenameDialog({isOpen: false, newName: ''})}>
                            Cancel
                        </Button>
                        <Button type="submit" variant="contained">
                            Rename
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            {/* Mixin Dialog */}
            <Dialog
                fullWidth
                open={mixinDialog.isOpen}
                maxWidth="sm"
                onClose={() => setMixinDialog({isOpen: false, mixinType: '', action: 'add'})}
            >
                <form onSubmit={handleMixinSubmit}>
                    <DialogTitle>
                        {mixinDialog.action === 'add' ? 'Add Mixin Type' : 'Remove Mixin Type'}
                    </DialogTitle>
                    <DialogContent>
                        <TextField
                            required
                            fullWidth
                            autoFocus={mixinDialog.action === 'add'}
                            margin="dense"
                            label="Mixin Type"
                            type="text"
                            value={mixinDialog.mixinType}
                            placeholder="e.g., mix:versionable, jmix:lastPublished"
                            disabled={mixinDialog.action === 'remove'}
                            helperText={mixinDialog.action === 'remove' ? 'This will remove the mixin type from the node' : ''}
                            error={mixinDialog.action === 'remove'}
                            onChange={e => setMixinDialog({...mixinDialog, mixinType: e.target.value})}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setMixinDialog({isOpen: false, mixinType: '', action: 'add'})}>
                            Cancel
                        </Button>
                        <Button
                            type="submit"
                            variant="contained"
                            color={mixinDialog.action === 'remove' ? 'error' : 'primary'}
                        >
                            {mixinDialog.action === 'add' ? 'Add' : 'Remove'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            {/* Notification */}
            {notification && (
                <Notification
                    type={notification.type}
                    message={notification.message}
                    onClose={() => setNotification(null)}
                />
            )}
        </Card>
    );
};

ActionsPanel.propTypes = {
    node: PropTypes.shape({
        uuid: PropTypes.string.isRequired,
        name: PropTypes.string,
        path: PropTypes.string.isRequired,
        workspace: PropTypes.string.isRequired,
        depth: PropTypes.number.isRequired,
        locked: PropTypes.bool.isRequired,
        mixinNodeTypes: PropTypes.arrayOf(PropTypes.string)
    }).isRequired,
    onNodeDeleted: PropTypes.func,
    onNodeRenamed: PropTypes.func
};

export default ActionsPanel;
