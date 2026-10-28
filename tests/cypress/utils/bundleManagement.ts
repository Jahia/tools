// ── Symbolic names ────────────────────────────────────────────────────────────
export const CORE_LIB = 'org.jahia.test.core.lib';
export const UTIL_LIB = 'org.jahia.test.util.lib';
export const CONSUMER_CORE = 'org.jahia.test.consumer.core';
export const CONSUMER_UTIL = 'org.jahia.test.consumer.util';
export const CONSUMER_MIXED = 'org.jahia.test.consumer.mixed';
export const CONSUMER_DYNAMIC = 'org.jahia.test.consumer.dynamic';
export const CONSUMER_OPTIONAL = 'org.jahia.test.consumer.optional';
export const CONSUMER_STOPPED = 'org.jahia.test.consumer.stopped';
export const NAMES_LIB = 'org.jahia.test.names.lib';
export const CONSUMER_NAMES = 'org.jahia.test.consumer.names';

export const stopBundle = (symbolicName: string): void => {
    cy.log(`Stopping bundle: ${symbolicName}`);
    cy.task('sshCommand', [`bundle:stop ${symbolicName}`]).then((response: string) => {
        expect(response).to.be.empty;
        cy.task('sshCommand', [`bundle:status ${symbolicName}`]).then((status: string) => {
            expect(status).to.contain('Resolved');
        });
    });
};

export const startBundle = (symbolicName: string): void => {
    cy.log(`Starting bundle: ${symbolicName}`);
    cy.task('sshCommand', [`bundle:start ${symbolicName}`]).then((response: string) => {
        expect(response).to.be.empty;
        cy.task('sshCommand', [`bundle:status ${symbolicName}`]).then((status: string) => {
            expect(status).to.contain('Active');
        });
    });
};

export const refreshBundle = (symbolicName: string): void => {
    cy.log(`Refreshing bundle: ${symbolicName}`);
    cy.task('sshCommand', [`bundle:refresh ${symbolicName}`]).then((response: string) => {
        expect(response).to.be.empty;
    });
};

// Deletes the configurations of the factory that also match the given LDAP filter, if any, and yields their number.
export const deleteAllConfigurations = (factoryPid: string, filter = ''): Cypress.Chainable<number> => {
    const query = filter ? `"(&(service.factoryPid=${factoryPid})${filter})"` : `"(service.factoryPid=${factoryPid})"`;
    cy.log(`Deleting the configurations matching ${query}`);
    return cy.task('sshCommand', [`config:list -s ${query}`]).then((listResponse: string) => {
        const configIds = listResponse
            .split(/\r?\n/)
            .map(line => line.trim())
            // Keep only lines that look like a config PID (start with the factoryPid)
            .filter(line => line.startsWith(factoryPid));

        cy.log(`Found ${configIds.length} configuration(s) to delete for factoryPid: ${factoryPid}`);
        configIds.forEach(configId => {
            cy.log(`Deleting configuration: ${configId}`);
            cy.task('sshCommand', [`config:delete ${configId}`]).then((deleteResponse: string) => {
                expect(deleteResponse).to.be.empty;
                cy.log(`Deleted configuration: ${configId}`);
            });
        });
        return cy.wrap(configIds.length);
    });
};

