import {findDependencies} from '../../support/gql';
import {waitUntilSAMStatusGreen} from "@jahia/cypress";

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
            findDependencies({regexp: 'module-dep.*', strictVersionsOnly: false}).should(r => {
                console.log(r);

                const case10 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case10');
                expect(case10.dependenciesUpgradables).to.eq(true);
                const case10Import = case10.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case10Import.type).to.eq('IMPORT_PACKAGE');
                expect(case10Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case10Import.optional).to.eq(false);
                expect(case10Import.error).to.eq('');
                expect(case10Import.summary).to.contains('status=OPEN_RANGE');

                const case11 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case11');
                expect(case11.dependenciesUpgradables).to.eq(true);
                const case11Import = case11.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case11Import.type).to.eq('IMPORT_PACKAGE');
                expect(case11Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case11Import.optional).to.eq(false);
                expect(case11Import.error).to.eq('');
                expect(case11Import.summary).to.contains('status=OPEN_RANGE');

                const case21 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case21');
                expect(case21.dependenciesUpgradables).to.eq(true);
                const case21Import = case21.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case21Import.type).to.eq('IMPORT_PACKAGE');
                expect(case21Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case21Import.optional).to.eq(false);
                expect(case21Import.error).to.eq('');
                expect(case21Import.summary).to.contains('status=OPEN_RANGE');
                const case21Depends = case21.dependencies.find(dep => dep.name === 'module-provider');
                expect(case21Depends.type).to.eq('JAHIA_DEPENDS');
                expect(case21Depends.version).to.eq('');
                expect(case21Depends.optional).to.eq(false);
                expect(case21Depends.error).to.eq('');
                expect(case21Depends.summary).to.contains('status=EMPTY');

                const case22 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case22');
                expect(case22.dependenciesUpgradables).to.eq(true);
                const case22Import = case22.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case22Import.type).to.eq('IMPORT_PACKAGE');
                expect(case22Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case22Import.optional).to.eq(false);
                expect(case22Import.error).to.eq('');
                expect(case22Import.summary).to.contains('status=OPEN_RANGE');
                const case22Depends = case22.dependencies.find(dep => dep.name === 'module-provider');
                expect(case22Depends.type).to.eq('JAHIA_DEPENDS');
                expect(case22Depends.version).to.eq('');
                expect(case22Depends.optional).to.eq(true);
                expect(case22Depends.error).to.eq('');
                expect(case22Depends.summary).to.contains('status=EMPTY');

                const case23 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case23');
                expect(case23.dependenciesUpgradables).to.eq(true);
                const case23Import = case23.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case23Import.type).to.eq('IMPORT_PACKAGE');
                expect(case23Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case23Import.optional).to.eq(false);
                expect(case23Import.error).to.eq('');
                expect(case23Import.summary).to.contains('status=OPEN_RANGE');
                const case23Depends = case23.dependencies.find(dep => dep.name === 'module-provider');
                expect(case23Depends.type).to.eq('JAHIA_DEPENDS');
                expect(case23Depends.version).to.eq('1.1.0');
                expect(case23Depends.optional).to.eq(false);
                expect(case23Depends.error).to.eq('');
                expect(case23Depends.summary).to.contains('status=OPEN_RANGE');

                const case24 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case24');
                expect(case24.dependenciesUpgradables).to.eq(true);
                const case24Import = case24.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case24Import.type).to.eq('IMPORT_PACKAGE');
                expect(case24Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case24Import.optional).to.eq(false);
                expect(case24Import.error).to.eq('');
                expect(case24Import.summary).to.contains('status=OPEN_RANGE');
                const case24Depends = case24.dependencies.find(dep => dep.name === 'module-provider');
                expect(case24Depends.type).to.eq('JAHIA_DEPENDS');
                expect(case24Depends.version).to.eq('1.1.0');
                expect(case24Depends.optional).to.eq(true);
                expect(case24Depends.error).to.eq('');
                expect(case24Depends.summary).to.contains('status=OPEN_RANGE');

                const case25 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case25');
                expect(case25.dependenciesUpgradables).to.eq(true);
                const case25Import = case25.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case25Import.type).to.eq('IMPORT_PACKAGE');
                expect(case25Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case25Import.optional).to.eq(false);
                expect(case25Import.error).to.eq('');
                expect(case25Import.summary).to.contains('status=OPEN_RANGE');
                const case25Depends = case25.dependencies.find(dep => dep.name === 'module-provider');
                expect(case25Depends.type).to.eq('JAHIA_DEPENDS');
                expect(case25Depends.version).to.eq('1.1.1');
                expect(case25Depends.optional).to.eq(true);
                expect(case25Depends.error).to.eq('');
                expect(case25Depends.summary).to.contains('status=OPEN_RANGE');

                const case26 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case26');
                expect(case26.dependenciesUpgradables).to.eq(true);
                const case26Import = case26.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case26Import.type).to.eq('IMPORT_PACKAGE');
                expect(case26Import.version).to.eq('[1.0.0,2.0.0)');
                expect(case26Import.optional).to.eq(false);
                expect(case26Import.error).to.eq('');
                expect(case26Import.summary).to.contains('status=OPEN_RANGE');

                const case27 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case27');
                expect(case27.dependenciesUpgradables).to.eq(true);
                const case27Import = case27.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case27Import.type).to.eq('IMPORT_PACKAGE');
                expect(case27Import.version).to.eq('[1.0.0,1.99.0)');
                expect(case27Import.optional).to.eq(false);
                expect(case27Import.error).to.eq('');
                expect(case27Import.summary).to.contains('status=OPEN_RANGE');

                const case31 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case31');
                expect(case31.dependenciesUpgradables).to.eq(false);
                const case31Import = case31.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case31Import.type).to.eq('IMPORT_PACKAGE');
                expect(case31Import.version).to.eq('[1.1.0,1.2.0)');
                expect(case31Import.optional).to.eq(false);
                expect(case31Import.error).to.eq('');
                expect(case31Import.summary).to.contains('status=RESTRICTIVE_RANGE');

                const case32 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case32');
                expect(case32.dependenciesUpgradables).to.eq(false);
                const case32Import = case32.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case32Import.type).to.eq('IMPORT_PACKAGE');
                expect(case32Import.version).to.eq('[1.1.0,2.0.0)');
                expect(case32Import.optional).to.eq(false);
                expect(case32Import.error).to.eq('');
                expect(case32Import.summary).to.contains('status=OPEN_RANGE');
                const case32Depends = case32.dependencies.find(dep => dep.name === 'module-provider');
                expect(case32Depends.type).to.eq('JAHIA_DEPENDS');
                expect(case32Depends.version).to.eq('[1.0.0,1.2.0)');
                expect(case32Depends.optional).to.eq(false);
                expect(case32Depends.error).to.eq('');
                expect(case32Depends.summary).to.contains('status=RESTRICTIVE_RANGE');

                const case33 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case33');
                expect(case33.dependenciesUpgradables).to.eq(false);
                const case33Import = case33.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case33Import.type).to.eq('IMPORT_PACKAGE');
                expect(case33Import.version).to.eq('1.1.0');
                expect(case33Import.optional).to.eq(false);
                expect(case33Import.error).to.eq('');
                expect(case33Import.summary).to.contains('status=STRICT_NO_RANGE');

                const case34 = r.bundles.find(bundle => bundle.bundleSymbolicName === 'module-dependant-case34');
                expect(case34.dependenciesUpgradables).to.eq(false);
                const case34Import = case34.dependencies.find(dep => dep.name === 'org.external.modules.provider');
                expect(case34Import.type).to.eq('IMPORT_PACKAGE');
                expect(case34Import.version).to.eq('[1.1.0,1.2.0)');
                expect(case34Import.optional).to.eq(false);
                expect(case34Import.error).to.eq('');
                expect(case34Import.summary).to.contains('status=RESTRICTIVE_RANGE');
            });
        });
    });
});
