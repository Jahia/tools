<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.jahia.bin.listeners.LoggingConfigListener" %>
<%@ page import="org.jahia.modules.tools.taglibs.GroovyConsoleHelper" %>
<%@ page import="org.jahia.registries.ServicesRegistry" %>
<%@ page import="org.jahia.services.scheduler.BackgroundJob" %>
<%@ page import="org.jahia.services.scheduler.JSR223ScriptJob" %>
<%@ page import="org.jahia.utils.ScriptEngineUtils" %>
<%@ page import="org.quartz.JobDataMap" %>
<%@ page import="org.quartz.JobDetail" %>
<%@ page import="org.quartz.SchedulerException" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="org.springframework.core.io.UrlResource" %>
<%@ page import="javax.script.*" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.nio.charset.Charset" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tools" uri="http://www.jahia.org/tags/tools" %>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Groovy Console</title>
</head>
<body>
<h1>Groovy Console&nbsp;<button data-src="#helpArea" data-fancybox><img src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="help" title="Help"/></button>
</h1>
<%
    long timer = System.currentTimeMillis();
    ScriptEngine engine = null;
    try {
        engine = ScriptEngineUtils.getInstance().scriptEngine("groovy");
%>
<c:if test="${not empty param.scriptURI && param.scriptURI != 'custom'}">
    <%
        pageContext.setAttribute("scriptContent", org.jahia.utils.FileUtils.getContent(new UrlResource(request.getParameter("scriptURI"))));
    %>
</c:if>
<c:if test="${not empty param.runScript and param.runScript eq 'true'}">
    <%
        final StringBuilder code = GroovyConsoleHelper.generateScriptSkeleton();

        boolean executeInBackground = request.getParameter("background") != null;
        if (executeInBackground) {
            code.append("\n");
            code.append("def log = ").append(LoggerFactory.class.getName()).append(".getLogger(\"org.jahia.tools.groovyConsole\");\n");
            code.append("def logger = log;\n");
            code.append("\n");
        }

        final String scriptURL = request.getParameter("scriptURI");
        boolean isPredefinedScript = false;
        if (StringUtils.isBlank(scriptURL) || "custom".equals(scriptURL)) {
            code.append(request.getParameter("script"));
        } else {
            code.append((String) pageContext.getAttribute("scriptContent"));
            isPredefinedScript = true;
        }
        if (executeInBackground) {
            File groovyConsole = File.createTempFile("groovyConsole", ".groovy");
            FileUtils.write(groovyConsole, code, Charset.defaultCharset());
            JobDetail jahiaJob = BackgroundJob.createJahiaJob("Groovy console script", JSR223ScriptJob.class);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(JSR223ScriptJob.JOB_SCRIPT_ABSOLUTE_PATH, groovyConsole.getAbsolutePath());
            jobDataMap.put("userkey", "root");
            jahiaJob.setJobDataMap(jobDataMap);
            try {
                ServicesRegistry.getInstance().getSchedulerService().scheduleJobNow(jahiaJob);
            } catch (SchedulerException e) {
                pageContext.setAttribute("error", e);
            }
            pageContext.setAttribute("result", "Being executed in background look at your console");
            pageContext.setAttribute("took", System.currentTimeMillis() - timer);
        } else {
            ScriptContext ctx = new SimpleScriptContext();
            ctx.setWriter(LoggingConfigListener.createLogAwareWriter(GroovyConsoleHelper.GROOVY_CONSOLE_FQCN));
            try {
                Bindings bindings = engine.createBindings();
                Logger lw = LoggerFactory.getLogger(GroovyConsoleHelper.GROOVY_CONSOLE_FQCN);
                bindings.put("log", lw);
                bindings.put("logger", lw);
                if (isPredefinedScript) {
                    final String[] paramNames = GroovyConsoleHelper.getScriptParamNames(scriptURL);
                    if (paramNames != null) {
                        for (String paramName : paramNames) {
                            bindings.put(paramName, request.getParameter("scriptParam_" + paramName));
                        }
                    }
                }
                ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);


                Object result = engine.eval(code.toString(), ctx);
                pageContext.setAttribute("result", result == null ? ((StringWriter) ctx.getWriter()).getBuffer().toString() : result);
                pageContext.setAttribute("took", System.currentTimeMillis() - timer);
            } finally {
                LoggingConfigListener.removeLogAwareWriter(GroovyConsoleHelper.GROOVY_CONSOLE_FQCN);
            }
        }
    %>
    <fieldset>
        <legend style="color: blue">Successfully executed in ${took} ms</legend>
        <p><strong>Result:</strong><br/>
        <pre>${not empty result ? fn:escapeXml(result) : '<empty>'}</pre>
        </p>
    </fieldset>
</c:if>
<%
} catch (ScriptException e) {
    if (e instanceof ScriptException && e.getMessage() != null && e.getMessage().startsWith(
            "Script engine not found for extension")) {
%><p>Groovy engine is not available.</p><%
} else {
    Throwable ex = e.getCause() != null ? e.getCause() : e;
    if (ex instanceof ScriptException && e.getCause() != null) {
        ex = ex.getCause();
    }
    pageContext.setAttribute("error", ex);
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    sw.flush();
    pageContext.setAttribute("stackTrace", sw.getBuffer().toString());
%>
<fieldset>
    <legend style="color: red">Error</legend>
    <p style="color: red">${fn:escapeXml(error)}</p>
    <a href="#show-stacktrace"
       onclick="var st=document.getElementById('stacktrace').style; st.display=st.display == 'none' ? '' : 'none'; return false;">show
        stacktrace</a>
    <pre id="stacktrace" style="display:none">${stackTrace}</pre>
</fieldset>
<%
        }
    }
%>
<form id="groovyForm" action="?" method="post">
    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
    <input type="hidden" id="runScript" name="runScript" value="true"/>
    <c:if test="${empty param.scriptURI or param.scriptURI eq 'custom'}">
        <input type="checkbox" value="background" name="background"
               id="background"/>&nbsp;<label for="background" title="Execute the script as a background job (separate thread)">Execute as a background job</label>
    </c:if>
    <c:set var="scripts" value="${tools:getGroovyConsoleScripts()}"/>
    <c:if test="${not empty scripts}">
        <p>
            Chose a pre-defined script to be executed:
            <select name="scriptURI"
                    onchange="document.getElementById('runScript').setAttribute('value','false'); document.getElementById('groovyForm').submit();">
                <option value="custom" class="scriptURISelection">---</option>
                    <%--@elvariable id="script" type="org.jahia.osgi.BundleResource"--%>
                <c:forEach items="${scripts}" var="script">
                    <c:remove var="currentScriptIsSelected"/>
                    <c:if test="${script.URI eq param.scriptURI}"><c:set
                            var="currentScriptIsSelected">selected='selected'</c:set><c:set var="currentScriptFilename"
                                                                                            value="${script.filename}"/></c:if>
                    <option value="${script.URI}" class="scriptURISelection" ${currentScriptIsSelected}><c:out
                            value="${script.filename} [${script.bundle.symbolicName}/${script.bundle.version}]"/></option>
                </c:forEach>
            </select>
            <c:if test="${not empty scriptContent}">
                <a class="fancybox-link" title="${fn:escapeXml(currentScriptFilename)}" href="#viewArea"><img
                        src="<c:url value='/icons/filePreview.png'/>" width="16" height="16" alt="view"
                        title="View"/></a>
            </c:if>
        </p>
    </c:if>
    <c:choose>
        <c:when test="${empty param.scriptURI or param.scriptURI eq 'custom'}">
            <p>${not empty scripts ? 'Or paste' : 'Paste'} here the Groovy code you would like to execute against
                Jahia:</p>

            <p>
                    <textarea rows="25" style="width: 100%" id="text" name="script"
                              onkeyup="if ((event || window.event).keyCode == 13 && (event || window.event).ctrlKey && confirm(<%=GroovyConsoleHelper.WARN_MSG%>')) document.getElementById('groovyForm').submit();">${param.script}</textarea>
            </p>
        </c:when>
        <c:otherwise>
            ${tools:getScriptCustomFormElements(param.scriptURI, pageContext.request)}
        </c:otherwise>
    </c:choose>
    <p>
        <input type="submit" value="Execute ([Ctrl+Enter])"
               onclick="if (!confirm('<%=GroovyConsoleHelper.WARN_MSG%>')) { return false; }"/>
    </p>
</form>
<%@ include file="gotoIndex.jspf" %>
<div style="display: none;">
    <div id="helpArea" style="display:none;max-width:500px;">
        <h3>How to use the Groovy console</h3>

        <h4>Using a custom script</h4>
        <p>You can run a custom script here, writing or pasting it into the textarea.</p>
        <p>If your script has to generate some output, you can use the built in logger: <em>log.info("Some output")</em>
        </p>

        <h4>Using a predefined script</h4>
        <p>You can as well package in any of your modules a predefined script, which can then be conveniently run from
            the console
            without you have to copy and paste it. You still have the possibility to write or paste a custom script.</p>
        <p>Your predefined scripts have to be defined in a specific folder:
            <em>src/main/resources/META-INF/groovyConsole</em></p>
        <p>If some predefined script requires some configurations, then you have to create in the same folder a file
            with the same name as the script
            and <em>.properties</em> as an extension. In this file, you can declare and configure the required
            parameters:</p>
        <ul>
            <li><em>script.parameters.names</em>: comma separated list of parameters</li>
            <li><em>script.param.xxx.type</em>: type of the parameter xxx <br/>
                Allowed values: <em>checkbox</em>, <em>text</em><br/>
                Default value: <em>checkbox</em></li>
            <li><em>script.param.xxx.label</em>: label for the parameter xxx</li>
            <li><em>script.param.xxx.default</em>: default value of the parameter xxx <br/>
                For a checkbox parameter, the checkbox is unchecked by default, use <em>true</em> as a default value
                otherwise <br/>
                For a text parameter, the input field is empty by default
            </li>
        </ul>

        <p><strong>Example: helloworld.properties</strong>
        <pre>
    script.parameters.names=active, name
    script.param.active.default=true
    script.param.name.type=text
    script.param.name.label=User name
        </pre>
        </p>

        <p><strong>Example: helloworld.groovy</strong>
        <pre>
    if (active) {
        log.info(String.format("Hello %s!!!", name == null || name.trim().length() == 0 ? "world" : name))
    } else {
        log.info("On mute")
    }
        </pre>
        </p>
    </div>
</div>
<c:if test="${not empty scriptContent}">
    <div style="display: none;">
        <div id="viewArea">
            <pre>${scriptContent}</pre>
        </div>
    </div>
</c:if>
<script type="module" src="<c:url value='/modules/tools/javascript/apps/fancybox.tools.bundle.js'/>"></script>

</body>
</html>
