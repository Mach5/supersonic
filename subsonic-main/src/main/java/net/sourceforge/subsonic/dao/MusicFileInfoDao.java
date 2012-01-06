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

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFileInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides database services for music file info.
 *
 * @author Sindre Mehus
 */
public class MusicFileInfoDao extends AbstractDao {

    private static final String COLUMNS = "id, path, comment, play_count, last_played, enabled";
    private MusicFileInfoRowMapper rowMapper = new MusicFileInfoRowMapper();

    /**
     * Returns paths for the highest rated music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of files to return.
     * @return Paths for the highest rated music files.
     */
    public List<String> getHighestRated(int offset, int count) {
        if (count < 1) {
            return new ArrayList<String>();
        }
        String sql = "select path from user_rating " +
                "where exists (select 1 from music_file_info where user_rating.path = music_file_info.path and enabled=true) " +
                "group by path " +
                "order by avg(rating) desc " +
                " limit " + count + " offset " + offset;
        return queryForString(sql);
    }

    /**
     * Returns info for the most frequently played music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return Info for the most frequently played music files.
     */
    public List<MusicFileInfo> getMostFrequentlyPlayed(int offset, int count) {
        if (count < 1) {
            return Collections.emptyList();
        }

        String sql = "select " + COLUMNS + " from music_file_info " +
                "where play_count > 0 and enabled=true " +
                "order by play_count desc limit " + count + " offset " + offset;
        return query(sql, rowMapper);
    }

    /**
     * Returns info for the most recently played music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return Info for the most recently played music files.
     */
    public List<MusicFileInfo> getMostRecentlyPlayed(int offset, int count) {
        if (count < 1) {
            return Collections.emptyList();
        }

        String sql = "select " + COLUMNS + " from music_file_info " +
                "where last_played is not null and enabled=true " +
                "order by last_played desc limit " + count + " offset " + offset;
        return query(sql, rowMapper);
    }

    /**
     * Sets the rating for a media file and a given user.
     *
     * @param username  The user name.
     * @param mediaFile The media file.
     * @param rating    The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MediaFile mediaFile, Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            return;
        }

        update("delete from user_rating where username=? and path=?", username, mediaFile.getPath());
        if (rating != null) {
            update("insert into user_rating values(?, ?, ?)", username, mediaFile.getPath(), rating);
        }
    }

    /**
     * Returns the average rating for the given media file.
     *
     * @param mediaFile The media file.
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MediaFile mediaFile) {
        try {
            return (Double) getJdbcTemplate().queryForObject("select avg(rating) from user_rating where path=?", new Object[]{mediaFile.getPath()}, Double.class);
        } catch (EmptyResultDataAccessException x) {
            return null;
        }
    }

    /**
     * Returns the rating for the given user and media file.
     *
     * @param username  The user name.
     * @param mediaFile The media file.
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MediaFile mediaFile) {
        try {
            return getJdbcTemplate().queryForInt("select rating from user_rating where username=? and path=?", new Object[]{username, mediaFile.getPath()});
        } catch (EmptyResultDataAccessException x) {
            return null;
        }
    }


    private static class MusicFileInfoRowMapper implements ParameterizedRowMapper<MusicFileInfo> {
        public MusicFileInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MusicFileInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5), rs.getBoolean(6));
        }
    }

}
