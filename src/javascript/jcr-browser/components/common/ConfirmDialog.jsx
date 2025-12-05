import React from 'react';
import PropTypes from 'prop-types';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogContentText,
    DialogActions,
    Button
} from '@mui/material';

const ConfirmDialog = ({isOpen, title, message, confirmText, cancelText, onConfirm, onCancel, variant}) => {
    const buttonColor = variant === 'danger' ? 'error' : 'primary';

    return (
        <Dialog
            open={isOpen}
            aria-labelledby="confirm-dialog-title"
            aria-describedby="confirm-dialog-description"
            onClose={onCancel}
        >
            <DialogTitle id="confirm-dialog-title">
                {title}
            </DialogTitle>
            <DialogContent>
                <DialogContentText id="confirm-dialog-description">
                    {message}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button color="inherit" onClick={onCancel}>
                    {cancelText || 'Cancel'}
                </Button>
                <Button autoFocus color={buttonColor} variant="contained" onClick={onConfirm}>
                    {confirmText || 'Confirm'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

ConfirmDialog.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    title: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    confirmText: PropTypes.string,
    cancelText: PropTypes.string,
    onConfirm: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    variant: PropTypes.oneOf(['primary', 'danger'])
};

ConfirmDialog.defaultProps = {
    confirmText: 'Confirm',
    cancelText: 'Cancel',
    variant: 'primary'
};

export default ConfirmDialog;
