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
package net.sourceforge.subsonic.service;

import net.sourceforge.subsonic.dao.*;
import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.util.FileUtil;

import java.util.*;
import java.io.File;

/**
 * Provides services for user rating and comments, as well
 * as details about how often and how recent albums have been played.
 *
 * @author Sindre Mehus
 */
public class MusicInfoService {

    private MusicFileInfoDao musicFileInfoDao;
    private MusicFileService musicFileService;
    private SecurityService securityService;

    /**
     * Returns music file info for the given path.
     *
     * @return Music file info for the given path, or <code>null</code> if not found.
     */
    public MusicFileInfo getMusicFileInfoForPath(String path) {
        return musicFileInfoDao.getMusicFileInfoForPath(path);
    }

    /**
     * Returns all music file infos with respect to the given row offset and count.
     * Disabled instances are also returned.
     *
     * @param offset Number of rows to skip.
     * @param count  Maximum number of rows to return.
     * @return Music file infos with respect to the given row offset and count.
     */
    public List<MusicFileInfo> getAllMusicFileInfos(int offset, int count) {
        return musicFileInfoDao.getAllMusicFileInfos(offset, count);
    }

    /**
     * Returns the highest rated music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of files to return.
     * @return The highest rated music files.
     */
    public List<MusicFile> getHighestRated(int offset, int count) {
        List<String> highestRated = musicFileInfoDao.getHighestRated(offset, count);
        List<MusicFile> result = new ArrayList<MusicFile>();
        for (String path : highestRated) {
            File file = new File(path);
            if (FileUtil.exists(file) && securityService.isReadAllowed(file)) {
                result.add(musicFileService.getMusicFile(path));
            }
        }
        return result;
    }

    /**
     * Returns info for the most frequently played music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return Info for the most frequently played music files.
     */
    public List<MusicFileInfo> getMostFrequentlyPlayed(int offset, int count) {
        return musicFileInfoDao.getMostFrequentlyPlayed(offset, count);
    }

    /**
     * Returns info for the most recently played music files.
     *
     * @param offset Number of files to skip.
     * @param count  Maximum number of elements to return.
     * @return Info for the most recently played music files.
     */
    public List<MusicFileInfo> getMostRecentlyPlayed(int offset, int count) {
        return musicFileInfoDao.getMostRecentlyPlayed(offset, count);
    }

    /**
     * Creates a new music file info.
     *
     * @param info The music file info to create.
     */
    public void createMusicFileInfo(MusicFileInfo info) {
        musicFileInfoDao.createMusicFileInfo(info);
    }

    /**
     * Updates the given music file info.
     *
     * @param info The music file info to update.
     */
    public void updateMusicFileInfo(MusicFileInfo info) {
        musicFileInfoDao.updateMusicFileInfo(info);
    }

    /**
     * Increments the play count and last played date for the given music file.
     *
     * @param file The music file.
     */
    public void incrementPlayCount(MusicFile file) {
        MusicFileInfo info = getMusicFileInfoForPath(file.getPath());
        if (info == null) {
            info = new MusicFileInfo(file.getPath());
            info.setLastPlayed(new Date());
            info.setPlayCount(1);
            createMusicFileInfo(info);
        } else {
            info.setLastPlayed(new Date());
            info.setPlayCount(info.getPlayCount() + 1);
            updateMusicFileInfo(info);
        }
    }

    /**
     * Sets the rating for a music file and a given user.
     *
     * @param username  The user name.
     * @param musicFile The music file.
     * @param rating    The rating between 1 and 5, or <code>null</code> to remove the rating.
     */
    public void setRatingForUser(String username, MusicFile musicFile, Integer rating) {
        musicFileInfoDao.setRatingForUser(username, musicFile, rating);
    }

    /**
     * Returns the average rating for the given music file.
     *
     * @param musicFile The music file.
     * @return The average rating, or <code>null</code> if no ratings are set.
     */
    public Double getAverageRating(MusicFile musicFile) {
        return musicFileInfoDao.getAverageRating(musicFile);
    }

    /**
     * Returns the rating for the given user and music file.
     *
     * @param username  The user name.
     * @param musicFile The music file.
     * @return The rating, or <code>null</code> if no rating is set.
     */
    public Integer getRatingForUser(String username, MusicFile musicFile) {
        return musicFileInfoDao.getRatingForUser(username, musicFile);
    }

    public void setMusicFileInfoDao(MusicFileInfoDao musicFileInfoDao) {
        this.musicFileInfoDao = musicFileInfoDao;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
