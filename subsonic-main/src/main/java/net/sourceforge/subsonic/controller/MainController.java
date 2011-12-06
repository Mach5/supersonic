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

import net.sourceforge.subsonic.domain.CoverArtScheme;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.domain.MusicFileInfo;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.AdService;
import net.sourceforge.subsonic.service.MusicFileService;
import net.sourceforge.subsonic.service.MusicInfoService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the main page.
 *
 * @author Sindre Mehus
 */
public class MainController extends ParameterizableViewController {

    private SecurityService securityService;
    private PlayerService playerService;
    private SettingsService settingsService;
    private MusicInfoService musicInfoService;
    private MusicFileService musicFileService;
    private AdService adService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        Player player = playerService.getPlayer(request, response);
        String[] paths = request.getParameterValues("path");
        String path = paths[0];
        MusicFile dir = musicFileService.getMusicFile(path);
        if (dir.isFile()) {
            dir = dir.getParent();
        }

        // Redirect if root directory.
        if (dir.isRoot()) {
            return new ModelAndView(new RedirectView("home.view?"));
        }

        List<MusicFile> children = paths.length == 1 ? dir.getChildren(true, true, true) : getMultiFolderChildren(paths);
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        map.put("dir", dir);
        map.put("ancestors", getAncestors(dir));
        map.put("children", children);
        map.put("artist", guessArtist(children));
        map.put("album", guessAlbum(children));
        map.put("player", player);
        map.put("user", securityService.getCurrentUser(request));
        map.put("multipleArtists", isMultipleArtists(children));
        map.put("visibility", userSettings.getMainVisibility());
        map.put("updateNowPlaying", request.getParameter("updateNowPlaying") != null);
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("brand", settingsService.getBrand());
        if (!settingsService.isLicenseValid()) {
            map.put("ad", adService.getAd());
        }

        try {
            map.put("navigateUpAllowed", !dir.getParent().isRoot());
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }

        MusicFileInfo musicInfo = musicInfoService.getMusicFileInfoForPath(dir.getPath());
        int playCount = musicInfo == null ? 0 : musicInfo.getPlayCount();
        String comment = musicInfo == null ? null : musicInfo.getComment();
        Date lastPlayed = musicInfo == null ? null : musicInfo.getLastPlayed();
        String username = securityService.getCurrentUsername(request);
        Integer userRating = musicInfoService.getRatingForUser(username, dir);
        Double averageRating = musicInfoService.getAverageRating(dir);

        if (userRating == null) {
            userRating = 0;
        }

        if (averageRating == null) {
            averageRating = 0.0D;
        }

        map.put("userRating", 10 * userRating);
        map.put("averageRating", Math.round(10.0D * averageRating));
        map.put("playCount", playCount);
        map.put("comment", comment);
        map.put("lastPlayed", lastPlayed);

        CoverArtScheme scheme = player.getCoverArtScheme();
        if (scheme != CoverArtScheme.OFF) {
            List<File> coverArts = getCoverArt(paths);
            int size = coverArts.size() > 1 ? scheme.getSize() : scheme.getSize() * 2;
            map.put("coverArts", coverArts);
            map.put("coverArtSize", size);
            if (coverArts.isEmpty() && dir.isAlbum()) {
                map.put("showGenericCoverArt", true);
            }

        }

        setPreviousAndNextAlbums(dir, map);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private String guessArtist(List<MusicFile> children) {
        MusicFile.MetaData metaData = getMetaData(children);
        return metaData == null ? null : metaData.getArtist();
    }

    private String guessAlbum(List<MusicFile> children) {
        MusicFile.MetaData metaData = getMetaData(children);
        return metaData == null ? null : metaData.getAlbum();
    }

    private MusicFile.MetaData getMetaData(List<MusicFile> children) {
        for (MusicFile child : children) {
            if (child.isFile() && child.getMetaData() != null) {
                return child.getMetaData();
            }
        }
        return null;
    }

    private List<File> getCoverArt(String[] paths) throws IOException {
        int limit = settingsService.getCoverArtLimit();
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        List<File> result = new ArrayList<File>();
        for (String path : paths) {
            MusicFile dir = musicFileService.getMusicFile(path);
            if (dir.isFile()) {
                dir = dir.getParent();
            }
            result.addAll(musicFileService.getCoverArt(dir, limit - result.size(), 2));
        }
        return result;
    }

    private List<MusicFile> getMultiFolderChildren(String[] paths) throws IOException {
        List<MusicFile> result = new ArrayList<MusicFile>();
        for (String path : paths) {
            MusicFile dir = musicFileService.getMusicFile(path);
            if (dir.isFile()) {
                dir = dir.getParent();
            }
            result.addAll(dir.getChildren(true, true, true));
        }
        return result;
    }

    private List<MusicFile> getAncestors(MusicFile dir) throws IOException {
        LinkedList<MusicFile> result = new LinkedList<MusicFile>();

        try {
            MusicFile parent = dir.getParent();
            while (parent != null && !parent.isRoot()) {
                result.addFirst(parent);
                parent = parent.getParent();
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

    private void setPreviousAndNextAlbums(MusicFile dir, Map<String, Object> map) throws IOException {
        if (dir.isAlbum() && !dir.getParent().isRoot()) {
            List<MusicFile> sieblings = dir.getParent().getChildren(true, true, true);
            for (Iterator<MusicFile> iterator = sieblings.iterator(); iterator.hasNext();) {
                MusicFile siebling = iterator.next();
                if (siebling.isFile()) {
                    iterator.remove();
                }
            }

            int index = sieblings.indexOf(dir);
            if (index > 0) {
                map.put("previousAlbum", sieblings.get(index - 1));
            }
            if (index < sieblings.size() - 1) {
                map.put("nextAlbum", sieblings.get(index + 1));
            }
        }
    }

    private boolean isMultipleArtists(List<MusicFile> children) {
        // Collect unique artist names.
        Set<String> artists = new HashSet<String>();
        for (MusicFile child : children) {
            MusicFile.MetaData metaData = child.getMetaData();
            if (metaData != null && metaData.getArtist() != null) {
                artists.add(metaData.getArtist().toLowerCase());
            }
        }

        // If zero or one artist, it is definitely not multiple artists.
        if (artists.size() < 2) {
            return false;
        }

        // Fuzzily compare artist names, allowing for some differences in spelling, whitespace etc.
        List<String> artistList = new ArrayList<String>(artists);
        for (String artist : artistList) {
            if (StringUtils.getLevenshteinDistance(artist, artistList.get(0)) > 3) {
                return true;
            }
        }
        return false;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMusicInfoService(MusicInfoService musicInfoService) {
        this.musicInfoService = musicInfoService;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    public void setAdService(AdService adService) {
        this.adService = adService;
    }
}
