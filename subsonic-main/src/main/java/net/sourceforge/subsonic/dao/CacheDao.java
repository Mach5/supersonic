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

import net.sourceforge.subsonic.domain.CacheElement;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Provides database services for caching.
 *
 * @author Sindre Mehus
 */
public class CacheDao extends AbstractDao {

    private static final String COLUMNS = "type, key, value, created";

    private CacheRowMapper rowMapper = new CacheRowMapper();

    /**
     * Creates a new cache element.
     *
     * @param element The cache element to create (or update).
     */
    public void createCacheElement(CacheElement element) {
        deleteCacheElement(element);
        String sql = "insert into cache (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")";
        update(sql, element.getType(), element.getKey(), element.getValue(), element.getCreated());
    }

    public CacheElement getCacheElement(int type, String key) {
        String sql = "select " + COLUMNS + " from cache where type=? and key=?";
        return queryOne(sql, rowMapper, type, key);
    }

    /**
     * Deletes the cache element with the given type and key.
     */
    public void deleteCacheElement(CacheElement element) {
        update("delete from cache where type=? and key=?", element.getType(), element.getKey());
    }

    private static class CacheRowMapper implements ParameterizedRowMapper<CacheElement> {
        public CacheElement mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CacheElement(rs.getInt(1), rs.getString(2), rs.getObject(3), rs.getLong(4));
        }
    }
}
