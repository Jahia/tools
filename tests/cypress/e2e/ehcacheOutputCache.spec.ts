// The module output cache viewers list the keys of the cache, and display each key and the content of an entry as
// text, exactly as they are stored.
const VIEWER_URL = '/modules/tools/ehcache/ehcache_cj.jsp';
const DEPENDENCIES_VIEWER_URL = '/modules/tools/ehcache/ehcache_cj_dep.jsp';
const DUMP_URL = '/modules/tools/ehcache/ehcache_dump.jsp';
const PAGE_URL = '/cms/render/live/en/sites/systemsite/home.html';

function toolAccessToken(): Cypress.Chainable<string> {
    return cy.request('POST', '/modules/tools/token').its('body.token');
}

function flushCache(): void {
    toolAccessToken().then(token => {
        cy.request({url: VIEWER_URL, qs: {flush: 'true', toolAccessToken: token}});
    });
}

// Renders the page with the given value in the parameters of the render, so that its cache keys hold the value.
function renderPage(value: string): void {
    cy.request({url: PAGE_URL, qs: {ec: value}}).its('status').should('eq', 200);
}

function parseHtml(body: string): Document {
    return new DOMParser().parseFromString(body, 'text/html');
}

// Returns the text of every cell of the key column of a viewer.
function keyCells(url: string): Cypress.Chainable<string[]> {
    return cy.request(url).then(response => Array.from(parseHtml(response.body)
        .querySelectorAll('#cacheTable tbody tr td:nth-child(1)')).map(td => td.textContent));
}

// Returns the key and the stored content of the first entry whose key holds the value and whose content holds markup.
function storedEntry(value: string): Cypress.Chainable<{key: string, content: string}> {
    return cy.request(DUMP_URL).then(response => {
        const doc = new DOMParser().parseFromString(response.body, 'text/xml');
        const entry = Array.from(doc.querySelectorAll('entry'))
            .map(e => ({key: e.getAttribute('key'), content: e.querySelector('content')?.textContent ?? ''}))
            .find(e => e.key.includes(value) && e.content.includes('<'));
        expect(entry, 'stored entry of the rendered page').to.exist;
        return entry;
    });
}

// Returns the text a viewer displays for the entry of the given key.
function displayedContent(url: string, key: string): Cypress.Chainable<string> {
    return toolAccessToken().then(token => cy.request({url, qs: {key, toolAccessToken: token}})
        .then(response => parseHtml(response.body).body.textContent));
}

function normalize(text: string): string {
    return text.replace(/\r\n?/g, '\n').trim();
}

describe('Module output cache viewers (/modules/tools/ehcache/ehcache_cj.jsp, ehcache_cj_dep.jsp)', () => {
    const run = Date.now().toString();
    const plainValue = `plain${run}`;
    const quotedValue = `"Ann" <ann.${run}@example.com>`;

    before(() => {
        cy.login();
        flushCache();
        renderPage(plainValue);
        renderPage(quotedValue);
    });

    beforeEach(() => {
        cy.login();
    });

    after(() => {
        flushCache();
        cy.logout();
    });

    it('displays the parameters held by a key exactly as they were written', () => {
        keyCells(VIEWER_URL).then(cells => {
            expect(cells.some(text => text.includes(plainValue)), 'key of the plain value listed').to.be.true;
            expect(cells.some(text => text.includes(quotedValue)), 'key of the quoted value listed as written').to.be.true;
        });
    });

    it('displays the content of an entry exactly as it is stored', () => {
        storedEntry(plainValue).then(({key, content}) => {
            displayedContent(VIEWER_URL, key).then(text => {
                expect(normalize(text)).to.include(normalize(content));
            });
        });
    });

    it('displays the content of an entry exactly as it is stored, from the dependencies page', () => {
        storedEntry(plainValue).then(({key, content}) => {
            displayedContent(DEPENDENCIES_VIEWER_URL, key).then(text => {
                expect(normalize(text)).to.include(normalize(content));
            });
        });
    });
});
