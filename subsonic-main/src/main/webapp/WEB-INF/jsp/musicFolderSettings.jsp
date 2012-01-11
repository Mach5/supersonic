<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html><head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="musicFolder"/>
</c:import>

<form:form commandName="command" action="musicFolderSettings.view" method="post">
<%--@elvariable id="command" type="net.sourceforge.subsonic.command.MusicFolderSettingsCommand"--%>

<table class="indent">
    <tr>
        <th><fmt:message key="musicfoldersettings.name"/></th>
        <th><fmt:message key="musicfoldersettings.path"/></th>
        <th style="padding-left:1em"><fmt:message key="musicfoldersettings.enabled"/></th>
        <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
        <th></th>
    </tr>

    <c:forEach items="${command.musicFolders}" var="folder" varStatus="loopStatus">
        <tr>
            <td><form:input path="musicFolders[${loopStatus.count-1}].name" size="20"/></td>
            <td><form:input path="musicFolders[${loopStatus.count-1}].path" size="40"/></td>
            <td align="center" style="padding-left:1em"><form:checkbox path="musicFolders[${loopStatus.count-1}].enabled" cssClass="checkbox"/></td>
            <td align="center" style="padding-left:1em"><form:checkbox path="musicFolders[${loopStatus.count-1}].delete" cssClass="checkbox"/></td>
            <td><c:if test="${not folder.existing}"><span class="warning"><fmt:message key="musicfoldersettings.notfound"/></span></c:if></td>
        </tr>
    </c:forEach>

    <tr>
        <th colspan="4" align="left" style="padding-top:1em"><fmt:message key="musicfoldersettings.add"/></th>
    </tr>

    <tr>
        <td><form:input path="newMusicFolder.name" size="20"/></td>
        <td><form:input path="newMusicFolder.path" size="40"/></td>
        <td align="center" style="padding-left:1em"><form:checkbox path="newMusicFolder.enabled" cssClass="checkbox"/></td>
        <td/>
    </tr>

</table>

    <p class="forward"><a href="musicFolderSettings.view?scanNow"><fmt:message key="musicfoldersettings.scannow"/></a></p>

    <div>
        <input type="checkbox" class="checkbox" ${model.fastCache ? "checked" : ""} name="fastCache" id="fastCache"/>
        <label for="fastCache"><fmt:message key="musicfoldersettings.fastcache"/></label>
    </div>

    <p class="detail" style="width:60%;white-space:normal;">
        <fmt:message key="musicfoldersettings.fastcache.description"/>
    </p>

    <p >
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'">
    </p>

</form:form>

<c:if test="${not empty model.error}">
    <p class="warning"><fmt:message key="${model.error}"/></p>
</c:if>

<c:if test="${model.reload}">
    <script type="text/javascript">
        parent.frames.upper.location.href="top.view?";
        parent.frames.left.location.href="left.view?";
    </script>
</c:if>

</body></html>