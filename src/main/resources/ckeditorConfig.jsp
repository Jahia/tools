<%@page import="org.apache.commons.io.Charsets,org.apache.commons.io.FileUtils,org.jahia.osgi.BundleUtils,org.jahia.settings.SettingsBean"
%>
<%@ page import="org.osgi.framework.Bundle" %>
<%@ page import="org.osgi.framework.Version" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileOutputStream" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.OutputStream" %>
<%@ page import="java.util.jar.Attributes" %>
<%@ page import="java.util.jar.JarEntry" %>
<%@ page import="java.util.jar.JarOutputStream" %>
<%@ page import="java.util.jar.Manifest" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%>
<%!

    public Version getExistingVersion() {
        Bundle bundle = BundleUtils.getBundleBySymbolicName("ckeditor-config", null);
        return bundle != null ? bundle.getVersion() : null;
    }

    public String getNewModuleVersion() {
        Version version = getExistingVersion();
        return version != null ? version.getMajor() + "." + version.getMinor() + "." + (version.getMicro() + 1) : "1.0.0";
    }

    public void createJar(String cfg, OutputStream os, String version) throws IOException {

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Bundle-ManifestVersion", "2");
        attrs.putValue("Bundle-Name", "CKEditor Custom Configuration");
        attrs.putValue("Bundle-SymbolicName", "ckeditor-config");
        attrs.putValue("Bundle-Version", version);
        attrs.putValue("Fragment-Host", "ckeditor");
        JarOutputStream jarOutputStream = new JarOutputStream(os, manifest);

        jarOutputStream.putNextEntry(new JarEntry("javascript/config.js"));
        jarOutputStream.write(cfg.getBytes(StandardCharsets.UTF_8));
        jarOutputStream.closeEntry();
        jarOutputStream.close();
    }
%><c:if test="${not empty param.config && param.action=='Create and download configuration'}" var="download"><%

    String ckEditorVersion = getNewModuleVersion();
    response.setContentType("application/java-archive; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"ckeditor-config-" + ckEditorVersion + ".jar\"");
    createJar(request.getParameter("config"), response.getOutputStream(), ckEditorVersion);
%></c:if><c:if test="${!download}"
>
    <%@ page contentType="text/html; charset=UTF-8" language="java" %>
    <?xml version="1.0" encoding="UTF-8" ?>
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml">
    <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <%@ include file="css.jspf" %>
        <title>CKEditor Custom Configuration</title>
    </head>
    <body>
    <h1>CKEditor Custom Configuration</h1>
    <c:if test="${not empty param.config}">
        <%
            Version existingVersion = getExistingVersion();
            if (existingVersion != null) {
                String oldVersionString = existingVersion.getMajor() + "." + existingVersion.getMinor()+ "." + existingVersion.getMicro();
                File targetFile = new File(SettingsBean.getInstance().getJahiaModulesDiskPath(),
                        "ckeditor-config-" + oldVersionString + ".jar");
                targetFile.delete();
            }
            String ckEditorVersion = getNewModuleVersion();
            File cfgFile = new File(FileUtils.getTempDirectory(), "ckeditor-config-" + ckEditorVersion + ".jar");
            FileOutputStream fos = new FileOutputStream(cfgFile);
            createJar(request.getParameter("config"), fos, ckEditorVersion);
            File targetFile = new File(SettingsBean.getInstance().getJahiaModulesDiskPath(), cfgFile.getName());
            FileUtils.copyFile(cfgFile, targetFile);
            FileUtils.deleteQuietly(cfgFile);
        %>
        <p style="color: blue;">
            CKEditor configuration bundle created and deployed to: <%= targetFile %><br/>
            Please, wait for the Jahia server to deploy the bundle for changes to be effective.
        </p>
    </c:if>
    <form id="cke" action="ckeditorConfig.jsp" method="post">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <p>Paste here your custom CKEditor configuration:</p>
        <p><textarea rows="20" cols="120" id="config" name="config"><c:if test="${empty param.config}">
            CKEDITOR.editorConfig = function( config ) {
            config.extraPlugins='mathjax';
            config.toolbar_Full[8]=['Image','Flash','Table','HorizontalRule','Smiley','SpecialChar','PageBreak','Mathjax'];
            }
        </c:if><c:if test="${not empty param.config}">${param.config}</c:if></textarea></p>
        <p><input type="submit" name="action" value="Create and download configuration"/><input type="submit" name="action"
                                                                                                value="Create and deploy configuration"/>
        </p>
    </form>
    <%@ include file="gotoIndex.jspf" %>
    </body>
    </html>
</c:if>
