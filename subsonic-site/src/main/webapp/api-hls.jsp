<h2 class="div"><a name="hls"></a>hls</h2>

<p>
    <code>http://your-server/rest/hls.view</code>
    <br>Since <a href="#versions">1.8.0</a>
</p>

<p>
    Creates an HLS (<a href="http://en.wikipedia.org/wiki/HTTP_Live_Streaming" target="_blank">HTTP Live Streaming</a>)
    playlist used for streaming video.  HLS is a streaming protocol implemented by Apple and works by breaking the overall
    stream into a sequence of small HTTP-based file downloads. It's supported by iOS and newer versions of Android.
    This method also supports <strong>adaptive bitrate streaming</strong>, see the <code>bitRate</code> parameter.
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
        <td>A string which uniquely identifies the video file to stream.</td>
    </tr>
    <tr>
        <td><code>bitRate</code></td>
        <td>No</td>
        <td></td>
        <td>If specified, the server will attempt to limit the bitrate to this value, in kilobits per second.
            If this parameter is specified more than once, the server will create a <strong>variant playlist</strong>,
            suitable for adaptive bitrate streaming. The playlist will support streaming at all the specified bitrates.</td>
    </tr>

</table>
<p>
    Returns an M3U8 playlist on success (content type "application/vnd.apple.mpegurl"), or an XML document on error
    (in which case the HTTP content type will start with "text/xml").
</p>
