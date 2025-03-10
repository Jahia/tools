import {getStartedModulesVersion} from '@jahia/cypress';
const toolUrl = '/modules/tools/provisioning.jsp';

describe('Provisioning tool tests', () => {
    it('Button should be disabled for invalidYaml with error message', () => {
        cy.login();
        cy.visit(toolUrl);
        cy.get('#provisioning').type(`- enable: 'myModule'to
site: 'digitall'`);
        cy.get('#submitYaml').should('be.disabled');
        cy.get('#errorField').should('be.visible').should('not.be.empty');
        cy.logout();
    });

    it('Button should be enabled for validYaml', () => {
        cy.login();
        cy.visit(toolUrl);
        cy.get('#provisioning').type('- aaa: \'myModule\'');
        cy.get('#submitYaml').should('be.enabled');
        cy.get('#errorField').should('not.be.visible');
        cy.logout();
    });

    it('Should send provisioning script', () => {
        cy.login();
        cy.visit(toolUrl);
        cy.get('#provisioning').type(
            `- installBundle:
  - 'mvn:org.jahia.modules/skins/8.2.0'
  autoStart: true`
        );
        cy.get('#submitYaml').click();
        cy.waitUntil(() => cy.get('#provisioningMessage').should('not.be.visible'));
        cy.get('#provisioningResult').should('be.visible').should('not.be.empty');
        // eslint-disable-next-line cypress/no-unnecessary-waiting
        cy.reload();
        cy.waitUntil(() => getStartedModulesVersion().then(modules => modules.some(module => module.id === 'skins' && module.version === '8.2.0')), {timeout: 30000});
        cy.runProvisioningScript({
            fileContent: '- uninstallBundle: "mvn:org.jahia.modules/skins/8.2.0"',
            type: 'application/yaml'
        });
        cy.logout();
    });
});
