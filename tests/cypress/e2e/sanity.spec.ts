describe('Tools index page link validation', () => {
    it('Should verify all links return 200 status code', () => {
        cy.login();
        cy.visit('/modules/tools/index.jsp');

        // Get all links on the page
        cy.get('a[href]').each($link => {
            const href = $link.attr('href');

            // Skip external links, javascript links, and anchors
            // eslint-disable-next-line no-script-url
            if (href && !href.includes('logout') && !href.startsWith('http') && !href.startsWith('javascript:') && !href.startsWith('#')) {
                // Handle relative URLs
                const url = href.startsWith('/') ? href : `/modules/tools/${href}`;

                cy.request({
                    url: url,
                    failOnStatusCode: false
                }).then(response => {
                    expect(response.status).to.eq(200);
                });
            }
        });

        cy.logout();
    });
});
