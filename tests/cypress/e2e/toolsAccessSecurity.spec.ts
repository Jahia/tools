/*
 * SEC-245 - Tools access authorization is enforced locally by every tool JSP (a second layer, independent of the
 * perimeter Shiro filter chain and of any URL-pattern matching). The check lives in the resource itself, so a request
 * that slips past upstream filters still cannot reach a JSP's root escalation.
 *
 * This spec enumerates every tool JSP from source (recursively, including subdirectories such as ehcache/) and asserts an unauthenticated request is denied with 403 on each.
 * Denial aborts the page before its body runs, so the sweep triggers no tool side effects.
 */
describe('SEC-245 - tools access authorization is enforced by every tool JSP', () => {
    // A representative tool page and the privileged JCR browser (the reported exploit target), used for the
    // authorized-access check. The authorized sweep is deliberately NOT run across all JSPs: many have side effects
    // (GC, thread dumps, maintenance mode) on a plain GET.
    const TOOL_URL = '/modules/tools/index.jsp';
    const PRIVILEGED_TOOL_URL = '/modules/tools/jcrBrowser.jsp';

    let jsps: string[] = [];

    before(() => {
        cy.task('listToolJsps').then((names: string[]) => {
            jsps = names;
            expect(jsps.length, 'tool JSPs discovered').to.be.greaterThan(0);
        });
    });

    it('denies an unauthenticated user with 403 on every tool JSP', () => {
        cy.clearCookies();
        cy.wrap(null).then(() => {
            jsps.forEach(jspPath => {
                cy.request({url: `/modules/tools/${jspPath}`, failOnStatusCode: false}).then(response => {
                    expect(response.status, `unauthenticated GET /modules/tools/${jspPath}`).to.eq(403);
                });
            });
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
