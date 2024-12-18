/* eslint-disable @typescript-eslint/no-explicit-any */
import Chainable = Cypress.Chainable

type FindDependenciesArguments = {
    nameRegExp?: string,
    withUnsupportedDependenciesOnly?: boolean,
    supported?: boolean,
    statuses?: Status[],
}

export enum Status {
    EMPTY,
    STRICT_NO_RANGE,
    SINGLE_VERSION_RANGE,
    RESTRICTIVE_RANGE,
    OPEN_RANGE,
    UNKNOWN
}

export const getBundles = ({
    nameRegExp,
    withUnsupportedDependenciesOnly,
    supported,
    statuses
}: FindDependenciesArguments): Chainable<any> => {
    return cy
        .apollo({
            queryFile: 'getBundles.graphql',
            variables: {nameRegExp, withUnsupportedDependenciesOnly, supported, statuses},
            errorPolicy: 'all'
        });
};
