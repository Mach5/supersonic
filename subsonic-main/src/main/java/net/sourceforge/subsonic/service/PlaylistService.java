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
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.StringUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private SettingsService settingsService;
    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;
    private PlaylistDao playlistDao;

    public List<Playlist> getPlaylistsForUser(String username) {
        return playlistDao.getPlaylistsForUser(username);
    }

    public Playlist getPlaylist(int id) {
        return playlistDao.getPlaylist(id);
    }

    public List<MediaFile> getSongsInPlaylist(int id) {
        return mediaFileDao.getSongsInPlaylist(id);
    }

    public void setSongsInPlaylist(int id, List<MediaFile> songs) {
        playlistDao.setSongsInPlaylist(id, songs);
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

    /**
     * Saves the given playlist to persistent storage.
     *
     * @param playQueue The playlist to save.
     * @throws IOException If an I/O error occurs.
     */
    @Deprecated
    public void savePlaylist(PlayQueue playQueue) throws IOException {
        String name = playQueue.getName();

        // Add m3u suffix if no other suitable suffix is given.
        if (!new PlaylistFilenameFilter().accept(getPlaylistDirectory(), name)) {
            name += ".m3u";
            playQueue.setName(name);
        }

        File playlistFile = new File(getPlaylistDirectory(), name);
        checkAccess(playlistFile);

        PrintWriter writer = new PrintWriter(playlistFile, StringUtil.ENCODING_UTF8);

        try {
            PlaylistFormat format = PlaylistFormat.getPlaylistFormat(playlistFile);
            format.savePlaylist(playQueue, writer);
        } finally {
            writer.close();
        }
    }

    /**
     * Loads a named playlist from persistent storage and into the provided playlist instance.
     *
     * @param playQueue The playlist to populate. Any existing entries in the playlist will
     *                 be removed.
     * @param name     The name of a previously persisted playlist.
     * @throws IOException If an I/O error occurs.
     */
    @Deprecated
    public void loadPlaylist(PlayQueue playQueue, String name) throws IOException {
        File playlistFile = new File(getPlaylistDirectory(), name);
        checkAccess(playlistFile);

        playQueue.setName(name);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(playlistFile), StringUtil.ENCODING_UTF8));
        try {
            PlaylistFormat format = PlaylistFormat.getPlaylistFormat(playlistFile);
            format.loadPlaylist(playQueue, reader, mediaFileService);
        } finally {
            reader.close();
        }
    }

    /**
     * Returns a list of all previously saved playlists.
     *
     * @return A list of all previously saved playlists.
     */
    @Deprecated
    public File[] getSavedPlaylists() {
        return FileUtil.listFiles(getPlaylistDirectory(), new PlaylistFilenameFilter(), true);
    }

    /**
     * Returns the saved playlist with the given name.
     *
     * @param name The name of the playlist.
     * @return The playlist, or <code>null</code> if not found.
     */
    @Deprecated
    public File getSavedPlaylist(String name) {
        for (File file : getSavedPlaylists()) {
            if (name.equals(file.getName())) {
                return file;
            }
        }
        return null;
    }

    /**
     * Deletes the named playlist from persistent storage.
     *
     * @param name The name of the playlist to delete.
     * @throws IOException If an I/O error occurs.
     */
    @Deprecated
    public void deletePlaylist(String name) throws IOException {
        File file = new File(getPlaylistDirectory(), name);
        checkAccess(file);
        file.delete();
    }

    /**
     * Returns the directory where playlists are stored.
     *
     * @return The directory where playlists are stored.
     */
    @Deprecated
    public File getPlaylistDirectory() {
        return new File(settingsService.getPlaylistFolder());
    }

    private void checkAccess(File file) {
        if (!securityService.isWriteAllowed(file)) {
            throw new SecurityException("Access denied to file " + file);
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
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
                throw new IOException("Error when writing playlist " + playQueue.getName());
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
                throw new IOException("Error when writing playlist " + playQueue.getName());
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
                throw new IOException("Failed to parse XSPF playlist " + playQueue.getName());
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
                throw new IOException("Error when writing playlist " + playQueue.getName());
            }
        }
    }
}
