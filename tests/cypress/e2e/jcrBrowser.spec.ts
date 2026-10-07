import {addNode, deleteNode} from '@jahia/cypress';

// Node names, paths and property names may carry characters that are significant in JavaScript and HTML. The JCR
// browser must display them as typed, and its breadcrumb links must still lead to the exact ancestor path.
const PARENT_PATH = '/sites/systemsite/files';
const FOLDER_NAME = 'Rock \'n\' roll';
const FOLDER_PATH = `${PARENT_PATH}/${FOLDER_NAME}`;
const CHILD_PATH = `${FOLDER_PATH}/leaf`;

// Opens the JCR browser with the given parameters, as an authenticated administrator, and returns the parsed page.
function openJcrBrowser(qs: Record<string, string>): Cypress.Chainable<Document> {
    return cy.request('POST', '/modules/tools/token').its('body.token').then(toolAccessToken => {
        return cy.request({url: '/modules/tools/jcrBrowser.jsp', qs: {...qs, toolAccessToken}});
    }).then(response => new DOMParser().parseFromString(response.body, 'text/html'));
}

// Runs the click handler of a link against a stubbed go(), and returns the arguments it was called with.
function navigationOf(onclick: string): string[] {
    const calls: string[][] = [];
    // eslint-disable-next-line no-new-func
    new Function('go', onclick)((...args: string[]) => calls.push(args));
    expect(calls, 'go() calls').to.have.length(1);
    return calls[0];
}

describe('JCR browser (/modules/tools/jcrBrowser.jsp)', () => {
    before(() => {
        addNode({
            parentPathOrId: PARENT_PATH,
            name: FOLDER_NAME,
            primaryNodeType: 'jnt:folder',
            children: [{name: 'leaf', primaryNodeType: 'jnt:folder'}]
        });
    });

    after(() => {
        deleteNode(FOLDER_PATH);
    });

    beforeEach(() => {
        cy.login();
    });

    afterEach(() => {
        cy.logout();
    });

    it('navigates to the exact ancestor path when a node name carries special characters', () => {
        openJcrBrowser({path: CHILD_PATH}).then(doc => {
            const links = Array.from(doc.querySelectorAll('a[href="#breadcrumbs"]'));
            expect(links.map(a => a.textContent), 'breadcrumb links').to.include(FOLDER_NAME);
            const link = links.find(a => a.textContent === FOLDER_NAME);
            expect(navigationOf(link.getAttribute('onclick'))).to.deep.eq(['path', FOLDER_PATH]);
        });
    });

    it('displays a path that does not exist exactly as it was requested', () => {
        const missingPath = `${PARENT_PATH}/Q&A <draft>`;
        openJcrBrowser({path: missingPath}).then(doc => {
            const messages = Array.from(doc.querySelectorAll('p'))
                .filter(p => p.textContent.startsWith('Item with the path'));
            expect(messages.map(p => p.querySelector('strong')?.textContent), 'not-found message').to.deep.eq([missingPath]);
        });
    });

    it('displays a property name that does not exist exactly as it was requested', () => {
        const missingProperty = 'Q&A <draft>';
        openJcrBrowser({path: FOLDER_PATH, action: 'removeProperty', value: missingProperty}).then(doc => {
            const messages = Array.from(doc.querySelectorAll('p'))
                .filter(p => p.textContent.startsWith('Cannot find property'));
            expect(messages.map(p => p.textContent.replace(/\s+/g, ' ').trim()), 'not-found message')
                .to.deep.eq([`Cannot find property ${missingProperty} on the node ${FOLDER_PATH}`]);
        });
    });

    it('displays the requested value exactly as it was sent when an action fails', () => {
        const missingMixin = 'Q&A <draft>';
        openJcrBrowser({path: FOLDER_PATH, action: 'addMixin', value: missingMixin}).then(doc => {
            const error = Array.from(doc.querySelectorAll('p')).find(p => p.textContent.startsWith('Error:'));
            expect(error?.textContent, 'error message').to.contain(missingMixin);
            expect(Array.from(doc.querySelectorAll('pre')).map(pre => pre.textContent).join('\n'), 'error details')
                .to.contain(missingMixin);
        });
    });
});
