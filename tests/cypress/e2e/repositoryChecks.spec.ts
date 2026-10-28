import {addNode} from '@jahia/cypress';
import gql from 'graphql-tag';
import {openTool} from '../support/tools';

// The lock check, integrity check and benchmark tools report the nodes they visit by path, and display each path as
// text.
const PARENT_PATH = '/sites/systemsite/contents/repository-checks';
const PLAIN_PATH = `${PARENT_PATH}/plain`;
const NAMED_PATH = `${PARENT_PATH}/Q&A <draft>`;

function lockNode(path: string): void {
    cy.apollo({
        mutation: gql`mutation($path: String!) {
            jcr { mutateNode(pathOrId: $path) { addMixins(mixins: ["jmix:lockable"]) } }
        }`,
        variables: {path}
    });
    cy.apollo({
        mutation: gql`mutation($path: String!) {
            jcr { mutateNode(pathOrId: $path) { lock(type: "user") } }
        }`,
        variables: {path}
    });
}

function unlockNode(path: string): void {
    cy.apollo({
        errorPolicy: 'all',
        mutation: gql`mutation($path: String!) {
            jcr { mutateNode(pathOrId: $path) { unlock(type: "user", force: true) } }
        }`,
        variables: {path}
    });
}

// Removes the nodes of a previous run, whether or not they exist.
function removeTestNodes(): void {
    unlockNode(PLAIN_PATH);
    unlockNode(NAMED_PATH);
    cy.apollo({
        errorPolicy: 'all',
        mutation: gql`mutation($path: String!) { jcr { deleteNode(pathOrId: $path) } }`,
        variables: {path: PARENT_PATH}
    });
}

function nodeUuid(path: string): Cypress.Chainable<string> {
    return cy.apollo({
        query: gql`query($path: String!) { jcr { nodeByPath(path: $path) { uuid } } }`,
        variables: {path}
    }).its('data.jcr.nodeByPath.uuid');
}

describe('Repository check tools (/modules/tools/checklocks.jsp, jcrIntegrityTools.jsp, benchmarks.jsp)', () => {
    before(() => {
        cy.login();
        removeTestNodes();
        addNode({
            parentPathOrId: '/sites/systemsite/contents',
            name: 'repository-checks',
            primaryNodeType: 'jnt:contentFolder',
            children: [{name: 'plain', primaryNodeType: 'jnt:contentFolder'}]
        });
        cy.executeGroovy('groovy/addFolder.groovy', {PARENT_PATH, FOLDER_NAME: 'Q&A <draft>'});
        cy.executeGroovy('groovy/addDanglingReference.groovy', {PARENT_PATH: NAMED_PATH});
        lockNode(PLAIN_PATH);
        lockNode(NAMED_PATH);
    });

    beforeEach(() => {
        cy.login();
    });

    after(() => {
        cy.login();
        removeTestNodes();
        cy.logout();
    });

    it('lists the locked nodes by path, exactly as the paths are written', () => {
        openTool('/modules/tools/checklocks.jsp', {workspace: 'default', operation: 'runJCRTest'}).then(doc => {
            const text = doc.body.textContent;
            expect(text, 'report of the plain path').to.contain(`${PLAIN_PATH} `);
            expect(text, 'report of the path with special characters').to.contain(`${NAMED_PATH} `);
        });
    });

    it('names the node an integrity check starts from, exactly as its path is written', () => {
        nodeUuid(NAMED_PATH).then(uuid => {
            openTool('/modules/tools/jcrIntegrityTools.jsp', {workspace: 'default', uuid, operation: 'runJCRTest'}).then(doc => {
                expect(doc.body.textContent, 'start path').to.contain(`Running integrity check from path: ${NAMED_PATH}\n`);
            });
        });
    });

    it('names the property of a reference that cannot be resolved, exactly as its path is written', () => {
        nodeUuid(NAMED_PATH).then(uuid => {
            openTool('/modules/tools/jcrIntegrityTools.jsp', {workspace: 'default', uuid, operation: 'runJCRTest'}).then(doc => {
                const warnings = Array.from(doc.querySelectorAll('span.warning')).map(span => span.textContent);
                expect(warnings.some(text => text.endsWith(`referenced from property ${NAMED_PATH}/reference/j:node`)), 'reference warning').to.be.true;
            });
        });
    });

    it('names the property of a reference that cannot be resolved in the JCR read benchmark, exactly as its path is written', () => {
        openTool('/modules/tools/benchmarks.jsp', {do: 'true', runJCRTest: 'on'}).then(doc => {
            const warnings = Array.from(doc.querySelectorAll('span.warning')).map(span => span.textContent);
            expect(warnings.some(text => text.endsWith(`referenced from ${NAMED_PATH}/reference/j:node`)), 'reference warning').to.be.true;
        });
    });
});
