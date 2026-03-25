import {
    CONSUMER_CORE,
    CONSUMER_DYNAMIC,
    CONSUMER_MIXED,
    CONSUMER_OPTIONAL,
    CONSUMER_STOPPED,
    CONSUMER_UTIL,
    CORE_LIB,
    deleteAllConfigurations,
    refreshBundle,
    startBundle,
    stopBundle,
    UTIL_LIB
} from '../utils/bundleManagement';
import {
    analyzeCustomPatterns,
    assertWire,
    DEPRECATION_FACTORY_PID,
    PATTERN_CORE,
    PATTERN_CORE_API,
    PATTERN_CORE_UTIL,
    PATTERN_UTIL,
    PATTERN_UTIL_COLLECTIONS,
    PATTERN_UTIL_IO,
    setDeprecationCustomConfig
} from '../utils/packageWires';

describe('Package wires UI', () => {
    before(() => {
        cy.log('Starting all consumer bundles for the suite...');
        startBundle(CONSUMER_CORE);
        startBundle(CONSUMER_UTIL);
        startBundle(CONSUMER_MIXED);
        startBundle(CONSUMER_DYNAMIC);
        startBundle(CONSUMER_OPTIONAL);
        stopBundle(CONSUMER_STOPPED);
    });

    after(() => {
        // Restart the stopped bundle to leave the suite clean (and prevent the SAM probe to fail)
        startBundle(CONSUMER_STOPPED);
    });

    beforeEach(() => {
        refreshBundle(CONSUMER_DYNAMIC); // Refresh the bundle to remove the dynamic imports
    });

    describe('mode = custom patterns', () => {
        beforeEach(() => {
            cy.login();
            cy.visit('/modules/tools/packageWiresAnalyzer.jsp');
        });

        it('Should return wires matching a single pattern when no provider filter is given', () => {
            analyzeCustomPatterns(new Map([
                ['', [PATTERN_CORE]]
            ]));
            cy.get('[data-test-id="wires-table"]').should('be.visible');
            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core');
        });

        it('Should return wires from all matching providers when multiple patterns are given and no provider filter is set', () => {
            // Two separate mappings both with empty provider → any provider
            analyzeCustomPatterns(new Map([
                ['', [PATTERN_CORE, PATTERN_UTIL]]
            ]));
            cy.get('[data-test-id="wires-table"]').should('be.visible');
            // Core wires
            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core');
            // Util wires
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io');
            assertWire(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util');
            assertWire(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections');
        });

        it('Should return only wires from the specified provider when a provider filter is given', () => {
            analyzeCustomPatterns(new Map([
                [CORE_LIB, [PATTERN_CORE]]
            ]));
            cy.get('[data-test-id="wires-table"]').should('be.visible');
            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core');
            assertWire(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core');
            // No util wires — provider filter excludes them
            cy.get(`[data-test-id="wires-requirer-group"][data-requirer-symbolic-name="${CONSUMER_UTIL}"]`)
                .should('not.exist');
        });

        it('Should return only wires from the filtered provider when multiple patterns are given but only one provider matches', () => {
            // Util-lib provider filter + both patterns → only util wires returned
            analyzeCustomPatterns(new Map([
                [UTIL_LIB, [PATTERN_CORE, PATTERN_UTIL]]
            ]));
            cy.get('[data-test-id="wires-table"]').should('be.visible');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io');
            assertWire(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util');
            assertWire(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections');
            // No core wires — provider filter excludes them
            cy.get(`[data-test-id="wires-requirer-group"][data-requirer-symbolic-name="${CONSUMER_CORE}"]`)
                .should('not.exist');
        });

        it('Should show an empty result when no package name matches the given pattern', () => {
            analyzeCustomPatterns(new Map([
                ['', ['org\\.jahia\\.test\\.nonexistent\\..*']]
            ]));
            cy.get('[data-test-id="wires-table"]').should('not.exist');
            cy.get('[data-test-id="results-badge"]').should('not.exist');
            // Empty-results message is shown instead
            cy.contains('No Jahia modules are wired to the requested packages').should('be.visible');
        });

        it('Should show an error when the pattern is an invalid regular expression', () => {
            analyzeCustomPatterns(new Map([
                ['', ['[unclosed bracket']]
            ]));
            cy.get('[data-test-id="results-error-badge"]').should('be.visible');
            cy.get('[data-test-id="results-error-message"]').should('be.visible')
                .invoke('text').invoke('trim').should('contain', 'Invalid regular expression');
        });

        it('Should include internal Jahia modules when showInternal=true is passed as a URL parameter', () => {
            const internalModuleSelector = '[data-test-id="internal-jahia-modules-included"]';
            cy.get(internalModuleSelector).should('not.exist');
            cy.visit('/modules/tools/packageWiresAnalyzer.jsp?showInternal=true');
            cy.get(internalModuleSelector).should('exist');
            analyzeCustomPatterns(new Map([
                ['', ['javax\\.servlet\\..*']]
            ]));
            cy.get('[data-test-id="wires-table"]').should('be.visible');
            // The Tools module (an internal Jahia module) must appear as a requirer
            cy.get('[data-test-id="wires-requirer-group"][data-requirer-symbolic-name="tools"]')
                .should('exist');
        });
    });

    describe('mode = Jahia deprecated packages', () => {
        beforeEach(() => {
            deleteAllConfigurations(DEPRECATION_FACTORY_PID);
        });

        it('Should have the deprecated mode enabled by default', () => {
            cy.login();
            cy.visit('/modules/tools/packageWiresAnalyzer.jsp');
            cy.get('input[data-test-id="mode"]:checked').should('have.value', 'deprecated');
        });

        it('Should be able to see the configuration used for the "Jahia deprecated packages" mode', () => {
            const deprecatedPatternsByProvider = new Map<string, string[]>();
            deprecatedPatternsByProvider.set(CORE_LIB, [PATTERN_CORE_UTIL]);
            deprecatedPatternsByProvider.set(UTIL_LIB, [PATTERN_UTIL_COLLECTIONS, PATTERN_UTIL_IO]);
            setDeprecationCustomConfig(deprecatedPatternsByProvider);
            cy.login();
            cy.visit('/modules/tools/packageWiresAnalyzer.jsp');
            // By default, <details> is closed, so the content is not visible.
            cy.get('[data-test-id="configured-patterns-details"]').should('not.have.attr', 'open');

            // Click the summary to expand the details
            cy.get('[data-test-id="configured-patterns-summary"]').click();

            // After clicking, <details> has the "open" attribute — content is expanded
            cy.get('[data-test-id="configured-patterns-details"]').should('have.attr', 'open');
            cy.get('[data-test-id="configured-patterns-table"]').should('be.visible');

            // CORE_LIB: 1 provider cell + 1 pattern cell
            cy.get('[data-test-id="configured-patterns-provider"]').eq(0)
                .invoke('text').invoke('trim').should('eq', CORE_LIB);
            cy.get('[data-test-id="configured-patterns-pattern"]').eq(0)
                .invoke('text').invoke('trim').should('eq', PATTERN_CORE_UTIL);

            // UTIL_LIB: 1 provider cell + 2 pattern cells
            cy.get('[data-test-id="configured-patterns-provider"]').eq(1)
                .invoke('text').invoke('trim').should('eq', UTIL_LIB);
            cy.get('[data-test-id="configured-patterns-pattern"]').eq(1)
                .invoke('text').invoke('trim').should('eq', PATTERN_UTIL_COLLECTIONS);
            cy.get('[data-test-id="configured-patterns-pattern"]').eq(2)
                .invoke('text').invoke('trim').should('eq', PATTERN_UTIL_IO);

            // Exact count: 2 providers, 3 patterns total
            cy.get('[data-test-id="configured-patterns-provider"]').should('have.length', 2);
            cy.get('[data-test-id="configured-patterns-pattern"]').should('have.length', 3);
        });

        it('Should list the modules matching the configuration', () => {
            const deprecatedPatternsByProvider = new Map<string, string[]>();
            deprecatedPatternsByProvider.set(CORE_LIB, [PATTERN_CORE_API, PATTERN_CORE_UTIL]);
            deprecatedPatternsByProvider.set(UTIL_LIB, [PATTERN_UTIL_COLLECTIONS, PATTERN_UTIL_IO]);
            setDeprecationCustomConfig(deprecatedPatternsByProvider);

            cy.login();
            cy.visit('/modules/tools/packageWiresAnalyzer.jsp');
            cy.get('[data-test-id="analyze-btn"]').click();

            // Results table is present
            cy.get('[data-test-id="wires-table"]').should('be.visible');

            // Badge shows the correct wire and module counts (execution time is ignored)
            cy.get('[data-test-id="results-wire-count"]').should('have.text', '5');
            cy.get('[data-test-id="results-module-count"]').should('have.text', '3');

            // Expected wires — mirrors the GraphQL expectation exactly:
            //   CORE_LIB → CONSUMER_CORE    [org.jahia.test.core.api]
            //   CORE_LIB → CONSUMER_MIXED   [org.jahia.test.core.api]
            //   UTIL_LIB → CONSUMER_UTIL    [org.jahia.test.util.collections]
            //   UTIL_LIB → CONSUMER_UTIL    [org.jahia.test.util.io]
            //   UTIL_LIB → CONSUMER_MIXED   [org.jahia.test.util.collections]

            // CONSUMER_CORE: 1 wire, CONSUMER_MIXED: 2 wires, CONSUMER_UTIL: 2 wires
            // → 3 group-header rows
            cy.get('[data-test-id="wires-requirer-group"]').should('have.length', 3);

            // Total wire rows: 1 + 2 + 2 = 5
            cy.get('[data-test-id="wires-row"]').should('have.length', 5);

            assertWire(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api');
            assertWire(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections');
            assertWire(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io');
            assertWire(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections');
        });

        it('Should show an error when no deprecated patterns are configured', () => {
            setDeprecationCustomConfig(new Map()); // Remove all patterns
            cy.login();
            cy.visit('/modules/tools/packageWiresAnalyzer.jsp');
            // The warning should be visible before clicking Analyze
            cy.contains('⚠ no patterns configured').should('be.visible');
            // The Analyze button should be disabled
            cy.get('[data-test-id="analyze-btn"]').should('be.disabled');
            // Enable the button by simulating a config (simulate what user would do if it was enabled)
            // For robustness, force enable and submit
            cy.get('[data-test-id="analyze-btn"]').invoke('prop', 'disabled', false).click({force: true});
            // Should show error badge and error message
            cy.get('[data-test-id="results-error-badge"]').should('be.visible');
            cy.get('[data-test-id="results-error-message"]').should('be.visible')
                .invoke('text').should('equal', 'No deprecated patterns configured');
        });
    });
});

