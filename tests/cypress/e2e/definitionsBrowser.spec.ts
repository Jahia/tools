describe('definitions browser tests (/modules/tools/definitionsBrowser.jsp)', () => {
    it('the bundle should not be listed once deleted from the definitions browser page', () => {
        cy.installBundle('testData/definitionsBrowser/tools-test-with-definitions-cnd.jar');
        cy.login();
        cy.visit('/modules/tools/definitionsBrowser.jsp');
        const toolsTestModuleSelector = 'a[name="tools-test-with-definitions-cnd"]';
        cy.get(toolsTestModuleSelector).siblings('a[href="#delete"]').click();
        cy.get(toolsTestModuleSelector).should('not.exist');
    });
});
