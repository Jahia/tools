import React from 'react';
import {createRoot} from 'react-dom/client';
import {ApolloClient, InMemoryCache, createHttpLink} from '@apollo/client';
import {ApolloProvider} from '@apollo/client/react';
import {setContext} from '@apollo/client/link/context';
import {ThemeProvider} from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme';
import JcrBrowserApp from './components/JcrBrowserApp';

// Get configuration from data attributes
const rootEl = document.getElementById('jcr-browser-root');

if (rootEl) {
    const config = {
        graphqlEndpoint: rootEl.dataset.graphqlEndpoint,
        csrfToken: rootEl.dataset.csrfToken,
        initialWorkspace: rootEl.dataset.initialWorkspace || 'default',
        initialUuid: rootEl.dataset.initialUuid || 'cafebabe-cafe-babe-cafe-babecafebabe'
    };

    // Create HTTP link
    const httpLink = createHttpLink({
        uri: config.graphqlEndpoint
    });

    // Create auth link to add CSRF token to headers
    const authLink = setContext((_, {headers}) => {
        return {
            headers: {
                ...headers,
                'X-CSRF-Token': config.csrfToken
            }
        };
    });

    // Create Apollo Client
    const client = new ApolloClient({
        link: authLink.concat(httpLink),
        cache: new InMemoryCache(),
        defaultOptions: {
            watchQuery: {
                fetchPolicy: 'cache-and-network',
                pollInterval: 30000 // Poll every 30 seconds for updates
            },
            query: {
                fetchPolicy: 'cache-first'
            }
        }
    });

    // Render the app
    const root = createRoot(rootEl);
    root.render(
        <ThemeProvider theme={theme}>
            <CssBaseline/>
            <ApolloProvider client={client}>
                <JcrBrowserApp
                    initialWorkspace={config.initialWorkspace}
                    initialUuid={config.initialUuid}
                />
            </ApolloProvider>
        </ThemeProvider>
    );
} else {
    console.error('JCR Browser root element not found');
}
