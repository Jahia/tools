import {openTool} from '../support/tools';

// The job administration tool reports the outcome of an action with the group and name of the job, and displays them
// as text.
const PAGE = '/modules/tools/jobadmin.jsp';
const GROUP = 'Q&A <draft> "group"';
const NAME = 'Zoé\'s <job>';

function resultLine(doc: Document, prefix: string): string {
    const line = Array.from(doc.querySelectorAll('p')).find(p => p.textContent.trim().startsWith(prefix));
    expect(line, `the line "${prefix}"`).to.not.equal(undefined);
    return line.querySelector('strong').textContent;
}

describe('Job administration tool (/modules/tools/jobadmin.jsp)', () => {
    beforeEach(() => {
        cy.login();
    });

    it('shows the group and name of a trigger it cannot find as typed', () => {
        openTool(PAGE, {action: 'cancel', group: GROUP, name: NAME}).then(doc => {
            expect(resultLine(doc, 'Unable to find trigger')).to.equal(`${GROUP}.${NAME}`);
        });
    });

    it('shows the group and name of a job it cannot delete as typed', () => {
        openTool(PAGE, {action: 'remove', group: GROUP, name: NAME}).then(doc => {
            expect(resultLine(doc, 'Unable to delete job')).to.equal(`${GROUP}.${NAME}`);
        });
    });

    it('shows the group and name of a paused job as typed', () => {
        openTool(PAGE, {action: 'pause', group: GROUP, name: NAME}).then(doc => {
            expect(resultLine(doc, 'Successfully paused')).to.equal(`${GROUP}.${NAME}`);
        });
    });

    it('shows the group and name of a resumed job as typed', () => {
        openTool(PAGE, {action: 'resume', group: GROUP, name: NAME}).then(doc => {
            expect(resultLine(doc, 'Successfully resumed')).to.equal(`${GROUP}.${NAME}`);
        });
    });
});
