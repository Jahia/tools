import {findDependencies} from '../../support/gql';
import {waitUntilSAMStatusGreen} from '@jahia/cypress';

function testBundle(r, bundleName: string, version: string, status: string) {
    const bundle = r.find(b => b.bundleSymbolicName === bundleName);
    console.log('testing bundle', bundle);
    const bundleImport = bundle.dependencies.find(dep => dep.name === 'org.external.modules.provider');
    expect(bundleImport.type).to.eq('IMPORT_PACKAGE');
    expect(bundleImport.version).to.eq(version);
    expect(bundleImport.optional).to.eq(false);
    expect(bundleImport.error).to.eq('');
    expect(bundleImport.status).to.eq(status);
}

function testBundleAndDependency(r, bundleName: string, dependsIsOptional: boolean, dependsVersion: string, dependsStatus: string) {
    const bundle = r.find(b => b.bundleSymbolicName === bundleName);
    console.log('testing bundle and dependency', bundle);
    const bundleImport = bundle.dependencies.find(dep => dep.name === 'org.external.modules.provider');
    expect(bundleImport.type).to.eq('IMPORT_PACKAGE');
    expect(bundleImport.version).to.eq('[1.1.0,2.0.0)');
    expect(bundleImport.optional).to.eq(false);
    expect(bundleImport.error).to.eq('');
    expect(bundleImport.status).to.eq('OPEN_RANGE');
    const bundleDepends = bundle.dependencies.find(dep => dep.name === 'module-provider');
    expect(bundleDepends.type).to.eq('JAHIA_DEPENDS');
    expect(bundleDepends.version).to.eq(dependsVersion);
    expect(bundleDepends.optional).to.eq(dependsIsOptional);
    expect(bundleDepends.error).to.eq('');
    expect(bundleDepends.status).to.eq(dependsStatus);
}

describe('Dependencies tool test', () => {
    it('Wait until SAM returns GREEN for medium severity', () => {
        // The timeout of 3mn (180) is there to allow for the cluster to finish its synchronization
        waitUntilSAMStatusGreen('MEDIUM', 180000);
    });

    describe('Test deployment of provider module and dependent modules to call the findDependencies tool', () => {
        before(() => {
            console.log('Before test, install modules');
            cy.installBundle('findDependenciesTool/module-provider-1.1.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-provider/1.1.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case10-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case10/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case11-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case11/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case21-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case21/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case22-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case22/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case23-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case23/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case24-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case24/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case25-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case25/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case26-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case26/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case27-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case27/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case31-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case31/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case32-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case32/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case33-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case33/1.0.0'}]);
            cy.installBundle('findDependenciesTool/module-dependant-case34-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case34/1.0.0'}]);
            waitUntilSAMStatusGreen('MEDIUM', 180000);
        });

        after(() => {
            console.log('After test, uninstall modules');
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case10/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case11/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case21/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case22/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case23/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case24/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case25/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case26/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case27/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case31/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case32/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case33/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-dependant-case34/1.0.0'}]);
            cy.runProvisioningScript([{uninstallBundle: 'module-provider/1.1.0'}]);
            waitUntilSAMStatusGreen('MEDIUM', 180000);
        });

        it('Test finding module that prevent minor upgrades', () => {
            findDependencies({nameRegExp: 'module-dep.*'}).should(result => {
                console.log('result', result);
                testBundle(result, 'module-dependant-case10', '[1.1.0,2.0.0)', 'OPEN_RANGE');
                testBundle(result, 'module-dependant-case11', '[1.1.0,2.0.0)', 'OPEN_RANGE');
                testBundleAndDependency(result, 'module-dependant-case21', false, '', 'EMPTY');
                testBundleAndDependency(result, 'module-dependant-case22', true, '', 'EMPTY');
                testBundleAndDependency(result, 'module-dependant-case23', false, '1.1.0', 'OPEN_RANGE');
                testBundleAndDependency(result, 'module-dependant-case24', true, '1.1.0', 'OPEN_RANGE');
                testBundleAndDependency(result, 'module-dependant-case25', true, '1.1.1', 'OPEN_RANGE');
                testBundle(result, 'module-dependant-case26', '[1.0.0,2.0.0)', 'OPEN_RANGE');
                testBundle(result, 'module-dependant-case27', '[1.0.0,1.99.0)', 'OPEN_RANGE');
                testBundle(result, 'module-dependant-case31', '[1.1.0,1.2.0)', 'RESTRICTIVE_RANGE');
                testBundleAndDependency(result, 'module-dependant-case32', false, '[1.0.0,1.2.0)', 'RESTRICTIVE_RANGE');
                testBundle(result, 'module-dependant-case33', '1.1.0', 'STRICT_NO_RANGE');
                testBundle(result, 'module-dependant-case34', '[1.1.0,1.2.0)', 'RESTRICTIVE_RANGE');
            });
        });
    });
});
