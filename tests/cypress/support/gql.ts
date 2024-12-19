/* eslint-disable @typescript-eslint/no-explicit-any */
import Chainable = Cypress.Chainable

type GetBundlesArguments = {
    nameRegExp?: string,
    areModules?: boolean,
    withUnsupportedDependenciesOnly?: boolean,
    supported?: boolean,
    statuses?: Status[],
}

export enum Status {
    EMPTY = 'EMPTY',
    STRICT_NO_RANGE = 'STRICT_NO_RANGE',
    SINGLE_VERSION_RANGE = 'SINGLE_VERSION_RANGE',
    RESTRICTIVE_RANGE = 'RESTRICTIVE_RANGE',
    OPEN_RANGE = 'OPEN_RANGE',
    UNKNOWN = 'UNKNOWN'
}

export const getBundles = ({
    nameRegExp,
    areModules,
    withUnsupportedDependenciesOnly,
    supported,
    statuses
}: GetBundlesArguments): Chainable<any> => {
    return cy
        .apollo({
            queryFile: 'getBundles.graphql',
            variables: {nameRegExp, areModules, withUnsupportedDependenciesOnly, supported, statuses},
            errorPolicy: 'all'
        });
};
