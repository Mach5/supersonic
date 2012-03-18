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
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static net.sourceforge.subsonic.domain.MediaFile.MediaType;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.*;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
public class MediaFileDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(MediaFileDao.class);
    private static final String COLUMNS = "id, path, folder, type, format, title, album, artist, disc_number, " +
            "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
            "parent_path, play_count, last_played, comment, created, last_modified, last_scanned, children_last_updated, present, version";

    private static final int VERSION = 1;

    private final RowMapper rowMapper = new MediaFileMapper();
    private final RowMapper musicFileInfoRowMapper = new MusicFileInfoMapper();

    /**
     * Returns the media file for the given path.
     *
     * @param path The path.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(String path) {
        return queryOne("select " + COLUMNS + " from media_file where path=? and present", rowMapper, path);
    }

    /**
     * Returns the media file for the given ID.
     *
     * @param id The ID.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(int id) {
        return queryOne("select " + COLUMNS + " from media_file where id=? and present", rowMapper, id);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(String path) {
        return query("select " + COLUMNS + " from media_file where parent_path=? and present", rowMapper, path);
    }

    /**
     * Creates or updates a media file.
     *
     * @param file The media file to create/update.
     */
    public synchronized void createOrUpdateMediaFile(MediaFile file) {
        String sql = "update media_file set " +
                "folder=?," +
                "type=?," +
                "format=?," +
                "title=?," +
                "album=?," +
                "artist=?," +
                "disc_number=?," +
                "track_number=?," +
                "year=?," +
                "genre=?," +
                "bit_rate=?," +
                "variable_bit_rate=?," +
                "duration_seconds=?," +
                "file_size=?," +
                "width=?," +
                "height=?," +
                "cover_art_path=?," +
                "parent_path=?," +
                "play_count=?," +
                "last_played=?," +
                "comment=?," +
                "last_modified=?," +
                "last_scanned=?," +
                "children_last_updated=?," +
                "present=?, " +
                "version=? " +
                "where path=?";

        int n = update(sql,
                file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                file.getLastModified(), file.getLastScanned(), file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getPath());

        if (n == 0) {

            // Copy values from obsolete table music_file_info.
            MediaFile musicFileInfo = getMusicFileInfo(file.getPath());
            if (musicFileInfo != null) {
                file.setComment(musicFileInfo.getComment());
                file.setLastPlayed(musicFileInfo.getLastPlayed());
                file.setPlayCount(musicFileInfo.getPlayCount());
            }

            update("insert into media_file (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")", null,
                    file.getPath(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                    file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                    file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                    file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                    file.getCreated(), file.getLastModified(), file.getLastScanned(),
                    file.getChildrenLastUpdated(), file.isPresent(), VERSION);
        }

        int id = queryForInt("select id from media_file where path=?", null, file.getPath());
        file.setId(id);
    }

    private MediaFile getMusicFileInfo(String path) {
        return queryOne("select play_count, last_played, comment from music_file_info where path=?", musicFileInfoRowMapper, path);
    }

    public List<String> getArtists() {
        return queryForStrings("select distinct artist from media_file where artist is not null and present order by artist");
    }

    public void deleteMediaFile(String path) {
        update("update media_file set present=false where path=?", path);
    }

    public List<String> getGenres() {
        return queryForStrings("select distinct genre from media_file where genre is not null and present order by genre");
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and play_count > 0 and present " +
                "order by play_count desc limit ? offset ?", rowMapper, ALBUM.name(), count, offset);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and last_played is not null and present " +
                "order by last_played desc limit ? offset ?", rowMapper, ALBUM.name(), count, offset);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and present order by created desc limit ? offset ?", rowMapper,
                ALBUM.name(), count, offset);
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     */
    public MediaLibraryStatistics getStatistics() {
        int artistCount = queryForInt("select count(distinct artist) from media_file where present", 0);
        int albumCount = queryForInt("select count(distinct album) from media_file where present", 0);
        int songCount = queryForInt("select count(id) from media_file where type in (?, ?) and present", 0, VIDEO.name(), AUDIO.name());
        long totalLengthInBytes = queryForLong("select sum(file_size) from media_file where present", 0L);
        long totalDurationInSeconds = queryForLong("select sum(duration_seconds) from media_file where present", 0L);

        return new MediaLibraryStatistics(artistCount, albumCount, songCount, totalLengthInBytes, totalDurationInSeconds);
    }

    public void markPresent(String path, Date lastScanned) {
        update("update media_file set present=?, last_scanned=? where path=?", true, lastScanned, path);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select id from media_file where true limit 1", 0);
        int maxId = queryForInt("select max(id) from media_file", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update media_file set present=false where id between ? and ? and last_scanned != ?", id, id + batchSize, lastScanned);
        }
    }

    private static class MediaFileMapper implements ParameterizedRowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    MediaType.valueOf(rs.getString(4)),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getInt(9) == 0 ? null : rs.getInt(9),
                    rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11),
                    rs.getString(12),
                    rs.getInt(13) == 0 ? null : rs.getInt(13),
                    rs.getBoolean(14),
                    rs.getInt(15) == 0 ? null : rs.getInt(15),
                    rs.getLong(16) == 0 ? null : rs.getLong(16),
                    rs.getInt(17) == 0 ? null : rs.getInt(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18),
                    rs.getString(19),
                    rs.getString(20),
                    rs.getInt(21),
                    rs.getTimestamp(22),
                    rs.getString(23),
                    rs.getTimestamp(24),
                    rs.getTimestamp(25),
                    rs.getTimestamp(26),
                    rs.getTimestamp(27),
                    rs.getBoolean(28));
        }
    }

    private static class MusicFileInfoMapper implements ParameterizedRowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setPlayCount(rs.getInt(1));
            file.setLastPlayed(rs.getTimestamp(2));
            file.setComment(rs.getString(3));
            return file;
        }
    }

}
