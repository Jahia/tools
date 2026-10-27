// The module output dependencies cache viewer lists, for each dependency, the cache keys that depend on it, and
// displays each key as text.
const VIEWER_URL = '/modules/tools/ehcache/ehcache_cj_dep.jsp';
const PAGE_URL = '/cms/render/live/en/sites/systemsite/home.html';

function flushCache(): void {
    cy.request('POST', '/modules/tools/token').its('body.token').then(toolAccessToken => {
        cy.request({url: VIEWER_URL, qs: {flush: 'true', toolAccessToken}});
    });
}

// Renders the page with the given value in the parameters of the render, so that its cache keys hold the value.
function renderPage(value: string): void {
    cy.request({url: PAGE_URL, qs: {ec: value}}).its('status').should('eq', 200);
}

// Returns the text of every cell of the dependent keys column of the viewer.
function dependentKeyCells(): Cypress.Chainable<string[]> {
    return cy.request(VIEWER_URL).then(response => {
        const doc = new DOMParser().parseFromString(response.body, 'text/html');
        return Array.from(doc.querySelectorAll('#cacheTable tbody tr td:nth-child(2)')).map(td => td.textContent);
    });
}

describe('Module output dependencies cache viewer (/modules/tools/ehcache/ehcache_cj_dep.jsp)', () => {
    const run = Date.now().toString();
    const plainValue = `plain${run}`;
    const quotedValue = `"Ann" <ann.${run}@example.com>`;

    before(() => {
        cy.login();
        flushCache();
        renderPage(plainValue);
        renderPage(quotedValue);
    });

    after(() => {
        flushCache();
        cy.logout();
    });

    it('displays the parameters held by a dependent key exactly as they were written', () => {
        dependentKeyCells().then(cells => {
            expect(cells.some(text => text.includes(plainValue)), 'key of the plain value listed').to.be.true;
            expect(cells.some(text => text.includes(quotedValue)), 'key of the quoted value listed as written').to.be.true;
        });
    });
});
