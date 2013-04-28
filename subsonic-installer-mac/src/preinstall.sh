#!/bin/bash

SUBSONIC_HOME="/Library/Application Support/Subsonic"

# Backup database.
#rm -rf "$SUBSONIC_HOME/jetty"

if [ -e "$SUBSONIC_HOME/db" ]; then
  rm -rf "$SUBSONIC_HOME/db.backup"
  cp -R "$SUBSONIC_HOME/db" "$SUBSONIC_HOME/db.backup"
fi

