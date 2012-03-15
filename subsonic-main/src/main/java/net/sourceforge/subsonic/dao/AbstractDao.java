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

import net.sourceforge.subsonic.Logger;

/**
 * Abstract superclass for all DAO's.
 *
 * @author Sindre Mehus
 */
public class AbstractDao {
    private static final Logger LOG = Logger.getLogger(AbstractDao.class);
    
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
        long t = System.nanoTime();
        int result = getJdbcTemplate().update(sql, args);
        log(sql, t);
        return result;
    }

    private void log(String sql, long startTimeNano) {
        long micros = (System.nanoTime() - startTimeNano) / 1000L;
        LOG.debug(micros + "  " + sql);
    }

    protected <T> List<T> query(String sql, RowMapper rowMapper, Object... args) {
        long t = System.nanoTime();
        List<T> result = getJdbcTemplate().query(sql, args, rowMapper);
        log(sql, t);
        return result;
    }

    protected List<String> queryForStrings(String sql, Object... args) {
        long t = System.nanoTime();
        List<String> result = getJdbcTemplate().queryForList(sql, args, String.class);
        log(sql, t);
        return result;
    }

    protected Integer queryForInt(String sql, Integer defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Integer> list = getJdbcTemplate().queryForList(sql, args, Integer.class);
        Integer result = list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
        log(sql, t);
        return result;
    }

    protected Long queryForLong(String sql, Long defaultValue, Object... args) {
        long t = System.nanoTime();
        List<Long> list = getJdbcTemplate().queryForList(sql, args, Long.class);
        Long result = list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
        log(sql, t);
        return result;
    }

    protected <T> T queryOne(String sql, RowMapper rowMapper, Object... args) {
        long t = System.nanoTime();
        List<T> list = query(sql, rowMapper, args);
        T result = list.isEmpty() ? null : list.get(0);
        log(sql, t);
        return result;
    }

    public void setDaoHelper(DaoHelper daoHelper) {
        this.daoHelper = daoHelper;
    }
}
