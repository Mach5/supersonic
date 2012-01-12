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
package net.sourceforge.subsonic.service;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.SearchCriteria;
import net.sourceforge.subsonic.domain.SearchResult;
import net.sourceforge.subsonic.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Provides services for searching for music.
 *
 * @author Sindre Mehus
 */
public class SearchService {

    private static final int INDEX_VERSION = 14;
    private static final Logger LOG = Logger.getLogger(SearchService.class);

    private MediaLibraryStatistics statistics;

    private boolean creatingIndex;
    private Timer timer;
    private SettingsService settingsService;
    private LuceneSearchService luceneSearchService;
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private int scanCount;


    public void init() {
        deleteOldIndexFiles();
        statistics = mediaFileDao.getStatistics();
        schedule();
    }

    /**
     * Schedule background execution of index creation.
     */
    public synchronized void schedule() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                createIndex();
            }
        };

        long daysBetween = settingsService.getIndexCreationInterval();
        int hour = settingsService.getIndexCreationHour();

        if (daysBetween == -1) {
            LOG.info("Automatic index creation disabled.");
            return;
        }

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTime().before(now)) {
            cal.add(Calendar.DATE, 1);
        }

        Date firstTime = cal.getTime();
        long period = daysBetween * 24L * 3600L * 1000L;
        timer.schedule(task, firstTime, period);

        LOG.info("Automatic index creation scheduled to run every " + daysBetween + " day(s), starting at " + firstTime);

        // In addition, create index immediately if it doesn't exist on disk.
        if (!isIndexCreated()) {
            LOG.info("Search index not found on disk. Creating it.");
            createIndex();
        }
    }

    /**
     * Returns whether the search index exists.
     *
     * @return Whether the search index exists.
     */
    @Deprecated
    private synchronized boolean isIndexCreated() {
        return getIndexFile().exists();
    }

    /**
     * Returns whether the search index is currently being created.
     *
     * @return Whether the search index is currently being created.
     */
    public synchronized boolean isIndexBeingCreated() {
        return creatingIndex;
    }

    /**
     * Returns the number of files scanned so far.
     */
    public int getScanCount() {
        return scanCount;
    }

    /**
     * Generates the search index.  If the index already exists it will be
     * overwritten.  The index is created asynchronously, i.e., this method returns
     * before the index is created.
     */
    public synchronized void createIndex() {
        if (isIndexBeingCreated()) {
            return;
        }
        creatingIndex = true;

        Thread thread = new Thread("Search Index Generator") {
            @Override
            public void run() {
                doCreateIndex();
            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void doCreateIndex() {
        LOG.info("Starting to create search index.");

        try {

            // Read entire music directory.
            mediaFileDao.setMediaStateUnknown();
            scanCount = 0;
            for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
                MediaFile root = mediaFileService.getMediaFile(musicFolder.getPath());
                scan(root);
            }
            mediaFileDao.setMediaStateNonExisting();

            // Update Lucene search index.
            LOG.info("Updating Lucene search index.");
            luceneSearchService.updateIndexes();

            // Update statistics
            statistics = mediaFileDao.getStatistics();

            LOG.info("Created search index with " + scanCount + " entries.");

        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        } finally {
            creatingIndex = false;
        }
    }

    private void scan(MediaFile file) {
        scanCount++;
        if (scanCount % 250 == 0) {
            LOG.info("Created search index with " + scanCount + " entries.");
        }

        mediaFileDao.setMediaStateExisting(file.getPath());

        for (MediaFile child : mediaFileService.getChildrenOf(file, true, false, false)) {
            scan(child);
        }
        for (MediaFile child : mediaFileService.getChildrenOf(file, false, true, false)) {
            scan(child);
        }
    }

    /**
     * Search for music files fulfilling the given search criteria.
     *
     * @param criteria  The search criteria.
     * @param indexType The search index to use.
     * @return The search result.
     * @throws IOException If an I/O error occurs.
     */
    public synchronized SearchResult search(SearchCriteria criteria, LuceneSearchService.IndexType indexType) throws IOException {

        if (!isIndexCreated() || isIndexBeingCreated()) {
            SearchResult empty = new SearchResult();
            empty.setOffset(criteria.getOffset());
            empty.setMediaFiles(Collections.<MediaFile>emptyList());
            return empty;
        }

        return luceneSearchService.search(criteria, indexType);
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     */
    public MediaLibraryStatistics getStatistics() {
        return statistics;
    }

    /**
     * Returns the file containing the index.
     *
     * @return The file containing the index.
     */
    private File getIndexFile() {
        return getIndexFile(INDEX_VERSION);
    }

    /**
     * Returns the index file for the given index version.
     *
     * @param version The index version.
     * @return The index file for the given index version.
     */
    private File getIndexFile(int version) {
        File home = SettingsService.getSubsonicHome();
        return new File(home, "subsonic" + version + ".index");
    }

    /**
     * Deletes old versions of the index file.
     */
    private void deleteOldIndexFiles() {
        for (int i = 2; i < INDEX_VERSION; i++) {
            File file = getIndexFile(i);
            try {
                if (FileUtil.exists(file)) {
                    if (file.delete()) {
                        LOG.info("Deleted old index file: " + file.getPath());
                    }
                }
            } catch (Exception x) {
                LOG.warn("Failed to delete old index file: " + file.getPath(), x);
            }
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setLuceneSearchService(LuceneSearchService luceneSearchService) {
        this.luceneSearchService = luceneSearchService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }
}
