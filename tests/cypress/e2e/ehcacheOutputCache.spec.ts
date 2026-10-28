import {cacheTableCells, flushCache, openTool, renderPage} from '../support/tools';

// The module output cache viewers list the keys of the cache, and display each key and the content of an entry as
// text, exactly as they are stored.
const VIEWER_URL = '/modules/tools/ehcache/ehcache_cj.jsp';
const DEPENDENCIES_VIEWER_URL = '/modules/tools/ehcache/ehcache_cj_dep.jsp';
const DUMP_URL = '/modules/tools/ehcache/ehcache_dump.jsp';

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

// Returns the preformatted text a viewer displays for the entry of the given key.
function displayedContent(url: string, key: string): Cypress.Chainable<string> {
    return openTool(url, {key}).then(doc => doc.querySelector('body > pre')?.textContent ?? '');
}

describe('Module output cache viewers (/modules/tools/ehcache/ehcache_cj.jsp, ehcache_cj_dep.jsp)', () => {
    const run = Date.now().toString();
    const plainValue = `plain${run}`;
    const quotedValue = `"Ann" <ann.${run}@example.com>`;

    before(() => {
        cy.login();
        flushCache(VIEWER_URL);
        renderPage(plainValue);
        renderPage(quotedValue);
    });

    beforeEach(() => {
        cy.login();
    });

    after(() => {
        flushCache(VIEWER_URL);
        cy.logout();
    });

    it('displays the parameters held by a key exactly as they were written', () => {
        cacheTableCells(VIEWER_URL, 1).then(cells => {
            expect(cells.some(text => text.includes(plainValue)), 'key of the plain value listed').to.be.true;
            expect(cells.some(text => text.includes(quotedValue)), 'key of the quoted value listed as written').to.be.true;
        });
    });

    it('displays the content of an entry exactly as it is stored', () => {
        storedEntry(plainValue).then(({key, content}) => {
            displayedContent(VIEWER_URL, key).then(text => {
                expect(text).to.eq(content);
            });
        });
    });

    it('displays the content of an entry exactly as it is stored, from the dependencies page', () => {
        storedEntry(plainValue).then(({key, content}) => {
            displayedContent(DEPENDENCIES_VIEWER_URL, key).then(text => {
                expect(text).to.eq(content);
            });
        });
    });
});
