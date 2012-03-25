Name:           supersonic
Version:        @VERSION@
Release:        @BUILD_NUMBER@
Summary:        A web-based music streamer, jukebox and Podcast receiver

Group:          Applications/Multimedia
License:        GPLv3
URL:            http://subsonic.org

%description
Supersonic is a web-based music streamer, jukebox and Podcast receiver,
providing access to your music collection wherever you are. Use it
to share your music with friends, or to listen to your music while away
from home.

Apps for Android, iPhone and Windows Phone are also available.

Java 1.6 or higher is required to run Subsonic.

Subsonic can be found at http://subsonic.org

%files
%defattr(644,root,root,755)
/usr/share/supersonic/supersonic-booter-jar-with-dependencies.jar
/usr/share/supersonic/supersonic.war
%attr(755,root,root) /usr/share/supersonic/supersonic.sh
%attr(755,root,root) /etc/init.d/supersonic
%attr(755,root,root) /var/supersonic/transcode/ffmpeg
%attr(755,root,root) /var/supersonic/transcode/lame
%config(noreplace) /etc/sysconfig/supersonic

%pre
# Stop Subsonic service.
if [ -e /etc/init.d/supersonic ]; then
  service supersonic stop
fi

exit 0

%post
ln -sf /usr/share/subsonic/subsonic.sh /usr/bin/subsonic
chmod 750 /var/subsonic

# Clear jetty cache.
rm -rf /var/subsonic/jetty

# For SELinux: Set security context
chcon -t java_exec_t /etc/init.d/subsonic 2>/dev/null

# Configure and start Subsonic service.
chkconfig --add subsonic
service subsonic start

exit 0

%preun
# Only do it if uninstalling, not upgrading.
if [ $1 = 0 ] ; then

  # Stop the service.
  [ -e /etc/init.d/subsonic ] && service subsonic stop

  # Remove symlink.
  rm -f /usr/bin/subsonic

  # Remove startup scripts.
  chkconfig --del subsonic

fi

exit 0

