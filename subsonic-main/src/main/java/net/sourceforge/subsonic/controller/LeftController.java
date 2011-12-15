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
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.Cache;
import net.sourceforge.subsonic.domain.CacheElement;
import net.sourceforge.subsonic.domain.InternetRadio;
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.MusicIndex;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.MusicFileService;
import net.sourceforge.subsonic.service.MusicIndexService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.util.StringUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Controller for the left index frame.
 *
 * @author Sindre Mehus
 */
public class LeftController extends ParameterizableViewController implements LastModified {

    private static final Logger LOG = Logger.getLogger(LeftController.class);

    private SearchService searchService;
    private SettingsService settingsService;
    private SecurityService securityService;
    private MusicFileService musicFileService;
    private MusicIndexService musicIndexService;
    private PlayerService playerService;
    private Cache musicFolderCache;

    /**
     * {@inheritDoc}
     */
    public long getLastModified(HttpServletRequest request) {
        saveSelectedMusicFolder(request);

        // When was settings last changed?
        long lastModified = settingsService.getSettingsChanged();

        // When was music folder(s) on disk last changed?
        List<MusicFolder> allMusicFolders = settingsService.getAllMusicFolders();
        MusicFolder selectedMusicFolder = getSelectedMusicFolder(request);
        if (selectedMusicFolder != null) {
            File file = selectedMusicFolder.getPath();
            lastModified = Math.max(lastModified, file.lastModified());
        } else {
            for (MusicFolder musicFolder : allMusicFolders) {
                File file = musicFolder.getPath();
                lastModified = Math.max(lastModified, file.lastModified());
            }
        }

        // When was music folder table last changed?
        for (MusicFolder musicFolder : allMusicFolders) {
            lastModified = Math.max(lastModified, musicFolder.getChanged().getTime());
        }

        // When was internet radio table last changed?
        for (InternetRadio internetRadio : settingsService.getAllInternetRadios()) {
            lastModified = Math.max(lastModified, internetRadio.getChanged().getTime());
        }

        // When was user settings last changed?
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        lastModified = Math.max(lastModified, userSettings.getChanged().getTime());

        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        saveSelectedMusicFolder(request);
        Map<String, Object> map = new HashMap<String, Object>();

        MediaLibraryStatistics statistics = searchService.getStatistics();
        Locale locale = RequestContextUtils.getLocale(request);

        List<MusicFolder> allMusicFolders = settingsService.getAllMusicFolders();
        MusicFolder selectedMusicFolder = getSelectedMusicFolder(request);
        List<MusicFolder> musicFoldersToUse = selectedMusicFolder == null ? allMusicFolders : Arrays.asList(selectedMusicFolder);
        String[] shortcuts = settingsService.getShortcutsAsArray();
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        MusicFolderCacheEntry cacheEntry = getCacheEntry(musicFoldersToUse, getLastModified(request));

        map.put("player", playerService.getPlayer(request, response));
        map.put("musicFolders", allMusicFolders);
        map.put("selectedMusicFolder", selectedMusicFolder);
        map.put("radios", settingsService.getAllInternetRadios());
        map.put("shortcuts", getShortcuts(musicFoldersToUse, shortcuts));
        map.put("captionCutoff", userSettings.getMainVisibility().getCaptionCutoff());
        map.put("partyMode", userSettings.isPartyModeEnabled());

        if (statistics != null) {
            map.put("statistics", statistics);
            long bytes = statistics.getTotalLengthInBytes();
            long hours = bytes * 8 / 1024 / 150 / 3600;
            map.put("hours", hours);
            map.put("bytes", StringUtil.formatBytes(bytes, locale));
        }

        map.put("indexedArtists", cacheEntry.indexedArtists);
        map.put("singleSongs", cacheEntry.singleSongs);
        map.put("indexes", cacheEntry.indexedArtists.keySet());
        map.put("user", securityService.getCurrentUser(request));

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private void saveSelectedMusicFolder(HttpServletRequest request) {
        if (request.getParameter("musicFolderId") == null) {
            return;
        }
        int musicFolderId = Integer.parseInt(request.getParameter("musicFolderId"));

        // Note: UserSettings.setChanged() is intentionally not called. This would break browser caching
        // of the left frame.
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        settings.setSelectedMusicFolderId(musicFolderId);
        settingsService.updateUserSettings(settings);
    }

    /**
     * Returns the selected music folder, or <code>null</code> if all music folders should be displayed.
     */
    private MusicFolder getSelectedMusicFolder(HttpServletRequest request) {
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        int musicFolderId = settings.getSelectedMusicFolderId();

        return settingsService.getMusicFolderById(musicFolderId);
    }

    protected List<MusicFile> getSingleSongs(List<MusicFolder> folders) throws IOException {
        List<MusicFile> result = new ArrayList<MusicFile>();
        for (MusicFolder folder : folders) {
            MusicFile parent = musicFileService.getMusicFile(folder.getPath());
            result.addAll(parent.getChildren(true, false, true));
        }
        return result;
    }

    public List<MusicFile> getShortcuts(List<MusicFolder> musicFoldersToUse, String[] shortcuts) {
        List<MusicFile> result = new ArrayList<MusicFile>();

        for (String shortcut : shortcuts) {
            for (MusicFolder musicFolder : musicFoldersToUse) {
                File file = new File(musicFolder.getPath(), shortcut);
                if (file.exists()) {
                    result.add(musicFileService.getMusicFile(file));
                }
            }
        }

        return result;
    }

    public MusicFolderCacheEntry getCacheEntry(List<MusicFolder> musicFoldersToUse, long lastModified) throws Exception {
        StringBuilder musicFolderIds = new StringBuilder();
        for (MusicFolder musicFolder : musicFoldersToUse) {
            musicFolderIds.append(musicFolder.getId()).append(",");
        }
        musicFolderIds.setLength(musicFolderIds.length() - 1);
        String key = musicFolderIds.toString();

        MusicFolderCacheEntry entry = musicFolderCache.getValue(key);

        if (entry == null || entry.lastModified < lastModified) {
            SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists = musicIndexService.getIndexedArtists(musicFoldersToUse);
            List<MusicFile> singleSongs = getSingleSongs(musicFoldersToUse);
            entry = new MusicFolderCacheEntry(indexedArtists, singleSongs, lastModified);
            musicFolderCache.put(key, entry);
        }

        return entry;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    public void setMusicIndexService(MusicIndexService musicIndexService) {
        this.musicIndexService = musicIndexService;
    }

    public void setMusicFolderCache(Cache musicFolderCache) {
        this.musicFolderCache = musicFolderCache;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public static class MusicFolderCacheEntry implements Serializable {

        private final SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists;
        private final List<MusicFile> singleSongs;
        private final long lastModified;

        public MusicFolderCacheEntry(SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists,
                                     List<MusicFile> singleSongs, long lastModified) {
            this.indexedArtists = indexedArtists;
            this.singleSongs = singleSongs;
            this.lastModified = lastModified;
        }

        public SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> getIndexedArtists() {
            return indexedArtists;
        }

        public List<MusicFile> getSingleSongs() {
            return singleSongs;
        }

        public long getLastModified() {
            return lastModified;
        }
    }
}
