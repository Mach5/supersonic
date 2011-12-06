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
package net.sourceforge.subsonic.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about available cover art.
 *
 * @author Christian Nedregård
 */
public class CoverArtReport implements Serializable {
    private int totalNumberOfAlbums;
    List<MusicFile> albumsWithCover = new ArrayList<MusicFile>();
    List<MusicFile> albumsWithoutCover = new ArrayList<MusicFile>();

    public CoverArtReport(List<MusicFile> albumsWithCover, List<MusicFile> albumsWithoutCover) {
        this.albumsWithCover = albumsWithCover;
        this.albumsWithoutCover = albumsWithoutCover;
        totalNumberOfAlbums = albumsWithCover.size() + albumsWithoutCover.size();
    }

    public int getTotalNumberOfAlbums() {
        return totalNumberOfAlbums;
    }

    public List<MusicFile> getAlbumsWithCover() {
        return albumsWithCover;
    }

    public List<MusicFile> getAlbumsWithoutCover() {
        return albumsWithoutCover;
    }
}