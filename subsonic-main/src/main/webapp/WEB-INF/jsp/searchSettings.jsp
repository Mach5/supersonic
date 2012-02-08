<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="net.sourceforge.subsonic.command.SearchSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="search"/>
</c:import>

<form:form commandName="command" action="searchSettings.view" method="post">

    <p>
        <span style="white-space: nowrap">
            <fmt:message key="searchsettings.auto"/>
            <form:select path="interval">
                <fmt:message key="searchsettings.interval.never" var="never"/>
                <fmt:message key="searchsettings.interval.one" var="one"/>
                <form:option value="-1" label="${never}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 7 14 30 60" delims=" " var="interval">
                    <fmt:message key="searchsettings.interval.many" var="many"><fmt:param value="${interval}"/></fmt:message>
                    <form:option value="${interval}" label="${many}"/>
                </c:forTokens>
            </form:select>
            <form:select path="hour">
                <c:forEach begin="0" end="23" var="hour">
                    <fmt:message key="searchsettings.hour" var="hourLabel"><fmt:param value="${hour}"/></fmt:message>
                    <form:option value="${hour}" label="${hourLabel}"/>
                </c:forEach>
            </form:select>
        </span>
    </p>

    <p>
    <div class="forward"><a href="searchSettings.view?update"><fmt:message key="searchsettings.manual"/></a></div>
    </p>

    <div>
        <form:checkbox path="fastCache" id="fastCache"/>
        <label for="fastCache"><fmt:message key="searchsettings.fastcache"/></label><br>
            <%--</p>--%>
            <%--<p class="detail">--%>
        <span class="detail" style="white-space: normal; width: 5%">
    </div>

    <p class="detail" style="width:60%;white-space:normal;">
        <fmt:message key="searchsettings.fastcache.description"/>
    </p>

    <p style="white-space: nowrap;">
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'">
    </p>

</form:form>

<h2><fmt:message key="searchsettings.cachestatistics"/></h2>
<p class="detail">
    <c:forEach items="${command.caches}" var="cache">
        ${cache.statistics}<br>
    </c:forEach>
</p>

<p>
<div class="forward"><a href="searchSettings.view?clear"><fmt:message key="searchsettings.clearcache"/></a></div>
</p>

<c:if test="${command.creatingIndex}">
    <p><b><fmt:message key="searchsettings.text"><fmt:param value="${command.brand}"/></fmt:message></b></p>
</c:if>

</body></html>