describe('definitions browser tests (/modules/tools/definitionsBrowser.jsp)', () => {
    it('the bundle should not be listed once deleted from the definitions browser page', () => {
        cy.installBundle('testData/definitionsBrowser/tools-test-with-definitions-cnd.jar');
        cy.runProvisioningScript([{startBundle: 'tools-test-with-definitions-cnd/0.0.1'}]);
        cy.login();
        cy.visit('/modules/tools/definitionsBrowser.jsp');
        const toolsTestModuleSelector = 'a[name="tools-test-with-definitions-cnd"]';

        // After deleting the module, it is no longer listed:
        cy.get(toolsTestModuleSelector).siblings('a[href="#delete"]').click();
        cy.get(toolsTestModuleSelector).should('not.exist');

        // When refreshing the page, the module is still not listed:
        cy.reload();
        cy.get(toolsTestModuleSelector).should('not.exist');

        cy.runProvisioningScript([{uninstallBundle: 'tools-test-with-definitions-cnd/0.0.1'}]);
    });
});
