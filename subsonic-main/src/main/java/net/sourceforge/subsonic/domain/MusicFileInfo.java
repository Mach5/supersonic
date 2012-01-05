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

import java.util.Date;

/**
 * Contains information about a {@link MediaFile}, including user rating and comments, as well
 * as details about how often and how recent the file has been played.
 *
 * @author Sindre Mehus
 */
public class MusicFileInfo {
    private Integer id;
    private String path;
    private String comment;
    private int playCount;
    private Date lastPlayed;
    private boolean enabled;

    public MusicFileInfo(String path) {
        this(null, path, null, 0, null);
    }

    public MusicFileInfo(Integer id, String path, String comment, int playCount, Date lastPlayed) {
        this(id, path, comment, playCount, lastPlayed, true);
    }

    public MusicFileInfo(Integer id, String path, String comment, int playCount, Date lastPlayed, boolean enabled) {
        this.id = id;
        this.path = path;
        this.comment = comment;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.enabled = enabled;
    }

    public Integer getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}