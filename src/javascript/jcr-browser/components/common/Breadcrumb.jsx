import React from 'react';
import PropTypes from 'prop-types';
import {Breadcrumbs, Link, Typography, Box} from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';

const Breadcrumb = ({path, onNavigate}) => {
    if (!path || path === '/') {
        return (
            <Box sx={{mb: 3}}>
                <Breadcrumbs aria-label="breadcrumb">
                    <Typography color="text.primary" sx={{display: 'flex', alignItems: 'center'}}>
                        <HomeIcon sx={{mr: 0.5, fontSize: 20}}/>
                        Root
                    </Typography>
                </Breadcrumbs>
            </Box>
        );
    }

    const parts = path.split('/').filter(Boolean);

    return (
        <Box sx={{mb: 3}}>
            <Breadcrumbs aria-label="breadcrumb">
                <Link
                    component="button"
                    variant="body2"
                    underline="hover"
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        cursor: 'pointer',
                        border: 'none',
                        background: 'none',
                        padding: 0
                    }}
                    onClick={() => onNavigate('/')}
                >
                    <HomeIcon sx={{mr: 0.5, fontSize: 20}}/>
                </Link>
                {parts.map((part, index) => {
                    const isLast = index === parts.length - 1;
                    const pathTo = '/' + parts.slice(0, index + 1).join('/');

                    return isLast ? (
                        <Typography key={pathTo} color="text.primary">
                            {part}
                        </Typography>
                    ) : (
                        <Link
                            key={pathTo}
                            component="button"
                            variant="body2"
                            underline="hover"
                            sx={{
                                cursor: 'pointer',
                                border: 'none',
                                background: 'none',
                                padding: 0
                            }}
                            onClick={() => onNavigate(pathTo)}
                        >
                            {part}
                        </Link>
                    );
                })}
            </Breadcrumbs>
        </Box>
    );
};

Breadcrumb.propTypes = {
    path: PropTypes.string.isRequired,
    onNavigate: PropTypes.func.isRequired
};

export default Breadcrumb;
