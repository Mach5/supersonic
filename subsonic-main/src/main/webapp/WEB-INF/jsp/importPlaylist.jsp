<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html><head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<h1 style="padding-bottom:0.5em">
    <fmt:message key="importPlaylist.title"/>
</h1>

<fmt:message key="importPlaylist.text"/>
<form method="post" enctype="multipart/form-data" action="importPlaylist.view">
    <input type="file" id="file" name="file" size="40"/>
    <input type="submit" value="<fmt:message key="common.ok"/>"/>
</form>

</body></html>