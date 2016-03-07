package org.jahia.modules.tools;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * SLF4J logger wrapper to also log into the provided instance of {@link PrintWriter}.
 *
 * @author Sergiy Shyrkov
 */
public class LoggerWrapper extends org.slf4j.ext.LoggerWrapper {

    private PrintWriter out;

    /**
     * Initializes an instance of this class.
     *
     * @param logger
     * @param fqcn
     */
    public LoggerWrapper(Logger logger, String fqcn, Writer out) {
        super(logger, fqcn);
        this.out = new PrintWriter(out, true);
    }

    @Override
    public void error(String msg) {
        out(msg, null, null);
        super.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        out(format, new Object[] { arg }, null);
        super.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        out(format, new Object[] { arg1, arg2 }, null);
        super.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object[] argArray) {
        out(format, argArray, null);
        super.error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t) {
        out(msg, null, t);
        super.error(msg, t);
    }

    public void info(Object msg) {
        info(String.valueOf(msg));
    }

    @Override
    public void info(String msg) {
        out(msg, null, null);
        super.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        out(format, new Object[] { arg }, null);
        super.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        out(format, new Object[] { arg1, arg2 }, null);
        super.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object[] argArray) {
        out(format, argArray, null);
        super.info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t) {
        out(msg, null, t);
        super.info(msg, t);
    }

    private void out(String format, Object[] argArray, Throwable t) {
        out.println(argArray != null ? MessageFormatter.arrayFormat(format, argArray) : format);

        if (t != null) {
            out.println(t.getMessage());
            t.printStackTrace(out);
        }
    }

    @Override
    public void warn(String msg) {
        out(msg, null, null);
        super.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        out(format, new Object[] { arg }, null);
        super.warn(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        out(format, new Object[] { arg1, arg2 }, null);
        super.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object[] argArray) {
        out(format, argArray, null);
        super.warn(format, argArray);
    }

    @Override
    public void warn(String msg, Throwable t) {
        out(msg, null, t);
        super.warn(msg, t);
    }

}
