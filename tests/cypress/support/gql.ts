/* eslint-disable @typescript-eslint/no-explicit-any */
import Chainable = Cypress.Chainable

interface AuthMethod {
    token?: string
    username?: string
    password?: string
}

type FindDependenciesArguments = {
    regexp: string,
    strictVersionsOnly?: boolean,
    auth?: AuthMethod
}

export const findDependencies = ({regexp, strictVersionsOnly, auth}: FindDependenciesArguments): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy
        .apollo({
            queryFile: 'findDependencies.graphql',
            variables: {regexp, strictVersionsOnly},
            errorPolicy: 'all'
        })
        .then((response: any) => {
            console.log(response);
            return response.data.admin.tools.findDependencies;
        });
};
