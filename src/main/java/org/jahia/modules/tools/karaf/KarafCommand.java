package org.jahia.modules.tools.karaf;

import java.security.Principal;

public interface KarafCommand {

    String executeCommand(final String command, final Long timeout, final Boolean silent, final Principal... principals);

}
