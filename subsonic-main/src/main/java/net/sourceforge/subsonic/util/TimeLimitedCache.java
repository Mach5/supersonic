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
package net.sourceforge.subsonic.util;

import net.sourceforge.subsonic.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class TimeLimitedCache<K, V> {

    private static final Logger LOG = Logger.getLogger(TimeLimitedCache.class);

    private final long ttlMillis;
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<K, Entry<V>>();

    public TimeLimitedCache(long ttl, TimeUnit timeUnit) {
        this.ttlMillis = TimeUnit.MILLISECONDS.convert(ttl, timeUnit);

        // Regularly remove expired cache elements.
        Runnable cleanupTask = new Runnable() {
            public void run() {
                cleanUp();
            }
        };
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(cleanupTask, ttl, ttl, timeUnit);
    }

    public V get(K key) {
        Entry<V> entry = map.get(key);
        return entry == null || entry.expires < System.currentTimeMillis() ? null : entry.value;
    }

    public void put(K key, V value) {
        map.put(key, new Entry<V>(value, System.currentTimeMillis() + ttlMillis));
    }

    private void cleanUp() {
        try {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<K,Entry<V>>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, Entry<V>> next =  iterator.next();
                if (next.getValue().expires < now) {
                    iterator.remove();
                }
            }
        } catch (Throwable x) {
            LOG.error("Programming error. Got exception while cleaning up TimeLimitedCache.", x);
        }
    }

    private static class Entry<V> {
        private final V value;
        private final long expires;

        public Entry(V value, long expires) {
            this.value = value;
            this.expires = expires;
        }
    }

}
