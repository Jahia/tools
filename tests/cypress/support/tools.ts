// Helpers shared by the specs of the tool pages served under /modules/tools.
const PAGE_URL = '/cms/render/live/en/sites/systemsite/home.html';

export function toolAccessToken(): Cypress.Chainable<string> {
    return cy.request('POST', '/modules/tools/token').its('body.token');
}

export function parseHtml(body: string): Document {
    return new DOMParser().parseFromString(body, 'text/html');
}

// Opens a tool page with the given parameters and a fresh tools access token, and returns the parsed page. A tool
// that walks the whole repository answers only once the walk ends, so it takes a longer response timeout.
export function openTool(url: string, qs: Record<string, string> = {}, timeout: number = Cypress.config('responseTimeout')): Cypress.Chainable<Document> {
    return toolAccessToken()
        .then(token => cy.request({url, qs: {...qs, toolAccessToken: token}, timeout}))
        .then(response => parseHtml(response.body));
}

// Empties the cache that the given cache viewer lists.
export function flushCache(viewerUrl: string, qs: Record<string, string> = {}): void {
    openTool(viewerUrl, {...qs, flush: 'true'});
}

// Renders a page with the given value in the parameters of the render, so that its cache keys hold the value.
export function renderPage(value: string): void {
    cy.request({url: PAGE_URL, qs: {ec: value}}).its('status').should('eq', 200);
}

// Returns the text of every cell of the given column (1-based) of the table of a cache viewer.
export function cacheTableCells(viewerUrl: string, column: number, qs: Record<string, string> = {}): Cypress.Chainable<string[]> {
    return openTool(viewerUrl, qs).then(doc => Array.from(doc.querySelectorAll(`#cacheTable tbody tr td:nth-child(${column})`))
        .map(td => td.textContent));
}
