#!/bin/bash

###################################################################################
# Shell script for starting Supersonic.  See https://github.com/Mach5/supersonic
#
# Author: Timo Reimann (devel@foo-lounge.de)
###################################################################################

SUPERSONIC_HOME=/var/supersonic
SUPERSONIC_HOST=0.0.0.0
SUPERSONIC_PORT=4040
SUPERSONIC_HTTPS_PORT=0
SUPERSONIC_CONTEXT_PATH=/
SUPERSONIC_MAX_MEMORY=100
SUPERSONIC_PIDFILE=${SUPERSONIC_HOME}/supersonic.pid
SUPERSONIC_ARGFILE=${SUPERSONIC_HOME}/supersonic.args
SUPERSONIC_DEFAULT_MUSIC_FOLDER=/var/music
SUPERSONIC_DEFAULT_PODCAST_FOLDER=/var/music/Podcast
SUPERSONIC_DEFAULT_PLAYLIST_FOLDER=/var/playlists
SUPERSONIC_SOUNDCARD_DEVICE=

quiet=0

usage() {
    echo "Usage: supersonic.sh [options] {start|stop|restart|status}"
    echo "  --help               This small usage guide."
    echo "  --home=DIR           The directory where Supersonic will create files."
    echo "                       Make sure it is writable. Default: /var/supersonic"
    echo "  --host=HOST          The host name or IP address on which to bind Supersonic."
    echo "                       Only relevant if you have multiple network interfaces and want"
    echo "                       to make Supersonic available on only one of them. The default value"
    echo "                       will bind Supersonic to all available network interfaces. Default: 0.0.0.0"
    echo "  --port=PORT          The port on which Supersonic will listen for"
    echo "                       incoming HTTP traffic. Default: 4040"
    echo "  --https-port=PORT    The port on which Supersonic will listen for"
    echo "                       incoming HTTPS traffic. Default: 0 (disabled)"
    echo "  --context-path=PATH  The context path, i.e., the last part of the Supersonic"
    echo "                       URL. Typically '/' or '/supersonic'. Default '/'"
    echo "  --max-memory=MB      The memory limit (max Java heap size) in megabytes."
    echo "                       Default: 100"
    echo "  --pidfile=PIDFILE    Write PID to this file.  Default '${SUPERSONIC_PIDFILE}'"
    echo "  --quiet              Don't print anything to standard out. Default false."
    echo "  --default-music-folder=DIR    Configure Supersonic to use this folder for music.  This option "
    echo "                                only has effect the first time Supersonic is started. Default '/var/music'"
    echo "  --default-podcast-folder=DIR  Configure Supersonic to use this folder for Podcasts.  This option "
    echo "                                only has effect the first time Supersonic is started. Default '/var/music/Podcast'"
    echo "  --default-playlist-folder=DIR Configure Supersonic to use this folder for playlists.  This option "
    echo "                                only has effect the first time Supersonic is started. Default '/var/playlists'"
    echo "  --soundcard-device=DEVICE     If provided, use the given soundcard device as output"
    exit 1
}

# Use JAVA_HOME if set, otherwise assume java is in the path.
JAVA=java
if [ -e "${JAVA_HOME}" ]
    then
    JAVA=${JAVA_HOME}/bin/java
fi

# Create Supersonic home directory.
mkdir -p ${SUPERSONIC_HOME}
LOG=${SUPERSONIC_HOME}/supersonic_sh.log
rm -f ${LOG}

get_pid() {
   if [ -s ${SUPERSONIC_PIDFILE} ]; then
      PID=`cat ${SUPERSONIC_PIDFILE}`
   else
      PID=""
   fi
}

start() {
   get_pid
   if [ -z $PID ]; then
        if [ $quiet -eq 0 ]; then
           echo -n "Starting Supersonic."
        fi

        cd $(dirname $0)
        if [ -L $0 ] && ([ -e /bin/readlink ] || [ -e /usr/bin/readlink ]); then
           cd $(dirname $(readlink $0))
        fi

        # Restarts must persist argument across calls to start()
        if [ "$1" = "keepargs" ]; then
            mapfile -t SUPERSONIC_ARGS < ${SUPERSONIC_ARGFILE}
        else
            SUPERSONIC_ARGS=()
            SUPERSONIC_ARGS=(-Xmx${SUPERSONIC_MAX_MEMORY}m \
            -Dsubsonic.home=${SUPERSONIC_HOME} \
            -Dsubsonic.host=${SUPERSONIC_HOST} \
            -Dsubsonic.port=${SUPERSONIC_PORT} \
            -Dsubsonic.httpsPort=${SUPERSONIC_HTTPS_PORT} \
            -Dsubsonic.contextPath=${SUPERSONIC_CONTEXT_PATH} \
            -Dsubsonic.defaultMusicFolder=${SUPERSONIC_DEFAULT_MUSIC_FOLDER} \
            -Dsubsonic.defaultPodcastFolder=${SUPERSONIC_DEFAULT_PODCAST_FOLDER} \
            -Dsubsonic.defaultPlaylistFolder=${SUPERSONIC_DEFAULT_PLAYLIST_FOLDER})
            [ "$SUPERSONIC_SOUNDCARD_DEVICE" ] && SUPERSONIC_ARGS+=(-Djavax.sound.sampled.SourceDataLine=#"$SUPERSONIC_SOUNDCARD_DEVICE")
            SUPERSONIC_ARGS+=(-Djava.awt.headless=true \
            -verbose:gc \
            -jar supersonic-booter-jar-with-dependencies.jar)
            echo "${SUPERSONIC_ARGS[@]}" > ${SUPERSONIC_ARGFILE}
        fi

        ${JAVA} "${SUPERSONIC_ARGS[@]}" > ${LOG} 2>&1 &

        # Write pid to pidfile if it is defined.
        if [ $SUPERSONIC_PIDFILE ]; then
            echo $! > ${SUPERSONIC_PIDFILE}
        fi

        if [ $quiet -eq 0 ]; then
            echo ".. Done."
            echo "Started Supersonic [PID $!, ${LOG}]"
        fi
   else
      echo "Supersonic is already running [PID $PID]"
   fi
}

stop() {
   get_pid

   if [ -s ${SUPERSONIC_ARGFILE} ] && [ "$1" != "keepargs" ]; then
      rm -f ${SUPERSONIC_ARGFILE}
   fi

   if [ -z $PID ]; then
      echo "Supersonic is not running."
      exit 1
   else
      echo -n "Stopping Supersonic."
      kill $PID
      sleep 1
      echo ".. Done."
      rm -f ${SUPERSONIC_PIDFILE}
   fi
}

restart() {
   get_pid
   if [ -z $PID ]; then
      status
   else
      echo "Restarting Supersonic..."
      stop keepargs
      start keepargs
   fi
}

status() {
   get_pid
   if [ -z  $PID ]; then
      echo "Supersonic is not running."
      exit 1
   else
      echo "Supersonic is running [PID $PID]"
   fi
}


# Maintain legacy functionality when calling script with no command
if [ "$#" = "0" ]; then
  start
fi


# Parse arguments.
while [ $# -ge 1 ]; do
   case $1 in
      -h)
         usage
         ;;
      --help)
         usage
         ;;
      --home=?*)
         SUPERSONIC_HOME=${1#--home=}
         ;;
      --host=?*)
         SUPERSONIC_HOST=${1#--host=}
         ;;
      --port=?*)
         SUPERSONIC_PORT=${1#--port=}
         ;;
      --https-port=?*)
         SUPERSONIC_HTTPS_PORT=${1#--https-port=}
         ;;
      --context-path=?*)
         SUPERSONIC_CONTEXT_PATH=${1#--context-path=}
         ;;
      --max-memory=?*)
         SUPERSONIC_MAX_MEMORY=${1#--max-memory=}
         ;;
      --pidfile=?*)
         SUPERSONIC_PIDFILE=${1#--pidfile=}
         ;;
      --quiet)
         quiet=1
         ;;
      --default-music-folder=?*)
         SUPERSONIC_DEFAULT_MUSIC_FOLDER=${1#--default-music-folder=}
         ;;
      --default-podcast-folder=?*)
         SUPERSONIC_DEFAULT_PODCAST_FOLDER=${1#--default-podcast-folder=}
         ;;
      --default-playlist-folder=?*)
         SUPERSONIC_DEFAULT_PLAYLIST_FOLDER=${1#--default-playlist-folder=}
         ;;
      --soundcard-device=?*)
         SUPERSONIC_SOUNDCARD_DEVICE=${1#--soundcard-device=}
         # parse remaining arguments for possible device name terms
         for TERM in "$@"
         do
             if [[ $TERM != --* && $TERM != "start" && $TERM != "stop" && $TERM != "restart" && $TERM != "status" ]]
             then
                 SUPERSONIC_SOUNDCARD_DEVICE="$SUPERSONIC_SOUNDCARD_DEVICE $TERM"
             fi
         done
         ;;
      start)
         start
         ;;
      stop)
         stop
         ;;
      restart)
         restart
         ;;
      status)
         status
         ;;
   esac
   shift
done
