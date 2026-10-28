import {cacheTableCells, flushCache, renderPage} from '../support/tools';

// The module output dependencies cache viewer lists, for each dependency, the cache keys that depend on it, and
// displays each key as text.
const VIEWER_URL = '/modules/tools/ehcache/ehcache_cj_dep.jsp';

describe('Module output dependencies cache viewer (/modules/tools/ehcache/ehcache_cj_dep.jsp)', () => {
    const run = Date.now().toString();
    const plainValue = `plain${run}`;
    const quotedValue = `"Ann" <ann.${run}@example.com>`;

    before(() => {
        cy.login();
        flushCache(VIEWER_URL);
        renderPage(plainValue);
        renderPage(quotedValue);
    });

    after(() => {
        flushCache(VIEWER_URL);
        cy.logout();
    });

    it('displays the parameters held by a dependent key exactly as they were written', () => {
        cacheTableCells(VIEWER_URL, 2).then(cells => {
            expect(cells.some(text => text.includes(plainValue)), 'key of the plain value listed').to.be.true;
            expect(cells.some(text => text.includes(quotedValue)), 'key of the quoted value listed as written').to.be.true;
        });
    });
});
