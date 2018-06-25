<%@ page contentType="text/html; charset=UTF-8" language="java"
%><?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ page import="java.util.*" %>
<%@ page import="java.lang.reflect.Method" %>
<%@ page import="java.lang.reflect.InvocationTargetException" %>
<%@ page import="org.jahia.bin.listeners.LoggingConfigListener" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%!

    private ClassLoader webappClassLoader = this.getClass().getClassLoader().getParent().getParent().getParent().getParent();
    private Class logManagerClass = null;
    private Class levelClass = null;
    private Method toLevelMethod = null;

    public Class getLogManager() {
        if (logManagerClass != null) {
            return logManagerClass;
        }
        try {
            logManagerClass = webappClassLoader.loadClass("org.apache.log4j.LogManager");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return logManagerClass;
    }

    public Class getLevelClass() {
        if (levelClass != null) {
            return levelClass;
        }
        try {
            levelClass = webappClassLoader.loadClass("org.apache.log4j.Level");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return levelClass;
    }

    public Object toLevel(String levelName) {
        Object level = null;
        try {
            if (toLevelMethod == null) {
                toLevelMethod = getLevelClass().getMethod("toLevel", String.class);
            }
            level = toLevelMethod.invoke(null, levelName);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return level;
    }

    public boolean setLoggerLevel(String loggerName, String loggerLevel) {
        LoggingConfigListener.setLoggerLevel(loggerName, loggerLevel);
        return true;
    }

    public List<Map<String,String>> getLoggers() {
        List<Map<String,String>> loggers = new ArrayList<Map<String,String>>();
        return loggers;
    }

    public String getLoggerName(Object logger) {
        String loggerName = null;
        try {
            Method method = logger.getClass().getMethod("getName");
            loggerName = (String) method.invoke(logger);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return loggerName;
    }

    public Object getLoggerEffectiveLevel(Object logger) {
        Object effectiveLevel = null;
        try {
            Method method = logger.getClass().getMethod("getEffectiveLevel");
            effectiveLevel = method.invoke(logger);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return effectiveLevel;
    }

    public Object getLoggerLevel(Object logger) {
        Object level = null;
        try {
            Method method = logger.getClass().getMethod("getLevel");
            level = method.invoke(logger);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return level;
    }

    public Object getLoggerParent(Object logger) {
        Object parent = null;
        try {
            Method method = logger.getClass().getMethod("getParent");
            parent = method.invoke(logger);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return parent;
    }

    public Object getRootLogger() {
        Class logManagerClass = getLogManager();
        Object rootLogger = null;
        try {
            Method getRootLoggerMethod = logManagerClass.getMethod("getRootLogger");
            rootLogger = getRootLoggerMethod.invoke(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return rootLogger;

    }

    public String getRootLoggerName() {
            Object rootLogger = getRootLogger();
        if (rootLogger == null) {
            return null;
        }
        return getLoggerName(rootLogger);
    }

    public Enumeration getCurrentLoggers() {
        Enumeration currentLoggers = null;
        Class logManagerClass = getLogManager();
        try {
            Method getCurrentLoggersMethod = logManagerClass.getMethod("getCurrentLoggers");
            currentLoggers = (Enumeration) getCurrentLoggersMethod.invoke(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return currentLoggers;
    }

%>
<% long beginPageLoadTime = System.currentTimeMillis();%>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Log4j Administration</title>
    <style type="text/css">

        <!--
        #content {
            margin: auto;
            padding: 0px;
            font-family: Arial,Helvetica,sans-serif;
            text-align: center;
            background-color: #ccc;

            border: 1px solid #000;
            width: 80%;
        }

        body {
            position: relative;
            margin: 10px;

            padding: 0px;
            color: #333;
        }

        h1 {
            margin-top: 20px;
            font: 1.5em Verdana, Arial, Helvetica sans-serif;
        }

        h2 {
            margin-top: 10px;
            font: 0.75em Verdana, Arial, Helvetica sans-serif;
            text-align: left;
        }

/*
        a, a:link, a:visited, a:active {
            color: red;
            text-decoration: none;
            text-transform: uppercase;
        }
*/

        table {
            width: 100%;
            background-color: #000;
            padding: 3px;
            border: 0px;
        }

        th {
            font-size: 0.75em;
            background-color: #ccc;
            color: #000;
            padding-left: 5px;
            text-align: center;
            border: 1px solid #ccc;
            white-space: nowrap;

        }

        td {
            font-size: 0.75em;
            background-color: #fff;
            white-space: nowrap;

        }

        td.center {
            font-size: 0.75em;
            background-color: #fff;
            text-align: center;

            white-space: nowrap;
        }

        .filterForm {

            font-size: 0.9em;
            background-color: #000;
            color: #fff;
            padding-left: 5px;
            text-align: left;
            border: 1px solid #000;

            white-space: nowrap;
        }

        .filterText, .filterText2 {

            font-size: 0.75em;
            background-color: #fff;
            color: #000;
            text-align: left;

            border: 1px solid #ccc;
            white-space: nowrap;
        }

        .filterButton {
            font-size: 0.75em;

            background-color: #000;
            color: #fff;

            padding-left: 5px;
            padding-right: 5px;

            text-align: center;
            border: 1px solid #ccc;

            width: 100px;
            white-space: nowrap;
        }

        span.active-level {
        	color: green;
        }
        -->
    </style>
</head>
<body onLoad="javascript:document.logFilterForm.logNameFilter.focus();">
<%@ include file="gotoIndex.jspf" %>

<%

    String containsFilter = "Contains";
    String beginsWithFilter = "Begins With";

    String[] logLevels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF"};
    String targetOperation = (String) request.getParameter("operation");
    String targetLogger = (String) request.getParameter("logger");
    String targetLogLevel = (String) request.getParameter("newLogLevel");
    String logNameFilter = (String) request.getParameter("logNameFilter");
    String logNameFilterType = (String) request.getParameter("logNameFilterType");

    pageContext.setAttribute("logLevels", logLevels);
%>
<div id="content">
<h1>Log4j Administration</h1>
<p>Please note that these settings are valid only during server run time and are not persisted between server restarts.</p>
<div class="filterForm">

    <form action="" name="logFilterForm">Filter Loggers:&nbsp;&nbsp;
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <input name="logNameFilter" type="text" size="50" value="<%=(logNameFilter == null ? "":logNameFilter)%>" class="filterText"/>

        <input name="logNameFilterType" type="submit" value="<%=beginsWithFilter%>" class="filterButton"/>&nbsp;

        <input name="logNameFilterType" type="submit" value="<%=containsFilter%>" class="filterButton"/>&nbsp;

        <input name="logNameClear" type="submit" value="Clear" class="filterButton" onclick='javascript:document.logFilterForm.logNameFilter.value=""; return true;'/>
        <input name="logNameReset" type="reset" value="Reset" class="filterButton"/>

        <param name="operation" value="changeLogLevel"/>

        <br/>
        Add logger:&nbsp;&nbsp;
        <input name="logger" type="text" size="50" value="" class="filterText"/>
        &nbsp;
        <select name="newLogLevel">
            <c:forEach items="${logLevels}" var="level">
                <option value="${level}"${level == 'DEBUG' ? ' selected="selected"' : ''}>${level}</option>
            </c:forEach>
        </select>&nbsp;
        <input name="operation" type="submit" value="Add" class="filterButton"/>
    </form>
</div>

<table cellspacing="1" cellpadding="3">
    <tr>
        <th width="35%">Logger</th>
        <th width="15%">Parent Logger</th>
        <th width="15%">Effective Level</th>
        <th width="35%">Change Log Level To</th>
    </tr>

    <c:if test="${param.operation == 'Add' && not empty param.logger}">
        <%
            setLoggerLevel(targetLogger, targetLogLevel);
        %>
    </c:if>
    <%
        Enumeration loggers = getCurrentLoggers();

        Map<String,Object> loggersMap = new LinkedHashMap<String,Object>();
        Object rootLogger = getRootLogger();
        String rootLoggerName = getLoggerName(rootLogger);

        if (!loggersMap.containsKey(rootLoggerName)) {

            loggersMap.put(rootLoggerName, rootLogger);
        }

        while (loggers.hasMoreElements()) {
            Object logger = loggers.nextElement();
            String loggerName = getLoggerName(logger);

            if (logNameFilter == null || logNameFilter.trim().length() == 0) {

                loggersMap.put(loggerName, logger);
            } else if (containsFilter.equals(logNameFilterType)) {

                if (loggerName.toUpperCase().indexOf(logNameFilter.toUpperCase()) >= 0) {

                    loggersMap.put(loggerName, logger);
                }

            } else {
// Either was no filter in IF, contains filter in ELSE IF, or begins with in ELSE
                if (loggerName.startsWith(logNameFilter)) {

                    loggersMap.put(loggerName, logger);
                }

            }
        }
        Set loggerKeys = loggersMap.keySet();

        String[] keys = new String[loggerKeys.size()];

        keys = (String[]) loggerKeys.toArray(keys);

        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < keys.length; i++) {

            String loggerName = keys[i];
            Object logger = loggersMap.get(loggerName);

// MUST CHANGE THE LOG LEVEL ON LOGGER BEFORE GENERATING THE LINKS AND THE
// CURRENT LOG LEVEL OR DISABLED LINK WON'T MATCH THE NEWLY CHANGED VALUES
            if ("changeLogLevel".equals(targetOperation) && targetLogger.equals(loggerName)) {
                setLoggerLevel(targetLogger, targetLogLevel);
            }

            loggerName = null;
            String loggerEffectiveLevel = null;
            String loggerParent = null;
            if (logger != null) {
                loggerName = getLoggerName(logger);
                loggerEffectiveLevel = String.valueOf(getLoggerEffectiveLevel(logger));
                loggerParent = (getLoggerParent(logger) == null ? "-" : getLoggerName(getLoggerParent(logger)));
            }
    %>
    <tr>
        <td align="left"><%=loggerName%></td>

        <td align="left"><%=loggerParent%></td>

        <td>
        	<% if (loggerName.equals(targetLogger)) {%>
        	<span class="active-level"><%=loggerEffectiveLevel%></span>
        	<% } else {%><%=loggerEffectiveLevel%><% } %>
        </td>
        <td class="center">
            <%
                for (int cnt = 0; cnt < logLevels.length; cnt++) {

                    String url = "?operation=changeLogLevel&logger=" + loggerName + "&newLogLevel=" + logLevels[cnt] + "&logNameFilter=" + (logNameFilter != null ? logNameFilter : "") + "&logNameFilterType=" + (logNameFilterType != null ? logNameFilterType : "");

                    if (getLoggerLevel(logger) == toLevel(logLevels[cnt]) ||
                            getLoggerEffectiveLevel(logger) == toLevel(logLevels[cnt])) {

            %>
            <span class="active-level">[<%=logLevels[cnt].toUpperCase()%>]</span>&nbsp;
            <%
            } else {
            %>
            <a href='<%=url%>&toolAccessToken=${toolAccessToken}'>[<%=logLevels[cnt]%>]</a>&nbsp;
            <%
                    }
                }
            %>
        </td>
    </tr>

    <%
        }
    %>
</table>
<h2>
	Source: <a href="http://ananthkannan.blogspot.com/2009/10/how-to-change-log-levels-on-fly-using.html" target="_blank" style="text-transform: none;">Ananth Kannan's blog</a><br/>
    Revision: 1.1 (Jahia)<br/>
    Page load time: <%=(System.currentTimeMillis() - beginPageLoadTime)%> ms
</h2>
</div>
</body>
</html>