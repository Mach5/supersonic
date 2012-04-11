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

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.dao.PlaylistDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.Playlist;

import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides services for loading and saving playlists to and from persistent storage.
 *
 * @author Sindre Mehus
 * @see net.sourceforge.subsonic.domain.PlayQueue
 */
public class PlaylistService {

    private static final Logger LOG = Logger.getLogger(PlaylistService.class);
    private MediaFileDao mediaFileDao;
    private PlaylistDao playlistDao;

    public List<Playlist> getPlaylistsForUser(String username) {
        return playlistDao.getPlaylistsForUser(username);
    }

    public Playlist getPlaylist(int id) {
        return playlistDao.getPlaylist(id);
    }

    public List<MediaFile> getFilesInPlaylist(int id) {
        return mediaFileDao.getFilesInPlaylist(id);
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        playlistDao.setFilesInPlaylist(id, files);
    }

    public void createPlaylist(Playlist playlist) {
        playlistDao.createPlaylist(playlist);
    }

    public boolean isReadAllowed(Playlist playlist, String username) {
        if (username == null) {
            return false;
        }
        if (username.equals(playlist.getUsername()) || playlist.isPublic()) {
            return true;
        }
        return playlistDao.getPlaylistUsers(playlist.getId()).contains(username);
    }

    public boolean isWriteAllowed(Playlist playlist, String username) {
        return username != null && username.equals(playlist.getUsername());
    }

    public void deletePlaylist(int id) {
        playlistDao.deletePlaylist(id);
    }

    public void setPlaylistDao(PlaylistDao playlistDao) {
        this.playlistDao = playlistDao;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    private static class PlaylistFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.endsWith(".m3u") || name.endsWith(".pls") || name.endsWith(".xspf");
        }
    }

    /**
     * Abstract superclass for playlist formats.
     */
    private abstract static class PlaylistFormat {
        public abstract void loadPlaylist(PlayQueue playQueue, BufferedReader reader, MediaFileService mediaFileService) throws IOException;

        public abstract void savePlaylist(PlayQueue playQueue, PrintWriter writer) throws IOException;

        public static PlaylistFormat getPlaylistFormat(File file) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".m3u")) {
                return new M3UFormat();
            }
            if (name.endsWith(".pls")) {
                return new PLSFormat();
            }
            if (name.endsWith(".xspf")) {
                return new XSPFFormat();
            }
            return null;
        }
    }

    /**
     * Implementation of M3U playlist format.
     */
    private static class M3UFormat extends PlaylistFormat {
        public void loadPlaylist(PlayQueue playQueue, BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            playQueue.clear();
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("#")) {
                    try {
                        MediaFile file = mediaFileService.getMediaFile(line);
                        if (file.getFile().exists()) {
                            playQueue.addFiles(true, file);
                        }
                    } catch (SecurityException x) {
                        LOG.warn(x.getMessage(), x);
                    }
                }
                line = reader.readLine();
            }
        }

        public void savePlaylist(PlayQueue playQueue, PrintWriter writer) throws IOException {
            writer.println("#EXTM3U");
            for (MediaFile file : playQueue.getFiles()) {
                writer.println(file.getPath());
            }
            if (writer.checkError()) {
                throw new IOException("Error when writing playlist");
            }
        }
    }

    /**
     * Implementation of PLS playlist format.
     */
    private static class PLSFormat extends PlaylistFormat {
        public void loadPlaylist(PlayQueue playQueue, BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            playQueue.clear();

            Pattern pattern = Pattern.compile("^File\\d+=(.*)$");
            String line = reader.readLine();
            while (line != null) {

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    try {
                        MediaFile file = mediaFileService.getMediaFile(matcher.group(1));
                        if (file.getFile().exists()) {
                            playQueue.addFiles(true, file);
                        }
                    } catch (SecurityException x) {
                        LOG.warn(x.getMessage(), x);
                    }
                }
                line = reader.readLine();
            }
        }

        public void savePlaylist(PlayQueue playQueue, PrintWriter writer) throws IOException {
            writer.println("[playlist]");
            int counter = 0;

            for (MediaFile file : playQueue.getFiles()) {
                counter++;
                writer.println("File" + counter + '=' + file.getPath());
            }
            writer.println("NumberOfEntries=" + counter);
            writer.println("Version=2");

            if (writer.checkError()) {
                throw new IOException("Error when writing playlist.");
            }
        }
    }

    /**
     * Implementation of XSPF (http://www.xspf.org/) playlist format.
     */
    private static class XSPFFormat extends PlaylistFormat {
        public void loadPlaylist(PlayQueue playQueue, BufferedReader reader, MediaFileService mediaFileService) throws IOException {
            playQueue.clear();

            SAXBuilder builder = new SAXBuilder();
            Document document;
            try {
                document = builder.build(reader);
            } catch (JDOMException x) {
                LOG.warn("Failed to parse XSPF playlist.", x);
                throw new IOException("Failed to parse XSPF playlist.");
            }

            Element root = document.getRootElement();
            Namespace ns = root.getNamespace();
            Element trackList = root.getChild("trackList", ns);
            List<?> tracks = trackList.getChildren("track", ns);

            for (Object obj : tracks) {
                Element track = (Element) obj;
                String location = track.getChildText("location", ns);
                if (location != null && location.startsWith("file://")) {
                    location = location.replaceFirst("file://", "");
                    try {
                        MediaFile file = mediaFileService.getMediaFile(location);
                        if (file.getFile().exists()) {
                            playQueue.addFiles(true, file);
                        }
                    } catch (SecurityException x) {
                        LOG.warn(x.getMessage(), x);
                    }
                }
            }
        }

        public void savePlaylist(PlayQueue playQueue, PrintWriter writer) throws IOException {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">");
            writer.println("    <trackList>");

            for (MediaFile file : playQueue.getFiles()) {
                writer.println("        <track><location>file://" + StringEscapeUtils.escapeXml(file.getPath()) + "</location></track>");
            }
            writer.println("    </trackList>");
            writer.println("</playlist>");

            if (writer.checkError()) {
                throw new IOException("Error when writing playlist.");
            }
        }
    }
}
