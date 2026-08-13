/*
 * Tools access authorization.
 *
 * Every tool JSP requires the tools-access permission. The permission is enforced at two independent layers: the
 * perimeter filter chain (which matches on the request URL) and, as a second layer, a check inside each JSP that does
 * not depend on any URL matching. An unauthorized request must therefore be denied whichever layer handles it: the
 * perimeter answers with a 302 redirect to the login page, the in-JSP check answers with a 403. Either way the page
 * body never runs, so this sweep triggers no tool side effects.
 *
 * The list of tool JSPs comes from the TOOL_JSPS configuration variable (see cypress.config.ts): the tests run from a
 * docker image built out of the tests/ folder alone, so the module sources cannot be enumerated at runtime. Adding a
 * tool page therefore means adding it to that list.
 */
describe('tools access authorization is enforced on every tool JSP', () => {
    // A representative tool page and the JCR browser (a representative privileged tool page), used for the
    // authorized-access check. The authorized sweep is deliberately NOT run across all JSPs: many have side effects
    // (GC, thread dumps, maintenance mode) on a plain GET.
    const TOOL_URL = '/modules/tools/index.jsp';
    const PRIVILEGED_TOOL_URL = '/modules/tools/jcrBrowser.jsp';
    const DOUBLE_ENCODED_TOOL_URL = '/modules/t%256Fols/groovyConsole.jsp%3fx/configs/xx.js?';

    const jsps: string[] = Cypress.env('TOOL_JSPS') as string[];

    it('denies an unauthorized request on every tool JSP', () => {
        expect(jsps, 'TOOL_JSPS configuration').to.be.an('array').and.not.to.be.empty;
        cy.clearCookies();
        // An unauthorized request is denied at whichever layer handles it: 302 (perimeter redirect to login) or 403
        // (in-JSP check). Both mean "not served"; a 200 would mean the guard was bypassed. The redirect must not be
        // followed, otherwise the observed status would be that of the login page rather than the denial itself.
        jsps.forEach(jspPath => {
            cy.request({url: `/modules/tools/${jspPath}`, failOnStatusCode: false, followRedirect: false}).then(response => {
                expect(response.status, `unauthorized GET /modules/tools/${jspPath}`).to.not.eq(200);
            });
        });
        cy.request({url: DOUBLE_ENCODED_TOOL_URL, failOnStatusCode: false}).then(response => {
            expect(response.status, `unauthorized GET ${DOUBLE_ENCODED_TOOL_URL}`).to.not.eq(200);
        });
    });

    it('allows an authorized administrator with 200 on representative tool pages', () => {
        cy.login();
        cy.request({url: TOOL_URL, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(200);
        });
        cy.request({url: PRIVILEGED_TOOL_URL, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(200);
        });
        cy.logout();
    });
});
