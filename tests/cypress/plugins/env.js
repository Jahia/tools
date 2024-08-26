module.exports = (on, config) => {
    config.baseUrl = process.env.JAHIA_URL;
    config.env.JAHIA_URL = process.env.JAHIA_URL;
    config.env.SUPER_USER_PASSWORD = process.env.SUPER_USER_PASSWORD;
    config.env.JAHIA_HOST = process.env.JAHIA_HOST;
    config.env.JAHIA_PROCESSING_HOST = process.env.JAHIA_PROCESSING_HOST;
    config.env.JAHIA_PORT = process.env.JAHIA_PORT;
    config.env.JAHIA_PROCESSING_PORT = process.env.JAHIA_PROCESSING_PORT;
    config.env.JAHIA_USERNAME = process.env.JAHIA_USERNAME;
    config.env.JAHIA_PASSWORD = process.env.JAHIA_PASSWORD;
    config.env.JAHIA_PORT_KARAF = process.env.JAHIA_PORT_KARAF;
    config.env.JAHIA_USERNAME_TOOLS = process.env.JAHIA_USERNAME_TOOLS;
    config.env.JAHIA_PASSWORD_TOOLS = process.env.JAHIA_PASSWORD_TOOLS;
    config.env.WORKSPACE_EDIT = process.env.WORKSPACE_EDIT;
    config.env.OPERATING_MODE = process.env.OPERATING_MODE;
    return config
}