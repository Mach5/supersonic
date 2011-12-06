/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.command;

import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.controller.*;

import java.util.*;

/**
 * Command used in {@link SearchController}.
 *
 * @author Sindre Mehus
 */
public class SearchCommand {

    private String query;
    private List<MusicFile> artists;
    private List<MusicFile> albums;
    private List<MusicFile> songs;
    private boolean isIndexBeingCreated;
    private User user;
    private boolean partyModeEnabled;
    private Player player;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isIndexBeingCreated() {
        return isIndexBeingCreated;
    }

    public void setIndexBeingCreated(boolean indexBeingCreated) {
        isIndexBeingCreated = indexBeingCreated;
    }

    public List<MusicFile> getArtists() {
        return artists;
    }

    public void setArtists(List<MusicFile> artists) {
        this.artists = artists;
    }

    public List<MusicFile> getAlbums() {
        return albums;
    }

    public void setAlbums(List<MusicFile> albums) {
        this.albums = albums;
    }

    public List<MusicFile> getSongs() {
        return songs;
    }

    public void setSongs(List<MusicFile> songs) {
        this.songs = songs;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isPartyModeEnabled() {
        return partyModeEnabled;
    }

    public void setPartyModeEnabled(boolean partyModeEnabled) {
        this.partyModeEnabled = partyModeEnabled;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public static class Match {
        private MusicFile musicFile;
        private String title;
        private String album;
        private String artist;

        public Match(MusicFile musicFile, String title, String album, String artist) {
            this.musicFile = musicFile;
            this.title = title;
            this.album = album;
            this.artist = artist;
        }

        public MusicFile getMusicFile() {
            return musicFile;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public String getArtist() {
            return artist;
        }
    }
}
