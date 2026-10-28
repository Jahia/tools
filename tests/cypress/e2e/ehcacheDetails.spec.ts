import {cacheTableCells, flushCache, renderPage} from '../support/tools';

// The cache details viewer lists the keys of a cache, and displays each key as text.
const VIEWER_URL = '/modules/tools/ehcache/ehcache_details.jsp';
const CACHE = {name: 'org.jahia.ehcachemanager.big', cache: 'HTMLCache'};

describe('Cache details viewer (/modules/tools/ehcache/ehcache_details.jsp)', () => {
    const run = Date.now().toString();
    const plainValue = `plain${run}`;
    const quotedValue = `"Ann" <ann.${run}@example.com>`;

    before(() => {
        cy.login();
        flushCache(VIEWER_URL, CACHE);
        renderPage(plainValue);
        renderPage(quotedValue);
    });

    after(() => {
        flushCache(VIEWER_URL, CACHE);
        cy.logout();
    });

    it('displays the parameters held by a key exactly as they were written', () => {
        cacheTableCells(VIEWER_URL, 1, CACHE).then(cells => {
            expect(cells.some(text => text.includes(plainValue)), 'key of the plain value listed').to.be.true;
            expect(cells.some(text => text.includes(quotedValue)), 'key of the quoted value listed as written').to.be.true;
        });
    });
});
