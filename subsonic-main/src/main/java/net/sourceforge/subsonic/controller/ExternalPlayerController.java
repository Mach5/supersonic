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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.ShareDao;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.Share;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.MusicFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;

/**
 * Controller for the page used to play shared music (Twitter, Facebook etc).
 *
 * @author Sindre Mehus
 */
public class ExternalPlayerController extends ParameterizableViewController {

    private static final Logger LOG = Logger.getLogger(ExternalPlayerController.class);
    private static final String GUEST_USERNAME = "guest";

    private MusicFileService musicFileService;
    private SettingsService settingsService;
    private SecurityService securityService;
    private PlayerService playerService;
    private ShareDao shareDao;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || !pathInfo.startsWith("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        Share share = shareDao.getShareByName(pathInfo.substring(1));
        if (share == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        if (share.getExpires() != null && share.getExpires().before(new Date())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        share.setLastVisited(new Date());
        share.setVisitCount(share.getVisitCount() + 1);
        shareDao.updateShare(share);

        List<MusicFile> songs = getSongs(share);
        List<File> coverArts = getCoverArts(songs);

        map.put("share", share);
        map.put("songs", songs);
        map.put("coverArts", coverArts);

        if (!coverArts.isEmpty()) {
            map.put("coverArt", coverArts.get(0));
        }
        map.put("redirectFrom", settingsService.getUrlRedirectFrom());
        map.put("player", getPlayer(request).getId());

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private List<MusicFile> getSongs(Share share) throws IOException {
        List<MusicFile> result = new ArrayList<MusicFile>();

        for (String path : shareDao.getSharedFiles(share.getId())) {
            try {
                MusicFile file = musicFileService.getMusicFile(path);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        result.addAll(file.getChildren(true, false, true));
                    } else {
                        result.add(file);
                    }
                }
            } catch (Exception x) {
                LOG.warn("Couldn't read file " + path);
            }
        }
        return result;
    }

    private List<File> getCoverArts(List<MusicFile> songs) throws IOException {
        List<File> result = new ArrayList<File>();
        for (MusicFile song : songs) {
            result.add(musicFileService.getCoverArt(song.getParent()));
        }
        return result;
    }


    private Player getPlayer(HttpServletRequest request) {

        // Create guest user if necessary.
        User user = securityService.getUserByName(GUEST_USERNAME);
        if (user == null) {
            user = new User(GUEST_USERNAME, RandomStringUtils.randomAlphanumeric(30), null);
            user.setStreamRole(true);
            securityService.createUser(user);
        }

        // Look for existing player.
        List<Player> players = playerService.getPlayersForUserAndClientId(GUEST_USERNAME, null);
        if (!players.isEmpty()) {
            return players.get(0);
        }

        // Create player if necessary.
        Player player = new Player();
        player.setIpAddress(request.getRemoteAddr());
        player.setUsername(GUEST_USERNAME);
        playerService.createPlayer(player);

        return player;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setShareDao(ShareDao shareDao) {
        this.shareDao = shareDao;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}