@echo off

REM  The directory where Supersonic will create files. Make sure it is writable.
set SUPERSONIC_HOME=c:\supersonic

REM  The host name or IP address on which to bind Supersonic. Only relevant if you have
REM  multiple network interfaces and want to make Supersonic available on only one of them.
REM  The default value 0.0.0.0 will bind Supersonic to all available network interfaces.
set SUPERSONIC_HOST=0.0.0.0

REM  The port on which Supersonic will listen for incoming HTTP traffic.
set SUPERSONIC_PORT=4040

REM  The port on which Supersonic will listen for incoming HTTPS traffic (0 to disable).
set SUPERSONIC_HTTPS_PORT=0

REM  The context path (i.e., the last part of the Supersonic URL).  Typically "/" or "/supersonic".
set SUPERSONIC_CONTEXT_PATH=/

REM  The memory limit (max Java heap size) in megabytes.
set MAX_MEMORY=100

java -Xmx%MAX_MEMORY%m  -Dsubsonic.home=%SUPERSONIC_HOME% -Dsubsonic.host=%SUPERSONIC_HOST% -Dsubsonic.port=%SUPERSONIC_PORT%  -Dsubsonic.httpsPort=%SUPERSONIC_HTTPS_PORT% -Dsubsonic.contextPath=%SUPERSONIC_CONTEXT_PATH% -jar subsonic-booter-jar-with-dependencies.jar

