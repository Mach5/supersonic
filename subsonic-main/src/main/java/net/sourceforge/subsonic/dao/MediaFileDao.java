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

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
public class MediaFileDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(MediaFileDao.class);
    private static final String COLUMNS = "id, path, media_type, format, is_directory, is_album, title, album, artist, disc_number, " +
            "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
            "parent_path, play_count, last_played, comment, created, last_modified, enabled";

//    private MediaFileMapper rowMapper = new MediaFileMapper();


    /**
     * Creates a new media file.
     *
     * @param file The media file to create.
     */
    public void createMediaFile(MediaFile file) {
        String sql = "insert into media_file (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")";
        update(sql, null,
                file.getPath(), file.getMediaType() == null ? null  : file.getMediaType().name(), file.getFormat(), file.isDirectory(), file.isAlbum(),
                file.getTitle(), file.getAlbum(), file.getArtist(), file.getDiscNumber(), file.getTrackNumber(),
                file.getYear(), file.getGenre(), file.getBitRate(), file.getVariableBitRate(), file.getDurationSeconds(),
                file.getFileSize(), file.getWidth(), file.getHeight(), file.getCoverArtPath(), file.getParentPath(),
                file.getPlayCount(), file.getLastPlayed(), file.getComment(), file.getCreated(), file.getLastModified(),
                file.getEnabled());

        LOG.debug("Created media_file for " + file.getPath());
    }

//    private static class MediaFileMapper implements ParameterizedRowMapper<MediaFileMapper> {
//        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
//            return new MediaFile(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5), rs.getBoolean(6));
//        }
//    }

}
