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

import net.sourceforge.subsonic.service.*;
import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.util.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.multiaction.*;
import org.springframework.web.servlet.view.*;

import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * Controller for listing, loading, appending and deleting playlists.
 *
 * @author Sindre Mehus
 */
public class LoadPlaylistController extends MultiActionController {

    private PlaylistService playlistService;
    private SecurityService securityService;
    private PlayerService playerService;
    private MusicFileService musicFileService;

    public ModelAndView loadPlaylist(HttpServletRequest request, HttpServletResponse response) {
        return loadOrAppendPlaylist(request, true);
    }

    public ModelAndView appendPlaylist(HttpServletRequest request, HttpServletResponse response) {
        return loadOrAppendPlaylist(request, false);
    }

    private ModelAndView loadOrAppendPlaylist(HttpServletRequest request, boolean load) {
        Map<String, Object> map = new HashMap<String, Object>();
        List<String> playlistNames = new ArrayList<String>();

        if (playlistService.getPlaylistDirectory().exists()) {
            File[] playlists = playlistService.getSavedPlaylists();
            for (File file : playlists) {
                playlistNames.add(file.getName());
            }
        }

        map.put("load", load);
        map.put("player", request.getParameter("player"));
        map.put("dir", request.getParameter("dir"));
        map.put("indexes", ServletRequestUtils.getIntParameters(request, "i"));
        map.put("playlistDirectory", playlistService.getPlaylistDirectory());
        map.put("playlistDirectoryExists", playlistService.getPlaylistDirectory().exists());
        map.put("playlists", playlistNames);
        map.put("user", securityService.getCurrentUser(request));
        return new ModelAndView("loadPlaylist", "model", map);
    }

    public ModelAndView loadPlaylistConfirm(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Player player = playerService.getPlayer(request, response);
        Playlist playlist = player.getPlaylist();

        String name = request.getParameter("name");
        playlistService.loadPlaylist(playlist, name);

        return reload(null);
    }

    public ModelAndView appendPlaylistConfirm(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Load the existing playlist.
        Playlist savedPlaylist = new Playlist();
        String name = request.getParameter("name");
        playlistService.loadPlaylist(savedPlaylist, name);

        // Update the existing playlist with new entries.
        List<MusicFile> files = getFilesToAppend(request, response);
        for (MusicFile file : files) {
            savedPlaylist.addFiles(true, file);
        }

        // Save the playlist again.
        playlistService.savePlaylist(savedPlaylist);

        String dir = StringUtils.trimToNull(request.getParameter("dir"));
        return reload(dir);
    }

    private List<MusicFile> getFilesToAppend(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String dir = StringUtils.trimToNull(request.getParameter("dir"));
        String playerId = StringUtils.trimToNull(request.getParameter("player"));
        int[] indexes = ServletRequestUtils.getIntParameters(request, "i");
        List<MusicFile> files = new ArrayList<MusicFile>();

        if (playerId != null) {
            Player player = playerService.getPlayerById(playerId);
            Playlist playlist = player.getPlaylist();
            for (int index : indexes) {
                MusicFile file = playlist.getFile(index);
                files.add(file);
            }
        } else if (dir != null) {
            List<MusicFile> children = musicFileService.getMusicFile(dir).getChildren(true, true, true);
            for (int index : indexes) {
                files.add(children.get(index));
            }
        }

        return files;
    }

    private ModelAndView reload(String dir) {
        List<ReloadFrame> reloadFrames = new ArrayList<ReloadFrame>();
        reloadFrames.add(new ReloadFrame("playlist", "playlist.view?"));

        if (dir == null) {
            reloadFrames.add(new ReloadFrame("main", "nowPlaying.view?"));
        } else {
            reloadFrames.add(new ReloadFrame("main", "main.view?pathUtf8Hex=" + StringUtil.utf8HexEncode(dir)));
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("reloadFrames", reloadFrames);

        return new ModelAndView("reload", "model", map);
    }

    public ModelAndView deletePlaylist(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        playlistService.deletePlaylist(name);

        return new ModelAndView(new RedirectView("loadPlaylist.view?"));
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }
}
