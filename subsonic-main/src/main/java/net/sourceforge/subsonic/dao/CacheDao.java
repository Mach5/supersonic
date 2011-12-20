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

import com.db4o.Db4oEmbedded;
import com.db4o.EmbeddedObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.query.Predicate;
import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.CacheElement;
import net.sourceforge.subsonic.service.SettingsService;

import java.io.File;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides database services for caching.
 *
 * @author Sindre Mehus
 */
public class CacheDao {

    private static final Logger LOG = Logger.getLogger(CacheDao.class);
    private static final int BATCH_SIZE = 100;

    private final EmbeddedObjectContainer db;
    private final ReadWriteLock dbLock = new ReentrantReadWriteLock();
    private int writeCount = 0;

    public CacheDao() {

        File subsonicHome = SettingsService.getSubsonicHome();
        File dbDir = new File(subsonicHome, "cache");
        File dbFile = new File(dbDir, "cache.dat");

        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

//        if (dbFile.exists()) {
//            try {
//                Defragment.defrag(dbFile.getPath());
//            }catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
        config.common().objectClass(CacheElement.class).objectField("id").indexed(true);
        config.common().objectClass(CacheElement.class).cascadeOnUpdate(true);
        config.common().objectClass(CacheElement.class).cascadeOnDelete(true);
        config.common().objectClass(CacheElement.class).cascadeOnActivate(true);

//        config.common().messageLevel(3);
        db = Db4oEmbedded.openFile(config, dbFile.getPath());

//        ((ObjectContainerBase) db).getNativeQueryHandler().addListener(new Db4oQueryExecutionListener() {
//            public void notifyQueryExecuted(NQOptimizationInfo info) {
//                System.out.println(info);
//            }
//        });
    }

    /**
     * Creates a new cache element.
     *
     * @param element The cache element to create (or update).
     */
    public void createCacheElement(CacheElement element) {
        dbLock.writeLock().lock();
        try{
            deleteCacheElement(element);
            db.store(element);

            if (writeCount++ == BATCH_SIZE) {
                db.commit();
                writeCount = 0;
            }

        } finally{
            dbLock.writeLock().unlock();
        }
    }

    public CacheElement getCacheElement(int type, String key) {
        dbLock.readLock().lock();
        try{

            ObjectSet<CacheElement> result = db.query(new CacheElementPredicate(type, key));
            if (result.size() > 1) {
                LOG.error("Programming error. Got " + result.size() + " cache elements of type " + type + " and key " + key);
            }
            return result.isEmpty() ? null : result.get(0);

        } finally{
            dbLock.readLock().unlock();
        }
    }

    /**
     * Deletes the cache element with the given type and key.
     */
    private void deleteCacheElement(CacheElement element) {
        // Retrieve it from the database first.
        element = getCacheElement(element.getType(), element.getKey());
        if (element != null) {
            db.delete(element);
        }
    }

    private static class CacheElementPredicate extends Predicate<CacheElement> {
        private static final long serialVersionUID = 54911003002373726L;

        private final long id;

        public CacheElementPredicate(int type, String key) {
            id = CacheElement.createId(type, key);
        }

        @Override
        public boolean match(CacheElement candidate) {
            return candidate.getId() == id;
        }
    }
}
