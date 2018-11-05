package org.jahia.modules.tools.karaf.impl;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.jahia.modules.tools.karaf.KarafCommand;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import javax.security.auth.Subject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.concurrent.*;

@Component(service = KarafCommand.class, immediate = true)
public class KarafCommandImpl implements KarafCommand {

    static final Long SERVICE_TIMEOUT = 5000L;

    private BundleContext bundleContext;

    private SessionFactory sessionFactory;

    private  ExecutorService executor;

    @Reference
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }


    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.executor = Executors.newCachedThreadPool();
    }

    @Deactivate
    public void deactivate(BundleContext bundleContext) {
        executor.shutdown();
    }

    private void waitForCommandService(String command) {
        // the commands are represented by services. Due to the asynchronous nature of services they may not be
        // immediately available. This code waits the services to be available, in their secured form. It
        // means that the code waits for the command service to appear with the roles defined.

        if (command == null || command.length() == 0) {
            return;
        }

        int spaceIdx = command.indexOf(' ');
        if (spaceIdx > 0) {
            command = command.substring(0, spaceIdx);
        }
        int colonIndx = command.indexOf(':');
        String scope = (colonIndx > 0) ? command.substring(0, colonIndx) : "*";
        String name  = (colonIndx > 0) ? command.substring(colonIndx + 1) : command;
        try {
            long start = System.currentTimeMillis();
            long cur   = start;
            while (cur - start < SERVICE_TIMEOUT) {
                if (sessionFactory.getRegistry().getCommand(scope, name) != null) {
                    return;
                }
                Thread.sleep(100);
                cur = System.currentTimeMillis();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommand(final String command, final Long timeout, final Boolean silent, final Principal... principals) {
        waitForCommandService(command);

        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final Session session = sessionFactory.create(System.in, printStream, System.err);

        final Callable<String> commandCallable = () -> {
            try {
                if (!silent) {
                    System.err.println(command);
                }
                Object result = session.execute(command);
                if (result != null) {
                    session.getConsole().println(result.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            printStream.flush();
            return byteArrayOutputStream.toString();
        };

        FutureTask<String> commandFuture;
        if (principals.length == 0) {
            commandFuture = new FutureTask<>(commandCallable);
        } else {
            // If principals are defined, run the command callable via Subject.doAs()
            commandFuture = new FutureTask<>(() -> {
                Subject subject = new Subject();
                subject.getPrincipals().addAll(Arrays.asList(principals));
                return Subject.doAs(subject, (PrivilegedExceptionAction<String>) commandCallable::call);
            });
        }

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? (e.getCause().getCause() != null ? e.getCause().getCause() : e.getCause()) : e;
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return response;
    }

}
