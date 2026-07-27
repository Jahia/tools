import {defineConfig} from 'cypress';

export default defineConfig({
    // Every tool JSP of the module, relative to /modules/tools/. Used by toolsAccess.spec.ts to assert that the
    // tools-access authorization is enforced on each of them. The list is static configuration on purpose: the tests
    // run from a docker image that only contains the tests/ folder, so the module sources are not readable at runtime.
    // When a JSP is added to or removed from impl/src/main/resources, update this list accordingly.
    env: {
        TOOL_JSPS: [
            'actions.jsp',
            'benchmarks.jsp',
            'cache.jsp',
            'checklocks.jsp',
            'choicelistInitializersRenderers.jsp',
            'ckeditorConfig.jsp',
            'dbQuery.jsp',
            'definitionsBrowser.jsp',
            'docConverter.jsp',
            'ehcache/ehcache_cj.jsp',
            'ehcache/ehcache_cj_dep.jsp',
            'ehcache/ehcache_details.jsp',
            'ehcache/ehcache_dump.jsp',
            'ehcache/ehcache_stats.jsp',
            'errorFileDumper.jsp',
            'groovyConsole.jsp',
            'importPackageChecker.jsp',
            'index.jsp',
            'jcrBrowser.jsp',
            'jcrConsole.jsp',
            'jcrExternalProviders.jsp',
            'jcrGc.jsp',
            'jcrIntegrityTools.jsp',
            'jcrJarsCleanup.jsp',
            'jcrQuery.jsp',
            'jcrQueryStats.jsp',
            'jcrSessions.jsp',
            'jcrVersionHistory.jsp',
            'jobInfo.jsp',
            'jobadmin.jsp',
            'karaf.jsp',
            'log4jAdmin.jsp',
            'maintenance.jsp',
            'memoryInfo.jsp',
            'modules.jsp',
            'modulesBrowser.jsp',
            'nodesVersusCndConsistency.jsp',
            'packageWiresAnalyzer.jsp',
            'provisioning.jsp',
            'pwdEncrypt.jsp',
            'render.jsp',
            'renderDump.jsp',
            'renderFilters.jsp',
            'rules.jsp',
            'search.jsp',
            'searchIndexCheck.jsp',
            'support.jsp',
            'systemInfo.jsp',
            'textExtractor.jsp',
            'threadDump.jsp',
            'threadDumpMgmt.jsp',
            'viewsession.jsp',
            'wcagChecker.jsp',
            'workflows.jsp'
        ]
    },
    chromeWebSecurity: false,
    defaultCommandTimeout: 30000,
    video: true,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
        configFile: 'reporter-config.json'
    },
    screenshotsFolder: './results/screenshots',
    videosFolder: './results/videos',
    viewportWidth: 1366,
    viewportHeight: 768,
    watchForFileChanges: false,
    e2e: {
        // We've imported your old cypress plugins here.
        // You may want to clean this up later by importing tcleahese.
        setupNodeEvents(on, config) {
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            require('cypress-terminal-report/src/installLogsPrinter')(on);
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            return require('./cypress/plugins/index.js')(on, config);
        },
        excludeSpecPattern: '*.ignore.ts',
        specPattern: [
            'cypress/e2e/api/testStart.spec.begin.ts',
            'cypress/e2e/*.spec.ts',
            'cypress/e2e/api/*.spec.begin.ts',
            'cypress/e2e/api/*.spec.ts',
            'cypress/e2e/api/shutdown.spec.final.ts'
        ],
        baseUrl: 'http://localhost:8080',
        experimentalSessionAndOrigin: false
    }
});
