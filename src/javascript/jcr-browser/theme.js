import {createTheme} from '@mui/material/styles';

// Create a theme instance with Jahia-friendly colors
const theme = createTheme({
    palette: {
        primary: {
            main: '#0d6efd', // Blue
            light: '#4d94ff',
            dark: '#0a58ca',
            contrastText: '#fff'
        },
        secondary: {
            main: '#6c757d', // Gray
            light: '#9ba3a9',
            dark: '#495057',
            contrastText: '#fff'
        },
        error: {
            main: '#dc3545',
            light: '#e35d6a',
            dark: '#c82333',
            contrastText: '#fff'
        },
        warning: {
            main: '#ffc107',
            light: '#ffcd38',
            dark: '#d39e00',
            contrastText: 'rgba(0, 0, 0, 0.87)'
        },
        info: {
            main: '#0dcaf0',
            light: '#3dd5f3',
            dark: '#087990',
            contrastText: 'rgba(0, 0, 0, 0.87)'
        },
        success: {
            main: '#198754',
            light: '#479f76',
            dark: '#146c43',
            contrastText: '#fff'
        },
        background: {
            default: '#f8f9fa',
            paper: '#ffffff'
        }
    },
    typography: {
        fontFamily: [
            '-apple-system',
            'BlinkMacSystemFont',
            '"Segoe UI"',
            'Roboto',
            '"Helvetica Neue"',
            'Arial',
            'sans-serif',
            '"Apple Color Emoji"',
            '"Segoe UI Emoji"',
            '"Segoe UI Symbol"'
        ].join(','),
        h1: {
            fontSize: '2.5rem',
            fontWeight: 500
        },
        h2: {
            fontSize: '2rem',
            fontWeight: 500
        },
        h3: {
            fontSize: '1.75rem',
            fontWeight: 500
        },
        h4: {
            fontSize: '1.5rem',
            fontWeight: 500
        },
        h5: {
            fontSize: '1.25rem',
            fontWeight: 500
        },
        h6: {
            fontSize: '1rem',
            fontWeight: 500
        }
    },
    components: {
        MuiCard: {
            styleOverrides: {
                root: {
                    borderRadius: 8,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)'
                }
            }
        },
        MuiButton: {
            styleOverrides: {
                root: {
                    textTransform: 'none',
                    borderRadius: 4
                }
            }
        },
        MuiChip: {
            styleOverrides: {
                root: {
                    borderRadius: 4
                }
            }
        }
    }
});

export default theme;
