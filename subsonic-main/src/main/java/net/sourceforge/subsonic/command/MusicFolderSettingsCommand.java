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
package net.sourceforge.subsonic.command;

import java.util.List;

import net.sourceforge.subsonic.controller.MusicFolderSettingsController;
import net.sourceforge.subsonic.controller.SearchSettingsController;
import net.sourceforge.subsonic.domain.Cache;
import net.sourceforge.subsonic.domain.MusicFolder;

/**
 * Command used in {@link MusicFolderSettingsController}.
 *
 * @author Sindre Mehus
 */
public class MusicFolderSettingsCommand {

    private String interval;
    private String hour;
    private boolean isCreatingIndex;
    private boolean fastCache;
    private List<MusicFolder> musicFolders;

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public boolean isCreatingIndex() {
        return isCreatingIndex;
    }

    public void setCreatingIndex(boolean creatingIndex) {
        isCreatingIndex = creatingIndex;
    }

    public boolean isFastCache() {
        return fastCache;
    }

    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    public void setMusicFolders(List<MusicFolder> musicFolders) {
        this.musicFolders = musicFolders;
    }

    public void setFastCache(boolean fastCache) {
        this.fastCache = fastCache;
    }

}