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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.subsonic.service.*;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.Playlist;
import net.sourceforge.subsonic.domain.Share;

/**
 * Controller for sharing music on Twitter, Facebook etc.
 *
 * @author Sindre Mehus
 */
public class ShareManagementController extends MultiActionController {

    private MusicFileService musicFileService;
    private SettingsService settingsService;
    private ShareService shareService;
    private PlayerService playerService;
    private SecurityService securityService;

    public ModelAndView createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {

        List<MusicFile> files = getMusicFiles(request);
        MusicFile dir = null;
        if (!files.isEmpty()) {
            dir = files.get(0);
            if (!dir.isAlbum()) {
                dir = dir.getParent();
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("urlRedirectionEnabled", settingsService.isUrlRedirectionEnabled());
        map.put("dir", dir);
        map.put("user", securityService.getCurrentUser(request));
        Share share = shareService.createShare(request, files);
        map.put("playUrl", shareService.getShareUrl(share));

        return new ModelAndView("createShare", "model", map);
    }

    private List<MusicFile> getMusicFiles(HttpServletRequest request) throws IOException {
        String dir = request.getParameter("dir");
        String playerId = request.getParameter("player");

        List<MusicFile> result = new ArrayList<MusicFile>();

        if (dir != null) {
            MusicFile album = musicFileService.getMusicFile(dir);
            int[] indexes = ServletRequestUtils.getIntParameters(request, "i");
            if (indexes.length == 0) {
                return Arrays.asList(album);
            }
            List<MusicFile> children = album.getChildren(true, true, true);
            for (int index : indexes) {
                result.add(children.get(index));
            }
        } else if (playerId != null) {
            Player player = playerService.getPlayerById(playerId);
            Playlist playlist = player.getPlaylist();
            Collections.addAll(result, playlist.getFiles());
        }

        return result;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setShareService(ShareService shareService) {
        this.shareService = shareService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}