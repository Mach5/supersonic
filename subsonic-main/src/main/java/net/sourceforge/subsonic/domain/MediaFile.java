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

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.service.ServiceLocator;
import net.sourceforge.subsonic.service.metadata.MetaData;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * A media file (audio, video or directory) with an assortment of its meta data.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class MediaFile {

    private static final Logger LOG = Logger.getLogger(MediaFile.class);

    private int id;
    private String path;
    private MediaType mediaType;
    private String format;
    private boolean isDirectory;
    private boolean isAlbum;
    private String title;
    private String albumName;
    private String artist;
    private Integer discNumber;
    private Integer trackNumber;
    private Integer year;
    private String genre;
    private Integer bitRate;
    private boolean variableBitRate;
    private Integer durationSeconds;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String coverArtPath;
    private File coverArtFile;
    private String parentPath;
    private Integer playCount;
    private Date lastPlayed;
    private String comment;
    private Date created;
    private Date lastModified;
    private Date childrenLastUpdated;
    private boolean enabled;
    private String name;
    private File file;

    public MediaFile(int id, String path, MediaType mediaType, String format, boolean isDirectory, boolean isAlbum, String title,
                     String albumName, String artist, Integer discNumber, Integer trackNumber, Integer year, String genre, Integer bitRate,
                     boolean variableBitRate, Integer durationSeconds, Long fileSize, Integer width, Integer height, String coverArtPath,
                     String parentPath, Integer playCount, Date lastPlayed, String comment, Date created, Date lastModified,
                     Date childrenLastUpdated, boolean enabled) {
        this.id = id;
        this.path = path;
        this.mediaType = mediaType;
        this.format = format;
        this.isDirectory = isDirectory;
        this.isAlbum = isAlbum;
        this.title = title;
        this.albumName = albumName;
        this.artist = artist;
        this.discNumber = discNumber;
        this.trackNumber = trackNumber;
        this.year = year;
        this.genre = genre;
        this.bitRate = bitRate;
        this.variableBitRate = variableBitRate;
        this.durationSeconds = durationSeconds;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.coverArtPath = coverArtPath;
        this.parentPath = parentPath;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.lastModified = lastModified;
        this.childrenLastUpdated = childrenLastUpdated;
        this.enabled = enabled;
        file = new File(path);
        coverArtFile = coverArtPath == null ? null : new File(coverArtPath);
        
        if (isAlbum) {
            name = albumName;
        } else if (isDirectory) {
            name = file.getName();
        } else {
            name = title;
        }
    }

    /**
     * For testing only.
     */
    public MediaFile() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getFile() {
        return file;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public boolean isVideo() {
        return mediaType == MediaType.VIDEO;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public boolean isFile() {
        return !isDirectory;
    }

    public boolean isAlbum() {
        return isAlbum;
    }

    public void setAlbum(boolean album) {
        isAlbum = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String album) {
        this.albumName = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getName() {
        return name;
    }


    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isVariableBitRate() {
        return variableBitRate;
    }

    public void setVariableBitRate(boolean variableBitRate) {
        this.variableBitRate = variableBitRate;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDurationString() {
        if (durationSeconds == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(8);

        int seconds = durationSeconds;

        int hours = seconds / 3600;
        seconds -= hours * 3600;

        int minutes = seconds / 60;
        seconds -= minutes * 60;

        if (hours > 0) {
            result.append(hours).append(':');
            if (minutes < 10) {
                result.append('0');
            }
        }

        result.append(minutes).append(':');
        if (seconds < 10) {
            result.append('0');
        }
        result.append(seconds);

        return result.toString();
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getCoverArtPath() {
        return coverArtPath;
    }

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }


    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public File getParentFile() {
        return file.getParentFile();
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns when the children was last updated in the database.
     */
    public Date getChildrenLastUpdated() {
        return childrenLastUpdated;
    }

    public void setChildrenLastUpdated(Date childrenLastUpdated) {
        this.childrenLastUpdated = childrenLastUpdated;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MediaFile && ((MediaFile) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Deprecated
    public static MediaFile forMusicFile(MusicFile musicFile, String coverArtPath) {

        MediaType mediaType = null;
        if (musicFile.isVideo()) {
            mediaType = MediaType.VIDEO;
        } else if (musicFile.isFile()) {
            mediaType = MediaType.AUDIO;  // TODO: is this correct?
        }

        MetaData metaData = musicFile.getMetaData();

        boolean isAlbum = false;

        try {
            isAlbum = musicFile.isAlbum();
        } catch (IOException x) {
            LOG.error("Error in isAlbum()", x);
        }

        String album = metaData == null ? musicFile.getName() : metaData.getAlbumName();

        return new MediaFile(0,
                musicFile.getPath(),
                mediaType,
                musicFile.isFile() ? StringUtils.lowerCase(musicFile.getSuffix()) : null,
                musicFile.isDirectory(),
                isAlbum,
                metaData == null ? null : metaData.getTitle(),
                album,
                metaData == null ? null : metaData.getArtist(),
                metaData == null ? null : metaData.getDiscNumber(),
                metaData == null ? null : metaData.getTrackNumber(),
                metaData == null ? null : metaData.getYear(),
                metaData == null ? null : metaData.getGenre(),
                metaData == null ? null : metaData.getBitRate(),
                metaData != null && Boolean.TRUE.equals(metaData.getVariableBitRate()),
                metaData == null ? null : metaData.getDurationSeconds(),
                musicFile.isFile() ? musicFile.length() : null,
                metaData == null ? null : metaData.getWidth(),
                metaData == null ? null : metaData.getHeight(),
                coverArtPath,
                musicFile.getFile().getParent(),
                0,
                null,
                null,
                new Date(),
                new Date(musicFile.lastModified()),
                new Date(0L),
                true);
    }

    @Deprecated
    public MusicFile toMusicFile() {
        return ServiceLocator.getMusicFileService().getMusicFile(path);
    }

    public File getCoverArtFile() {
        return coverArtFile;
    }

    public boolean isRoot() {
        return parentPath == null;
    }
}
