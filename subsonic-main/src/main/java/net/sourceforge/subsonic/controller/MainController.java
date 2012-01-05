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
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFileInfo;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.AdService;
import net.sourceforge.subsonic.service.MediaFileService;
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
    private MediaFileService mediaFileService;
    private AdService adService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        Player player = playerService.getPlayer(request, response);
        String[] paths = request.getParameterValues("path");
        String path = paths[0];
        MediaFile dir = mediaFileService.getMediaFile(path);
        if (dir.isFile()) {
            dir = mediaFileService.getParentOf(dir);
        }

        // Redirect if root directory.
        if (dir.isRoot()) {
            return new ModelAndView(new RedirectView("home.view?"));
        }

        List<MediaFile> children = paths.length == 1 ? mediaFileService.getChildrenOf(dir, true, true, true) : getMultiFolderChildren(paths);
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
            MediaFile parent = mediaFileService.getParentOf(dir);
            map.put("parent", parent);
            map.put("navigateUpAllowed", !parent.isRoot());
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
            List<File> coverArts = getCoverArts(dir, children);
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

    private String guessArtist(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getArtist();
            }
        }
        return null;
    }

    private String guessAlbum(List<MediaFile> children) {
        for (MediaFile child : children) {
            if (child.isFile() && child.getArtist() != null) {
                return child.getAlbumName();
            }
        }
        return null;
    }

    private List<File> getCoverArts(MediaFile dir, List<MediaFile> children) throws IOException {
        int limit = settingsService.getCoverArtLimit();
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        List<File> coverArts = new ArrayList<File>();
        if (dir.isAlbum()) {
            coverArts.add(mediaFileService.getCoverArt(dir));
        } else {
            for (MediaFile child : children) {
                if (child.isDirectory()) {
                    File coverArt = mediaFileService.getCoverArt(child);
                    if (coverArt != null) {
                        coverArts.add(coverArt);
                        if (coverArts.size() > limit) {
                            break;
                        }
                    }
                }
            }
        }
        return coverArts;
    }

    private List<MediaFile> getMultiFolderChildren(String[] paths) throws IOException {
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (String path : paths) {
            MediaFile dir = mediaFileService.getMediaFile(path);
            if (dir.isFile()) {
                dir = mediaFileService.getParentOf(dir);
            }
            result.addAll(mediaFileService.getChildrenOf(dir, true, true, true));
        }
        return result;
    }

    private List<MediaFile> getAncestors(MediaFile dir) throws IOException {
        LinkedList<MediaFile> result = new LinkedList<MediaFile>();

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            while (parent != null && !parent.isRoot()) {
                result.addFirst(parent);
                parent = mediaFileService.getParentOf(parent);
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

    private void setPreviousAndNextAlbums(MediaFile dir, Map<String, Object> map) throws IOException {
        MediaFile parent = mediaFileService.getParentOf(dir);

        if (dir.isAlbum() && !parent.isRoot()) {
            List<MediaFile> sieblings = mediaFileService.getChildrenOf(parent, false, true, true);

            int index = sieblings.indexOf(dir);
            if (index > 0) {
                map.put("previousAlbum", sieblings.get(index - 1));
            }
            if (index < sieblings.size() - 1) {
                map.put("nextAlbum", sieblings.get(index + 1));
            }
        }
    }

    private boolean isMultipleArtists(List<MediaFile> children) {
        // Collect unique artist names.
        Set<String> artists = new HashSet<String>();
        for (MediaFile child : children) {
            if (child.getArtist() != null) {
                artists.add(child.getArtist().toLowerCase());
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

    public void setAdService(AdService adService) {
        this.adService = adService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
