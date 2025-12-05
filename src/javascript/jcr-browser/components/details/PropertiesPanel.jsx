import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {
    Card,
    CardHeader,
    CardContent,
    TextField,
    Typography,
    Chip,
    Box,
    IconButton,
    Collapse,
    List,
    ListItem,
    ListItemText
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import TextFieldsIcon from '@mui/icons-material/TextFields';
import TagIcon from '@mui/icons-material/Tag';
import ToggleOnIcon from '@mui/icons-material/ToggleOn';
import EventIcon from '@mui/icons-material/Event';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import LabelIcon from '@mui/icons-material/Label';

const PropertiesPanel = ({properties}) => {
    const [isExpanded, setIsExpanded] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    const filteredProperties = properties.filter(prop =>
        prop.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const formatPropertyValue = prop => {
        if (prop.multiple) {
            if (prop.values.length === 0) {
                return (
                    <Typography variant="body2" color="text.secondary" fontStyle="italic">
                        []
                    </Typography>
                );
            }

            return (
                <List dense disablePadding sx={{ml: 2}}>
                    {prop.values.map((val, idx) => (
                        // eslint-disable-next-line react/no-array-index-key
                        <ListItem key={idx} disablePadding>
                            <ListItemText
                                primary={formatValue(val, prop.type)}
                                primaryTypographyProps={{
                                    variant: 'body2',
                                    fontFamily: 'monospace'
                                }}
                            />
                        </ListItem>
                    ))}
                </List>
            );
        }

        return (
            <Typography variant="body2" fontFamily="monospace">
                {formatValue(prop.value, prop.type)}
            </Typography>
        );
    };

    const formatValue = (value, type) => {
        if (value === null || value === undefined) {
            return <Typography component="span" variant="body2" color="text.secondary" fontStyle="italic">null</Typography>;
        }

        switch (type) {
            case 'BOOLEAN':
                return (
                    <Chip
                        label={value}
                        color={value === 'true' ? 'success' : 'default'}
                        size="small"
                    />
                );
            case 'DATE':
                try {
                    return new Date(value).toLocaleString();
                } catch {
                    return value;
                }

            case 'LONG':
            case 'DOUBLE':
                return <Typography component="span" variant="body2" color="primary">{value}</Typography>;
            case 'BINARY':
                return <Typography component="span" variant="body2" color="info.main">{value}</Typography>;
            case 'REFERENCE':
            case 'WEAKREFERENCE':
                return <Typography component="span" variant="body2" color="warning.main">{value}</Typography>;
            default:
                return value;
        }
    };

    const getTypeIcon = type => {
        const icons = {
            STRING: <TextFieldsIcon/>,
            LONG: <TagIcon/>,
            DOUBLE: <TagIcon/>,
            BOOLEAN: <ToggleOnIcon/>,
            DATE: <EventIcon/>,
            BINARY: <InsertDriveFileIcon/>,
            REFERENCE: <LinkIcon/>,
            WEAKREFERENCE: <LinkOffIcon/>
        };
        return icons[type] || <LabelIcon/>;
    };

    return (
        <Card sx={{height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden'}}>
            <CardHeader
                title={
                    <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%'}}>
                        <Box sx={{display: 'flex', alignItems: 'center'}}>
                            <IconButton
                                size="small"
                                aria-label={isExpanded ? 'collapse' : 'expand'}
                                sx={{mr: 1}}
                                onClick={() => setIsExpanded(!isExpanded)}
                            >
                                {isExpanded ? <ExpandLessIcon/> : <ExpandMoreIcon/>}
                            </IconButton>
                            <Typography variant="h6">
                                Properties ({properties.length})
                            </Typography>
                        </Box>
                        {isExpanded && (
                            <TextField
                                size="small"
                                placeholder="Search properties..."
                                value={searchTerm}
                                sx={{width: 200}}
                                variant="outlined"
                                onChange={e => setSearchTerm(e.target.value)}
                            />
                        )}
                    </Box>
                }
            />
            <Collapse in={isExpanded} sx={{flexGrow: 1, overflow: 'hidden'}}>
                <CardContent sx={{height: '100%', overflow: 'auto'}}>
                    {filteredProperties.length === 0 ? (
                        <Typography variant="body2" color="text.secondary">
                            {searchTerm ? 'No properties match your search' : 'No properties found'}
                        </Typography>
                    ) : (
                        <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
                            {filteredProperties.map(prop => (
                                <Card key={prop.name} variant="outlined" sx={{p: 2}}>
                                    <Box sx={{display: 'flex', alignItems: 'flex-start'}}>
                                        <Box sx={{mr: 2, color: 'text.secondary'}}>
                                            {getTypeIcon(prop.type)}
                                        </Box>
                                        <Box sx={{flexGrow: 1}}>
                                            <Box sx={{mb: 1, display: 'flex', alignItems: 'center', gap: 1}}>
                                                <Typography variant="subtitle2" fontWeight="bold">
                                                    {prop.name}
                                                </Typography>
                                                <Chip
                                                    label={`${prop.type}${prop.multiple ? ' []' : ''}`}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                            </Box>
                                            <Box>
                                                {formatPropertyValue(prop)}
                                            </Box>
                                            {prop.path && (
                                                <Typography variant="caption" color="text.secondary" sx={{mt: 1, display: 'block'}}>
                                                    Path: {prop.path}
                                                </Typography>
                                            )}
                                        </Box>
                                    </Box>
                                </Card>
                            ))}
                        </Box>
                    )}
                </CardContent>
            </Collapse>
        </Card>
    );
};

PropertiesPanel.propTypes = {
    properties: PropTypes.arrayOf(
        PropTypes.shape({
            name: PropTypes.string.isRequired,
            type: PropTypes.string.isRequired,
            multiple: PropTypes.bool.isRequired,
            value: PropTypes.string,
            values: PropTypes.arrayOf(PropTypes.string),
            path: PropTypes.string
        })
    ).isRequired
};

export default PropertiesPanel;
