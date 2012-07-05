<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <style type="text/css">
        #wrapper { position:absolute; left:64px; top:128px; width:320; height:196; background-color:#CCCC00; }
    </style>
    <script type="text/javascript" src="<c:url value="/script/swfobject.js"/>"></script>
    <script type="text/javascript">

        function createPlayer() {
            var flashvars = {
//                file:"http://localhost:8080/rest/stream.view?u=admin%26p=admin%26c=test%26v=1.6%26id=7289%26format=.mp3",
                file:"http://localhost:8080/rest/stream.view?u=admin&p=admin&c=test&v=1.6&id=7289&format=.mp3",
                autostart:"true"
            }
            var params = {
                allowfullscreen:"true",
                allowscriptaccess:"always"
            }
            var attributes = {
                id:"player1",
                name:"player1"
            }
            swfobject.embedSWF("<c:url value="/flash/jw-player-5.6.swf"/>", "placeholder1", "320", "196", "9.0.115", false, flashvars, params, attributes);
        }
    </script>
</head>
<body onload="createPlayer();">

<div id="wrapper">
    <div id="placeholder1"></div>
</div>

</body>
</html>