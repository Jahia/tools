/* eslint-disable @typescript-eslint/no-explicit-any */
import Chainable = Cypress.Chainable

interface AuthMethod {
    token?: string
    username?: string
    password?: string
}

type FindDependenciesArguments = {
    nameRegExp: string,
    strictVersionsOnly?: boolean,
    auth?: AuthMethod
}

// TODO rename
export const findDependencies = ({nameRegExp, strictVersionsOnly, auth}: FindDependenciesArguments): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy
        .apollo({
            queryFile: 'findDependencies.graphql',
            variables: {nameRegExp, strictVersionsOnly},
            errorPolicy: 'all'
        })
        .then((response: any) => {
            console.log(response);
            return response.data.admin.tools.bundles;
        });
};
