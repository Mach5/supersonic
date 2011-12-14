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

import java.util.Arrays;

/**
 * Unit test of {@link net.sourceforge.subsonic.dao.MusicFolderDao}.
 *
 * @author Sindre Mehus
 */
public class CacheDaoTestCase extends DaoTestCaseBase {

    @Override
    protected void setUp() throws Exception {
        getJdbcTemplate().execute("delete from cache");
    }

    public void test() {
        Object value = Arrays.asList(1, 2, 3);
        long created = System.currentTimeMillis();
        CacheElement element = new CacheElement(1, "foo", value, created);
        cacheDao.createCacheElement(element);

        element = cacheDao.getCacheElement(element.getType(), element.getKey());
        assertEquals(value, element.getValue());
        assertEquals(created, element.getCreated());

        value = Arrays.asList(4, 5, 6);
        created++;
        element.setValue(value);
        element.setCreated(created);
        cacheDao.createCacheElement(element);

        element = cacheDao.getCacheElement(element.getType(), element.getKey());
        assertEquals(value, element.getValue());
        assertEquals(created, element.getCreated());

        cacheDao.deleteCacheElement(element);
        assertNull(cacheDao.getCacheElement(element.getType(), element.getKey()));
    }
}