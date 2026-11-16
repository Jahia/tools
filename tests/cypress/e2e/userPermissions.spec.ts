/**
 * End-to-end tests for the User Permissions Inspector tool (/modules/tools/userPermissions.jsp).
 *
 * Test scenarios:
 *   - Form rendering (all fields visible, placeholder hints)
 *   - No-permissions state (user has no roles on the node)
 *   - Error state (node path does not exist)
 *   - Direct user GRANT: role card, GRANT tag, node permissions, site permissions (external ACE)
 *   - Group-based GRANT: user is a member of the granting group → group badge, site permissions,
 *     j:sourceAce resolved to the actual content node
 *   - Group principal lookup (g: prefix): inspect the group itself instead of a user
 *   - Combined grants: both a direct user role and a group role appear simultaneously
 *
 * Test data created in `before()` and cleaned up in `after()`:
 *   - User:  UP_TEST_USER  (member of UP_TEST_GROUP)
 *   - Group: UP_TEST_GROUP (global — no site scope)
 *   - Node:  /sites/systemsite/up-test-content  (jnt:contentFolder)
 *
 * Roles used: `editor` and `contributor` — both are built-in Jahia roles present in any
 * standard Jahia installation. Both have `jnt:externalPermissions` nodes, so they produce
 * external ACEs at site level when granted on a content node.
 */
import {
    addNode,
    addUserToGroup,
    createGroup,
    createUser,
    deleteGroup,
    deleteNode,
    deleteUser,
    grantRoles,
    revokeRoles
} from '@jahia/cypress';

// ─── Constants ───────────────────────────────────────────────────────────────

const TOOL_URL = '/modules/tools/userPermissions.jsp';
const SITE_KEY = 'systemsite';
const UP_TEST_USER = 'up-test-user';
const UP_TEST_PASSWORD = 'Password1!';
const UP_TEST_GROUP = 'up-test-group';
const CONTENT_NODE_NAME = 'up-test-content';
const NODE_PATH = `/sites/${SITE_KEY}/${CONTENT_NODE_NAME}`;

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Navigates to the tool, fills the form, and submits it.
 *
 * @param usernameOrGroup  Username or "g:groupname" to inspect.
 * @param nodePath         JCR node path to inspect permissions on.
 * @param userSite         Optional site key to select in the site dropdown.
 */
function submitForm(usernameOrGroup: string, nodePath: string, userSite?: string): void {
    cy.visit(TOOL_URL);
    cy.get('#username').clear();
    cy.get('#username').type(usernameOrGroup);
    if (userSite) {
        cy.get('#userSite').select(userSite);
    }

    cy.get('#nodePath').clear();
    cy.get('#nodePath').type(nodePath);
    cy.get('input[type="submit"]').click();
}

/**
 * Returns the role card element for the given role name.
 * Relies on the `data-role-name` attribute added to each `.role-card`.
 */
function getRoleCard(roleName: string): Cypress.Chainable<JQuery<HTMLElement>> {
    return cy.get(`.role-card[data-role-name="${roleName}"]`);
}

// ─── Suite ───────────────────────────────────────────────────────────────────

describe('User Permissions Inspector Tool (/modules/tools/userPermissions.jsp)', () => {
    // ── Global test-data setup / teardown ────────────────────────────────────

    before(() => {
        // Cleanup first — guards against leftover state from a previously interrupted run.
        // Revoke roles BEFORE deleting the node so Jahia's AclListener also removes the
        // site-level external ACEs that live at /sites/systemsite/j:acl.
        revokeRoles(NODE_PATH, ['editor'], UP_TEST_USER, 'USER');
        revokeRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        revokeRoles(NODE_PATH, ['reader'], UP_TEST_USER, 'USER');
        deleteNode(NODE_PATH);
        deleteGroup(UP_TEST_GROUP, SITE_KEY);
        deleteUser(UP_TEST_USER);

        createUser(UP_TEST_USER, UP_TEST_PASSWORD);
        // Site-scoped group: isMember(user, null, group, SITE_KEY) resolves reliably.
        createGroup(UP_TEST_GROUP, false, SITE_KEY);
        addUserToGroup(UP_TEST_USER, UP_TEST_GROUP, SITE_KEY);
        addNode({
            parentPathOrId: `/sites/${SITE_KEY}`,
            name: CONTENT_NODE_NAME,
            primaryNodeType: 'jnt:contentFolder'
        });
    });

    after(() => {
        deleteNode(NODE_PATH);
        deleteGroup(UP_TEST_GROUP, SITE_KEY);
        deleteUser(UP_TEST_USER);
    });

    beforeEach(() => {
        cy.login();
    });

    afterEach(() => {
        cy.logout();
    });

    // ── Form rendering ───────────────────────────────────────────────────────

    describe('Form rendering', () => {
        it('should display all form fields', () => {
            cy.visit(TOOL_URL);
            cy.get('#username').should('be.visible');
            cy.get('#userSite').should('be.visible');
            cy.get('#nodePath').should('be.visible');
            cy.get('#workspace').should('be.visible');
            cy.get('input[type="submit"]').should('be.visible');
        });

        it('should show the g: prefix hint in the username placeholder', () => {
            cy.visit(TOOL_URL);
            cy.get('#username').should('have.attr', 'placeholder').and('contain', 'g:');
        });

        it('should list available sites in the site dropdown', () => {
            cy.visit(TOOL_URL);
            cy.get('#userSite option').should('have.length.greaterThan', 1);
            cy.get('#userSite option').contains(SITE_KEY).should('exist');
        });
    });

    // ── Empty / error states ─────────────────────────────────────────────────

    describe('Empty and error states', () => {
        it('should not show test roles before they are explicitly granted', () => {
            // G:users membership may produce inherited roles on site nodes, so we cannot
            // assert "no results" in general. Instead we verify that our test-specific
            // roles (editor / contributor) are absent before any grants are made.
            submitForm(UP_TEST_USER, NODE_PATH);
            cy.get('.role-card[data-role-name="editor"]').should('not.exist');
            cy.get('.role-card[data-role-name="contributor"]').should('not.exist');
        });

        it('should show an error message for a non-existent node path', () => {
            submitForm(UP_TEST_USER, '/this/path/does/not/exist/at/all');
            cy.get('[data-test-id="error-msg"]')
                .should('be.visible')
                .and('contain', 'not found');
        });
    });

    // ── Direct user GRANT ────────────────────────────────────────────────────

    describe('Direct user role grant (editor → up-test-user)', () => {
        before(() => {
            grantRoles(NODE_PATH, ['editor'], UP_TEST_USER, 'USER');
        });

        after(() => {
            revokeRoles(NODE_PATH, ['editor'], UP_TEST_USER, 'USER');
        });

        it('should display a role card with the correct role name', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            getRoleCard('editor').should('be.visible');
        });

        it('should show a GRANT tag on the role card', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            getRoleCard('editor').within(() => {
                cy.get('.tag-grant').should('be.visible');
            });
        });

        it('should display the user principal (not highlighted as group)', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            getRoleCard('editor').within(() => {
                cy.get('[data-test-id="role-principal"]')
                    .should('contain', UP_TEST_USER)
                    .and('not.have.class', 'role-principal-group');
            });
        });

        it('should list node-level permissions for the editor role', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            getRoleCard('editor').within(() => {
                cy.get('[data-test-id="node-perms-col"] .perm-list li')
                    .should('have.length.greaterThan', 0);
            });
        });

        it('should show site-level (external ACE) permissions for the editor role', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            getRoleCard('editor').within(() => {
                cy.get('[data-test-id="site-perms-col"] .perm-section')
                    .should('have.length.greaterThan', 0);
            });
        });

        it('should display the content node path where the role was granted', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            getRoleCard('editor').within(() => {
                cy.get('.role-card-meta code').first().should('contain', NODE_PATH);
            });
        });
    });

    // ── Group-based GRANT ────────────────────────────────────────────────────

    // Use `editor` (same as direct-user tests) — it is a guaranteed built-in role with
    // external permissions. `contributor` may not exist or may have no permissions defined.

    describe('Group-based role grant (editor → up-test-group, user is member)', () => {
        before(() => {
            grantRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        });

        after(() => {
            revokeRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        });

        it('should display the role card when inspecting a user who is a member of the granting group', () => {
            // Pass userSite so matchesPrincipal resolves site-scoped group membership correctly.
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            // The group-granted card has the group principal badge; selector scopes by principal.
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .contains('[data-test-id="role-principal"]', `g:${UP_TEST_GROUP}`)
                .should('exist');
        });

        it('should show a GRANT tag on the group-based role card', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .filter(':has([data-test-id="role-principal"].role-principal-group)')
                .find('.tag-grant')
                .should('be.visible');
        });

        it('should highlight the principal badge in blue (group style) for a group-based grant', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            // At least one editor card must carry the group-principal class and the group name.
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .find('[data-test-id="role-principal"].role-principal-group')
                .should('exist')
                .and('contain', `g:${UP_TEST_GROUP}`);
        });

        it('should show site-level permissions for the group-granted role', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .filter(':has([data-test-id="role-principal"].role-principal-group)')
                .find('[data-test-id="site-perms-col"] .perm-section')
                .should('have.length.greaterThan', 0);
        });

        it('should resolve the j:sourceAce to the actual content node and show it in the card meta', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            // The content grant path must point to the node where the role was granted,
            // not to the site node where the external ACE lives.
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .filter(':has([data-test-id="role-principal"].role-principal-group)')
                .find('.role-card-meta code')
                .first()
                .should('contain', NODE_PATH);
        });

        it('should display the jContent edit link for the grant node', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .filter(':has([data-test-id="role-principal"].role-principal-group)')
                .find('.role-card-meta .edit-link')
                .should('be.visible');
        });
    });

    // ── Group principal lookup (g: prefix) ───────────────────────────────────

    describe('Group principal lookup using g: prefix', () => {
        before(() => {
            grantRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        });

        after(() => {
            revokeRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        });

        it('should display the role card when inspecting the group directly', () => {
            submitForm(`g:${UP_TEST_GROUP}`, NODE_PATH);
            getRoleCard('editor').should('be.visible');
        });

        it('should show the group as the matched principal', () => {
            submitForm(`g:${UP_TEST_GROUP}`, NODE_PATH);
            getRoleCard('editor').within(() => {
                cy.get('[data-test-id="role-principal"]')
                    .should('contain', `g:${UP_TEST_GROUP}`);
            });
        });

        it('should NOT show roles that were granted only to the individual user, not the group', () => {
            // Grant a second role directly to the user only, then inspect the group — it must not appear.
            grantRoles(NODE_PATH, ['reader'], UP_TEST_USER, 'USER');
            submitForm(`g:${UP_TEST_GROUP}`, NODE_PATH);
            cy.get('.role-card[data-role-name="reader"]').should('not.exist');
            revokeRoles(NODE_PATH, ['reader'], UP_TEST_USER, 'USER');
        });
    });

    // ── Combined grants ──────────────────────────────────────────────────────

    describe('Combined grants: editor granted to user directly AND to the group', () => {
        before(() => {
            grantRoles(NODE_PATH, ['editor'], UP_TEST_USER, 'USER');
            grantRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        });

        after(() => {
            revokeRoles(NODE_PATH, ['editor'], UP_TEST_USER, 'USER');
            revokeRoles(NODE_PATH, ['editor'], UP_TEST_GROUP, 'GROUP');
        });

        it('should show two separate editor role cards — one per principal', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            // Two editor cards: one for the direct user grant, one for the group grant.
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .should('have.length', 2);
        });

        it('should show one card with user principal and one with group principal', () => {
            submitForm(UP_TEST_USER, NODE_PATH, SITE_KEY);
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .find('[data-test-id="role-principal"]:not(.role-principal-group)')
                .should('exist');
            cy.get('[data-test-id="role-card"][data-role-name="editor"]')
                .find('[data-test-id="role-principal"].role-principal-group')
                .should('exist');
        });
    });
});
