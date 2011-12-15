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

/**
 * @author Sindre Mehus
 * @version $Id$
 */
@Deprecated
public class TypesafeCache<T> {

    private final Cache cache;
    private final Class<T> clazz;

    public TypesafeCache(Cache cache, Class<T> clazz) {
        this.cache = cache;
        this.clazz = clazz;
    }

    public T getValue(String key) {
        CacheElement element = cache.get(key);
        if (element == null) {
            return null;
        }
        return isCorrectType(element) ? (T) element.getValue() : null;
    }

    private boolean isCorrectType(CacheElement element) {
        return clazz.isAssignableFrom(element.getValue().getClass());
    }

    public CacheElement get(String key) {
        CacheElement element = cache.get(key);
        return isCorrectType(element) ? element : null;
    }

    public void put(String key, T value) {
        // TODO: Check type
        cache.put(key, value);
    }

    @Override
    public String toString() {
        return cache.toString();
    }
}
