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
package net.sourceforge.subsonic.domain;

import net.sourceforge.subsonic.dao.CacheDao;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class Cache {

    private final int type;
    private final CacheDao cacheDao;

    public Cache(int type, CacheDao cacheDao) {
        this.type = type;
        this.cacheDao = cacheDao;
    }

    public CacheElement get(String key) {
        return cacheDao.getCacheElement(type, key);
    }

    public void put(String key, Object value) {
        CacheElement element = new CacheElement(type, key, value, System.currentTimeMillis());
        cacheDao.createCacheElement(element);
    }
}
