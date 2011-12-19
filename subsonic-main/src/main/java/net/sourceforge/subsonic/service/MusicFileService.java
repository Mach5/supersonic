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

import net.sourceforge.subsonic.domain.Cache;
import net.sourceforge.subsonic.domain.CacheElement;
import net.sourceforge.subsonic.service.metadata.JaudiotaggerParser;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.Pair;

import net.sourceforge.subsonic.util.TimeLimitedCache;
import org.apache.commons.io.filefilter.FileFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides services for instantiating and caching music files and cover art.
 *
 * @author Sindre Mehus
 */
public class MusicFileService {

    private final File NULL_FILE = new File("NULL");

    private Cache childDirCache;
    private Cache coverArtCache;
    private Cache musicFileDiskCache;
    private final TimeLimitedCache<File, MusicFile> musicFileMemoryCache;

    private SecurityService securityService;
    private SettingsService settingsService;

    public MusicFileService() {
        musicFileMemoryCache = new TimeLimitedCache<File, MusicFile>(10, TimeUnit.SECONDS);
    }

    /**
     * Returns a music file instance for the given file.  If possible, a cached value is returned.
     *
     * @param file A file on the local file system.
     * @return A music file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MusicFile getMusicFile(File file) {

        // Look in fast memory cache first.
        MusicFile cachedMusicFile = musicFileMemoryCache.get(file);
        if (cachedMusicFile != null) {
            return cachedMusicFile;
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }

        cachedMusicFile = musicFileDiskCache.getValue(file.getPath());
        if (cachedMusicFile != null && cachedMusicFile.lastModified() >= FileUtil.lastModified(file)) {
            musicFileMemoryCache.put(file, cachedMusicFile);
            return cachedMusicFile;
        }

        MusicFile musicFile = new MusicFile(file);


        // Put in caches.
        musicFileMemoryCache.put(file, musicFile);
        musicFileDiskCache.put(file.getPath(), musicFile);

        return musicFile;
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
     * Returns a cover art image for the given directory.
     */
    public File getCoverArt(MusicFile dir) throws IOException {

        // Look in cache.
        CacheElement element = coverArtCache.get(dir.getPath());
        if (element != null) {

            // Check if cache is up-to-date.
            if (element.getCreated() > FileUtil.lastModified(dir.getFile())) {
                File file = (File) element.getValue();
                return file.equals(NULL_FILE) ? null : file;
            }
        }

        File coverArt = getBestCoverArt(FileUtil.listFiles(dir.getFile(), FileFileFilter.FILE));
        coverArtCache.put(dir.getPath(), coverArt == null ? NULL_FILE : coverArt);
        return coverArt;
    }

    private File getBestCoverArt(File[] candidates) {
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
     * Returns the (sorted) child directories of the given parent. If possible, a cached
     * value is returned.
     *
     * @param parent The parent directory.
     * @return The child directories.
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings({"unchecked"})
    public synchronized List<MusicFile> getChildDirectories(MusicFile parent) throws IOException {
        Pair<MusicFile, List<MusicFile>> value = childDirCache.getValue(parent.getPath());
        if (value != null) {

            // Check if cache is up-to-date.
            MusicFile cachedParent = value.getFirst();
            if (cachedParent.lastModified() >= parent.lastModified()) {
                return value.getSecond();
            }
        }

        List<MusicFile> children = parent.getChildren(false, true, true);
        childDirCache.put(parent.getPath(), new Pair<MusicFile, List<MusicFile>>(parent, children));

        return children;
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

    public void setMusicFileCache(Cache musicFileCache) {
        this.musicFileDiskCache = musicFileCache;
    }

    public void setChildDirCache(Cache childDirCache) {
        this.childDirCache = childDirCache;
    }

    public void setCoverArtCache(Cache coverArtCache) {
        this.coverArtCache = coverArtCache;
    }
}
