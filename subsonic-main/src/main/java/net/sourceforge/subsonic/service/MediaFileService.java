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
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.service.metadata.JaudiotaggerParser;
import net.sourceforge.subsonic.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
public class MediaFileService {

    private static final Logger LOG = Logger.getLogger(MediaFileService.class);

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

    public MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPath());
    }

    public List<MediaFile> getChildrenOf(String parentPath, boolean includeFiles, boolean includeDirectories, boolean sort) throws IOException {
        return getChildrenOf(new File(parentPath), includeFiles, includeDirectories, sort);
    }

    public List<MediaFile> getChildrenOf(File parent, boolean includeFiles, boolean includeDirectories, boolean sort) throws IOException {
        return getChildrenOf(getMediaFile(parent), includeFiles, includeDirectories, sort);
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @return All children media files.
     * @throws IOException If an I/O error occurs.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort) throws IOException {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        // Make sure children are stored and up-to-date in the database.
        updateChildren(parent);

        List<MediaFile> result = new ArrayList<MediaFile>();
        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPath())) {
            if (child.isDirectory() && includeDirectories) {
                result.add(child);
            }
            if (child.isFile() && includeFiles) {
                result.add(child);
            }
        }

        if (sort) {
            Collections.sort(result, new MediaFileSorter());
        }

        return result;
    }

    private void updateChildren(MediaFile parent) {

        // Check timestamps.
        if (parent.getChildrenLastUpdated().getTime() >= parent.getLastModified().getTime()) {
            return;
        }

        List<MediaFile> storedChildren = mediaFileDao.getChildrenOf(parent.getPath());
        Map<String, MediaFile> storedChildrenMap = new HashMap<String, MediaFile>();
        for (MediaFile child : storedChildren) {
            storedChildrenMap.put(child.getPath(), child);
        }

        List<File> children = listMediaFiles(parent);

        for (File child : children) {
            if (storedChildrenMap.remove(child.getPath()) == null) {
                // Add children that are not already stored.
                mediaFileDao.createOrUpdateMediaFile(createMediaFile(child));
            }
        }

        // Delete children that no longer exist on disk.
        for (String path : storedChildrenMap.keySet()) {
            mediaFileDao.deleteMediaFile(path);
        }

        // Update timestamp in parent.
        parent.setChildrenLastUpdated(parent.getLastModified());
        mediaFileDao.createOrUpdateMediaFile(parent);
    }

    private List<File> listMediaFiles(MediaFile parent) {
        List<File> result = new ArrayList<File>();
        for (File child : FileUtil.listFiles(parent.getFile())) {
            String suffix = FilenameUtils.getExtension(child.getName()).toLowerCase();
            if (!isExcluded(child) && (FileUtil.isDirectory(child) || isMusicFile(suffix) || isVideoFile(suffix))) {
                result.add(child);
            }
        }
        return result;
    }

    private boolean isMusicFile(String suffix) {
        for (String s : settingsService.getMusicFileTypesAsArray()) {
            if (suffix.equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isVideoFile(String suffix) {
        for (String s : settingsService.getVideoFileTypesAsArray()) {
            if (suffix.equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given file is excluded.
     *
     * @param file The child file in question.
     * @return Whether the child file is excluded.
     */
    private  boolean isExcluded(File file) {

        // Exclude all hidden files starting with a "." or "@eaDir" (thumbnail dir created on Synology devices).
        return file.getName().startsWith(".") || file.getName().startsWith("@eaDir");
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
        } catch (IOException x) {
            LOG.error("Failed to create media file.", x);
        }

        return MediaFile.forMusicFile(musicFile, coverArtPath);
    }

    private boolean useFastCache() {
        return settingsService.isFastCacheEnabled() && !searchService.isIndexBeingCreated();
    }

    public File getCoverArt(MediaFile mediaFile) throws IOException {
        File dir = mediaFile.getFile();
        if (dir.isFile()) {
            dir = dir.getParentFile();
        }
        return getCoverArt(dir);
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
            MediaFile mediaFile = getMediaFile(candidate);
            MusicFile musicFile = mediaFile.toMusicFile();
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

    /**
     * Comparator for sorting media files.
     */
    private static class MediaFileSorter implements Comparator<MediaFile> {

        public int compare(MediaFile a, MediaFile b) {
            if (a.isFile() && b.isDirectory()) {
                return 1;
            }

            if (a.isDirectory() && b.isFile()) {
                return -1;
            }

            if (a.isDirectory() && b.isDirectory()) {
                return a.getName().compareToIgnoreCase(b.getName());
            }

            // Compare by disc number, if present.
            Integer discA = a.getDiscNumber();
            Integer discB = b.getDiscNumber();
            if (discA != null && discB != null) {
                int i = discA.compareTo(discB);
                if (i != 0) {
                    return i;
                }
            }

            Integer trackA = a.getTrackNumber();
            Integer trackB = b.getTrackNumber();

            if (trackA == null && trackB != null) {
                return 1;
            }

            if (trackA != null && trackB == null) {
                return -1;
            }

            if (trackA == null && trackB == null) {
                return a.getName().compareToIgnoreCase(b.getName());
            }

            return trackA.compareTo(trackB);
        }
    }
}
