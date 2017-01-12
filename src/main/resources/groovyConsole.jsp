<%@ page contentType="text/html; charset=UTF-8" language="java"
        %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.jahia.tools.patches.LoggerWrapper" %>
<%@ page import="org.jahia.utils.ScriptEngineUtils" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="javax.script.*" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="org.jahia.services.scheduler.JSR223ScriptJob" %>
<%@ page import="org.jahia.services.scheduler.BackgroundJob" %>
<%@ page import="org.quartz.JobDetail" %>
<%@ page import="org.quartz.JobDataMap" %>
<%@ page import="org.jahia.registries.ServicesRegistry" %>
<%@ page import="org.quartz.SchedulerException" %>
<%@ page import="org.springframework.core.io.UrlResource" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="org.apache.commons.io.IOUtils" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.jahia.modules.tools.taglibs.GroovyConsoleHelper" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tools" uri="http://www.jahia.org/tools" %>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Groovy Console</title>
    <%@ include file="css.jspf" %>
    <link type="text/css" href="<c:url value='/modules/assets/css/jquery.fancybox.css'/>" rel="stylesheet"/>
    <script type="text/javascript" src="<c:url value='/modules/jquery/javascript/jquery.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/modules/assets/javascript/jquery.fancybox.pack.js'/>"></script>
    <script type="text/javascript">
        $(document).ready(function () {
            $('#helpLink').fancybox({
                'hideOnContentClick': false,
                'titleShow': false,
                'transitionOut': 'none'
            });
        });
    </script>
</head>
<body>
<h1>Groovy Console <span style="position: absolute;"><a id="helpLink" title="Help" href="#helpArea"><img
        src="<c:url value='/icons/help.png'/>" width="16" height="16" alt="help" title="Help"></a></span></h1>
<%
    long timer = System.currentTimeMillis();
    ScriptEngine engine = null;
    try {
        engine = ScriptEngineUtils.getInstance().scriptEngine("groovy");
%>
<c:if test="${not empty param.runScript and param.runScript eq 'true'}">
<%
    final StringBuilder code = GroovyConsoleHelper.generateScriptSkeleton();

    final String scriptURL = request.getParameter("scriptURI");
    boolean isPredefinedScript = false;
    if (StringUtils.isBlank(scriptURL) || "custom".equals(scriptURL)) {
        code.append(request.getParameter("script"));
    } else {
        final UrlResource resource = new UrlResource(scriptURL);
        String scriptContent = null;
        InputStream in = null;
        try {
            in = resource.getInputStream();
            scriptContent = IOUtils.toString(in, "UTF-8");
        } finally {
            IOUtils.closeQuietly(in);
        }
        code.append(scriptContent);
        isPredefinedScript = true;
    }
    if (request.getParameter("background") != null) {
        File groovyConsole = File.createTempFile("groovyConsole", ".groovy");
        FileUtils.write(groovyConsole, code);
        JobDetail jahiaJob = BackgroundJob.createJahiaJob("Groovy console script", JSR223ScriptJob.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(JSR223ScriptJob.JOB_SCRIPT_ABSOLUTE_PATH,groovyConsole.getAbsolutePath());
        jobDataMap.put("userkey","root");
        jahiaJob.setJobDataMap(jobDataMap);
        try {
            ServicesRegistry.getInstance().getSchedulerService().scheduleJobNow(jahiaJob);
        } catch (SchedulerException e) {
            pageContext.setAttribute("error",e);
        }
        pageContext.setAttribute("result", "Being executed in background look at your console");
        pageContext.setAttribute("took", System.currentTimeMillis() - timer);
    } else {
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setWriter(new StringWriter());
        Bindings bindings = engine.createBindings();
        bindings.put("log", new LoggerWrapper(LoggerFactory.getLogger("org.jahia.tools.groovyConsole"),
                "org.jahia.tools.groovyConsole", ctx.getWriter()));
        if (isPredefinedScript) {
            final String[] paramNames = GroovyConsoleHelper.getScriptParamNames(scriptURL);
            if (paramNames != null)
                for (String paramName : paramNames) {
                    bindings.put(paramName, request.getParameter("scriptParam_" + paramName));
                }
        }
        ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        Object result = engine.eval(code.toString(), ctx);
        pageContext.setAttribute("result", result == null ? ((StringWriter) ctx.getWriter()).getBuffer().toString() : result);
        pageContext.setAttribute("took", System.currentTimeMillis() - timer);
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
    <a href="#show-stacktrace" onclick="var st=document.getElementById('stacktrace').style; st.display=st.display == 'none' ? '' : 'none'; return false;">show stacktrace</a>
    <pre id="stacktrace" style="display:none">${stackTrace}</pre>
</fieldset>
<%
        }
    }
%>
<form id="groovyForm" action="?" method="post">
    <input type="hidden" id="runScript" name="runScript" value="true" />
    <c:if test="${empty param.scriptURI or param.scriptURI eq 'custom'}">
        <input type="checkbox" value="background" name="background" id="background">Background</input>
    </c:if>
    <c:set var="scripts" value="${tools:getGroovyConsoleScripts()}" />
    <c:if test="${not empty scripts}">
    <p>
        <select name="scriptURI" onchange="handleScriptSelection()">
            <option value="custom" class="scriptURISelection">Custom script</option>
                <%--@elvariable id="script" type="org.jahia.osgi.BundleResource"--%>
            <c:forEach items="${scripts}" var="script">
                <c:remove var="currentScriptIsSelected" />
                <c:if test="${script.URI eq param.scriptURI}"><c:set var="currentScriptIsSelected">selected='selected'</c:set> </c:if>
                <option value="${script.URI}" class="scriptURISelection" ${currentScriptIsSelected}><c:out value="${script.filename} (${script.bundle.symbolicName} ${script.bundle.version})" /></option>
            </c:forEach>
        </select>
        <script type="text/javascript">
            function handleScriptSelection() {
                document.getElementById('runScript').setAttribute('value','false');
                document.getElementById('groovyForm').submit();
            }
        </script>
    </p>
    </c:if>
        <c:choose>
            <c:when test="${empty param.scriptURI or param.scriptURI eq 'custom'}">
                <p>Paste here the Groovy code you would like to execute against Jahia:</p>

                <p>
                    <textarea rows="25" style="width: 100%" id="text" name="script"
                        onkeyup="if ((event || window.event).keyCode == 13 && (event || window.event).ctrlKey && confirm('<%=GroovyConsoleHelper.WARN_MSG%>')) document.getElementById('groovyForm').submit();">${param.script}</textarea>
                </p>
            </c:when>
            <c:otherwise>
                ${tools:getScriptCustomFormElements(param.scriptURI, pageContext.request)}
            </c:otherwise>
        </c:choose>
    <p>
        <input type="submit" value="Execute ([Ctrl+Enter])" onclick="if (!confirm('<%=GroovyConsoleHelper.WARN_MSG%>')) { return false; }"/>
    </p>
</form>
<%@ include file="gotoIndex.jspf" %>
<div style="display: none;">
    <div id="helpArea">
        <h3>How to use the Groovy console</h3>

        <h4>Using a custom script</h4>
        <p>You can run a custom script here, writing or pasting it in the textarea.</p>
        <p>If your script has to generate some output, you can use the built in logger: <em>log.info("Some output")</em></p>

        <h4>Using a predefined script</h4>
        <p>You can as well package in any of your modules a predefined script, which can then be conveniently run from the console
            without you have to copy and paste it. You still have the possibility to write or paste a custom script.</p>
        <p>Your predefined scripts have to be defined in a specific folder:
            <em>src/main/resources/META-INF/groovyConsole</em></p>
        <p>If some predefined script requires some configurations, then you have to create in the same folder a file with the same name as the script
        and <em>.properties</em> as an extension. In this file, you can declare and configure the required parameters:</p>
        <ul>
            <li><em>script.parameters.names</em> : comma separated list of parameters</li>
            <li><em>script.param.xxx.type</em> : type of the parameter xxx <br />
            Allowed values: <em>checkbox</em>, <em>text</em><br />
            Default value: <em>checkbox</em></li>
            <li><em>script.param.xxx.label</em> : label for the parameter xxx</li>
            <li><em>script.param.xxx.default</em> : default value of the parameter xxx <br />
            For a checkbox parameter, the checkbox is unchecked by default, use <em>true</em> as a default value otherwise <br />
            For a text parameter, the input field is empty by default</li>
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
</body>
</html>