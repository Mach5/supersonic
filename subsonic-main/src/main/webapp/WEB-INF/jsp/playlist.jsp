<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/scripts.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/prototype.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" language="javascript">
        function toggleStar(mediaFileId, imageId) {
            if ($(imageId).src.indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
                $(imageId).src = "<spring:theme code="ratingOffImage"/>";
                starService.unstar(mediaFileId);
            }
            else if ($(imageId).src.indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
                $(imageId).src = "<spring:theme code="ratingOnImage"/>";
                starService.star(mediaFileId);
            }
        }
    </script>
</head>
<body class="mainframe bgcolor1">

<h1>
    ${model.playlist.name}
</h1>

<c:if test="${not empty model.playlist.comment}">
    <h2>${model.playlist.comment}</h2>
</c:if>

<table style="border-collapse:collapse">
    <c:forEach items="${model.files}" var="song" varStatus="loopStatus">

        <sub:url value="/main.view" var="mainUrl">
            <sub:param name="path" value="${song.parentPath}"/>
        </sub:url>

        <tr>
            <c:import url="playAddDownload.jsp">
                <c:param name="id" value="${song.id}"/>
                <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyModeEnabled or not song.directory)}"/>
                <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyModeEnabled}"/>
                <c:param name="starEnabled" value="true"/>
                <c:param name="starred" value="${not empty song.starredDate}"/>
                <c:param name="video" value="${song.video and model.player.web}"/>
                <c:param name="asTable" value="true"/>
            </c:import>

            <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-left:0.25em;padding-right:1.25em">
                    ${song.title}
            </td>

            <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-right:1.25em">
                <a href="${mainUrl}"><span class="detail">${song.albumName}</span></a>
            </td>

            <td ${loopStatus.count % 2 == 1 ? "class='bgcolor2'" : ""} style="padding-right:0.25em">
                <span class="detail">${song.artist}</span>
            </td>
            </tr>

        </c:forEach>
    </table>

</body></html>