import {getBundles, Status} from '../../support/gql';
import {waitUntilSAMStatusGreen} from '@jahia/cypress';

function testBundle(bundles, bundleName: string, version: string, status: string) {
    const bundle = bundles.find(b => b.bundleSymbolicName === bundleName);
    console.log('testing bundle', bundle);
    const bundleImport = bundle.dependencies.find(dep => dep.name === 'org.external.modules.provider');
    expect(bundleImport.type).to.eq('IMPORT_PACKAGE');
    expect(bundleImport.version).to.eq(version);
    expect(bundleImport.optional).to.eq(false);
    expect(bundleImport.error).to.eq('');
    expect(bundleImport.status).to.eq(status);
}

function testBundleAndDependency(bundles, bundleName: string, dependsIsOptional: boolean, dependsVersion: string, dependsStatus: string) {
    const bundle = bundles.find(b => b.bundleSymbolicName === bundleName);
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
        // waitUntilSAMStatusGreen('MEDIUM', 180000);
    });

    describe('Test deployment of provider module and dependent modules to call the findDependencies tool', () => {
        before(() => {
            console.log('Before test, install modules');
            cy.installBundle('testData/bundles/module-provider-1.1.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-provider/1.1.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case10-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case10/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case11-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case11/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case21-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case21/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case22-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case22/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case23-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case23/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case24-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case24/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case25-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case25/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case26-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case26/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case27-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case27/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case31-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case31/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case32-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case32/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case33-1.0.0.jar');
            cy.runProvisioningScript([{startBundle: 'module-dependant-case33/1.0.0'}]);
            cy.installBundle('testData/bundles/module-dependant-case34-1.0.0.jar');
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
            getBundles({nameRegExp: 'module-dep.*'}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('data');
                expect(result.data).to.have.property('admin');
                expect(result.data.admin).to.have.property('tools');
                expect(result.data.admin.tools).to.have.property('bundles');
                expect(result.data.admin.tools.bundles).to.have.length(13);

                const bundles = result.data.admin.tools.bundles;
                testBundle(bundles, 'module-dependant-case10', '[1.1.0,2.0.0)', 'OPEN_RANGE');
                testBundle(bundles, 'module-dependant-case11', '[1.1.0,2.0.0)', 'OPEN_RANGE');
                testBundleAndDependency(bundles, 'module-dependant-case21', false, '', 'EMPTY');
                testBundleAndDependency(bundles, 'module-dependant-case22', true, '', 'EMPTY');
                testBundleAndDependency(bundles, 'module-dependant-case23', false, '1.1.0', 'OPEN_RANGE');
                testBundleAndDependency(bundles, 'module-dependant-case24', true, '1.1.0', 'OPEN_RANGE');
                testBundleAndDependency(bundles, 'module-dependant-case25', true, '1.1.1', 'OPEN_RANGE');
                testBundle(bundles, 'module-dependant-case26', '[1.0.0,2.0.0)', 'OPEN_RANGE');
                testBundle(bundles, 'module-dependant-case27', '[1.0.0,1.99.0)', 'OPEN_RANGE');
                testBundle(bundles, 'module-dependant-case31', '[1.1.0,1.2.0)', 'RESTRICTIVE_RANGE');
                testBundleAndDependency(bundles, 'module-dependant-case32', false, '[1.0.0,1.2.0)', 'RESTRICTIVE_RANGE');
                testBundle(bundles, 'module-dependant-case33', '1.1.0', 'STRICT_NO_RANGE');
                testBundle(bundles, 'module-dependant-case34', '[1.1.0,1.2.0)', 'RESTRICTIVE_RANGE');
            });
        });

        it('Test invalid regular expression for nameRegExp parameter', () => {
            getBundles({nameRegExp: '*invalid.*'}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('errors');
                expect(result.errors).to.have.length(1);
                expect(result.errors[0].message).to.eq('Invalid regular expression: *invalid.*');
            });
        });

        it('Test withUnsupportedDependenciesOnly parameter', () => {
            getBundles({nameRegExp: 'module-dep.*', withUnsupportedDependenciesOnly: true}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('data');
                expect(result.data).to.have.property('admin');
                expect(result.data.admin).to.have.property('tools');
                expect(result.data.admin.tools).to.have.property('bundles');
                // Only the bundles with unsupported dependencies are returned:
                expect(result.data.admin.tools.bundles).to.have.length(10);
                const expectedBundles = [
                    'module-dependant-case10',
                    'module-dependant-case21',
                    'module-dependant-case22',
                    'module-dependant-case23',
                    'module-dependant-case24',
                    'module-dependant-case25',
                    'module-dependant-case31',
                    'module-dependant-case32',
                    'module-dependant-case33'
                ];
                expectedBundles.forEach(bundleName => {
                    const bundleExists = result.data.admin.tools.bundles.some(b => b.bundleSymbolicName === bundleName);
                    expect(bundleExists).to.be.true;
                });
            });
        });

        it('Test both parameters "supported" and "statuses" can not be used together', () => {
            const statuses = [Status.RESTRICTIVE_RANGE, Status.OPEN_RANGE];
            getBundles({supported: true, statuses: statuses}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('errors');
                expect(result.errors[0].message).to.eq('The \'supported\' and \'statuses\' parameters cannot be used together');
            });
        });
        it('Test "supported" parameter set to true', () => {
            getBundles({nameRegExp: 'module-dependant-case10', supported: true}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('data');
                expect(result.data).to.have.property('admin');
                expect(result.data.admin).to.have.property('tools');
                expect(result.data.admin.tools).to.have.property('bundles');
                expect(result.data.admin.tools.bundles).to.have.length(1);
                const bundle = result.data.admin.tools.bundles[0];
                expect(bundle.dependencies).to.have.length(1);
                expect(bundle.dependencies[0].name).to.eq('org.external.modules.provider');
                expect(bundle.dependencies[0].status).to.eq('OPEN_RANGE');
            });
        });
        it('Test "supported" parameter set to false', () => {
            getBundles({nameRegExp: 'module-dependant-case10', supported: false}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('data');
                expect(result.data).to.have.property('admin');
                expect(result.data.admin).to.have.property('tools');
                expect(result.data.admin.tools).to.have.property('bundles');
                expect(result.data.admin.tools.bundles).to.have.length(1);
                const bundle = result.data.admin.tools.bundles[0];
                expect(bundle.dependencies).to.have.length(4);
                const expectedBundles = [
                    'org.apache.naming.java',
                    'org.jahia.defaults.config.spring',
                    'org.jahia.exceptions',
                    'org.jahia.services'
                ];
                expectedBundles.forEach(dependencyName => {
                    const depExists = bundle.dependencies.some(b => b.name === dependencyName);
                    expect(depExists).to.be.true;
                });
            });
        });
        it('Test "statuses" parameter set to valid value with result', () => {
            const statuses = [Status.OPEN_RANGE];
            getBundles({nameRegExp: 'module-dependant-case10', statuses: statuses}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('data');
                expect(result.data).to.have.property('admin');
                expect(result.data.admin).to.have.property('tools');
                expect(result.data.admin.tools).to.have.property('bundles');
                expect(result.data.admin.tools.bundles).to.have.length(1);
                const bundle = result.data.admin.tools.bundles[0];
                expect(bundle.dependencies).to.have.length(1);
                expect(bundle.dependencies[0].name).to.eq('org.external.modules.provider');
                expect(bundle.dependencies[0].status).to.eq('OPEN_RANGE');
            });
        });
        it('Test "statuses" parameter set to valid value with no result', () => {
            const statuses = [Status.SINGLE_VERSION_RANGE];
            getBundles({nameRegExp: 'module-dependant-case10', statuses: statuses}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('data');
                expect(result.data).to.have.property('admin');
                expect(result.data.admin).to.have.property('tools');
                expect(result.data.admin.tools).to.have.property('bundles');
                expect(result.data.admin.tools.bundles).to.have.length(1);
                const bundle = result.data.admin.tools.bundles[0];
                expect(bundle.dependencies).to.have.length(0);
            });
        });
        it('Test "statuses" parameter set to invalid value', () => {
            getBundles({nameRegExp: 'module-dependant-case10', statuses: []}).should(result => {
                console.log('result', result);
                expect(result).to.have.property('errors');
                expect(result.errors[0].message).to.eq('At least one status must be provided (via the \'statuses\' parameter)');
            });
        });
    });
});
