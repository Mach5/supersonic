<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/scripts.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/prototype.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" language="javascript">

        function init() {
//            TODO
//            dwr.engine.setErrorHandler(null);
            getPlaylist();
        }

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

        function getPlaylist() {
            playlistService.getPlaylist(${model.playlist.id}, playlistCallback);
        }

        function playlistCallback(playlist) {
            songs = playlist.entries;

            if (songs.length == 0) {
                $("empty").show();
            } else {
                $("empty").hide();
            }

            // Delete all the rows except for the "pattern" row
            dwr.util.removeAllRows("playlistBody", { filter:function(tr) {
                return (tr.id != "pattern");
            }});

            // Create a new set cloned from the pattern row
            for (var i = 0; i < songs.length; i++) {
                var song  = songs[i];
                var id = i + 1;
                dwr.util.cloneNode("pattern", { idSuffix:id });
                if (song.starredDate != null) {
                    $("starSong" + id).src = "<spring:theme code='ratingOnImage'/>";
                } else {
                    $("starSong" + id).src = "<spring:theme code='ratingOffImage'/>";
                }
                if ($("title" + id)) {
                    dwr.util.setValue("title" + id, truncate(song.title));
                    $("title" + id).title = song.title;
                }
                if ($("album" + id)) {
                    dwr.util.setValue("album" + id, truncate(song.album));
                    $("album" + id).title = song.album;
//TODO
//                    $("albumUrl" + id).href = song.albumUrl;
                }
                if ($("artist" + id)) {
                    dwr.util.setValue("artist" + id, truncate(song.artist));
                    $("artist" + id).title = song.artist;
                }
                if ($("duration" + id)) {
                    dwr.util.setValue("duration" + id, song.durationAsString);
                }

                $("pattern" + id).show();
                $("pattern" + id).className = (i % 2 == 0) ? "bgcolor2" : "bgcolor1";
            }
        }

        function truncate(s) {
            var cutoff = 30;

            if (s.length > cutoff) {
                return s.substring(0, cutoff) + "...";
            }
            return s;
        }

    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">

<h1>
    ${model.playlist.name}
</h1>

<c:if test="${not empty model.playlist.comment}">
    <h2>${model.playlist.comment}</h2>
</c:if>

<p id="empty" style="visibility: hidden;"><em><fmt:message key="playlist.empty"/></em></p>

<table style="border-collapse:collapse;white-space:nowrap;">
    <tbody id="playlistBody">
    <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
        <td><a href="javascript:noop()">
            <img id="starSong" onclick="onStar(this.id.substring(8) - 1)" src="<spring:theme code="ratingOffImage"/>"
                 alt="" title=""></a></td>
        <td><a href="javascript:noop()">
            <img id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" src="<spring:theme code="removeImage"/>"
                 alt="<fmt:message key="playlist.remove"/>" title="<fmt:message key="playlist.remove"/>"></a></td>
        <td><a href="javascript:noop()">
            <img id="up" onclick="onUp(this.id.substring(2) - 1)" src="<spring:theme code="upImage"/>"
                 alt="<fmt:message key="playlist.up"/>" title="<fmt:message key="playlist.up"/>"></a></td>
        <td><a href="javascript:noop()">
            <img id="down" onclick="onDown(this.id.substring(4) - 1)" src="<spring:theme code="downImage"/>"
                 alt="<fmt:message key="playlist.down"/>" title="<fmt:message key="playlist.down"/>"></a></td>

        <td style="padding-left: 0.1em"><input type="checkbox" class="checkbox" id="songIndex"></td>
        <td style="padding-right:0.25em"></td>


        <td style="padding-right:1.25em"><span id="title">Title</span></td>
        <td style="padding-right:1.25em"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
        <td style="padding-right:1.25em"><span id="artist" class="detail">Artist</span></td>
        <td style="padding-right:1.25em;text-align:right;"><span id="duration" class="detail">Duration</span></td>
    </tr>
    </tbody>
</table>

</body></html>