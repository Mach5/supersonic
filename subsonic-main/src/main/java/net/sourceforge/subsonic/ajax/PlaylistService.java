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
package net.sourceforge.subsonic.ajax;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContextFactory;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.Playlist;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.SecurityService;

/**
 * Provides AJAX-enabled services for manipulating playlists.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class PlaylistService {

    private MediaFileService mediaFileService;
    private SecurityService securityService;
    private net.sourceforge.subsonic.service.PlaylistService playlistService;

    /**
     * Returns the playlist with the given ID.
     */
    public PlaylistInfo getPlaylist(int id) throws Exception {
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();

        Playlist playlist = playlistService.getPlaylist(id);
        List<MediaFile> files = playlistService.getFilesInPlaylist(id);

        String username = securityService.getCurrentUsername(request);
        mediaFileService.populateStarredDate(files, username);
        return new PlaylistInfo(playlist, createEntries(files));
    }

    private List<PlaylistInfo.Entry> createEntries(List<MediaFile> files) {
        List<PlaylistInfo.Entry> result = new ArrayList<PlaylistInfo.Entry>();
        for (MediaFile file : files) {
            result.add(new PlaylistInfo.Entry(file.getId(), file.getTitle(), file.getArtist(), file.getAlbumName(),
                    file.getDurationString(), file.getStarredDate() != null));
        }

        return result;
    }

    public void setPlaylistService(net.sourceforge.subsonic.service.PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}