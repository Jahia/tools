import {openTool} from '../support/tools';

// The Import-Package checker lists the bundles that import a package matching the given expression, with the matching
// clauses of each one. The test-consumer-labels test module carries the name and the clause attribute below.
const URL = '/modules/tools/importPackageChecker.jsp';
const SYMBOLIC_NAME = 'org.jahia.test.consumer.labels';
const BUNDLE_NAME = 'Test Consumer Q&A <draft>';
const PACKAGE_REGEXP = 'org\\.jahia\\.test\\.labels';
const FORM_VALUE = 'Q&A "draft" <1>';

// Runs the checker on the package of the test module, and returns the text of the name and of each clause it lists for
// the test module.
function testModuleEntry(): Cypress.Chainable<{name: string, clauses: string[]}> {
    return openTool(URL, {do: 'Execute', regexp: PACKAGE_REGEXP}).then(doc => {
        const entry = Array.from(doc.querySelectorAll('fieldset > ul > li'))
            .find(li => li.querySelector('strong').textContent.includes(`(${SYMBOLIC_NAME})`));
        expect(entry, `the entry of ${SYMBOLIC_NAME}`).not.to.equal(undefined);
        return {
            name: entry.querySelector('strong').textContent,
            clauses: Array.from(entry.querySelectorAll('ul > li')).map(li => li.textContent)
        };
    });
}

describe('Import-Package checker (/modules/tools/importPackageChecker.jsp)', () => {
    beforeEach(() => {
        cy.login();
    });

    it('displays the bundle name as text', () => {
        testModuleEntry().then(({name}) => {
            expect(name).to.match(new RegExp(`^${Cypress._.escapeRegExp(`${BUNDLE_NAME} (${SYMBOLIC_NAME})`)} \\[\\d+\\]$`));
        });
    });

    it('displays the matching clauses as text', () => {
        testModuleEntry().then(({clauses}) => {
            expect(clauses).to.have.length(1);
            expect(clauses[0]).to.match(/^org\.jahia\.test\.labels;/).and.to.contain('note=Q&A <draft>');
        });
    });

    it('keeps the submitted values in the form', () => {
        openTool(URL, {regexp: FORM_VALUE, version: FORM_VALUE}).then(doc => {
            expect(doc.querySelector<HTMLInputElement>('input#regexp').value).to.eq(FORM_VALUE);
            expect(doc.querySelector<HTMLInputElement>('input#version').value).to.eq(FORM_VALUE);
        });
    });
});
