<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "api"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
<%@ include file="menu.jsp" %>

<div id="content">
<div id="main-col">
<h1>Subsonic API</h1>

<p>
    The Subsonic API allows anyone to build their own programs using Subsonic as the server, whether they're
    on the web, the desktop or on mobile devices. As an example, all the Subsonic<a href="apps.jsp">apps</a> are built using the
    Subsonic API.
</p>
<p>
    Feel free to join the <a href="http://groups.google.com/group/subsonic-app-developers">Subsonic App Developers</a> group
    for discussions, suggestions and questions.
</p>

<h2 class="div">Introduction</h2>

<p>
    The Subsonic API allows you to call methods that respond in <a
        href="http://en.wikipedia.org/wiki/Representational_State_Transfer">REST</a> style xml.
    Individual methods are detailed below.
</p>

<p>
    Please note that all methods take the following parameters:
</p>

<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>u</code></td>
        <td>Yes</td>
        <td></td>
        <td>The username.</td>
    </tr>
    <tr>
        <td><code>p</code></td>
        <td>Yes</td>
        <td></td>
        <td>The password, either in clear text or hex-encoded with a "enc:" prefix.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>v</code></td>
        <td>Yes</td>
        <td></td>
        <td>The protocol version implemented by the client, i.e., the version of the
            <code>subsonic-rest-api.xsd</code> schema used (see below).</td>
    </tr>
    <tr>
        <td><code>c</code></td>
        <td>Yes</td>
        <td></td>
        <td>A unique string identifying the client application.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>f</code></td>
        <td>No</td>
        <td>xml</td>
        <td>Request data to be returned in this format. Supported values are "xml", "json" (since <a href="#versions">1.4.0</a>)
            and "jsonp" (since <a href="#versions">1.6.0</a>). If using jsonp, specify name of javascript callback function using
            a <code>callback</code> parameter.</td>
    </tr>
</table>

<p>
    For example:
</p>

<p>
    <code>http://your-server/rest/getIndexes.view?u=joe&amp;p=sesame&amp;v=1.1.0&amp;c=myapp</code>, or<br/>
    <code>http://your-server/rest/getIndexes.view?u=joe&amp;p=enc:736573616d65&amp;v=1.1.0&amp;c=myapp</code>
</p>

<p>
    Starting with API version 1.2.0 it is no longer necessary to send the username and password as part of the URL.
    Instead, HTTP <a href="http://en.wikipedia.org/wiki/Basic_access_authentication">Basic</a> authentication could be
    used.
    (Only <em>preemptive</em> authentication is supported, meaning that the credentials should be supplied by the client
    without being challenged for it.)
</p>

<p>
    Note that UTF-8 should be used when sending parameters to API methods. The XML returned
    will also be encoded with UTF-8.
</p>

<p>
    All methods (except those that return binary data) returns XML documents conforming to the
    <code>subsonic-rest-api.xsd</code> schema. This schema (as well as example XML documents) can be found
    at <code>http://your-server/xsd/</code>
</p>

<h2 class="div">Error handling</h2>

<p>
    If a method fails it will return an error code and message in an <code>&lt;error&gt;</code> element.
    In addition, the <code>status</code> attribute of the <code>&lt;subsonic-response&gt;</code> root element
    will be set to <code>failed</code> instead of <code>ok</code>. For example:
</p>

            <pre>
   &lt;?xml version="1.0" encoding="UTF-8"?&gt;
   &lt;subsonic-response xmlns="http://subsonic.org/restapi"
                      status="failed" version="1.1.0"&gt;
       &lt;error code="40" message="Wrong username or password"/&gt;
   &lt;/subsonic-response&gt;
            </pre>

<p>
    The following error codes are defined:
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Code</th>
        <th class="param-heading">Description</th>
    </tr>
    <tr class="table-altrow">
        <td><code>0</code></td>
        <td>A generic error.</td>
    </tr>
    <tr>
        <td><code>10</code></td>
        <td>Required parameter is missing.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>20</code></td>
        <td>Incompatible Subsonic REST protocol version. Client must upgrade.</td>
    </tr>
    <tr>
        <td><code>30</code></td>
        <td>Incompatible Subsonic REST protocol version. Server must upgrade.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>40</code></td>
        <td>Wrong username or password.</td>
    </tr>
    <tr>
        <td><code>50</code></td>
        <td>User is not authorized for the given operation.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>60</code></td>
        <td>The trial period for the Subsonic server is over. Please donate to get a license key. Visit subsonic.org for details.</td>
    </tr>
    <tr>
        <td><code>70</code></td>
        <td>The requested data was not found.</td>
    </tr>
</table>

<h2 class="div"><a name="versions"></a>Versions</h2>

<p>
    This table shows the REST API version implemented in different Subsonic versions:
</p>
<table width="50%" class="bottomspace">
    <tr>
        <th class="param-heading">Subsonic version</th>
        <th class="param-heading">REST API version</th>
    </tr>
    <tr class="table-altrow">
        <td>4.7</td>
        <td>1.8.0</td>
    </tr>
    <tr>
        <td>4.6</td>
        <td>1.7.0</td>
    </tr>
    <tr class="table-altrow">
        <td>4.5</td>
        <td>1.6.0</td>
    </tr>
    <tr>
        <td>4.3.1</td>
        <td>1.5.0</td>
    </tr>
    <tr class="table-altrow">
        <td>4.2</td>
        <td>1.4.0</td>
    </tr>
    <tr>
        <td>4.1</td>
        <td>1.3.0</td>
    </tr>
    <tr class="table-altrow">
        <td>4.0</td>
        <td>1.2.0</td>
    </tr>
    <tr>
        <td>3.9</td>
        <td>1.1.1</td>
    </tr>
    <tr class="table-altrow">
        <td>3.8</td>
        <td>1.0.0</td>
    </tr>
</table>
<p>
    Note that a Subsonic server is backward compatible with a REST client if and only if the major version is the same,
    and the minor version of the client is less than or equal to the server's. For example, if the server has
    REST API version 2.2, it supports client versions 2.0, 2.1 and 2.2, but not versions 1.x, 2.3+ or 3.x. The third
    part of the version number is not used to determine compatibility.
</p>

<h1>List of API methods</h1>

<h2 class="div">ping</h2>

<p>
    <code>http://your-server/rest/ping.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Used to test connectivity with the server. Takes no extra parameters.
</p>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/ping_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getLicense</h2>

<p>
    <code>http://your-server/rest/getLicense.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Get details about the software license. Takes no extra parameters. Please note that access to the
    REST API requires that the server has a valid license (after a 30-day trial period). To get a license key you can
    give a donation to the Subsonic project.
</p>

<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;license&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/license_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getMusicFolders</h2>

<p>
    <code>http://your-server/rest/getMusicFolders.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns all configured top-level music folders. Takes no extra parameters.
</p>

<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;musicFolders&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/musicFolders_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getNowPlaying</h2>

<p>
    <code>http://your-server/rest/getNowPlaying.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns what is currently being played by all users. Takes no extra parameters.
</p>

<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;nowPlaying&gt;</code>
    element on success.  <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/nowPlaying_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getIndexes</h2>

<p>
    <code>http://your-server/rest/getIndexes.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns an indexed structure of all artists.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>musicFolderId</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, only return artists in the music folder with the given ID. See <code>getMusicFolders</code>.
        </td>
    </tr>
    <tr>
        <td><code>ifModifiedSince</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, only return a result if the artist collection has changed since the given time.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;indexes&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/indexes_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getMusicDirectory</h2>

<p>
    <code>http://your-server/rest/getMusicDirectory.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns a listing of all files in a music directory. Typically used to get list of albums for an artist,
    or list of songs for an album.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the music folder. Obtained by calls to getIndexes or getMusicDirectory.
        </td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;directory&gt;</code>
    element on success.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/directory_example_1.xml?view=markup">Example 1</a>.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/directory_example_2.xml?view=markup">Example 2</a>.
</p>

<h2 class="div">search</h2>

<p>
    <code>http://your-server/rest/search.view</code>
    <br>Since <a href="#versions">1.0.0</a>
    <br>Deprecated since <a href="#versions">1.4.0</a>, use <code>search2</code> instead.
</p>

<p>
    Returns a listing of files matching the given search criteria. Supports paging through the result.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>artist</code></td>
        <td>No</td>
        <td></td>
        <td>Artist to search for.</td>
    </tr>
    <tr>
        <td><code>album</code></td>
        <td>No</td>
        <td></td>
        <td>Album to searh for.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>title</code></td>
        <td>No</td>
        <td></td>
        <td>Song title to search for.</td>
    </tr>
    <tr>
        <td><code>any</code></td>
        <td>No</td>
        <td></td>
        <td>Searches all fields.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>count</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of results to return.</td>
    </tr>
    <tr>
        <td><code>offset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset. Used for paging.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>newerThan</code></td>
        <td>No</td>
        <td></td>
        <td>Only return matches that are newer than this. Given as milliseconds since 1970.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;searchResult&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/searchResult_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">search2</h2>

<p>
    <code>http://your-server/rest/search2.view</code>
    <br>Since <a href="#versions">1.4.0</a>
</p>

<p>
    Returns albums, artists and songs matching the given search criteria. Supports paging through the result.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>query</code></td>
        <td>Yes</td>
        <td></td>
        <td>Search query.</td>
    </tr>
    <tr>
        <td><code>artistCount</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of artists to return.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>artistOffset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset for artists. Used for paging.</td>
    </tr>
    <tr>
        <td><code>albumCount</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of albums to return.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>albumOffset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset for albums. Used for paging.</td>
    </tr>
    <tr>
        <td><code>songCount</code></td>
        <td>No</td>
        <td>20</td>
        <td>Maximum number of songs to return.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>songOffset</code></td>
        <td>No</td>
        <td>0</td>
        <td>Search result offset for songs. Used for paging.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;searchResult2&gt;</code>
    element on success.  <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/searchResult2_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getPlaylists</h2>

<p>
    <code>http://your-server/rest/getPlaylists.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns all playlists a user is allowed to play.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>no</td>
        <td></td>
        <td>(Since <a href="#versions">1.8.0</a>) If specified, return playlists for this user rather than for the authenticated user. The authenticated user must
            have admin role if this parameter is used.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;playlists&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/playlists_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getPlaylist</h2>

<p>
    <code>http://your-server/rest/getPlaylist.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns a listing of files in a saved playlist.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>yes</td>
        <td></td>
        <td>ID of the playlist to return, as obtained by <code>getPlaylists</code>.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;playlist&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/playlist_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">createPlaylist</h2>

<p>
    <code>http://your-server/rest/createPlaylist.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Creates or updates a saved playlist. Note: The user must be authorized to create playlists (see Settings &gt; Users
    &gt; User is allowed to create and delete playlists).
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>playlistId</code></td>
        <td>Yes (if updating)</td>
        <td></td>
        <td>The playlist ID.</td>
    </tr>
    <tr>
        <td><code>name</code></td>
        <td>Yes (if creating)</td>
        <td></td>
        <td>The human-readable name of the playlist.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>songId</code></td>
        <td>Yes</td>
        <td></td>
        <td>ID of a song in the playlist. Use one <code>songId</code> parameter for each song in the playlist.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">deletePlaylist</h2>

<p>
    <code>http://your-server/rest/deletePlaylist.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Deletes a saved playlist.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>yes</td>
        <td></td>
        <td>ID of the playlist to delete, as obtained by <code>getPlaylists</code>.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">download</h2>

<p>
    <code>http://your-server/rest/download.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Downloads a given media file.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the file to download. Obtained by calls to getMusicDirectory.</td>
    </tr>
</table>
<p>
    Returns binary data on success, or an XML document on error (in which case the HTTP content type will start with "text/xml").
</p>

<h2 class="div">stream</h2>

<p>
    <code>http://your-server/rest/stream.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Streams a given media file.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the file to stream. Obtained by calls to getMusicDirectory.</td>
    </tr>
    <tr>
        <td><code>maxBitRate</code></td>
        <td>No</td>
        <td></td>
        <td>(Since <a href="#versions">1.2.0</a>) If specified, the server will attempt to limit the bitrate
            to this value, in kilobits per second. If set to zero, no limit is imposed.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>format</code></td>
        <td>No</td>
        <td></td>
        <td>(Since <a href="#versions">1.6.0</a>) Specifies the preferred target format (e.g., "mp3" or "flv") in case there are multiple applicable transcodings.</td>
    </tr>
    <tr>
        <td><code>timeOffset</code></td>
        <td>No</td>
        <td></td>
        <td>Only applicable to video streaming. If specified, start streaming at the given offset (in seconds) into the video.
            Typically used to implement video skipping.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>size</code></td>
        <td>No</td>
        <td></td>
        <td>(Since <a href="#versions">1.6.0</a>) Only applicable to video streaming. Requested video size specified as WxH, for instance "640x480".</td>
    </tr>
    <tr>
        <td><code>estimateContentLength</code></td>
        <td>No</td>
        <td>false</td>
        <td>(Since <a href="#versions">1.8.0</a>). If set to "true", the <em>Content-Length</em> HTTP header will be set to an estimated value
            for transcoded or downsampled media.</td>
    </tr></table>
<p>
    Returns binary data on success, or an XML document on error (in which case the HTTP content type will start with "text/xml").
</p>

<h2 class="div">getCoverArt</h2>

<p>
    <code>http://your-server/rest/getCoverArt.view</code>
    <br>Since <a href="#versions">1.0.0</a>
</p>

<p>
    Returns a cover art image.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the cover art file to download. Obtained by calls to getMusicDirectory.
        </td>
    </tr>
    <tr>
        <td><code>size</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, scale image to this size.</td>
    </tr>
</table>
<p>
    Returns the cover art image in binary form.
</p>

<h2 class="div">getAvatar</h2>

<p>
    <code>http://your-server/rest/getAvatar.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Returns the avatar (personal image) for a user.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>Yes</td>
        <td></td>
        <td>The user in question.</td>
    </tr>
</table>
<p>
    Returns the avatar image in binary form.
</p>


<h2 class="div">scrobble</h2>

<p>
    <code>http://your-server/rest/scrobble.view</code>
    <br>Since <a href="#versions">1.5.0</a>
</p>

<p>
    "Scrobbles" a given music file on last.fm. Requires that the user has configured his/her last.fm
    credentials on the Subsonic server (Settings &gt; Personal).
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the file to scrobble.</td>
    </tr>
    <tr>
        <td><code>submission</code></td>
        <td>No</td>
        <td>True</td>
        <td>Whether this is a "submission" or a "now playing" notification.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">changePassword</h2>

<p>
    <code>http://your-server/rest/changePassword.view</code>
    <br>Since <a href="#versions">1.1.0</a>
</p>

<p>
    Changes the password of an existing Subsonic user, using the following parameters.
    You can only change your own password unless you have admin privileges.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>Yes</td>
        <td></td>
        <td>The name of the user which should change its password.</td>
    </tr>
    <tr>
        <td><code>password</code></td>
        <td>Yes</td>
        <td></td>
        <td>The new password of the new user, either in clear text of hex-encoded (see above).</td>
    </tr>
</table>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">getUser</h2>

<p>
    <code>http://your-server/rest/getUser.view</code>
    <br>Since <a href="#versions">1.3.0</a>
</p>

<p>
    Get details about a given user, including which authorization roles it has.
    Can be used to enable/disable certain features in the client, such as jukebox control.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>Yes</td>
        <td></td>
        <td>The name of the user to retrieve. You can only retrieve your own user unless you have admin privileges.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;user&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/user_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">createUser</h2>

<p>
    <code>http://your-server/rest/createUser.view</code>
    <br>Since <a href="#versions">1.1.0</a>
</p>

<p>
    Creates a new Subsonic user, using the following parameters:
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>Yes</td>
        <td></td>
        <td>The name of the new user.</td>
    </tr>
    <tr>
        <td><code>password</code></td>
        <td>Yes</td>
        <td></td>
        <td>The password of the new user, either in clear text of hex-encoded (see above).</td>
    </tr>
    <tr class="table-altrow">
        <td><code>email</code></td>
        <td>Yes</td>
        <td></td>
        <td>The email address of the new user.</td>
    </tr>
    <tr>
        <td><code>ldapAuthenticated</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is authenicated in LDAP.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>adminRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is administrator.</td>
    </tr>
    <tr>
        <td><code>settingsRole</code></td>
        <td>No</td>
        <td>true</td>
        <td>Whether the user is allowed to change settings and password.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>streamRole</code></td>
        <td>No</td>
        <td>true</td>
        <td>Whether the user is allowed to play files.</td>
    </tr>
    <tr>
        <td><code>jukeboxRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to play files in jukebox mode.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>downloadRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to download files.</td>
    </tr>
    <tr>
        <td><code>uploadRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to upload files.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>playlistRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to create and delete playlists. Since 1.8.0, changing this role has no effect.</td>
    </tr>
    <tr>
        <td><code>coverArtRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to change cover art and tags.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>commentRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to create and edit comments and ratings.</td>
    </tr>
    <tr>
        <td><code>podcastRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>Whether the user is allowed to administrate Podcasts.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>shareRole</code></td>
        <td>No</td>
        <td>false</td>
        <td>(Since <a href="#versions">1.8.0</a>)Whether the user is allowed to share files with anyone.</td>
    </tr>
</table>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>
<h2 class="div">deleteUser</h2>

<p>
    <code>http://your-server/rest/deleteUser.view</code>
    <br>Since <a href="#versions">1.3.0</a>
</p>

<p>
    Deletes an existing Subsonic user, using the following parameters:
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>username</code></td>
        <td>Yes</td>
        <td></td>
        <td>The name of the user to delete.</td>
    </tr>
</table>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">getChatMessages</h2>

<p>
    <code>http://your-server/rest/getChatMessages.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Returns the current visible (non-expired) chat messages.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>since</code></td>
        <td>No</td>
        <td></td>
        <td>Only return messages newer than this time (in millis since Jan 1 1970).</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;chatMessages&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/chatMessages_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">addChatMessage</h2>

<p>
    <code>http://your-server/rest/addChatMessage.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Adds a message to the chat log.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>message</code></td>
        <td>Yes</td>
        <td></td>
        <td>The chat message.</td>
    </tr>
</table>

<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">getAlbumList</h2>

<p>
    <code>http://your-server/rest/getAlbumList.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Returns a list of random, newest, highest rated etc. albums. Similar to the album lists
    on the home page of the Subsonic web interface.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>type</code></td>
        <td>Yes</td>
        <td></td>
        <td>The list type. Must be one of the following: <code>random</code>, <code>newest</code>,
            <code>highest</code>, <code>frequent</code>, <code>recent</code>. Since <a href="#versions">1.8.0</a>
            you can also use <code>alphabetical</code> to page through all albums sorted alphabetically by artist
            and album name.</td>
    </tr>
    <tr>
        <td><code>size</code></td>
        <td>No</td>
        <td>10</td>
        <td>The number of albums to return. Max 500.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>offset</code></td>
        <td>No</td>
        <td>0</td>
        <td>The list offset. Useful if you for example want to page through the list of newest albums.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;albumList&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/albumList_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getRandomSongs</h2>

<p>
    <code>http://your-server/rest/getRandomSongs.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Returns random songs matching the given criteria.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>size</code></td>
        <td>No</td>
        <td>10</td>
        <td>The maximum number of songs to return. Max 500.</td>
    </tr>
    <tr>
        <td><code>genre</code></td>
        <td>No</td>
        <td></td>
        <td>Only returns songs belonging to this genre.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>fromYear</code></td>
        <td>No</td>
        <td></td>
        <td>Only return songs published after or in this year.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>toYear</code></td>
        <td>No</td>
        <td></td>
        <td>Only return songs published before or in this year.</td>
    </tr>
    <tr>
        <td><code>musicFolderId</code></td>
        <td>No</td>
        <td></td>
        <td>Only return songs in the music folder with the given ID. See getMusicFolders.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;randomSongs&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/randomSongs_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getLyrics</h2>

<p>
    <code>http://your-server/rest/getLyrics.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Searches for and returns lyrics for a given song.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>artist</code></td>
        <td>No</td>
        <td></td>
        <td>The artist name.</td>
    </tr>
    <tr>
        <td><code>title</code></td>
        <td>No</td>
        <td></td>
        <td>The song title.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;lyrics&gt;</code>
    element on success. The <code>&lt;lyrics&gt;</code> element is empty if no matching lyrics was found.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/lyrics_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">jukeboxControl</h2>

<p>
    <code>http://your-server/rest/jukeboxControl.view</code>
    <br>Since <a href="#versions">1.2.0</a>
</p>

<p>
    Controls the jukebox, i.e., playback directly on the server's audio hardware. Note: The user must
    be authorized to control the jukebox (see Settings &gt; Users &gt; User is allowed to play files in jukebox mode).
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>action</code></td>
        <td>Yes</td>
        <td></td>
        <td>The operation to perform. Must be one of: <code>get</code>, <code>status</code> (since <a href="#versions">1.7.0</a>), <code>set</code> (since <a href="#versions">1.7.0</a>),
            <code>start</code>, <code>stop</code>, <code>skip</code>, <code>add</code>, <code>clear</code>, <code>remove</code>, <code>shuffle</code>, <code>setGain</code>
        </td>
    </tr>
    <tr>
        <td><code>index</code></td>
        <td>No</td>
        <td></td>
        <td>Used by <code>skip</code> and <code>remove</code>. Zero-based index of the song to skip to or remove.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>offset</code></td>
        <td>No</td>
        <td></td>
        <td>(Since <a href="#versions">1.7.0</a>) Used by <code>skip</code>. Start playing this many seconds into the track.</td>
    </tr>
    <tr>
        <td><code>id</code></td>
        <td>No</td>
        <td></td>
        <td>Used by <code>add</code> and <code>set</code>. ID of song to add to the jukebox playlist. Use multiple <code>id</code> parameters
            to add many songs in the same request. (<code>set</code> is similar to a <code>clear</code> followed by a <code>add</code>, but
            will not change the currently playing track.)
        </td>
    </tr>
    <tr class="table-altrow">
        <td><code>gain</code></td>
        <td>No</td>
        <td></td>
        <td>Used by <code>setGain</code> to control the playback volume. A float value between 0.0 and 1.0.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;jukeboxStatus&gt;</code> element on success, unless the <code>get</code>
    action is used, in which case a nested <code>&lt;jukeboxPlaylist&gt;</code> element is returned.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/jukeboxStatus_example_1.xml?view=markup">Example 1</a>.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/jukeboxPlaylist_example_1.xml?view=markup">Example 2</a>.
</p>

<h2 class="div">getPodcasts</h2>

<p>
    <code>http://your-server/rest/getPodcasts.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>

<p>
    Returns all podcast channels the server subscribes to and their episodes. Takes no extra parameters.
</p>

<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;podcasts&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/podcasts_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">getShares</h2>
<p>
    <code>http://your-server/rest/getShares.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>
<p>
    Returns information about shared media this user is allowed to manage. Takes no extra parameters.
</p>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;shares&gt;</code>
    element on success. <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/shares_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">createShare</h2>
<p>
    <code>http://your-server/rest/createShare.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>
<p>
    Creates a public URL that can be used by anyone to stream music or video from the Subsonic server.  The URL is short and
    suitable for posting on Facebook, Twitter etc. Note: The user must be authorized to share (see Settings &gt; Users
    &gt; User is allowed to share files with anyone).
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>ID of a song, album or video to share. Use one <code>id</code> parameter for each entry to share.</td>
    </tr>
    <tr>
        <td><code>description</code></td>
        <td>No</td>
        <td></td>
        <td>A user-defined description that will be displayed to people visiting the shared media.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>expires</code></td>
        <td>No</td>
        <td></td>
        <td>The time at which the share expires. Given as milliseconds since 1970.</td>
    </tr>
</table>
<p>
    Returns a <code>&lt;subsonic-response&gt;</code> element with a nested <code>&lt;shares&gt;</code>
    element on success, which in turns contains a single <code>&lt;share&gt;</code> element for the newly created share.
    <a href="http://subsonic.svn.sourceforge.net/viewvc/subsonic/trunk/subsonic-main/src/main/webapp/xsd/shares_example_1.xml?view=markup">Example</a>.
</p>

<h2 class="div">updateShare</h2>
<p>
    <code>http://your-server/rest/updateShare.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>
<p>
    Updates the description and/or expiration date for an existing share.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>ID of the share to update.</td>
    </tr>
    <tr>
        <td><code>description</code></td>
        <td>No</td>
        <td></td>
        <td>A user-defined description that will be displayed to people visiting the shared media.</td>
    </tr>
    <tr class="table-altrow">
        <td><code>expires</code></td>
        <td>No</td>
        <td></td>
        <td>The time at which the share expires. Given as milliseconds since 1970, or zero to remove the expiration.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>

<h2 class="div">deleteShare</h2>
<p>
    <code>http://your-server/rest/deleteShare.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>
<p>
    Deletes an existing share.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>ID of the share to delete.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>


<h2 class="div">setRating</h2>

<p>
    <code>http://your-server/rest/setRating.view</code>
    <br>Since <a href="#versions">1.6.0</a>
</p>

<p>
    Sets the rating for a music file.
</p>
<table width="100%" class="bottomspace">
    <tr>
        <th class="param-heading">Parameter</th>
        <th class="param-heading">Required</th>
        <th class="param-heading">Default</th>
        <th class="param-heading">Comment</th>
    </tr>
    <tr class="table-altrow">
        <td><code>id</code></td>
        <td>Yes</td>
        <td></td>
        <td>A string which uniquely identifies the file (song) or folder (album/artist) to rate.</td>
    </tr>
    <tr>
        <td><code>rating</code></td>
        <td>Yes</td>
        <td></td>
        <td>The rating between 1 and 5 (inclusive), or 0 to remove the rating.</td>
    </tr>
</table>
<p>
    Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
</p>


</div>

<div id="side-col">

    <%@ include file="google-translate.jsp" %>
    <%@ include file="donate.jsp" %>
    <%@ include file="merchandise.jsp" %>

</div>

<div class="clear">
</div>
</div>
<hr/>
<%@ include file="footer.jsp" %>
</div>


</body>
</html>
