describe('Shutdown via API - mutation.admin.jahia.shutdown', () => {
    it('Shutdown success', function () {
        // This test must be the last test for obviously reason
        cy.apollo({
            mutationFile: 'shutdown.graphql',
            variables: {
                timeout: 100
            }
        })
            .its('data.admin.jahia.shutdown')
            .should('eq', true);
    });
});
