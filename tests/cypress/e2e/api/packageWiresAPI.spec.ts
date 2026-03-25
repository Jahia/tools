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
} from '../../utils/bundleManagement';
import {
    containsPackageWires,
    DEPRECATION_FACTORY_PID,
    getDeprecatedPackageWires,
    getPackageWires,
    PackageWire,
    packageWireDef,
    PATTERN_ALL,
    PATTERN_CORE,
    PATTERN_CORE_API,
    PATTERN_CORE_UTIL,
    PATTERN_UTIL,
    PATTERN_UTIL_COLLECTIONS,
    PATTERN_UTIL_IO,
    setDeprecationCustomConfig
} from '../../utils/packageWires';

// ── Setup strategy ────────────────────────────────────────────────────────────
// Start all consumers ONCE before the whole suite.
// Happy-path tests need no per-test setup at all.
// Edge-case tests stop only the specific bundle they need absent,
// then restart it afterwards so the suite state is always clean.
// Provider bundles (core-lib, util-lib, other-lib) are always running.
// consumer-stopped is intentionally left stopped throughout.

describe('Package wires GraphQL API', () => {
    before(() => {
        cy.log('Starting all consumer bundles for the suite...');
        startBundle(CONSUMER_CORE);
        startBundle(CONSUMER_UTIL);
        startBundle(CONSUMER_MIXED);
        startBundle(CONSUMER_DYNAMIC);
        startBundle(CONSUMER_OPTIONAL);
        // CONSUMER_STOPPED is intentionally NOT started — it stays stopped for the whole suite
        stopBundle(CONSUMER_STOPPED);
    });

    after(() => {
        // Restart the stopped bundle to leave the suite clean (and prevent the SAM probe to fail)
        startBundle(CONSUMER_STOPPED);
    });

    beforeEach(() => {
        refreshBundle(CONSUMER_DYNAMIC); // Refresh the bundle to remove the dynamic imports
    });

    describe('packageWires', () => {
        it('Should return wires matching a single pattern when no provider filter is given', () => {
            getPackageWires(null, [PATTERN_CORE]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core'), // Static import
                    packageWireDef(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core')
                ]);
            });
        });

        it('Should return wires from all matching providers when multiple patterns are given and no provider filter is set', () => {
            getPackageWires(null, [PATTERN_CORE, PATTERN_UTIL]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    // Core wires
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core'), // Static import
                    packageWireDef(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core'),
                    // Util wires
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections')
                ]);
            });
        });

        it('Should return only wires from the specified provider when a provider filter is given', () => {
            getPackageWires(CORE_LIB, [PATTERN_CORE]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core'), // Static import
                    packageWireDef(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core')
                ]);
            });
        });

        it('Should return only wires from the filtered provider when multiple patterns are given but only one provider matches', () => {
            // Both core and util patterns given, but util-lib filter suppresses core wires
            getPackageWires(UTIL_LIB, [PATTERN_CORE, PATTERN_UTIL]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections')
                ]);
            });
        });

        it('Should include the dynamic imports only when those imports are in use', () => {
            const utilRegex = 'org\\.jahia\\.test\\.util';
            // Before activating the service, no wires are expected towards test-consumer-dynamic
            getPackageWires(CORE_LIB, [PATTERN_CORE_UTIL]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, []);
            });
            getPackageWires(UTIL_LIB, [utilRegex]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    // Contains other consumers, but not test-consumer-dynamic
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util')
                ]);
            });

            // Activate the service
            cy.executeGroovy('groovy/activateDynamicConsumer.groovy', {
                BUNDLE_SYMBOLIC_NAME: 'javascript-modules-engine-test-module'
            }).then(result => {
                expect(result).to.contain('Core Service');
            });

            // Now, the wires should be returned
            getPackageWires(CORE_LIB, [PATTERN_CORE_UTIL]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    packageWireDef(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core.util') // Dynamic wire
                ]);
            });
            getPackageWires(UTIL_LIB, [utilRegex]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, [
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util'),
                    // Now, test-consumer-dynamic is included
                    packageWireDef(UTIL_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.util')
                ]);
            });
        });

        it('Should return a GraphQL error when the provider bundle does not exist', () => {
            // Error is thrown before the bundle scan — no setup needed
            getPackageWires('org.jahia.test.nonexistent.lib', [PATTERN_CORE]).should(result => {
                expect(result).to.have.property('errors');
                expect(result.errors).to.have.length.greaterThan(0);
                expect(result.errors[0].message).to.contain('org.jahia.test.nonexistent.lib');
            });
        });

        it('Should return an empty list when no package name matches the given pattern', () => {
            getPackageWires(null, ['org\\.jahia\\.test\\.nonexistent\\..*']).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, []);
            });
        });

        it('Should not include a bundle in results when it is stopped', () => {
            // Consumer-stopped is never started in this suite (see before())
            getPackageWires(null, [PATTERN_CORE]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                const stoppedAppears = wires.some(w => w.requirerBundle.symbolicName === CONSUMER_STOPPED);
                expect(stoppedAppears, `${CONSUMER_STOPPED} should not appear in results`).to.be.false;
            });
        });

        it('Should not include a bundle in results when none of its wires match the pattern', () => {
            // Consumer-nomatches only imports org.jahia.test.other — never matches core/util patterns
            getPackageWires(null, [PATTERN_CORE]).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                const noMatchAppears = wires.some(w => w.requirerBundle.symbolicName === 'org.jahia.test.consumer.nomatches');
                expect(noMatchAppears, 'consumer-nomatches should not appear in results').to.be.false;
            });
        });

        it('Should return an empty list when the matching import is optional and unresolved', () => {
            // Consumer-optional declares org.jahia.test.nonexistent;resolution:=optional
            // No bundle exports it, so no wire is established
            getPackageWires(null, ['org\\.jahia\\.test\\.nonexistent(\\..*)?']).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                containsPackageWires(wires, []);
            });
        });

        it('Should include internal Jahia modules when showInternal=true is passed as a request parameter', () => {
            // The Tools module itself has Jahia-GroupId: org.jahia.modules and imports javax.servlet.*
            // Without showInternal it must be absent; with it, it must appear.
            getPackageWires(null, ['javax\\.servlet\\..*']).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                const toolsAppears = wires.some(w => w.requirerBundle.symbolicName === 'tools');
                expect(toolsAppears, 'tools module should NOT appear without showInternal').to.be.false;
            });
            getPackageWires(null, ['javax\\.servlet\\..*'], true).should(result => {
                expect(result).to.have.property('data');
                const wires: PackageWire[] = result.data.admin.tools.packageWires;
                const toolsAppears = wires.some(w => w.requirerBundle.symbolicName === 'tools');
                expect(toolsAppears, 'tools module should appear with showInternal=true').to.be.true;
            });
        });
    });

    describe('deprecatedPackageWires', () => {
        beforeEach(() => {
            deleteAllConfigurations(DEPRECATION_FACTORY_PID);
        });

        after(() => {
            deleteAllConfigurations(DEPRECATION_FACTORY_PID);
        });

        it('Should return all wires from all configured providers when a wildcard pattern is set', () => {
            const deprecatedPatternsByProvider = new Map<string, string[]>();
            deprecatedPatternsByProvider.set(CORE_LIB, [PATTERN_ALL]);
            deprecatedPatternsByProvider.set(UTIL_LIB, [PATTERN_ALL]);
            setDeprecationCustomConfig(deprecatedPatternsByProvider);
            getDeprecatedPackageWires().then(result => {
                const wires: PackageWire[] = result?.data?.admin?.tools?.deprecatedPackageWires;
                expect(wires).to.be.an('array');
                containsPackageWires(wires, [
                    // Core wires
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_DYNAMIC, 'org.jahia.test.core'), // Static import
                    packageWireDef(CORE_LIB, CONSUMER_OPTIONAL, 'org.jahia.test.core'),
                    // Util wires
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections')
                ]);
            });
        });

        it('Should return only wires matching the configured patterns when multiple specific patterns are set per provider', () => {
            const deprecatedPatternsByProvider = new Map<string, string[]>();
            deprecatedPatternsByProvider.set(CORE_LIB, [PATTERN_CORE_API, PATTERN_CORE_UTIL]);
            deprecatedPatternsByProvider.set(UTIL_LIB, [PATTERN_UTIL_COLLECTIONS, PATTERN_UTIL_IO]);
            setDeprecationCustomConfig(deprecatedPatternsByProvider);
            getDeprecatedPackageWires().then(result => {
                const wires: PackageWire[] = result?.data?.admin?.tools?.deprecatedPackageWires;
                expect(wires).to.be.an('array');
                containsPackageWires(wires, [
                    // Core wires
                    packageWireDef(CORE_LIB, CONSUMER_CORE, 'org.jahia.test.core.api'),
                    packageWireDef(CORE_LIB, CONSUMER_MIXED, 'org.jahia.test.core.api'),
                    // Util wires
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.collections'),
                    packageWireDef(UTIL_LIB, CONSUMER_UTIL, 'org.jahia.test.util.io'),
                    packageWireDef(UTIL_LIB, CONSUMER_MIXED, 'org.jahia.test.util.collections')
                ]);
            });
        });

        it('Should return a GraphQL error when no deprecated patterns are configured', () => {
            setDeprecationCustomConfig(new Map()); // Empty map → no patterns
            getDeprecatedPackageWires().then(result => {
                expect(result).to.have.property('errors');
                expect(result.errors).to.have.length.greaterThan(0);
                expect(result.errors[0].message).to.contain('No deprecated patterns configured');
            });
        });
    });
});

