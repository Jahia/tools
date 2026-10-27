// The cache details viewer lists the keys of a cache, and displays each key as text.
const VIEWER_URL = '/modules/tools/ehcache/ehcache_details.jsp';
const CACHE = {name: 'org.jahia.ehcachemanager.big', cache: 'HTMLCache'};
const PAGE_URL = '/cms/render/live/en/sites/systemsite/home.html';

function toolAccessToken(): Cypress.Chainable<string> {
    return cy.request('POST', '/modules/tools/token').its('body.token');
}

function flushCache(): void {
    toolAccessToken().then(token => {
        cy.request({url: VIEWER_URL, qs: {...CACHE, flush: 'true', toolAccessToken: token}});
    });
}

// Renders the page with the given value in the parameters of the render, so that its cache keys hold the value.
function renderPage(value: string): void {
    cy.request({url: PAGE_URL, qs: {ec: value}}).its('status').should('eq', 200);
}

// Returns the text of every cell of the key column of the viewer.
function keyCells(): Cypress.Chainable<string[]> {
    return toolAccessToken().then(token => cy.request({url: VIEWER_URL, qs: {...CACHE, toolAccessToken: token}}).then(response => {
        const doc = new DOMParser().parseFromString(response.body, 'text/html');
        return Array.from(doc.querySelectorAll('#cacheTable tbody tr td:nth-child(1)')).map(td => td.textContent);
    }));
}

describe('Cache details viewer (/modules/tools/ehcache/ehcache_details.jsp)', () => {
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

    it('displays the parameters held by a key exactly as they were written', () => {
        keyCells().then(cells => {
            expect(cells.some(text => text.includes(plainValue)), 'key of the plain value listed').to.be.true;
            expect(cells.some(text => text.includes(quotedValue)), 'key of the quoted value listed as written').to.be.true;
        });
    });
});
