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

import java.util.List;

import org.springframework.jdbc.core.*;

/**
 * Abstract superclass for all DAO's.
 *
 * @author Sindre Mehus
 */
public class AbstractDao {
    private DaoHelper daoHelper;

    /**
     * Returns a JDBC template for performing database operations.
     * @return A JDBC template.
     */
    public JdbcTemplate getJdbcTemplate() {
        return daoHelper.getJdbcTemplate();
    }

    protected String questionMarks(String columns) {
        int count = columns.split(", ").length;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append('?');
            if (i < count - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    protected int update(String sql, Object... args) {
        return getJdbcTemplate().update(sql, args);
    }

    protected <T> List<T> query(String sql, RowMapper rowMapper, Object... args) {
        return getJdbcTemplate().query(sql, args, rowMapper);
    }

    protected List<String> queryForString(String sql) {
        return getJdbcTemplate().queryForList(sql, String.class);
    }

    protected Integer queryForInt(String sql) {
        List<Integer> result = getJdbcTemplate().queryForList(sql, Integer.class);
        return result.isEmpty() ? null : result.get(0);
    }

    protected <T> T queryOne(String sql, RowMapper rowMapper, Object... args) {
        List<T> result = query(sql, rowMapper, args);
        return result.isEmpty() ? null : result.get(0);
    }

    public void setDaoHelper(DaoHelper daoHelper) {
        this.daoHelper = daoHelper;
    }
}
