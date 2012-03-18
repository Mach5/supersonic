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
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.service.metadata.JaudiotaggerParser;
import net.sourceforge.subsonic.service.metadata.MetaData;
import net.sourceforge.subsonic.service.metadata.MetaDataParser;
import net.sourceforge.subsonic.service.metadata.MetaDataParserFactory;
import net.sourceforge.subsonic.util.FileUtil;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sourceforge.subsonic.domain.MediaFile.MediaType.*;

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
    private MediaScannerService mediaScannerService;
    private MediaFileDao mediaFileDao;
    private MetaDataParserFactory metaDataParserFactory;

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
        MediaFile result = element == null ? null : (MediaFile) element.getObjectValue();
        if (result != null) {
            return result;
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }

        // Secondly, look in database.
        result = mediaFileDao.getMediaFile(file.getPath());
        if (result != null) {
            result = checkLastModified(result);
            mediaFileMemoryCache.put(new Element(file, result));
            return result;
        }

        // Not found, must read from disk.
        result = createMediaFile(file);

        // Put in cache and database.
        mediaFileMemoryCache.put(new Element(file, result));
        mediaFileDao.createOrUpdateMediaFile(result);

        return result;
    }

    private MediaFile checkLastModified(MediaFile mediaFile) {
        if (useFastCache() || mediaFile.getLastModified().getTime() >= FileUtil.lastModified(mediaFile.getFile())) {
            return mediaFile;
        }
        mediaFile = createMediaFile(mediaFile.getFile());
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
        return  mediaFile;
    }

    /**
     * Returns a media file instance for the given path name. If possible, a cached value is returned.
     *
     * @param pathName A path name for a file on the local file system.
     * @return A media file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MediaFile getMediaFile(String pathName) {
        return getMediaFile(new File(pathName));
    }

    // TODO: Optimize with memory caching.
    public MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (!securityService.isReadAllowed(mediaFile.getFile())) {
            throw new SecurityException("Access denied to file " + mediaFile);
        }
        return mediaFile;
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPath());
    }

    public List<MediaFile> getChildrenOf(String parentPath, boolean includeFiles, boolean includeDirectories, boolean sort) {
        return getChildrenOf(new File(parentPath), includeFiles, includeDirectories, sort);
    }

    public List<MediaFile> getChildrenOf(File parent, boolean includeFiles, boolean includeDirectories, boolean sort) {
        return getChildrenOf(getMediaFile(parent), includeFiles, includeDirectories, sort);
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort) {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        // Make sure children are stored and up-to-date in the database.
        updateChildren(parent);

        List<MediaFile> result = new ArrayList<MediaFile>();
        for (MediaFile child : mediaFileDao.getChildrenOf(parent.getPath())) {
            child = checkLastModified(child);
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

    /**
     * Returns the first direct child (excluding directories).
     * This is a convenience method.
     *
     * @return The first child, or <code>null</code> if not found.
     */
    public MediaFile getFirstChildOf(MediaFile parent) {
        List<MediaFile> children = getChildrenOf(parent, true, false, true);
        return children.isEmpty() ? null : children.get(0);
    }

    /**
     * Returns whether the given file is the root of a media folder.
     *
     * @see MusicFolder
     */
    public boolean isRoot(MediaFile mediaFile) {
        for (MusicFolder musicFolder : settingsService.getAllMusicFolders(false, true)) {
            if (mediaFile.getPath().equals(musicFolder.getPath().getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all genres in the music collection.
     *
     * @return Sorted list of genres.
     */
    public List<String> getGenres() {
        return mediaFileDao.getGenres();
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count) {
        return mediaFileDao.getMostFrequentlyPlayedAlbums(offset, count);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count) {
        return mediaFileDao.getMostRecentlyPlayedAlbums(offset, count);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count) {
        return mediaFileDao.getNewestAlbums(offset, count);
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

        List<File> children = filterMediaFiles(FileUtil.listFiles(parent.getFile()));
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
        parent.setPresent(true);
        mediaFileDao.createOrUpdateMediaFile(parent);
    }

    private List<File> filterMediaFiles(File[] candidates) {
        List<File> result = new ArrayList<File>();
        for (File candidate : candidates) {
            String suffix = FilenameUtils.getExtension(candidate.getName()).toLowerCase();
            if (!isExcluded(candidate) && (FileUtil.isDirectory(candidate) || isMusicFile(suffix) || isVideoFile(suffix))) {
                result.add(candidate);
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
    private boolean isExcluded(File file) {

        // Exclude all hidden files starting with a "." or "@eaDir" (thumbnail dir created on Synology devices).
        String name = file.getName();
        return name.startsWith(".") || name.startsWith("@eaDir") || name.equals("Thumbs.db");
    }

    private MediaFile createMediaFile(File file) {
        MediaFile mediaFile = new MediaFile();
        Date now = new Date();
        mediaFile.setPath(file.getPath());
        mediaFile.setFolder(securityService.getRootFolderForFile(file));
        mediaFile.setParentPath(file.getParent());
        mediaFile.setLastModified(new Date(FileUtil.lastModified(file)));
        mediaFile.setLastScanned(now);
        mediaFile.setPlayCount(0);
        mediaFile.setChildrenLastUpdated(new Date(0));
        mediaFile.setCreated(now);
        mediaFile.setMediaType(DIRECTORY);
        mediaFile.setPresent(true);

        if (file.isFile()) {
            String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(mediaFile.getPath())));
            mediaFile.setFormat(format);
            mediaFile.setFileSize(FileUtil.length(file));
            mediaFile.setMediaType(isMusicFile(format) ? AUDIO : VIDEO);

            MetaDataParser parser = metaDataParserFactory.getParser(file);
            if (parser != null) {
                MetaData metaData = parser.getMetaData(file);
                mediaFile.setArtist(metaData.getArtist());
                mediaFile.setAlbumName(metaData.getAlbumName());
                mediaFile.setTitle(metaData.getTitle());
                mediaFile.setDiscNumber(metaData.getDiscNumber());
                mediaFile.setTrackNumber(metaData.getTrackNumber());
                mediaFile.setGenre(metaData.getGenre());
                mediaFile.setYear(metaData.getYear());
                mediaFile.setDurationSeconds(metaData.getDurationSeconds());
                mediaFile.setBitRate(metaData.getBitRate());
                mediaFile.setVariableBitRate(metaData.getVariableBitRate());
                mediaFile.setHeight(metaData.getHeight());
                mediaFile.setWidth(metaData.getWidth());
            }

        } else {

            // Is this an album?
            if (!isRoot(mediaFile)) {
                File[] children = FileUtil.listFiles(file);
                File firstChild = null;
                for (File child : filterMediaFiles(children)) {
                    if (FileUtil.isFile(child)) {
                        firstChild = child;
                        break;
                    }
                }

                if (firstChild != null) {
                    mediaFile.setMediaType(ALBUM);

                    // Guess artist/album name.
                    MetaDataParser parser = metaDataParserFactory.getParser(firstChild);
                    if (parser != null) {
                        mediaFile.setArtist(parser.guessArtist(firstChild));
                        mediaFile.setAlbumName(parser.guessAlbum(firstChild, mediaFile.getArtist()));
                    }

                    // Look for cover art.
                    try {
                        File coverArt = findCoverArt(children);
                        if (coverArt != null) {
                            mediaFile.setCoverArtPath(coverArt.getPath());
                        }
                    } catch (IOException x) {
                        LOG.error("Failed to find cover art.", x);
                    }

                } else {
                    mediaFile.setArtist(file.getName());
                }
            }
        }

        return mediaFile;
    }

    private boolean useFastCache() {
        return settingsService.isFastCacheEnabled() && !mediaScannerService.isScanning();
    }

    public void refreshMediaFile(MediaFile mediaFile) {
        mediaFile = createMediaFile(mediaFile.getFile());
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
        mediaFileMemoryCache.remove(mediaFile.getFile());
    }

    /**
     * Returns a cover art image for the given media file.
     */
    public File getCoverArt(MediaFile mediaFile) {
        if (mediaFile.getCoverArtFile() != null) {
            return mediaFile.getCoverArtFile();
        }
        MediaFile parent = getParentOf(mediaFile);
        return parent == null ? null : parent.getCoverArtFile();
    }

    /**
     * Finds a cover art image for the given directory, by looking for it on the disk.
     */
    private File findCoverArt(File[] candidates) throws IOException {
        for (String mask : settingsService.getCoverArtFileTypesAsArray()) {
            for (File candidate : candidates) {
                if (candidate.isFile() && candidate.getName().toUpperCase().endsWith(mask.toUpperCase()) && !candidate.getName().startsWith(".")) {
                    return candidate;
                }
            }
        }

        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        JaudiotaggerParser parser = new JaudiotaggerParser();
        for (File candidate : candidates) {
            if (parser.isApplicable(candidate)) {
                if (parser.isImageAvailable(getMediaFile(candidate))) {
                    return candidate;
                } else {
                    return null;
                }
            }
        }
        return null;
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

    public void setMediaScannerService(MediaScannerService mediaScannerService) {
        this.mediaScannerService = mediaScannerService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    /**
     * Returns all media files that are children, grand-children etc of a given media file.
     * Directories are not included in the result.
     *
     * @param sort Whether to sort files in the same directory.
     * @return All descendant music files.
     */
    public List<MediaFile> getDescendantsOf(MediaFile ancestor, boolean sort) {

        if (ancestor.isFile()) {
            return Arrays.asList(ancestor);
        }

        List<MediaFile> result = new ArrayList<MediaFile>();

        for (MediaFile child : getChildrenOf(ancestor, true, true, sort)) {
            if (child.isDirectory()) {
                result.addAll(getDescendantsOf(child, sort));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }

    public void updateMediaFile(MediaFile mediaFile) {
        mediaFileDao.createOrUpdateMediaFile(mediaFile);
    }

    /**
     * Increments the play count and last played date for the given media file and its
     * directory.
     */
    public void incrementPlayCount(MediaFile file) {
        file.setLastPlayed(new Date());
        file.setPlayCount(file.getPlayCount() + 1);
        updateMediaFile(file);

        MediaFile parent = getParentOf(file);
        if (!isRoot(parent)) {
            parent.setLastPlayed(new Date());
            parent.setPlayCount(parent.getPlayCount() + 1);
            updateMediaFile(parent);
        }
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
