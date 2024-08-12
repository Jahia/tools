/* eslint-disable @typescript-eslint/no-explicit-any */
import Chainable = Cypress.Chainable

interface AuthMethod {
    token?: string
    username?: string
    password?: string
}

type HealthCheckArguments = {
    severity: string,
    includes?: string | Array<string>
    health?: 'GREEN' | 'YELLOW' | 'RED'
    auth?: AuthMethod
}

type FindDependenciesArguments = {
    regexp: string,
    strictVersionsOnly?: boolean,
    auth?: AuthMethod
}

export const healthCheck = ({severity, includes, auth, health}: HealthCheckArguments): Chainable<any> => {
    if (auth) {
        cy.apolloClient(auth);
    }

    return cy
        .apollo({
            queryFile: 'healthcheck.graphql',
            variables: {severity, includes, health},
            errorPolicy: 'all'
        })
        .then((response: any) => {
            console.log(response);
            return response.data.admin.jahia.healthCheck;
        });
};

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
