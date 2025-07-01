<%@ page import="org.jahia.registries.ServicesRegistry, org.jahia.services.scheduler.SchedulerService, org.quartz.*, java.text.SimpleDateFormat, java.util.Date"
%>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>
<%
SchedulerService service = ServicesRegistry.getInstance().getSchedulerService();
Scheduler scheduler = service.getScheduler();
pageContext.setAttribute("service", service);
String jobGroup = request.getParameter("group");
pageContext.setAttribute("group", jobGroup);
%><%
List<JobDetail> allJobs = service.getAllJobs();
pageContext.setAttribute("allJobs", allJobs);
List<Object> jobs = new LinkedList<Object>();
pageContext.setAttribute("jobs", jobs);

for (JobDetail job : allJobs) {
    if (jobGroup != null && jobGroup.length() > 0 && !job.getGroup().equals(jobGroup)) {
        continue;
    }
    if (!String.valueOf(job.getJobDataMap().get("status")).equals("added") && scheduler.getTriggersOfJob(job.getName(), job.getGroup()).length == 0) {
        jobs.add(new Object[] {job, null});
    }
}

pageContext.setAttribute("allCount", jobs.size());
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core"  prefix="c"
%><c:if test="${param.file}"><%
response.setContentType("text/csv;charset=UTF-8");
response.setHeader("Content-Disposition", "attachment; filename=\"jobs-"
        + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".csv\"");
pageContext.setAttribute("newLineChar", "\n");
%><c:set var="nl" value="${fn:escapeXml(newLineChar)}"/>#,Group.Name,State,Start,End,Duration${nl}
<c:forEach items="${jobs}" var="jobElement" varStatus="status">
<c:set var="job" value="${jobElement[0]}"/>
<c:set var="state" value="${job.jobDataMap.status}"/>
${status.index + 1},${fn:escapeXml(job.fullName)},${state},${job.jobDataMap.begin},${job.jobDataMap.end},${job.jobDataMap.duration}${nl}
</c:forEach>
</c:if><c:if test="${not param.file}">
    <%@ page contentType="text/html; charset=UTF-8" language="java" %>
    <!DOCTYPE html>
    <html>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<c:set var="title" value="Completed Job Info"/>
<head>
    <%@ include file="commons/html_header.jspf" %>
</head>
<body>
<c:set var="headerActions">
    <li><a href="?file=true&amp;group=${fn:escapeXml(param.group)}&toolAccessToken=${toolAccessToken}" target="_blank"><span class="material-symbols-outlined">download</span>download as a file</a></li>
    <li> <a href="#refresh" onclick="window.location.reload(); return false;" title="refresh"><span class="material-symbols-outlined">refresh</span>Refresh</a></li>
</c:set>
<c:set var="description">
    <p>Completed job count: <strong>${allCount}</strong></p>
</c:set>
    <%@ include file="commons/header.jspf" %>

    <br/></p>

    <c:if test="${not empty jobs}">

    <table border="1" cellspacing="0" cellpadding="5">
        <thead>
            <tr>
                <th>#</th>
                <th>Group.Name</th>
                <th>State</th>
                <th>Start</th>
                <th>End</th>
                <th>Duration</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach items="${jobs}" var="jobElement" varStatus="status">
            <c:set var="job" value="${jobElement[0]}"/>
            <c:set var="state" value="${job.jobDataMap.status}"/>
            <tr style="${'executing' == state ? 'color: green; font-weight: bold;' : ''}">
                <td><strong>${status.index + 1}</strong></td>
                <td title="class: ${job.jobClass.name}">${fn:escapeXml(job.fullName)}</td>
                <td style="text-align: center;">${state}</td>
                <td style="text-align: center;">${job.jobDataMap.begin}</td>
                <td style="text-align: center;">${job.jobDataMap.end}</td>
                <td style="text-align: center;">${job.jobDataMap.duration}</td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
    </c:if>

    <%@ include file="commons/footer.jspf" %>
</body>
</html>
</c:if>
