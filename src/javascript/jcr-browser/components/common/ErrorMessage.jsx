import React from 'react';
import PropTypes from 'prop-types';
import {Alert, AlertTitle, Box, Typography} from '@mui/material';

const ErrorMessage = ({error}) => {
    return (
        <Box sx={{m: 3}}>
            <Alert severity="error">
                <AlertTitle>Error Loading JCR Browser</AlertTitle>
                <Typography variant="body2">{error.message}</Typography>
                {error.networkError && (
                    <Typography variant="caption" color="text.secondary" sx={{mt: 2, display: 'block'}}>
                        Network error: {error.networkError.message}
                    </Typography>
                )}
                {error.graphQLErrors && error.graphQLErrors.length > 0 && (
                    <Box sx={{mt: 2}}>
                        <Typography variant="body2" fontWeight="bold">GraphQL Errors:</Typography>
                        <ul style={{marginTop: 8, marginBottom: 0, paddingLeft: 20}}>
                            {error.graphQLErrors.map((err, idx) => (
                                // eslint-disable-next-line react/no-array-index-key
                                <li key={idx}>
                                    <Typography variant="caption">{err.message}</Typography>
                                </li>
                            ))}
                        </ul>
                    </Box>
                )}
            </Alert>
        </Box>
    );
};

ErrorMessage.propTypes = {
    error: PropTypes.shape({
        message: PropTypes.string.isRequired,
        networkError: PropTypes.shape({
            message: PropTypes.string
        }),
        graphQLErrors: PropTypes.arrayOf(
            PropTypes.shape({
                message: PropTypes.string
            })
        )
    }).isRequired
};

export default ErrorMessage;
