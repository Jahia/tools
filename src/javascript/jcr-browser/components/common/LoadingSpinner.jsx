import React from 'react';
import {Box, CircularProgress, Typography} from '@mui/material';

const LoadingSpinner = () => {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: 5
            }}
        >
            <CircularProgress/>
            <Typography variant="body1" sx={{mt: 2}}>
                Loading JCR Browser...
            </Typography>
        </Box>
    );
};

export default LoadingSpinner;
