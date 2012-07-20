<%@ page import="java.net.URL" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%! String current = "apps"; %>
<%@ include file="header.jsp" %>

<body>

<a name="top"/>

<div id="container">
    <%@ include file="menu.jsp" %>

    <div id="content">
        <div id="main-col">
            <h1 class="bottomspace">Subsonic Apps</h1>

            <p>Check out the steadily growing list of Subsonic apps. These provide fun and alternative ways to
                enjoy your media collection - no matter where you are.</p>

            <h2>On your phone</h2>

            <div class="floatcontainer margin10-t margin10-b">
                <ul class="stars column-left">
                    <li><a href="#android">Subsonic</a> for Android</li>
                    <li><a href="#winphone">Subsonic</a> for Windows Phone</li>
                    <li><a href="#isub">iSub</a> for iPhone/iPad</li>
                </ul>
                <ul class="stars column-right">
                    <li><a href="#zsubsonic">Z-Subsonic</a> for iPhone/iPad</li>
                    <li><a href="#substream">SubStream</a> for iPhone/iPad</li>
                    <li><a href="#hypersonic">Hypersonic</a> for iPhone/iPad</li>
                </ul>
            </div>

            <h2>On your device</h2>

            <div class="floatcontainer margin10-t margin10-b">
                <ul class="stars column-left">
                    <li><a href="#sonicair">SonicAir</a> for BlackBerry PlayBook</li>
                    <li><a href="#mmtm">My Music To Me</a> for Sonos</li>
                    <li><a href="#chumby">Subsonic</a> for Chumby</li>
                </ul>
                <ul class="stars column-right">
                    <li><a href="#subsonictv">SubsonicTV</a> for Roku</li>
                    <li><a href="#subsonicchannel">Subsonic Channel</a> for Roku</li>
                    <li><a href="#xo">XO</a> for webOS and BlackBerry PlayBook</li>
                </ul>
            </div>

            <h2>On your desktop</h2>

            <div class="floatcontainer margin10-t margin10-b">
                <ul class="stars column-left">
                    <li><a href="#subair">SubAir</a> for desktops</li>
                    <li><a href="#submariner">Submariner</a> for Mac</li>
                    <li><a href="#thumper">Thumper</a> for Mac</li>
                    <li><a href="#subclient">Subclient</a> for Windows</li>
                </ul>
                <ul class="stars column-right"> 
                    <li><a href="#subgadget">SubGadget</a> for Windows</li>
                    <li><a href="#periscope">Periscope</a> for Windows</li>
                    <li><a href="#subwiji">SubWiji</a> for Windows</li>
                </ul>
            </div>

            <h2>In your browser</h2>

            <div class="floatcontainer margin10-t margin10-b">
                <ul class="stars column-left">
                    <li><a href="#perisonic">Perisonic</a> for Google Chrome</li>
                </ul>
                <ul class="stars column-right">
                    <li><a href="#minisub">MiniSub</a> for Google Chrome</li>
                </ul>
            </div>

            <p>
                Please note that most of the apps are made by third-party developers, and are not maintained by
                the Subsonic project. Some apps are commercial, while some are available for free.
            </p>
            <p>Also note that after a 30-day trial period you need a license key to use the apps.
                You get a license key by giving a donation to the Subsonic project. The license never expires and is
                valid for all current and future apps. By donating you also get other benefits; see info box on the
                right.
            </p>
            <p>
                Interested in making your own Subsonic app? Check out the <a href="api.jsp">API</a>.
            </p>

            <%@ include file="apps-android.jsp" %>
            <%@ include file="apps-isub.jsp" %>
            <%@ include file="apps-zsubsonic.jsp" %>
            <%@ include file="apps-substream.jsp" %>
            <%@ include file="apps-hypersonic.jsp" %>
            <%@ include file="apps-winphone.jsp" %>
            <%@ include file="apps-subair.jsp" %>
            <%@ include file="apps-sonicair.jsp" %>
            <%@ include file="apps-mmtm.jsp" %>
            <%@ include file="apps-chumby.jsp" %>
            <%@ include file="apps-subsonictv.jsp" %>
            <%@ include file="apps-subsonicchannel.jsp" %>
            <%@ include file="apps-xo.jsp" %>
            <%@ include file="apps-submariner.jsp" %>
            <%@ include file="apps-thumper.jsp" %>
            <%@ include file="apps-subgadget.jsp" %>
            <%@ include file="apps-periscope.jsp" %>
            <%@ include file="apps-subwiji.jsp" %>
            <%@ include file="apps-subclient.jsp" %>
            <%@ include file="apps-perisonic.jsp" %>
            <%@ include file="apps-minisub.jsp" %>

        </div>

        <div id="side-col">
            <%@ include file="google-translate.jsp" %>
            <%@ include file="donate.jsp" %>
        </div>

        <div class="clear">
        </div>
    </div>
    <hr/>
    <%@ include file="footer.jsp" %>
</div>

</body>
</html>
