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
import net.sourceforge.subsonic.util.Util;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
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
            "parent_path, play_count, last_played, comment, created, last_modified, children_last_updated, present, version";

    private static final int VERSION = 1;

    private final RowMapper rowMapper = new MediaFileMapper();
    private final RowMapper archiveRowMapper = new ArchivedMediaFileMapper();

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
     * Returns a subset of all media files.
     */
    public List<MediaFile> getMediaFiles(int offset, int size) {
        return query("select " + COLUMNS + " from media_file order by id limit ? offset ?", rowMapper, size, offset);
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
                "children_last_updated=?," +
                "present=?, " +
                "version=? " +
                "where path=?";

        int n = update(sql,
                file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                file.getLastModified(), file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getPath());

        if (n == 0) {

            // Copy values from archive.
            MediaFile archived = getArchivedMediaFile(file.getPath());
            if (archived != null) {
                file.setComment(archived.getComment());
                file.setLastPlayed(archived.getLastPlayed());
                file.setPlayCount(archived.getPlayCount());
                file.setCreated(archived.getCreated());
            }

            update("insert into media_file (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")", null,
                    file.getPath(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                    file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                    file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                    file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                    file.getCreated(), file.getLastModified(),
                    file.getChildrenLastUpdated(), file.isPresent(), VERSION);

            update("delete from media_file_archive where path=?", file.getPath());
        }

        int id = queryForInt("select id from media_file where path=?", null, file.getPath());
        file.setId(id);
    }

    private MediaFile getArchivedMediaFile(String path) {
        return queryOne("select path, play_count, last_played, comment, created from media_file_archive where path=?", archiveRowMapper, path);
    }

    public List<String> getArtists() {
        return queryForStrings("select distinct artist from media_file where artist is not null order by artist");
    }

    public void deleteMediaFile(String path) {
        // Update archive.
        update("delete from media_file_archive where path=?", path);
        update("insert into media_file_archive(path, play_count, last_played, comment, created) " +
                "select path, play_count, last_played, comment, created from media_file where path=?", path);

        update("delete from media_file where path=?", path);
    }

    public List<String> getGenres() {
        return queryForStrings("select distinct genre from media_file where genre is not null order by genre");
    }

    public MediaFile getRandomAlbum() {
        int min = queryForInt("select min(id) from media_file", 0);
        int max = queryForInt("select max(id) from media_file", 0);
        return queryOne("select " + COLUMNS + " from media_file where type=? and id > ? limit 1", rowMapper, ALBUM.name(), Util.randomInt(min, max));
    }

    public MediaFile getRandomSong(Integer fromYear, Integer toYear, String genre, String musicFolderPath) {
        Integer min;
        Integer max;
        if (musicFolderPath != null) {
            min = queryForInt("select min(id) from media_file where folder=?", 0, musicFolderPath);
            max = queryForInt("select max(id) from media_file where folder=?", 0, musicFolderPath);
        } else {
            min = queryForInt("select min(id) from media_file", 0);
            max = queryForInt("select max(id) from media_file", 0);
        }

        StringBuilder whereClause = new StringBuilder("type in ('AUDIO', 'VIDEO') and id > ").append(Util.randomInt(min, max));

        if (fromYear != null) {
            whereClause.append(" and year >= ").append(fromYear);
        }
        if (toYear != null) {
            whereClause.append(" and year <= ").append(toYear);
        }
        if (genre != null) {
            whereClause.append(" and genre = '").append(genre).append("'");
        }
        if (musicFolderPath != null) {
            whereClause.append(" and folder = '").append(musicFolderPath).append("'");
        }

        return queryOne("select " + COLUMNS + " from media_file where " + whereClause + " limit 1", rowMapper);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and play_count > 0 " +
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
        return query("select " + COLUMNS + " from media_file where type=? and last_played is not null " +
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
        return query("select " + COLUMNS + " from media_file where type=? order by created desc limit ? offset ?", rowMapper,
                ALBUM.name(), count, offset);
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     */
    public MediaLibraryStatistics getStatistics() {
        int artistCount = queryForInt("select count(distinct artist) from media_file", 0);
        int albumCount = queryForInt("select count(distinct album) from media_file", 0);
        int songCount = queryForInt("select count(id) from media_file where type in (?, ?)", 0, VIDEO.name(), AUDIO.name());
        long totalLengthInBytes = queryForLong("select sum(file_size) from media_file", 0L);
        long totalDurationInSeconds = queryForLong("select sum(duration_seconds) from media_file", 0L);

        return new MediaLibraryStatistics(artistCount, albumCount, songCount, totalLengthInBytes, totalDurationInSeconds);
    }

    public void setAllMediaFilesNotPresent() {
        update("update media_file set present=false");
    }

    public void setMediaFilePresent(String path) {
        update("update media_file set present=? where path=?", true, path);
    }

    public void archiveNotPresent() {
        update("delete from media_file_archive where exists (select 1 from media_file where media_file.path=media_file_archive.path and not media_file.present)");

        update("insert into media_file_archive(path, play_count, last_played, comment, created) " +
                "select path, play_count, last_played, comment, created from media_file where not present");

        update("delete from media_file where not present");
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
                    rs.getBoolean(27));
        }
    }

    private static class ArchivedMediaFileMapper implements ParameterizedRowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setPath(rs.getString(1));
            file.setPlayCount(rs.getInt(2));
            file.setLastPlayed(rs.getTimestamp(3));
            file.setComment(rs.getString(4));
            file.setCreated(rs.getTimestamp(5));

            return file;
        }
    }

}
