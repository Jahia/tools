import {gql} from '@apollo/client';

export const GET_NODE = gql`
  query GetNode($uuid: String, $path: String, $workspace: String) {
    admin {
      tools {
        jcrBrowser {
          node(uuid: $uuid, path: $path, workspace: $workspace) {
            uuid
            name
            path
            primaryNodeType
            mixinNodeTypes
            hasChildren
            childrenCount
            locked
            versionable
            workspace
            depth
            properties {
              name
              type
              multiple
              value
              values
              path
            }
          }
        }
      }
    }
  }
`;

export const GET_NODE_DETAILS = gql`
  query GetNodeDetails($uuid: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          node(uuid: $uuid, workspace: $workspace) {
            uuid
            name
            path
            primaryNodeType
            mixinNodeTypes
            hasChildren
            childrenCount
            locked
            versionable
            workspace
            depth
            properties {
              name
              type
              multiple
              value
              values
              path
            }
          }
        }
      }
    }
  }
`;

export const GET_NODE_CHILDREN = gql`
  query GetNodeChildren($uuid: String!, $workspace: String!, $limit: Int, $offset: Int) {
    admin {
      tools {
        jcrBrowser {
          node(uuid: $uuid, workspace: $workspace) {
            uuid
            children(limit: $limit, offset: $offset) {
              uuid
              name
              path
              primaryNodeType
              mixinNodeTypes
              hasChildren
              childrenCount
              locked
            }
          }
        }
      }
    }
  }
`;
