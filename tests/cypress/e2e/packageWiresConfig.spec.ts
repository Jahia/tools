import {elementTexts, openTool} from '../support/tools';
import {deleteDeprecationCustomConfig, setDeprecationCustomConfig} from '../utils/packageWires';

// The package wires analyzer lists the providers and patterns of the deprecated packages configuration, and the error
// that the analysis answers when a configured provider is not installed. The provider below is not installed.
const URL = '/modules/tools/packageWiresAnalyzer.jsp';
const PROVIDER = 'org.jahia.test."R&amp;D"<lib>';
const PATTERN = 'org\\.jahia\\.test\\."R&amp;D"<draft>';
const ANALYZE = {mode: 'deprecated', analyze: 'Analyze'};

function texts(doc: Document, selector: string): string[] {
    return elementTexts(doc, selector).map(text => text.trim());
}

describe('Package wires analyzer configuration (/modules/tools/packageWiresAnalyzer.jsp)', () => {
    before(() => {
        setDeprecationCustomConfig(new Map([[PROVIDER, [PATTERN]]]));
        // The configuration applies asynchronously: wait until the analysis reports the provider.
        cy.login();
        cy.waitUntil(() => openTool(URL, ANALYZE).then(doc => (doc.querySelector('[data-test-id="results-error-message"]')?.textContent || '')
            .includes('Bundle with symbolic name \'org.jahia.test."R&')), {timeout: 30000, interval: 500});
    });

    after(() => {
        deleteDeprecationCustomConfig();
    });

    beforeEach(() => {
        cy.login();
    });

    it('displays the configured providers and patterns as text', () => {
        openTool(URL).then(doc => {
            expect(texts(doc, '[data-test-id="configured-patterns-provider"]')).to.include(PROVIDER);
            expect(texts(doc, '[data-test-id="configured-patterns-pattern"]')).to.include(PATTERN);
            expect(texts(doc, '#configuredPatternsArea p strong')).to.include(PROVIDER);
            expect(texts(doc, '#configuredPatternsArea li code')).to.include(PATTERN);
        });
    });

    it('displays the configuration error of the deprecated packages analysis as text', () => {
        openTool(URL, ANALYZE).then(doc => {
            const message = doc.querySelector('[data-test-id="results-error-message"]');
            expect(message, 'the error message').not.to.equal(null);
            expect(message.textContent).to.contain(`'${PROVIDER}'`);
        });
    });

    it('keeps the showInternal parameter of the form as typed', () => {
        const value = 'x "R&amp;D" <i>';
        openTool(URL, {showInternal: value}).then(doc => {
            const input = doc.querySelector<HTMLInputElement>('#wiresForm input[name="showInternal"]');
            expect(input, 'the showInternal field').not.to.equal(null);
            expect(input.value).to.eq(value);
        });
    });
});
