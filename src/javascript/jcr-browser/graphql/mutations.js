import {gql} from '@apollo/client';

export const LOCK_NODE = gql`
  mutation LockNode($uuid: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          lockNode(uuid: $uuid, workspace: $workspace) {
            uuid
            name
            path
            locked
          }
        }
      }
    }
  }
`;

export const UNLOCK_NODE = gql`
  mutation UnlockNode($uuid: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          unlockNode(uuid: $uuid, workspace: $workspace) {
            uuid
            name
            path
            locked
          }
        }
      }
    }
  }
`;

export const DELETE_NODE = gql`
  mutation DeleteNode($uuid: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          deleteNode(uuid: $uuid, workspace: $workspace)
        }
      }
    }
  }
`;

export const RENAME_NODE = gql`
  mutation RenameNode($uuid: String!, $newName: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          renameNode(uuid: $uuid, newName: $newName, workspace: $workspace) {
            uuid
            name
            path
          }
        }
      }
    }
  }
`;

export const ADD_MIXIN = gql`
  mutation AddMixin($uuid: String!, $mixinType: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          addMixin(uuid: $uuid, mixinType: $mixinType, workspace: $workspace) {
            uuid
            name
            mixinNodeTypes
          }
        }
      }
    }
  }
`;

export const REMOVE_MIXIN = gql`
  mutation RemoveMixin($uuid: String!, $mixinType: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          removeMixin(uuid: $uuid, mixinType: $mixinType, workspace: $workspace) {
            uuid
            name
            mixinNodeTypes
          }
        }
      }
    }
  }
`;

export const SET_PROPERTY = gql`
  mutation SetProperty($uuid: String!, $propertyName: String!, $propertyValue: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          setProperty(uuid: $uuid, propertyName: $propertyName, propertyValue: $propertyValue, workspace: $workspace) {
            uuid
            name
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

export const REMOVE_PROPERTY = gql`
  mutation RemoveProperty($uuid: String!, $propertyName: String!, $workspace: String!) {
    admin {
      tools {
        jcrBrowser {
          removeProperty(uuid: $uuid, propertyName: $propertyName, workspace: $workspace) {
            uuid
            name
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
