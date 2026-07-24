/// <reference types="cypress" />
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

const fs = require('fs');
const path = require('path');
const sshCommand = require('./ssh')
const env = require('./env');

/**
 * @type {Cypress.PluginConfig}
 */
module.exports = (on, config) => {
    env(on, config);

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    require('@jahia/cypress/dist/plugins/registerPlugins').registerPlugins(on, config);

    // custom tasks (Useful to run code in Node from cypress tests)
    on('task', {
        sshCommand(commands) {
            return sshCommand(commands, {
                hostname: config.env.JAHIA_HOST,
                port: config.env.JAHIA_PORT_KARAF,
                username: config.env.JAHIA_USERNAME_TOOLS,
                password: config.env.JAHIA_PASSWORD_TOOLS,
            })
        },
        listToolJsps() {
            // Recursively enumerate the module's tool JSPs from source (including subdirectories such as ehcache/)
            // so the access-authorization sweep covers every page and cannot drift.
            const root = path.resolve(__dirname, '../../../impl/src/main/resources');
            const walk = dir => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
                const full = path.join(dir, entry.name);
                if (entry.isDirectory()) {
                    return walk(full);
                }
                return entry.name.endsWith('.jsp') ? [path.relative(root, full).split(path.sep).join('/')] : [];
            });
            return walk(root);
        },
    });
  
    return config;
};