# subsonic.nsi

!include "WordFunc.nsh"
!include "MUI.nsh"

!insertmacro VersionCompare

# The name of the installer
Name "Supersonic"

# The default installation directory
InstallDir $PROGRAMFILES\Supersonic

# Registry key to check for directory (so if you install again, it will
# overwrite the old one automatically)
InstallDirRegKey HKLM "Software\Supersonic" "Install_Dir"

#--------------------------------
#Interface Configuration

!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Header\orange.bmp"
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\Getting Started.html"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "View Getting Started document"

#--------------------------------
# Pages

# This page checks for JRE
Page custom CheckInstalledJRE

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Languages
!insertmacro MUI_LANGUAGE "English"

Section "Subsonic"

  SectionIn RO

  # Install for all users
  SetShellVarContext "all"

  # Take backup of existing subsonic-service.exe.vmoptions
  CopyFiles /SILENT $INSTDIR\subsonic-service.exe.vmoptions $TEMP\subsonic-service.exe.vmoptions

  # Silently uninstall existing version.
  ExecWait '"$INSTDIR\uninstall.exe" /S _?=$INSTDIR'

  # Remove previous Jetty temp directory.
  RMDir /r "c:\supersonic\jetty"

  # Set output path to the installation directory.
  SetOutPath $INSTDIR

  # Write files.
  File ..\..\..\target\supersonic-agent.exe
  File ..\..\..\target\subsonic-agent.exe.vmoptions
  File ..\..\..\target\supersonic-agent-elevated.exe
  File ..\..\..\target\subsonic-agent-elevated.exe.vmoptions
  File ..\..\..\target\supersonic-service.exe
  File ..\..\..\target\subsonic-service.exe.vmoptions
  File ..\..\..\..\subsonic-booter\target\supersonic-booter-jar-with-dependencies.jar
  File ..\..\..\..\subsonic-main\README.TXT
  File ..\..\..\..\subsonic-main\LICENSE.TXT
  File "..\..\..\..\subsonic-main\Getting Started.html"
  File ..\..\..\..\subsonic-main\target\supersonic.war
  File ..\..\..\..\subsonic-main\target\classes\version.txt
  File ..\..\..\..\subsonic-main\target\classes\build_number.txt

  # Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\Supersonic "Install_Dir" "$INSTDIR"

  # Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Supersonic" "DisplayName" "Supersonic"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Supersonic" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Supersonic" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Supersonic" "NoRepair" 1
  WriteUninstaller "uninstall.exe"

  # Restore subsonic-service.exe.vmoptions
  CopyFiles /SILENT  $TEMP\subsonic-service.exe.vmoptions $INSTDIR\subsonic-service.exe.vmoptions
  Delete $TEMP\subsonic-service.exe.vmoptions

  # Write transcoding pack files.
  SetOutPath "c:\supersonic\transcode"
  File ..\..\..\..\subsonic-transcode\windows\*.*

  # Add Windows Firewall exception.
  # (Requires NSIS plugin found on http://nsis.sourceforge.net/NSIS_Simple_Firewall_Plugin to be installed
  # as NSIS_HOME/Plugins/SimpleFC.dll)
  SimpleFC::AddApplication "Supersonic Service" "$INSTDIR\supersonic-service.exe" 0 2 "" 1
  SimpleFC::AddApplication "Supersonic Agent" "$INSTDIR\supersonic-agent.exe" 0 2 "" 1
  SimpleFC::AddApplication "Supersonic Agent (Elevated)" "$INSTDIR\supersonic-agent-elevated.exe" 0 2 "" 1

  # Install and start service.
  ExecWait '"$INSTDIR\supersonic-service.exe" -install'
  ExecWait '"$INSTDIR\supersonic-service.exe" -start'

  # Start agent.
  Exec '"$INSTDIR\supersonic-agent-elevated.exe" -balloon'

SectionEnd


Section "Start Menu Shortcuts"

  CreateDirectory "$SMPROGRAMS\Supersonic"
  CreateShortCut "$SMPROGRAMS\Supersonic\Open Supersonic.lnk"          "$INSTDIR\supersonic.url"         ""         "$INSTDIR\supersonic-agent.exe"  0
  CreateShortCut "$SMPROGRAMS\Supersonic\Supersonic Tray Icon.lnk"     "$INSTDIR\supersonic-agent.exe"   "-balloon" "$INSTDIR\supersonic-agent.exe"  0
  CreateShortCut "$SMPROGRAMS\Supersonic\Start Supersonic Service.lnk" "$INSTDIR\supersonic-service.exe" "-start"   "$INSTDIR\supersonic-service.exe"  0
  CreateShortCut "$SMPROGRAMS\Supersonic\Stop Supersonic Service.lnk"  "$INSTDIR\supersonic-service.exe" "-stop"    "$INSTDIR\supersonic-service.exe"  0
  CreateShortCut "$SMPROGRAMS\Supersonic\Uninstall Supersonic.lnk"     "$INSTDIR\uninstall.exe"        ""         "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\Supersonic\Getting Started.lnk"        "$INSTDIR\Getting Started.html" ""         "$INSTDIR\Getting Started.html" 0

  CreateShortCut "$SMSTARTUP\Supersonic.lnk"                         "$INSTDIR\supersonic-agent.exe"   ""         "$INSTDIR\supersonic-agent.exe"  0

SectionEnd


# Uninstaller

Section "Uninstall"

  # Uninstall for all users
  SetShellVarContext "all"

  # Stop and uninstall service if present.
  ExecWait '"$INSTDIR\supersonic-service.exe" -stop'
  ExecWait '"$INSTDIR\supersonic-service.exe" -uninstall'

  # Stop agent by killing it.
  # (Requires NSIS plugin found on http://nsis.sourceforge.net/Processes_plug-in to be installed
  # as NSIS_HOME/Plugins/Processes.dll)
  Processes::KillProcess "supersonic-agent"
  Processes::KillProcess "supersonic-agent-elevated"
  Processes::KillProcess "ffmpeg"
  Processes::KillProcess "lame"

  # Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Supersonic"
  DeleteRegKey HKLM SOFTWARE\Supersonic

  # Remove files.
  Delete "$SMSTARTUP\Supersonic.lnk"
  RMDir /r "$SMPROGRAMS\Supersonic"
  Delete "$INSTDIR\build_number.txt"
  Delete "$INSTDIR\elevate.exe"
  Delete "$INSTDIR\Getting Started.html"
  Delete "$INSTDIR\LICENSE.TXT"
  Delete "$INSTDIR\README.TXT"
  Delete "$INSTDIR\supersonic.url"
  Delete "$INSTDIR\supersonic.war"
  Delete "$INSTDIR\supersonic-agent.exe"
  Delete "$INSTDIR\subsonic-agent.exe.vmoptions"
  Delete "$INSTDIR\supersonic-agent-elevated.exe"
  Delete "$INSTDIR\subsonic-agent-elevated.exe.vmoptions"
  Delete "$INSTDIR\supersonic-booter-jar-with-dependencies.jar"
  Delete "$INSTDIR\supersonic-service.exe"
  Delete "$INSTDIR\subsonic-service.exe.vmoptions"
  Delete "$INSTDIR\uninstall.exe"
  Delete "$INSTDIR\version.txt"
  RMDir /r "$INSTDIR\log"
  RMDir "$INSTDIR"

  # Remove Windows Firewall exception.
  # (Requires NSIS plugin found on http://nsis.sourceforge.net/NSIS_Simple_Firewall_Plugin to be installed
  # as NSIS_HOME/Plugins/SimpleFC.dll)
  SimpleFC::RemoveApplication "$INSTDIR\elevate.exe"
  SimpleFC::RemoveApplication "$INSTDIR\supersonic-service.exe"
  SimpleFC::RemoveApplication "$INSTDIR\supersonic-agent.exe"
  SimpleFC::RemoveApplication "$INSTDIR\supersonic-agent-elevated.exe"

SectionEnd


Function CheckInstalledJRE
    # Read the value from the registry into the $0 register
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" CurrentVersion

    # Check JRE version. At least 1.6 is required.
    #   $1=0  Versions are equal
    #   $1=1  Installed version is newer
    #   $1=2  Installed version is older (or non-existent)
    ${VersionCompare} $0 "1.6" $1
    IntCmp $1 2 InstallJRE 0 0
    Return

    InstallJRE:
      # Launch Java web installer.
      MessageBox MB_OK "Java 6 was not found and will now be installed."
      File /oname=$TEMP\jre-setup.exe jre-6u27-windows-i586-iftw.exe
      ExecWait '"$TEMP\jre-setup.exe"' $0
      Delete "$TEMP\jre-setup.exe"

FunctionEnd
