import {addNode, createSite, deleteSite, getNodeByPath} from '@jahia/cypress';
import gql from 'graphql-tag';

const TEST_BUNDLE = 'tools-test-with-definitions-cnd/0.0.1';
const SITE_KEY = 'testSite';
const testComponentSelector = 'a[id="toolstestwithdefinitionscnd_testComponent"]';
const otherComponentSelector = 'a[id="toolstestwithdefinitionscnd_otherComponent"]';

function checkNodeDoesNotExist(path: string) {
    cy.apollo({
        errorPolicy: 'all',
        query: gql`
            query {
                jcr(workspace: LIVE) {
                    nodeByPath(path:"${path}") {
                        path
                    }
                }
            }
        `
    }).should(response => {
        expect(response.errors).to.exist;
        expect(response.errors[0].message).to.eq(`javax.jcr.PathNotFoundException: ${path}`);
    });
}

function checkNodeExists(path: string, propertyName: string = undefined) {
    getNodeByPath(path, [propertyName]).then(res => {
        expect(res.data?.jcr?.nodeByPath?.uuid).to.exist;
        if (propertyName) {
            expect(res.data?.jcr?.nodeByPath?.properties[0]?.name).to.eq(propertyName);
            expect(res.data?.jcr?.nodeByPath?.properties[0]?.value).to.eq('sample');
        }
    });
}

function restartBundle() {
    cy.runProvisioningScript([{stopBundle: TEST_BUNDLE}]);
    cy.runProvisioningScript([{startBundle: TEST_BUNDLE}]);
}

describe('definitions browser tests (/modules/tools/definitionsBrowser.jsp)', () => {
    beforeEach(() => {
        cy.installBundle('testData/definitionsBrowser/tools-test-with-definitions-cnd.jar');
        cy.runProvisioningScript([{startBundle: TEST_BUNDLE}]);
        // Create a site
        createSite(SITE_KEY, {
            templateSet: 'tools-test-with-definitions-cnd',
            serverName: 'localhost',
            locale: 'en'
        });
        // Create content and ensure it is created
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}`,
            name: 'testNode',
            primaryNodeType: 'toolstestwithdefinitionscnd:testComponent',
            properties: [
                {name: 'testProp', value: 'sample'}
            ]
        });
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}`,
            name: 'otherNode',
            primaryNodeType: 'toolstestwithdefinitionscnd:otherComponent',
            properties: [
                {name: 'otherProp', value: 'sample'}
            ]
        });
        checkNodeExists(`/sites/${SITE_KEY}/testNode`, 'testProp');
        checkNodeExists(`/sites/${SITE_KEY}/otherNode`, 'otherProp');
    });
    afterEach(() => {
        deleteSite(SITE_KEY);
        cy.runProvisioningScript([{uninstallBundle: TEST_BUNDLE}]);
    });

    it('the bundle should not be listed once removed from the definitions browser page', () => {
        // GIVEN:
        // Going to the list of definitions:
        cy.login();
        cy.visit('/modules/tools/definitionsBrowser.jsp');
        // The module is listed:
        const toolsTestModuleSelector = 'a[id="tools-test-with-definitions-cnd"]';
        cy.get(toolsTestModuleSelector).should('be.visible');
        // Both node types are listed:
        cy.get(testComponentSelector).should('be.visible');
        cy.get(otherComponentSelector).should('be.visible');

        // WHEN:
        // removing the module:
        cy.get(toolsTestModuleSelector).siblings('a[href="#delete"][class="delete-definitions"]').click();

        // THEN:
        // The module is removed:
        cy.get(toolsTestModuleSelector).should('not.exist');
        // And the content using that node type are removed:
        checkNodeDoesNotExist(`/sites/${SITE_KEY}/testNode`);
        checkNodeDoesNotExist(`/sites/${SITE_KEY}/otherNode`);
        // Same state after restarting the bundle:
        restartBundle();
        cy.reload();
        cy.get(toolsTestModuleSelector).should('not.exist');
        checkNodeDoesNotExist(`/sites/${SITE_KEY}/testNode`);
        checkNodeDoesNotExist(`/sites/${SITE_KEY}/otherNode`);
    });

    it('the component (node type) should not be listed once removed from the definitions browser page', () => {
        // GIVEN:
        // Going to the list of definitions:
        cy.login();
        cy.visit('/modules/tools/definitionsBrowser.jsp');
        // Both node types are listed:
        cy.get(testComponentSelector).should('be.visible');
        cy.get(otherComponentSelector).should('be.visible');

        // WHEN:
        // deleting the component:
        cy.get(testComponentSelector).siblings('a[href="#delete"][class="delete-nodetype"]').click();

        // THEN:
        // The node type is removed:
        cy.get(testComponentSelector).should('not.exist');
        // But the other one is still present:
        cy.get(otherComponentSelector).should('exist');
        // The page using the node type removed has also been removed:
        checkNodeDoesNotExist(`/sites/${SITE_KEY}/testNode`);
        // But not the page of the other node type:
        checkNodeExists(`/sites/${SITE_KEY}/otherNode`);
        // Same state after restarting the bundle:
        restartBundle();
        cy.reload();
        cy.get(testComponentSelector).should('not.exist');
        cy.get(otherComponentSelector).should('exist');
        checkNodeDoesNotExist(`/sites/${SITE_KEY}/testNode`);
        checkNodeExists(`/sites/${SITE_KEY}/otherNode`);
    });
});
