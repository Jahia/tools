/*
 * Tools access authorization.
 *
 * Every tool JSP requires the tools-access permission. The permission is enforced at two independent layers: the
 * perimeter filter chain (which matches on the request URL) and, as a second layer, a check inside each JSP that does
 * not depend on any URL matching. An unauthorized request must therefore be denied whichever layer handles it: the
 * perimeter answers with a 302 redirect to the login page, the in-JSP check answers with a 403. Either way the page
 * body never runs, so this sweep triggers no tool side effects.
 *
 * This spec enumerates every tool JSP from source (recursively, including subdirectories such as ehcache/) so the
 * check cannot drift as pages are added, and asserts that an unauthorized request is denied on each.
 */
describe('tools access authorization is enforced on every tool JSP', () => {
    // A representative tool page and the JCR browser (a representative privileged tool page), used for the
    // authorized-access check. The authorized sweep is deliberately NOT run across all JSPs: many have side effects
    // (GC, thread dumps, maintenance mode) on a plain GET.
    const TOOL_URL = '/modules/tools/index.jsp';
    const PRIVILEGED_TOOL_URL = '/modules/tools/jcrBrowser.jsp';
    const DOUBLE_ENCODED_TOOL_URL = '/modules/t%256Fols/groovyConsole.jsp%3fx/configs/xx.js?';

    // An unauthorized request is denied at whichever layer handles it: 302 (perimeter redirect to login) or 403
    // (in-JSP check). Both mean "not served"; a 200 would mean the guard was bypassed. The redirect must not be
    // followed, otherwise the observed status would be that of the login page rather than the denial itself.
    const DENIED_STATUSES = [302, 403];

    let jsps: string[] = [];

    before(() => {
        cy.task('listToolJsps').then((names: string[]) => {
            jsps = names;
            expect(jsps.length, 'tool JSPs discovered').to.be.greaterThan(0);
        });
    });

    it('denies an unauthorized request on every tool JSP', () => {
        cy.clearCookies();
        cy.wrap(null).then(() => {
            jsps.forEach(jspPath => {
                cy.request({url: `/modules/tools/${jspPath}`, failOnStatusCode: false, followRedirect: false}).then(response => {
                    expect(response.status, `unauthorized GET /modules/tools/${jspPath}`).to.be.oneOf(DENIED_STATUSES);
                });
            });
        });
        cy.request({url: DOUBLE_ENCODED_TOOL_URL, failOnStatusCode: false}).then(response => {
            expect(response.status, `unauthorized GET ${DOUBLE_ENCODED_TOOL_URL}`).to.be.oneOf(DENIED_STATUSES);
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
