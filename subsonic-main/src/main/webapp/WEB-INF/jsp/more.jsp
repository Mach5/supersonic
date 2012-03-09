<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <style type="text/css">
        #progressBar {width: 350px; height: 10px; border: 1px solid black; display:none;}
        #progressBarContent {width: 0; height: 10px; background: url("<c:url value="/icons/progress.png"/>") repeat;}
    </style>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/util.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/transferService.js"/>"></script>

    <script type="text/javascript">
        function refreshProgress() {
            transferService.getUploadInfo(updateProgress);
        }

        function updateProgress(uploadInfo) {

            var progressBar = document.getElementById("progressBar");
            var progressBarContent = document.getElementById("progressBarContent");
            var progressText = document.getElementById("progressText");


            if (uploadInfo.bytesTotal > 0) {
                var percent = Math.ceil((uploadInfo.bytesUploaded / uploadInfo.bytesTotal) * 100);
                var width = parseInt(percent * 3.5) + 'px';
                progressBarContent.style.width = width;
                progressText.innerHTML = percent + "<fmt:message key="more.upload.progress"/>";
                progressBar.style.display = "block";
                progressText.style.display = "block";
                window.setTimeout("refreshProgress()", 1000);
            } else {
                progressBar.style.display = "none";
                progressText.style.display = "none";
                window.setTimeout("refreshProgress()", 5000);
            }
        }
    </script>

</head>
<body class="mainframe bgcolor1" onload="${model.user.uploadRole ? "refreshProgress()" : ""}">

<h1>
    <img src="<spring:theme code="moreImage"/>" alt=""/>
    <fmt:message key="more.title"/>
</h1>

<c:if test="${model.user.streamRole}">
    <h2><img src="<spring:theme code="randomImage"/>" alt=""/>&nbsp;<fmt:message key="more.random.title"/></h2>

    <form method="post" action="randomPlaylist.view?">
        <table>
            <tr>
                <td><fmt:message key="more.random.text"/></td>
                <td>
                    <select name="size">
                        <option value="5"><fmt:message key="more.random.songs"><fmt:param value="5"/></fmt:message></option>
                        <option value="10" selected="true"><fmt:message key="more.random.songs"><fmt:param value="10"/></fmt:message></option>
                        <option value="20"><fmt:message key="more.random.songs"><fmt:param value="20"/></fmt:message></option>
                        <option value="50"><fmt:message key="more.random.songs"><fmt:param value="50"/></fmt:message></option>
                    </select>
                </td>
                <td><fmt:message key="more.random.genre"/></td>
                <td>
                    <select name="genre">
                        <option value="any"><fmt:message key="more.random.anygenre"/></option>
                        <c:forEach items="${model.genres}" var="genre">
                            <option value="${genre}"><str:truncateNicely upper="20">${genre}</str:truncateNicely></option>
                        </c:forEach>
                    </select>
                </td>
                <td><fmt:message key="more.random.year"/></td>
                <td>
                    <select name="year">
                        <option value="any"><fmt:message key="more.random.anyyear"/></option>

                        <c:forEach begin="0" end="${model.currentYear - 2006}" var="yearOffset">
                            <c:set var="year" value="${model.currentYear - yearOffset}"/>
                            <option value="${year} ${year}">${year}</option>
                        </c:forEach>

                        <option value="2005 2010">2005 &ndash; 2010</option>
                        <option value="2000 2005">2000 &ndash; 2005</option>
                        <option value="1990 2000">1990 &ndash; 2000</option>
                        <option value="1980 1990">1980 &ndash; 1990</option>
                        <option value="1970 1980">1970 &ndash; 1980</option>
                        <option value="1960 1970">1960 &ndash; 1970</option>
                        <option value="1950 1960">1950 &ndash; 1960</option>
                        <option value="0 1949">&lt; 1950</option>
                    </select>
                </td>
                <td><fmt:message key="more.random.folder"/></td>
                <td>
                    <select name="musicFolderId">
                        <option value="-1"><fmt:message key="more.random.anyfolder"/></option>
                        <c:forEach items="${model.musicFolders}" var="musicFolder">
                            <option value="${musicFolder.id}">${musicFolder.name}</option>
                        </c:forEach>
                    </select>
                </td>
                <td>
                    <input type="submit" value="<fmt:message key="more.random.ok"/>">
                </td>
            </tr>
            <c:if test="${not model.clientSidePlaylist}">
                <tr>
                    <td colspan="9">
                        <input type="checkbox" name="autoRandom" id="autoRandom" class="checkbox"/>
                        <label for="autoRandom"><fmt:message key="more.random.auto"/></label>
                    </td>
                </tr>
            </c:if>
        </table>
    </form>
</c:if>
<h2><img src="<spring:theme code="androidImage"/>" alt=""/>&nbsp;<fmt:message key="more.apps.title"/></h2>
<fmt:message key="more.apps.text"/>

<h2><img src="<spring:theme code="wapImage"/>" alt=""/>&nbsp;<fmt:message key="more.mobile.title"/></h2>
<fmt:message key="more.mobile.text"><fmt:param value="${model.brand}"/></fmt:message>

<h2><img src="<spring:theme code="podcastImage"/>" alt=""/>&nbsp;<fmt:message key="more.podcast.title"/></h2>
<fmt:message key="more.podcast.text"/>

<c:if test="${model.user.uploadRole}">

    <h2><img src="<spring:theme code="uploadImage"/>" alt=""/>&nbsp;<fmt:message key="more.upload.title"/></h2>

    <form method="post" enctype="multipart/form-data" action="upload.view">
        <table>
            <tr>
                <td><fmt:message key="more.upload.source"/></td>
                <td colspan="2"><input type="file" id="file" name="file" size="40"/></td>
            </tr>
            <tr>
                <td><fmt:message key="more.upload.target"/></td>
                <td><input type="text" id="dir" name="dir" size="37" value="${model.uploadDirectory}"/></td>
                <td><input type="submit" value="<fmt:message key="more.upload.ok"/>"/></td>
            </tr>
            <tr>
                <td colspan="2">
                    <input type="checkbox" checked name="unzip" id="unzip" class="checkbox"/>
                    <label for="unzip"><fmt:message key="more.upload.unzip"/></label>
                </td>
            </tr>
        </table>
    </form>


    <p class="detail" id="progressText"/>

    <div id="progressBar">
        <div id="progressBarContent"/>
    </div>

</c:if>

</body></html>
