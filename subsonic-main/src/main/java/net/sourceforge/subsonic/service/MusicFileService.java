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

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Provides services for instantiating and caching music files and cover art.
 *
 * @author Sindre Mehus
 */
public class MusicFileService {

    private Cache musicFileCache;
    private Cache childDirCache;
    private Cache coverArtCache;

    private SecurityService securityService;
    private SettingsService settingsService;

    /**
     * Returns a music file instance for the given file.  If possible, a cached value is returned.
     *
     * @param file A file on the local file system.
     * @return A music file instance.
     * @throws SecurityException If access is denied to the given file.
     */
    public MusicFile getMusicFile(File file) {
        CacheElement element = musicFileCache.get(file.getPath());
        if (element != null) {

            // Check if cache is up-to-date.
            MusicFile cachedMusicFile = (MusicFile) element.getValue();
            if (cachedMusicFile.lastModified() >= file.lastModified()) {
                return cachedMusicFile;
            }
        }

        if (!securityService.isReadAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }

        MusicFile musicFile = new MusicFile(file);
        // TODO: Consider reading metadata before caching.
//        musicFile.getMetaData();

        musicFileCache.put(file.getPath(), musicFile);

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
     * Equivalent to <code>getCoverArt(dir, 1, 1)</code>.
     */
    public File getCoverArt(MusicFile dir) throws IOException {
        List<File> list = getCoverArt(dir, 1, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Returns a list of appropriate cover art images for the given directory.
     *
     * @param dir   The directory.
     * @param limit Maximum number of images to return.
     * @param depth Recursion depth when searching for images.
     * @return A list of appropriate cover art images for the directory.
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings({"unchecked"})
    public List<File> getCoverArt(MusicFile dir, int limit, int depth) throws IOException {

        // Look in cache.
        CacheElement element = coverArtCache.get(dir.getPath());
        if (element != null) {

            // Check if cache is up-to-date.
            if (element.getCreated() > getDirectoryLastModified(dir.getFile())) {
                List<File> result = (List<File>) element.getValue();
                return result.subList(0, Math.min(limit, result.size()));
            }
        }

        List<File> result = new ArrayList<File>();
        listCoverArtRecursively(dir, result, limit, depth);

        coverArtCache.put(dir.getPath(), result);
        return result;
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
        CacheElement element = childDirCache.get(parent.getPath());
        if (element != null) {

            // Check if cache is up-to-date.
            Pair<MusicFile, List<MusicFile>> value = (Pair<MusicFile, List<MusicFile>>) element.getValue();
            MusicFile cachedParent = value.getFirst();
            if (cachedParent.lastModified() >= parent.lastModified()) {
                return value.getSecond();
            }
        }

        List<MusicFile> children = parent.getChildren(false, true, true);
        childDirCache.put(parent.getPath(), new Pair<MusicFile, List<MusicFile>>(parent, children));

        return children;
    }

    private long getDirectoryLastModified(File dir) {
        long lastModified = dir.lastModified();
        File[] subDirs = FileUtil.listFiles(dir, DirectoryFileFilter.INSTANCE);
        for (File subDir : subDirs) {
            lastModified = Math.max(lastModified, subDir.lastModified());
        }
        return lastModified;
    }

    private void listCoverArtRecursively(MusicFile dir, List<File> coverArtFiles, int limit, int depth) throws IOException {
        if (depth == 0 || coverArtFiles.size() == limit) {
            return;
        }

        File[] files = FileUtil.listFiles(dir.getFile());

        // Sort alphabetically
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                if (a.isFile() && b.isDirectory()) {
                    return 1;
                }
                if (a.isDirectory() && b.isFile()) {
                    return -1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        for (File file : files) {
            if (file.isDirectory() && !dir.isExcluded(file)) {
                listCoverArtRecursively(getMusicFile(file), coverArtFiles, limit, depth - 1);
            }
        }

        if (coverArtFiles.size() == limit) {
            return;
        }

        File best = getBestCoverArt(files);
        if (best != null) {
            coverArtFiles.add(best);
        }
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
        this.musicFileCache = musicFileCache;
    }

    public void setChildDirCache(Cache childDirCache) {
        this.childDirCache = childDirCache;
    }

    public void setCoverArtCache(Cache coverArtCache) {
        this.coverArtCache = coverArtCache;
    }
}
