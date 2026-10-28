import {openTool} from '../support/tools';

// The package wires analyzer lists every bundle in its provider list, and the requirer and provider of each wire it
// finds. The test-names-lib and test-consumer-names test modules carry the names below.
const URL = '/modules/tools/packageWiresAnalyzer.jsp';
const LIB_SYMBOLIC_NAME = 'org.jahia.test.names.lib';
const LIB_NAME = 'Test "Names" Library R&amp;D <lib> Zoé';
const CONSUMER_SYMBOLIC_NAME = 'org.jahia.test.consumer.names';
const CONSUMER_NAME = 'Test "Names" Consumer R&amp;D <draft> Zoé';
const PACKAGE_NAME = 'org.jahia.test.names';

function optionLabel(doc: Document, symbolicName: string): string {
    const option = Array.from(doc.querySelectorAll<HTMLOptionElement>('#providerSelectTemplate option'))
        .find(o => o.value === symbolicName);
    expect(option, `the option of ${symbolicName}`).not.to.equal(undefined);
    return option.textContent;
}

describe('Package wires analyzer names (/modules/tools/packageWiresAnalyzer.jsp)', () => {
    beforeEach(() => {
        cy.login();
    });

    it('displays the bundle names of the provider list as text', () => {
        openTool(URL).then(doc => {
            expect(optionLabel(doc, LIB_SYMBOLIC_NAME)).to.match(
                new RegExp(`^\\[\\d+\\] ${Cypress._.escapeRegExp(`${LIB_NAME} (${LIB_SYMBOLIC_NAME})`)}$`));
            expect(optionLabel(doc, CONSUMER_SYMBOLIC_NAME)).to.match(
                new RegExp(`^\\[\\d+\\] ${Cypress._.escapeRegExp(`${CONSUMER_NAME} (${CONSUMER_SYMBOLIC_NAME})`)}$`));
        });
    });

    it('displays the requirer and provider names of a wire as text', () => {
        const params = JSON.stringify([{[LIB_SYMBOLIC_NAME]: ['org\\.jahia\\.test\\.names']}]);
        openTool(URL, {mode: 'custom', params, analyze: 'Analyze'}).then(doc => {
            const group = doc.querySelector(`[data-test-id="wires-requirer-group"][data-requirer-symbolic-name="${CONSUMER_SYMBOLIC_NAME}"]`);
            expect(group, 'the requirer group of the test module').not.to.equal(null);
            expect(group.textContent).to.contain(CONSUMER_NAME).and.to.contain(`(${CONSUMER_SYMBOLIC_NAME})`);

            const row = doc.querySelector(`[data-test-id="wires-row"][data-package-name="${PACKAGE_NAME}"][data-provider-symbolic-name="${LIB_SYMBOLIC_NAME}"]`);
            expect(row, 'the wire of the test module').not.to.equal(null);
            expect(row.querySelector('[data-test-id="wires-row-package"]').textContent).to.eq(PACKAGE_NAME);
            expect(row.querySelector('.provider-cell').textContent).to.contain(LIB_NAME);
            expect(row.querySelector('[data-test-id="wires-row-provider"]').textContent).to.eq(`(${LIB_SYMBOLIC_NAME})`);
        });
    });
});
