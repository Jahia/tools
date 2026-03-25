import Chainable = Cypress.Chainable;
export const DEPRECATION_FACTORY_PID = 'org.jahia.modules.tools.deprecatedpackagewires';
// The config instance created will be 'org.jahia.modules.tools.deprecatedpackagewires-custom'
const DEPRECATION_CONFIG_ID = 'custom';

export const PATTERN_CORE = 'org\\.jahia\\.test\\.core(\\..*)?';
export const PATTERN_CORE_API = 'org\\.jahia\\.test\\.core\\.api(\\..*)?';
export const PATTERN_CORE_UTIL = 'org\\.jahia\\.test\\.core\\.util(\\..*)?';
export const PATTERN_UTIL = 'org\\.jahia\\.test\\.util(\\..*)?';
export const PATTERN_UTIL_IO = 'org\\.jahia\\.test\\.util\\.io(\\..*)?';
export const PATTERN_UTIL_COLLECTIONS = 'org\\.jahia\\.test\\.util\\.collections(\\..*)?';
export const PATTERN_ALL = '.*';

export const setDeprecationCustomConfig = (deprecatedPatternsByProvider: Map<string, string[]>): void => {
    // Create the properties field based on the deprecatedPatternsByProvider map
    const properties = {};
    deprecatedPatternsByProvider.forEach((patterns, provider) => {
        patterns.forEach((pattern, index) => {
            properties[`providers.${provider}[${index}]`] = pattern;
        });
    });

    const conf = {
        editConfiguration: DEPRECATION_FACTORY_PID,
        configIdentifier: DEPRECATION_CONFIG_ID,
        properties: properties
    };
    cy.runProvisioningScript({fileContent: JSON.stringify([conf]), type: 'application/json'});
};

export type Bundle = {
    id: number;
    symbolicName: string;
    name: string;
}
export type PackageWire = {
    providerBundle: Bundle;
    requirerBundle: Bundle;
    packageName: string;
}
export type WireExpectation = {
    providerBundleSymbolicName: string;
    requirerBundleSymbolicName: string;
    packageName: string;
}
export const packageWireDef = (
    providerBundleSymbolicName: string,
    requirerBundleSymbolicName: string,
    packageName: string
): WireExpectation => ({providerBundleSymbolicName, requirerBundleSymbolicName, packageName});
/**
 * Asserts that the given packageWires array:
 *   1. Has exactly the same length as the expected array.
 *   2. Contains every expected entry (order-independent).
 */
export const containsPackageWires = (
    wires: PackageWire[],
    expected: WireExpectation[]
): void => {
    expected.forEach(({providerBundleSymbolicName, requirerBundleSymbolicName, packageName}) => {
        const match = wires.some(w =>
            w.providerBundle.symbolicName === providerBundleSymbolicName &&
            w.requirerBundle.symbolicName === requirerBundleSymbolicName &&
            w.packageName === packageName
        );
        expect(
            match,
            `Expected a wire: ${providerBundleSymbolicName} → ${requirerBundleSymbolicName} [${packageName}]`
        ).to.be.true;
    });

    const format = (list: Array<{
        providerBundleSymbolicName: string,
        requirerBundleSymbolicName: string,
        packageName: string
    }>) =>
        list.map(e => `  • ${e.providerBundleSymbolicName} → ${e.requirerBundleSymbolicName} [${e.packageName}]`).join('\n');

    const actualAsExpectations = wires.map(w => ({
        providerBundleSymbolicName: w.providerBundle.symbolicName,
        requirerBundleSymbolicName: w.requirerBundle.symbolicName,
        packageName: w.packageName
    }));

    expect(wires,
        `Expected ${expected.length} wires:\n${format(expected)}\n\nBut got ${wires.length}:\n${format(actualAsExpectations)}`
    ).to.have.length(expected.length);
};

export type PackageWiresResponse = {
    data?: {
        admin?: {
            tools?: {
                packageWires?: PackageWire[];
                deprecatedPackageWires?: PackageWire[];
            };
        };
    };
    errors?: readonly { message: string }[];
};

export const getPackageWires = (
    providerBundleSymbolicName: string, packageRegexes: string[], showInternal = false): Chainable<PackageWiresResponse> => {
    const uri = showInternal ? `${Cypress.config().baseUrl}/modules/graphql?showInternal=true` : undefined;
    return cy
        .apollo({
            queryFile: 'packageWires.graphql',
            variables: {providerBundleSymbolicName, packageRegexes},
            errorPolicy: 'all',
            ...(uri && {context: {uri}})
        }) as Chainable<PackageWiresResponse>;
};

export const getDeprecatedPackageWires = (showInternal = false): Chainable<PackageWiresResponse> => {
    const uri = showInternal ? `${Cypress.config().baseUrl}/modules/graphql?showInternal=true` : undefined;
    return cy
        .apollo({
            queryFile: 'deprecatedPackageWires.graphql',
            errorPolicy: 'all',
            ...(uri && {context: {uri}})
        }) as Chainable<PackageWiresResponse>;
};

/**
 * Asserts that a wire row exists in the results table for the given
 * provider, requirer and package name, using data attributes for robust matching.
 */
export const assertWire = (provider: string, requirer: string, packageName: string): void => {
    cy.get(`[data-test-id="wires-requirer-group"][data-requirer-symbolic-name="${requirer}"]`)
        .should('exist');
    cy.get(`[data-test-id="wires-row"][data-provider-symbolic-name="${provider}"][data-package-name="${packageName}"]`)
        .should('exist');
};

/**
 * Drives the custom-patterns UI:
 * - Selects the "Custom patterns" radio
 * - For each entry in the map, clicks "＋ Add provider mapping", selects the provider,
 *   and fills in all pattern inputs (adding extra rows via "＋ pattern" as needed)
 * - Clicks "Analyze"
 *
 * Pass null/empty string as the provider key to leave the provider as "(any provider)".
 */
export const analyzeCustomPatterns = (patternsByProvider: Map<string, string[]>): void => {
    cy.get('input[data-test-id="mode"][value="custom"]').click();
    cy.get('#customFields').should('be.visible');

    patternsByProvider.forEach((patterns, provider) => {
        cy.get('[data-test-id="custom-add-mapping-btn"]').click();

        // Target the last added mapping block
        cy.get('[data-test-id="custom-mapping-block"]').last().within(() => {
            // Select provider
            if (provider) {
                cy.get('[data-test-id="custom-mapping-provider"]').select(provider);
            }

            // Fill in patterns — first input already exists, add more via ＋ pattern button
            patterns.forEach((pattern, index) => {
                if (index > 0) {
                    cy.get('.btn-add-pattern').click();
                }

                cy.get('[data-test-id="custom-mapping-pattern"]').eq(index).clear();
                cy.get('[data-test-id="custom-mapping-pattern"]').eq(index).type(pattern);
            });
        });
    });

    cy.get('[data-test-id="analyze-btn"]').click();
};
