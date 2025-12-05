import React from 'react';
import PropTypes from 'prop-types';
import {Snackbar, Alert} from '@mui/material';

const Notification = ({type, message, onClose, duration}) => {
    return (
        <Snackbar
            open
            autoHideDuration={duration}
            anchorOrigin={{vertical: 'top', horizontal: 'right'}}
            onClose={onClose}
        >
            <Alert
                severity={type}
                variant="filled"
                sx={{width: '100%'}}
                onClose={onClose}
            >
                {message}
            </Alert>
        </Snackbar>
    );
};

Notification.propTypes = {
    type: PropTypes.oneOf(['success', 'error', 'warning', 'info']).isRequired,
    message: PropTypes.string.isRequired,
    onClose: PropTypes.func.isRequired,
    duration: PropTypes.number
};

Notification.defaultProps = {
    duration: 5000 // Auto-close after 5 seconds by default
};

export default Notification;
