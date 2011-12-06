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

import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.service.MusicFileService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for saving playlists.
 *
 * @author Sindre Mehus
 */
public class ChangeCoverArtController extends ParameterizableViewController {

    private MusicFileService musicFileService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String path = request.getParameter("path");
        String artist = request.getParameter("artist");
        String album = request.getParameter("album");
        MusicFile dir = musicFileService.getMusicFile(path);

        MusicFile child = dir.getFirstChild();
        if (child != null) {
            MusicFile.MetaData metaData = child.getMetaData();
            if (artist == null) {
                artist = metaData.getArtist();
            }
            if (album == null) {
                album = metaData.getAlbum();
            }

        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("path", path);
        map.put("artist", artist);
        map.put("album", album);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);

        return result;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }
}
