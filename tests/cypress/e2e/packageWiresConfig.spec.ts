import {openTool} from '../support/tools';
import {deleteDeprecationCustomConfig, setDeprecationCustomConfig} from '../utils/packageWires';

// The package wires analyzer lists the providers and patterns of the deprecated packages configuration, and the error
// that the analysis answers when a configured provider is not installed. The provider below is not installed.
const URL = '/modules/tools/packageWiresAnalyzer.jsp';
const PROVIDER = 'org.jahia.test."R&amp;D"<lib>';
const PATTERN = 'org\\.jahia\\.test\\."R&amp;D"<draft>';

function texts(doc: Document, selector: string): string[] {
    return Array.from(doc.querySelectorAll(selector)).map(element => element.textContent.trim());
}

describe('Package wires analyzer configuration (/modules/tools/packageWiresAnalyzer.jsp)', () => {
    before(() => {
        setDeprecationCustomConfig(new Map([[PROVIDER, [PATTERN]]]));
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
        openTool(URL, {mode: 'deprecated', analyze: 'Analyze'}).then(doc => {
            const message = doc.querySelector('[data-test-id="results-error-message"]');
            expect(message, 'the error message').not.to.equal(null);
            expect(message.textContent).to.contain(`'${PROVIDER}'`);
        });
    });
});
