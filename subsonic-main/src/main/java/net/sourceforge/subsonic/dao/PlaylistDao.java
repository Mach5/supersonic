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
package net.sourceforge.subsonic.dao;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.Playlist;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides database services for playlists.
 *
 * @author Sindre Mehus
 */
public class PlaylistDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(PlaylistDao.class);
    private static final String COLUMNS = "id, username, is_public, name, comment, song_count, duration_seconds, created, last_scanned, present";
    private final RowMapper rowMapper = new PlaylistMapper();

    public List<Playlist> getPlaylistsForUser(String username) {
        List<Playlist> result1 = query("select " + COLUMNS + " from playlist where username=? order by name", rowMapper, username);
        List<Playlist> result2 = query("select " + prefix(COLUMNS, "playlist") + " from playlist, playlist_user where " +
                "playlist.id = playlist_user.playlist_id and " +
                "playlist.username != ? and " +
                "playlist_user.username = ? order by playlist.name", rowMapper, username, username);
        result1.addAll(result2);
        return result1;
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return queryForStrings("select username from playlist_user where playlist_id=?", playlistId);
    }

    public void addPlaylistUser(int playlistId, String username) {
        if (!getPlaylistUsers(playlistId).contains(username)) {
            update("insert into playlist_user(playlist_id,username) values (?,?)", playlistId, username);
        }
    }

    public void removePlaylistUser(int playlistId, String username) {
        update("delete from playlist_user where playlist_id=? and username=?", playlistId, username);
    }

    private static class PlaylistMapper implements ParameterizedRowMapper<Playlist> {
        public Playlist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Playlist(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getBoolean(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getTimestamp(8));
        }
    }
}
