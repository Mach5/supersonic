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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.Cache;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.util.FileUtil;

import java.io.File;

/**
 * Provides services for instantiating and caching music files and cover art.
 *
 * @author Sindre Mehus
 */
@Deprecated
public class MusicFileService {

    private static final Logger LOG = Logger.getLogger(MusicFileService.class);

    private final File NULL_FILE = new File("NULL");

    private Cache coverArtCache;
    private Cache musicFileDiskCache;
    private Ehcache musicFileMemoryCache;

    private SecurityService securityService;
    private SettingsService settingsService;
    private SearchService searchService;
    private MediaFileDao mediaFileDao;

    /**
     * Returns a music file instance for the given file.  If possible, a cached value is returned.
     *
     * @param file A file on the local file system.
     * @return A music file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MusicFile getMusicFile(File file) {

        // Look in fast memory cache first.
        Element element = musicFileMemoryCache.get(file);
        MusicFile cachedMusicFile = element == null ? null : (MusicFile) element.getObjectValue();
        if (cachedMusicFile != null) {
            return cachedMusicFile;
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }


        cachedMusicFile = musicFileDiskCache.getValue(file.getPath());
        if (cachedMusicFile != null) {
            if (useFastCache() || cachedMusicFile.lastModified() >= FileUtil.lastModified(file)) {
                musicFileMemoryCache.put(new Element(file, cachedMusicFile));
                return cachedMusicFile;
            }
        }

        MusicFile musicFile = new MusicFile(file);

        // Put in caches.
        musicFileMemoryCache.put(new Element(file, musicFile));
        musicFileDiskCache.put(file.getPath(), musicFile);

        return musicFile;
    }

    private boolean useFastCache() {
        return settingsService.isFastCacheEnabled() && !searchService.isIndexBeingCreated();
    }

    /**
     * Returns a music file instance for the given path name. If possible, a cached value is returned.
     *
     * @param pathName A path name for a file on the local file system.
     * @return A music file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MusicFile getMusicFile(String pathName) {
        return getMusicFile(new File(pathName));
    }

    /**
     * Register in service locator so that non-Spring objects can access me.
     * This method is invoked automatically by Spring.
     */
    public void init() {
        ServiceLocator.setMusicFileService(this);
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMusicFileDiskCache(Cache musicFileDiskCache) {
        this.musicFileDiskCache = musicFileDiskCache;
    }

    public void setMusicFileMemoryCache(Ehcache musicFileMemoryCache) {
        this.musicFileMemoryCache = musicFileMemoryCache;
    }

    public void setCoverArtCache(Cache coverArtCache) {
        this.coverArtCache = coverArtCache;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }
}
