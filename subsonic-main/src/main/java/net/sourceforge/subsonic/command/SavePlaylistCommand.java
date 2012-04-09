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

import net.sourceforge.subsonic.controller.*;
import net.sourceforge.subsonic.domain.*;
import org.springframework.util.*;

/**
 * Command used in {@link SavePlaylistController}.
 *
 * @author Sindre Mehus
 */
public class SavePlaylistCommand {

    private PlayQueue playQueue;
    private String name;

    public SavePlaylistCommand(PlayQueue playQueue) {
        this.playQueue = playQueue;
        name = playQueue.getName();
    }

    public PlayQueue getPlayQueue() {
        return playQueue;
    }

    public void setPlayQueue(PlayQueue playQueue) {
        this.playQueue = playQueue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}