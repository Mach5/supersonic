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
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MediaType;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
public class MediaFileDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(MediaFileDao.class);
    private static final String COLUMNS = "id, path, media_type, format, is_directory, is_album, title, album, artist, disc_number, " +
            "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
            "parent_path, play_count, last_played, comment, created, last_modified, children_last_updated, enabled";

    private final MediaFileMapper rowMapper = new MediaFileMapper();

    /**
     * Returns the media file for the given path.
     *
     * @param path The path.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(String path) {
        return queryOne("select " + COLUMNS + " from media_file where path=?", rowMapper, path);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(String path) {
        return query("select " + COLUMNS + " from media_file where parent_path=?", rowMapper, path);
    }

    /**
     * Creates a new media file.
     *
     * @param file The media file to create.
     */
    public void createOrUpdateMediaFile(MediaFile file) {
        update("delete from media_file where path=?", file.getPath());

        String sql = "insert into media_file (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")";
        update(sql, null,
                file.getPath(), file.getMediaType() == null ? null : file.getMediaType().name(), file.getFormat(), file.isDirectory(), file.isAlbum(),
                file.getTitle(), file.getAlbumName(), file.getArtist(), file.getDiscNumber(), file.getTrackNumber(),
                file.getYear(), file.getGenre(), file.getBitRate(), file.isVariableBitRate(), file.getDurationSeconds(),
                file.getFileSize(), file.getWidth(), file.getHeight(), file.getCoverArtPath(), file.getParentPath(),
                file.getPlayCount(), file.getLastPlayed(), file.getComment(), file.getCreated(), file.getLastModified(),
                file.getChildrenLastUpdated(), file.isEnabled());
        LOG.debug("Created/updated media_file for " + file.getPath());
    }

    public void deleteMediaFile(String path) {
        update("delete from media_file where path=?", path);
    }

    public List<String> getGenres() {
        return queryForString("select distinct genre from media_file where genre is not null order by genre");
    }

    private static class MediaFileMapper implements ParameterizedRowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3) == null ? null : MediaType.valueOf(rs.getString(3)),
                    rs.getString(4),
                    rs.getBoolean(5),
                    rs.getBoolean(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11),
                    rs.getInt(12) == 0 ? null : rs.getInt(12),
                    rs.getString(13),
                    rs.getInt(14) == 0 ? null : rs.getInt(14),
                    rs.getBoolean(15),
                    rs.getInt(16) == 0 ? null : rs.getInt(16),
                    rs.getLong(17) == 0 ? null : rs.getLong(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18),
                    rs.getInt(19) == 0 ? null : rs.getInt(19),
                    rs.getString(20),
                    rs.getString(21),
                    rs.getInt(22),
                    rs.getTimestamp(23),
                    rs.getString(24),
                    rs.getTimestamp(25),
                    rs.getTimestamp(26),
                    rs.getTimestamp(27),
                    rs.getBoolean(28));
        }
    }
}
