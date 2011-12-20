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

    private final String name;
    private final int type;
    private final CacheDao cacheDao;

    private long hits;
    private long misses;
    private long totalLookupTimeMicros;

    public Cache(String name, int type, CacheDao cacheDao) {
        this.name = name;
        this.type = type;
        this.cacheDao = cacheDao;
    }

    public CacheElement get(String key) {

        long t0 = System.nanoTime();
        CacheElement element = cacheDao.getCacheElement(type, key);
        long t1 = System.nanoTime();
        totalLookupTimeMicros += (t1 - t0) / 1000L;

//        LOG.debug((element == null ? "M " : "  ") + name + ", " + key + ": " + (t1 - t0) / 1000L);

        if (element == null) {
            misses++;
        } else {
            hits++;
        }
        return element;
    }

    public <T> T getValue(String key) {
        CacheElement element = get(key);
        return element == null ? null : (T) element.getValue();
    }

    public void put(String key, Object value) {
        CacheElement element = new CacheElement(type, key, value, System.currentTimeMillis());
        cacheDao.createCacheElement(element);
        System.out.println(("P ") + name + ", " + key);
    }

    public String getStatistics() {
        long accesses = hits + misses;
        double hitRate = accesses == 0 ? 0.0 : (double) hits / (double) accesses;
        long hitRatePercentage = Math.round(hitRate * 100.0);
        long avgLookupTime = accesses == 0 ? 0 : totalLookupTimeMicros / accesses;
        return "Cache '" + name + "': " + hits + " of " + accesses + " hits (" + hitRatePercentage + "%). " +
                "Avg time: " + avgLookupTime + " microsec.";
    }
}
