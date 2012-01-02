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
import net.sourceforge.subsonic.domain.CacheElement;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.service.metadata.JaudiotaggerParser;
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.Util;
import org.apache.commons.io.filefilter.FileFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
public class MediaFileService {

    private static final Logger LOG = Logger.getLogger(MediaFileService.class);

    private final File NULL_FILE = new File("NULL");

    private Ehcache mediaFileMemoryCache;

    private SecurityService securityService;
    private SettingsService settingsService;
    private SearchService searchService;
    private MediaFileDao mediaFileDao;

    /**
     * Returns a media file instance for the given file.  If possible, a cached value is returned.
     *
     * @param file A file on the local file system.
     * @return A media file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MediaFile getMediaFile(File file) {

        // Look in fast memory cache first.
        Element element = mediaFileMemoryCache.get(file);
        MediaFile cachedMediaFile = element == null ? null : (MediaFile) element.getObjectValue();
        if (cachedMediaFile != null) {
            return cachedMediaFile;
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }

        cachedMediaFile = mediaFileDao.getMediaFile(file.getPath());
        if (cachedMediaFile != null) {
            if (useFastCache() || cachedMediaFile.getLastModified().getTime() >= FileUtil.lastModified(file)) {
                mediaFileMemoryCache.put(new Element(file, cachedMediaFile));
                return cachedMediaFile;
            }
        }

        MediaFile mediaFile = createMediaFile(file);

        // Put in caches.
        mediaFileMemoryCache.put(new Element(file, mediaFile));
        mediaFileDao.createOrUpdateMediaFile(mediaFile);

        return mediaFile;
    }

    private MediaFile createMediaFile(File file) {

        MusicFile musicFile = new MusicFile(file);

        String coverArtPath = null;
        try {
            if (musicFile.isAlbum()) {
                File coverArt = getCoverArt(file);
                if (coverArt != null) {
                    coverArtPath = coverArt.getPath();
                }
            }

            return MediaFile.forMusicFile(musicFile, coverArtPath);
        } catch (IOException x) {
            LOG.error("Failed to create media file.", x);
        }
    }

    private boolean useFastCache() {
        return settingsService.isFastCacheEnabled() && !searchService.isIndexBeingCreated();
    }

    /**
     * Returns a media file instance for the given path name. If possible, a cached value is returned.
     *
     * @param pathName A path name for a file on the local file system.
     * @return A memdia file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MediaFile getMediaFile(String pathName) {
        return getMediaFile(new File(pathName));
    }

    /**
     * Returns a cover art image for the given directory.
     */
    private File getCoverArt(File dir) throws IOException {
        File[] candidates = FileUtil.listFiles(dir, FileFileFilter.FILE);

        for (String mask : settingsService.getCoverArtFileTypesAsArray()) {
            for (File candidate : candidates) {
                if (candidate.getName().toUpperCase().endsWith(mask.toUpperCase()) && !candidate.getName().startsWith(".")) {
                    return candidate;
                }
            }
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        JaudiotaggerParser parser = new JaudiotaggerParser();
        for (File candidate : candidates) {
            MusicFile musicFile = getMusicFile(candidate);
            if (parser.isApplicable(musicFile)) {
                if (parser.isImageAvailable(musicFile)) {
                    return candidate;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Register in service locator so that non-Spring objects can access me.
     * This method is invoked automatically by Spring.
     */
    public void init() {
        ServiceLocator.setMediaFileService(this);
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileMemoryCache(Ehcache mediaFileMemoryCache) {
        this.mediaFileMemoryCache = mediaFileMemoryCache;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }
}
