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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.MediaFileService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.ajax.ChatService;
import net.sourceforge.subsonic.ajax.LyricsInfo;
import net.sourceforge.subsonic.ajax.LyricsService;
import net.sourceforge.subsonic.command.UserSettingsCommand;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.MusicIndex;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.PlayerTechnology;
import net.sourceforge.subsonic.domain.Playlist;
import net.sourceforge.subsonic.domain.RandomSearchCriteria;
import net.sourceforge.subsonic.domain.SearchCriteria;
import net.sourceforge.subsonic.domain.SearchResult;
import net.sourceforge.subsonic.domain.Share;
import net.sourceforge.subsonic.domain.TranscodeScheme;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.domain.PodcastChannel;
import net.sourceforge.subsonic.domain.PodcastEpisode;
import net.sourceforge.subsonic.service.JukeboxService;
import net.sourceforge.subsonic.service.MusicInfoService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.PlaylistService;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.ShareService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.service.TranscodingService;
import net.sourceforge.subsonic.service.LuceneSearchService;
import net.sourceforge.subsonic.service.AudioScrobblerService;
import net.sourceforge.subsonic.service.PodcastService;
import net.sourceforge.subsonic.util.StringUtil;
import net.sourceforge.subsonic.util.XMLBuilder;

import static net.sourceforge.subsonic.security.RESTRequestParameterProcessingFilter.decrypt;
import static net.sourceforge.subsonic.util.XMLBuilder.Attribute;
import static net.sourceforge.subsonic.util.XMLBuilder.AttributeSet;

/**
 * Multi-controller used for the REST API.
 * <p/>
 * For documentation, please refer to api.jsp.
 *
 * @author Sindre Mehus
 */
public class RESTController extends MultiActionController {

    private static final Logger LOG = Logger.getLogger(RESTController.class);

    private SettingsService settingsService;
    private SecurityService securityService;
    private PlayerService playerService;
    private MediaFileService mediaFileService;
    private TranscodingService transcodingService;
    private DownloadController downloadController;
    private CoverArtController coverArtController;
    private UserSettingsController userSettingsController;
    private LeftController leftController;
    private HomeController homeController;
    private StatusService statusService;
    private StreamController streamController;
    private ShareService shareService;
    private SearchService searchService;
    private PlaylistService playlistService;
    private ChatService chatService;
    private LyricsService lyricsService;
    private net.sourceforge.subsonic.ajax.PlaylistService playlistControlService;
    private JukeboxService jukeboxService;
    private AudioScrobblerService audioScrobblerService;
    private PodcastService podcastService;
    private MusicInfoService musicInfoService;

    public void ping(HttpServletRequest request, HttpServletResponse response) throws Exception {
        XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
        response.getWriter().print(builder);
    }

    public void getLicense(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        String email = settingsService.getLicenseEmail();
        String key = settingsService.getLicenseCode();
        Date date = settingsService.getLicenseDate();
        boolean valid = settingsService.isLicenseValid();

        AttributeSet attributes = new AttributeSet();
        attributes.add("valid", valid);
        if (valid) {
            attributes.add("email", email);
            attributes.add("key", key);
            attributes.add("date", StringUtil.toISO8601(date));
        }

        builder.add("license", attributes, true);
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getMusicFolders(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("musicFolders", false);

        for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
            AttributeSet attributes = new AttributeSet();
            attributes.add("id", musicFolder.getId());
            if (musicFolder.getName() != null) {
                attributes.add("name", musicFolder.getName());
            }
            builder.add("musicFolder", attributes, true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getIndexes(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        long ifModifiedSince = ServletRequestUtils.getLongParameter(request, "ifModifiedSince", 0L);
        long lastModified = leftController.getLastModified(request);

        if (lastModified <= ifModifiedSince) {
            builder.endAll();
            response.getWriter().print(builder);
            return;
        }

        builder.add("indexes", "lastModified", lastModified, false);

        List<MusicFolder> musicFolders = settingsService.getAllMusicFolders();
        Integer musicFolderId = ServletRequestUtils.getIntParameter(request, "musicFolderId");
        if (musicFolderId != null) {
            for (MusicFolder musicFolder : musicFolders) {
                if (musicFolderId.equals(musicFolder.getId())) {
                    musicFolders = Arrays.asList(musicFolder);
                    break;
                }
            }
        }

        List<MediaFile> shortcuts = leftController.getShortcuts(musicFolders, settingsService.getShortcutsAsArray());
        for (MediaFile shortcut : shortcuts) {
            builder.add("shortcut", true,
                    new Attribute("name", shortcut.getName()),
                    new Attribute("id", StringUtil.utf8HexEncode(shortcut.getPath())));
        }

        SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists = leftController.getCacheEntry(musicFolders, lastModified).getIndexedArtists();

        for (Map.Entry<MusicIndex, SortedSet<MusicIndex.Artist>> entry : indexedArtists.entrySet()) {
            builder.add("index", "name", entry.getKey().getIndex(), false);

            for (MusicIndex.Artist artist : entry.getValue()) {
                for (MediaFile mediaFile : artist.getMediaFiles()) {
                    if (mediaFile.isDirectory()) {
                        builder.add("artist", true,
                                new Attribute("name", artist.getName()),
                                new Attribute("id", StringUtil.utf8HexEncode(mediaFile.getPath())));
                    }
                }
            }
            builder.end();
        }

        // Add children
        Player player = playerService.getPlayer(request, response);
        List<MediaFile> singleSongs = leftController.getSingleSongs(musicFolders);
        for (MediaFile singleSong : singleSongs) {
            builder.add("child", createAttributesForMediaFile(player, null, singleSong), true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getMusicDirectory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);

        MediaFile dir;
        try {
            String path = StringUtil.utf8HexDecode(ServletRequestUtils.getRequiredStringParameter(request, "id"));
            dir = mediaFileService.getMediaFile(path);
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
            return;
        }

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("directory", false,
                new Attribute("id", StringUtil.utf8HexEncode(dir.getPath())),
                new Attribute("name", dir.getName()));

        File coverArt = mediaFileService.getCoverArt(dir);

        for (MediaFile child : mediaFileService.getChildrenOf(dir, true, true, true)) {
            AttributeSet attributes = createAttributesForMediaFile(player, coverArt, child);
            builder.add("child", attributes, true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    @Deprecated
    public void search(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        Player player = playerService.getPlayer(request, response);

        String any = request.getParameter("any");
        String artist = request.getParameter("artist");
        String album = request.getParameter("album");
        String title = request.getParameter("title");

        StringBuilder query = new StringBuilder();
        if (any != null) {
            query.append(any).append(" ");
        }
        if (artist != null) {
            query.append(artist).append(" ");
        }
        if (album != null) {
            query.append(album).append(" ");
        }
        if (title != null) {
            query.append(title);
        }

        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(query.toString().trim());
        criteria.setCount(ServletRequestUtils.getIntParameter(request, "count", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "offset", 0));

        SearchResult result = searchService.search(criteria, LuceneSearchService.IndexType.SONG);
        builder.add("searchResult", false,
                new Attribute("offset", result.getOffset()),
                new Attribute("totalHits", result.getTotalHits()));

        for (MediaFile mediaFile : result.getMediaFiles()) {
            File coverArt = mediaFileService.getCoverArt(mediaFile);
            AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
            builder.add("match", attributes, true);
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void search2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        Player player = playerService.getPlayer(request, response);

        builder.add("searchResult2", false);

        String query = request.getParameter("query");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery(StringUtils.trimToEmpty(query));
        criteria.setCount(ServletRequestUtils.getIntParameter(request, "artistCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "artistOffset", 0));
        SearchResult artists = searchService.search(criteria, LuceneSearchService.IndexType.ARTIST);
        for (MediaFile mediaFile : artists.getMediaFiles()) {
            builder.add("artist", true,
                    new Attribute("name", mediaFile.getName()),
                    new Attribute("id", StringUtil.utf8HexEncode(mediaFile.getPath())));
        }

        criteria.setCount(ServletRequestUtils.getIntParameter(request, "albumCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "albumOffset", 0));
        SearchResult albums = searchService.search(criteria, LuceneSearchService.IndexType.ALBUM);
        for (MediaFile mediaFile : albums.getMediaFiles()) {
            AttributeSet attributes = createAttributesForMediaFile(player, null, mediaFile);
            builder.add("album", attributes, true);
        }

        criteria.setCount(ServletRequestUtils.getIntParameter(request, "songCount", 20));
        criteria.setOffset(ServletRequestUtils.getIntParameter(request, "songOffset", 0));
        SearchResult songs = searchService.search(criteria, LuceneSearchService.IndexType.SONG);
        for (MediaFile mediaFile : songs.getMediaFiles()) {
            File coverArt = mediaFileService.getCoverArt(mediaFile);
            AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
            builder.add("song", attributes, true);
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getPlaylists(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        builder.add("playlists", false);

        for (File playlist : playlistService.getSavedPlaylists()) {
            String id = StringUtil.utf8HexEncode(playlist.getName());
            String name = FilenameUtils.getBaseName(playlist.getName());
            builder.add("playlist", true, new Attribute("id", id), new Attribute("name", name));
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);

        XMLBuilder builder = createXMLBuilder(request, response, true);

        try {
            String id = StringUtil.utf8HexDecode(ServletRequestUtils.getRequiredStringParameter(request, "id"));
            File file = playlistService.getSavedPlaylist(id);
            if (file == null) {
                throw new Exception("Playlist not found.");
            }
            Playlist playlist = new Playlist();
            playlistService.loadPlaylist(playlist, id);

            builder.add("playlist", false, new Attribute("id", StringUtil.utf8HexEncode(playlist.getName())),
                    new Attribute("name", FilenameUtils.getBaseName(playlist.getName())));
            List<MediaFile> result;
            synchronized (playlist) {
                result = playlist.getFiles();
            }
            for (MediaFile mediaFile : result) {
                File coverArt = mediaFileService.getCoverArt(mediaFile);
                AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                builder.add("entry", attributes, true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void jukeboxControl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);

        User user = securityService.getCurrentUser(request);
        if (!user.isJukeboxRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to use jukebox.");
            return;
        }

        try {
            boolean returnPlaylist = false;
            String action = ServletRequestUtils.getRequiredStringParameter(request, "action");
            if ("start".equals(action)) {
                playlistControlService.doStart(request, response);
            } else if ("stop".equals(action)) {
                playlistControlService.doStop(request, response);
            } else if ("skip".equals(action)) {
                int index = ServletRequestUtils.getRequiredIntParameter(request, "index");
                int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);
                playlistControlService.doSkip(request, response, index, offset);
            } else if ("add".equals(action)) {
                String[] ids = ServletRequestUtils.getStringParameters(request, "id");
                List<String> paths = new ArrayList<String>(ids.length);
                for (String id : ids) {
                    paths.add(StringUtil.utf8HexDecode(id));
                }
                playlistControlService.doAdd(request, response, paths);
            } else if ("set".equals(action)) {
                String[] ids = ServletRequestUtils.getStringParameters(request, "id");
                List<String> paths = new ArrayList<String>(ids.length);
                for (String id : ids) {
                    paths.add(StringUtil.utf8HexDecode(id));
                }
                playlistControlService.doSet(request, response, paths);
            } else if ("clear".equals(action)) {
                playlistControlService.doClear(request, response);
            } else if ("remove".equals(action)) {
                int index = ServletRequestUtils.getRequiredIntParameter(request, "index");
                playlistControlService.doRemove(request, response, index);
            } else if ("shuffle".equals(action)) {
                playlistControlService.doShuffle(request, response);
            } else if ("setGain".equals(action)) {
                float gain = ServletRequestUtils.getRequiredFloatParameter(request, "gain");
                jukeboxService.setGain(gain);
            } else if ("get".equals(action)) {
                returnPlaylist = true;
            } else if ("status".equals(action)) {
                // No action necessary.
            } else {
                throw new Exception("Unknown jukebox action: '" + action + "'.");
            }

            XMLBuilder builder = createXMLBuilder(request, response, true);

            Player player = playerService.getPlayer(request, response);
            Player jukeboxPlayer = jukeboxService.getPlayer();
            boolean controlsJukebox = jukeboxPlayer != null && jukeboxPlayer.getId().equals(player.getId());
            Playlist playlist = player.getPlaylist();

            List<Attribute> attrs = new ArrayList<Attribute>(Arrays.asList(
                    new Attribute("currentIndex", controlsJukebox && !playlist.isEmpty() ? playlist.getIndex() : -1),
                    new Attribute("playing", controlsJukebox && !playlist.isEmpty() && playlist.getStatus() == Playlist.Status.PLAYING),
                    new Attribute("gain", jukeboxService.getGain()),
                    new Attribute("position", controlsJukebox && !playlist.isEmpty() ? jukeboxService.getPosition() : 0)));

            if (returnPlaylist) {
                builder.add("jukeboxPlaylist", attrs, false);
                List<MediaFile> result;
                synchronized (playlist) {
                    result = playlist.getFiles();
                }
                for (MediaFile mediaFile : result) {
                    File coverArt = mediaFileService.getCoverArt(mediaFile);
                    AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                    builder.add("entry", attributes, true);
                }
            } else {
                builder.add("jukeboxStatus", attrs, false);
            }

            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void createPlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);

        User user = securityService.getCurrentUser(request);
        if (!user.isPlaylistRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to create playlists.");
            return;
        }

        try {

            String playlistId = request.getParameter("playlistId");
            String name = request.getParameter("name");
            if (playlistId == null && name == null) {
                error(request, response, ErrorCode.MISSING_PARAMETER, "Playlist ID or name must be specified.");
                return;
            }

            Playlist playlist = new Playlist();
            playlist.setName(playlistId != null ? StringUtil.utf8HexDecode(playlistId) : name);

            String[] ids = ServletRequestUtils.getStringParameters(request, "songId");
            for (String id : ids) {
                playlist.addFiles(true, mediaFileService.getMediaFile(StringUtil.utf8HexDecode(id)));
            }
            playlistService.savePlaylist(playlist);

            XMLBuilder builder = createXMLBuilder(request, response, true);
            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void deletePlaylist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request, true);

        User user = securityService.getCurrentUser(request);
        if (!user.isPlaylistRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to delete playlists.");
            return;
        }

        try {
            String id = StringUtil.utf8HexDecode(ServletRequestUtils.getRequiredStringParameter(request, "id"));
            playlistService.deletePlaylist(id);

            XMLBuilder builder = createXMLBuilder(request, response, true);
            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getAlbumList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("albumList", false);

        try {
            int size = ServletRequestUtils.getIntParameter(request, "size", 10);
            int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);

            size = Math.max(0, Math.min(size, 500));
            offset = Math.max(0, Math.min(offset, 5000));

            String type = ServletRequestUtils.getRequiredStringParameter(request, "type");

            List<HomeController.Album> albums;
            if ("highest".equals(type)) {
                albums = homeController.getHighestRated(offset, size);
            } else if ("frequent".equals(type)) {
                albums = homeController.getMostFrequent(offset, size);
            } else if ("recent".equals(type)) {
                albums = homeController.getMostRecent(offset, size);
            } else if ("newest".equals(type)) {
                albums = homeController.getNewest(offset, size);
            } else {
                albums = homeController.getRandom(size);
            }

            for (HomeController.Album album : albums) {
                MediaFile mediaFile = mediaFileService.getMediaFile(album.getPath());
                File coverArt = null;
                if (album.getCoverArtPath() != null) {
                    coverArt = new File(album.getCoverArtPath());
                }
                AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                builder.add("album", attributes, true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getRandomSongs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("randomSongs", false);

        try {
            int size = ServletRequestUtils.getIntParameter(request, "size", 10);
            size = Math.max(0, Math.min(size, 500));
            String genre = ServletRequestUtils.getStringParameter(request, "genre");
            Integer fromYear = ServletRequestUtils.getIntParameter(request, "fromYear");
            Integer toYear = ServletRequestUtils.getIntParameter(request, "toYear");
            Integer musicFolderId = ServletRequestUtils.getIntParameter(request, "musicFolderId");
            RandomSearchCriteria criteria = new RandomSearchCriteria(size, genre, fromYear, toYear, musicFolderId);

            for (MediaFile mediaFile : searchService.getRandomSongs(criteria)) {
                File coverArt = mediaFileService.getCoverArt(mediaFile);
                AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                builder.add("song", attributes, true);
            }
            builder.endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getNowPlaying(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("nowPlaying", false);

        for (TransferStatus status : statusService.getAllStreamStatuses()) {

            Player player = status.getPlayer();
            File file = status.getFile();
            if (player != null && player.getUsername() != null && file != null) {

                String username = player.getUsername();
                UserSettings userSettings = settingsService.getUserSettings(username);
                if (!userSettings.isNowPlayingAllowed()) {
                    continue;
                }

                MediaFile mediaFile = mediaFileService.getMediaFile(file);
                File coverArt = mediaFileService.getCoverArt(mediaFile);

                long minutesAgo = status.getMillisSinceLastUpdate() / 1000L / 60L;
                if (minutesAgo < 60) {
                    AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                    attributes.add("username", username);
                    attributes.add("playerId", player.getId());
                    if (player.getName() != null) {
                        attributes.add("playerName", player.getName());
                    }
                    attributes.add("minutesAgo", minutesAgo);
                    builder.add("entry", attributes, true);
                }
            }
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    private AttributeSet createAttributesForMediaFile(Player player, File coverArt, MediaFile mediaFile) {
        MediaFile parent = mediaFileService.getParentOf(mediaFile);
        AttributeSet attributes = new AttributeSet();
        attributes.add("id", StringUtil.utf8HexEncode(mediaFile.getPath()));
        try {
            if (!parent.isRoot()) {
                attributes.add("parent", StringUtil.utf8HexEncode(parent.getPath()));
            }
        } catch (SecurityException x) {
            // Ignored.
        }
        attributes.add("title", mediaFile.getTitle());
        attributes.add("isDir", mediaFile.isDirectory());

        String username = player.getUsername();
        if (username != null) {
            Integer rating = musicInfoService.getRatingForUser(username, mediaFile);
            if (rating != null) {
                attributes.add("userRating", rating);
            }
            Double avgRating = musicInfoService.getAverageRating(mediaFile);
            if (avgRating != null) {
                attributes.add("averageRating", avgRating);
            }
        }

        if (mediaFile.isFile()) {
            attributes.add("album", mediaFile.getAlbumName());
            attributes.add("artist", mediaFile.getArtist());
            Integer duration = mediaFile.getDurationSeconds();
            if (duration != null) {
                attributes.add("duration", duration);
            }
            Integer bitRate = mediaFile.getBitRate();
            if (bitRate != null) {
                attributes.add("bitRate", bitRate);
            }

            Integer track = mediaFile.getTrackNumber();
            if (track != null) {
                attributes.add("track", track);
            }

            Integer year = mediaFile.getYear();
            if (year != null) {
                attributes.add("year", year);
            }

            String genre = mediaFile.getGenre();
            if (genre != null) {
                attributes.add("genre", genre);
            }

            attributes.add("size", mediaFile.getFileSize());
            String suffix = mediaFile.getFormat();
            attributes.add("suffix", suffix);
            attributes.add("contentType", StringUtil.getMimeType(suffix));
            attributes.add("isVideo", mediaFile.isVideo());

            if (coverArt != null) {
                attributes.add("coverArt", StringUtil.utf8HexEncode(coverArt.getPath()));
            }

            if (transcodingService.isTranscodingRequired(mediaFile, player)) {
                String transcodedSuffix = transcodingService.getSuffix(player, mediaFile, null);
                attributes.add("transcodedSuffix", transcodedSuffix);
                attributes.add("transcodedContentType", StringUtil.getMimeType(transcodedSuffix));
            }

            String path = getRelativePath(mediaFile);
            if (path != null) {
                attributes.add("path", path);
            }

        } else {

            File childCoverArt = mediaFileService.getCoverArt(mediaFile);
            if (childCoverArt != null) {
                attributes.add("coverArt", StringUtil.utf8HexEncode(childCoverArt.getPath()));
            }

            String artist = resolveArtist(mediaFile);
            if (artist != null) {
                attributes.add("artist", artist);
            }

        }
        return attributes;
    }

    private String resolveArtist(MediaFile file) {

        // If directory, find artist from metadata in child.
        if (file.isDirectory()) {
            file = mediaFileService.getFirstChildOf(file);
            if (file == null) {
                return null;
            }
        }
        return file.getArtist();
    }

    private String getRelativePath(MediaFile musicFile) {

        String filePath = musicFile.getPath();

        // Convert slashes.
        filePath = filePath.replace('\\', '/');

        String filePathLower = filePath.toLowerCase();

        List<MusicFolder> musicFolders = settingsService.getAllMusicFolders(false, true);
        for (MusicFolder musicFolder : musicFolders) {
            String folderPath = musicFolder.getPath().getPath();
            folderPath = folderPath.replace('\\', '/');
            String folderPathLower = folderPath.toLowerCase();

            if (filePathLower.startsWith(folderPathLower)) {
                String relativePath = filePath.substring(folderPath.length());
                return relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
            }
        }

        return null;
    }

    public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isDownloadRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to download files.");
            return null;
        }

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        long lastModified = downloadController.getLastModified(request);

        if (ifModifiedSince != -1 && lastModified != -1 && lastModified <= ifModifiedSince) {
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return null;
        }

        if (lastModified != -1) {
            response.setDateHeader("Last-Modified", lastModified);
        }

        return downloadController.handleRequest(request, response);
    }

    public ModelAndView stream(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isStreamRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to play files.");
            return null;
        }

        streamController.handleRequest(request, response);
        return null;
    }

    public void scrobble(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        Player player = playerService.getPlayer(request, response);

        if (!settingsService.getUserSettings(player.getUsername()).isLastFmEnabled()) {
            error(request, response, ErrorCode.GENERIC, "Scrobbling is not enabled for " + player.getUsername() + ".");
            return;
        }

        try {
            String path = StringUtil.utf8HexDecode(ServletRequestUtils.getRequiredStringParameter(request, "id"));
            MediaFile file = mediaFileService.getMediaFile(path);
            boolean submission = ServletRequestUtils.getBooleanParameter(request, "submission", true);
            audioScrobblerService.register(file, player.getUsername(), submission);
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
            return;
        }

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getPodcasts(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);
        XMLBuilder builder = createXMLBuilder(request, response, true);
        builder.add("podcasts", false);

        for (PodcastChannel channel : podcastService.getAllChannels()) {
            AttributeSet channelAttrs = new AttributeSet();
            channelAttrs.add("id", channel.getId());
            channelAttrs.add("url", channel.getUrl());
            channelAttrs.add("status", channel.getStatus().toString().toLowerCase());
            if (channel.getTitle() != null) {
                channelAttrs.add("title", channel.getTitle());
            }
            if (channel.getDescription() != null) {
                channelAttrs.add("description", channel.getDescription());
            }
            if (channel.getErrorMessage() != null) {
                channelAttrs.add("errorMessage", channel.getErrorMessage());
            }
            builder.add("channel", channelAttrs, false);

            List<PodcastEpisode> episodes = podcastService.getEpisodes(channel.getId(), false);
            for (PodcastEpisode episode : episodes) {
                AttributeSet episodeAttrs = new AttributeSet();

                String path = episode.getPath();
                if (path != null) {
                    MediaFile mediaFile = mediaFileService.getMediaFile(path);
                    File coverArt = mediaFileService.getCoverArt(mediaFile);
                    episodeAttrs.addAll(createAttributesForMediaFile(player, coverArt, mediaFile));
                    episodeAttrs.add("streamId", StringUtil.utf8HexEncode(mediaFile.getPath()));
                }

                episodeAttrs.add("id", episode.getId());  // Overwrites the previous "id" attribute.
                episodeAttrs.add("status", episode.getStatus().toString().toLowerCase());

                if (episode.getTitle() != null) {
                    episodeAttrs.add("title", episode.getTitle());
                }
                if (episode.getDescription() != null) {
                    episodeAttrs.add("description", episode.getDescription());
                }
                if (episode.getPublishDate() != null) {
                    episodeAttrs.add("publishDate", episode.getPublishDate());
                }

                builder.add("episode", episodeAttrs, true);
            }

            builder.end(); // <channel>
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void getShares(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);

        User user = securityService.getCurrentUser(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        builder.add("shares", false);
        for (Share share : shareService.getSharesForUser(user)) {
            builder.add("share", createAttributesForShare(share), false);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId())) {
                File coverArt = mediaFileService.getCoverArt(mediaFile);
                AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                builder.add("entry", attributes, true);
            }

            builder.end();
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void createShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        Player player = playerService.getPlayer(request, response);

        User user = securityService.getCurrentUser(request);
        if (!user.isShareRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to share media.");
            return;
        }

        if (!settingsService.isUrlRedirectionEnabled()) {
            error(request, response, ErrorCode.GENERIC, "Sharing is only supported for *.subsonic.org domain names.");
            return;
        }

        XMLBuilder builder = createXMLBuilder(request, response, true);

        try {

            List<MediaFile> files = new ArrayList<MediaFile>();
            for (String id : ServletRequestUtils.getRequiredStringParameters(request, "id")) {
                files.add(mediaFileService.getMediaFile(StringUtil.utf8HexDecode(id)));
            }

            // TODO: Update api.jsp

            Share share = shareService.createShare(request, files);
            share.setDescription(request.getParameter("description"));
            long expires = ServletRequestUtils.getLongParameter(request, "expires", 0L);
            if (expires != 0) {
                share.setExpires(new Date(expires));
            }
            shareService.updateShare(share);

            builder.add("shares", false);
            builder.add("share", createAttributesForShare(share), false);

            for (MediaFile mediaFile : shareService.getSharedFiles(share.getId())) {
                File coverArt = mediaFileService.getCoverArt(mediaFile);
                AttributeSet attributes = createAttributesForMediaFile(player, coverArt, mediaFile);
                builder.add("entry", attributes, true);
            }

            builder.endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void deleteShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            request = wrapRequest(request);
            User user = securityService.getCurrentUser(request);
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

            Share share = shareService.getShareById(id);
            if (share == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Shared media not found.");
                return;
            }
            if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Not authorized to delete shared media.");
                return;
            }

            shareService.deleteShare(id);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void updateShare(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            request = wrapRequest(request);
            User user = securityService.getCurrentUser(request);
            int id = ServletRequestUtils.getRequiredIntParameter(request, "id");

            Share share = shareService.getShareById(id);
            if (share == null) {
                error(request, response, ErrorCode.NOT_FOUND, "Shared media not found.");
                return;
            }
            if (!user.isAdminRole() && !share.getUsername().equals(user.getUsername())) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, "Not authorized to modify shared media.");
                return;
            }

            share.setDescription(request.getParameter("description"));
            String expiresString = request.getParameter("expires");
            if (expiresString != null) {
                long expires = Long.parseLong(expiresString);
                share.setExpires(expires == 0L ? null : new Date(expires));
            }
            shareService.updateShare(share);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

            } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    private List<Attribute> createAttributesForShare(Share share) {
        List<Attribute> attributes = new ArrayList<Attribute>();

        attributes.add(new Attribute("id", share.getId()));
        attributes.add(new Attribute("url", shareService.getShareUrl(share)));
        attributes.add(new Attribute("username", share.getUsername()));
        attributes.add(new Attribute("created", StringUtil.toISO8601(share.getCreated())));
        attributes.add(new Attribute("visitCount", share.getVisitCount()));
        attributes.add(new Attribute("description", share.getDescription()));
        attributes.add(new Attribute("expires", StringUtil.toISO8601(share.getExpires())));
        attributes.add(new Attribute("lastVisited", StringUtil.toISO8601(share.getLastVisited())));

        return attributes;
    }

    public ModelAndView videoPlayer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        Map<String, Object> map = new HashMap<String, Object>();
        String path = request.getParameter("path");
        MediaFile file = mediaFileService.getMediaFile(path);

        int timeOffset = ServletRequestUtils.getIntParameter(request, "timeOffset", 0);
        timeOffset = Math.max(0, timeOffset);
        Integer duration = file.getDurationSeconds();
        if (duration != null) {
            map.put("skipOffsets", VideoPlayerController.createSkipOffsets(duration));
            timeOffset = Math.min(duration, timeOffset);
            duration -= timeOffset;
        }

        map.put("id", request.getParameter("id"));
        map.put("u", request.getParameter("u"));
        map.put("p", request.getParameter("p"));
        map.put("c", request.getParameter("c"));
        map.put("v", request.getParameter("v"));
        map.put("video", file);
        map.put("maxBitRate", ServletRequestUtils.getIntParameter(request, "maxBitRate", VideoPlayerController.DEFAULT_BIT_RATE));
        map.put("duration", duration);
        map.put("timeOffset", timeOffset);
        map.put("bitRates", VideoPlayerController.BIT_RATES);
        map.put("autoplay", ServletRequestUtils.getBooleanParameter(request, "autoplay", true));

        ModelAndView result = new ModelAndView("rest/videoPlayer");
        result.addObject("model", map);
        return result;
    }

    public ModelAndView getCoverArt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        return coverArtController.handleRequest(request, response);
    }

    public void changePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        try {

            String username = ServletRequestUtils.getRequiredStringParameter(request, "username");
            String password = decrypt(ServletRequestUtils.getRequiredStringParameter(request, "password"));

            User authUser = securityService.getCurrentUser(request);
            if (!authUser.isAdminRole() && !username.equals(authUser.getUsername())) {
                error(request, response, ErrorCode.NOT_AUTHORIZED, authUser.getUsername() + " is not authorized to change password for " + username);
                return;
            }

            User user = securityService.getUserByName(username);
            user.setPassword(password);
            securityService.updateUser(user);

            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);

        String username;
        try {
            username = ServletRequestUtils.getRequiredStringParameter(request, "username");
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
            return;
        }

        User currentUser = securityService.getCurrentUser(request);
        if (!username.equals(currentUser.getUsername()) && !currentUser.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, currentUser.getUsername() + " is not authorized to get details for other users.");
            return;
        }

        User requestedUser = securityService.getUserByName(username);
        if (requestedUser == null) {
            error(request, response, ErrorCode.NOT_FOUND, "No such user: " + username);
            return;
        }

        UserSettings userSettings = settingsService.getUserSettings(username);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        List<Attribute> attributes = Arrays.asList(
                new Attribute("username", requestedUser.getUsername()),
                new Attribute("email", requestedUser.getEmail()),
                new Attribute("scrobblingEnabled", userSettings.isLastFmEnabled()),
                new Attribute("adminRole", requestedUser.isAdminRole()),
                new Attribute("settingsRole", requestedUser.isSettingsRole()),
                new Attribute("downloadRole", requestedUser.isDownloadRole()),
                new Attribute("uploadRole", requestedUser.isUploadRole()),
                new Attribute("playlistRole", requestedUser.isPlaylistRole()),
                new Attribute("coverArtRole", requestedUser.isCoverArtRole()),
                new Attribute("commentRole", requestedUser.isCommentRole()),
                new Attribute("podcastRole", requestedUser.isPodcastRole()),
                new Attribute("streamRole", requestedUser.isStreamRole()),
                new Attribute("jukeboxRole", requestedUser.isJukeboxRole()),
                new Attribute("shareRole", requestedUser.isShareRole())
        );

        builder.add("user", attributes, true);
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void createUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to create new users.");
            return;
        }

        try {
            UserSettingsCommand command = new UserSettingsCommand();
            command.setUsername(ServletRequestUtils.getRequiredStringParameter(request, "username"));
            command.setPassword(decrypt(ServletRequestUtils.getRequiredStringParameter(request, "password")));
            command.setEmail(ServletRequestUtils.getRequiredStringParameter(request, "email"));
            command.setLdapAuthenticated(ServletRequestUtils.getBooleanParameter(request, "ldapAuthenticated", false));
            command.setAdminRole(ServletRequestUtils.getBooleanParameter(request, "adminRole", false));
            command.setCommentRole(ServletRequestUtils.getBooleanParameter(request, "commentRole", false));
            command.setCoverArtRole(ServletRequestUtils.getBooleanParameter(request, "coverArtRole", false));
            command.setDownloadRole(ServletRequestUtils.getBooleanParameter(request, "downloadRole", false));
            command.setStreamRole(ServletRequestUtils.getBooleanParameter(request, "streamRole", true));
            command.setUploadRole(ServletRequestUtils.getBooleanParameter(request, "uploadRole", false));
            command.setJukeboxRole(ServletRequestUtils.getBooleanParameter(request, "jukeboxRole", false));
            command.setPlaylistRole(ServletRequestUtils.getBooleanParameter(request, "playlistRole", false));
            command.setPodcastRole(ServletRequestUtils.getBooleanParameter(request, "podcastRole", false));
            command.setSettingsRole(ServletRequestUtils.getBooleanParameter(request, "settingsRole", true));
            command.setTranscodeSchemeName(ServletRequestUtils.getStringParameter(request, "transcodeScheme", TranscodeScheme.OFF.name()));
            command.setShareRole(ServletRequestUtils.getBooleanParameter(request, "shareRole", false));

            userSettingsController.createUser(command);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void deleteUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        User user = securityService.getCurrentUser(request);
        if (!user.isAdminRole()) {
            error(request, response, ErrorCode.NOT_AUTHORIZED, user.getUsername() + " is not authorized to delete users.");
            return;
        }

        try {
            String username = ServletRequestUtils.getRequiredStringParameter(request, "username");
            securityService.deleteUser(username);

            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);

        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        } catch (Exception x) {
            LOG.warn("Error in REST API.", x);
            error(request, response, ErrorCode.GENERIC, getErrorMessage(x));
        }
    }

    public void getChatMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        XMLBuilder builder = createXMLBuilder(request, response, true);

        long since = ServletRequestUtils.getLongParameter(request, "since", 0L);

        builder.add("chatMessages", false);

        for (ChatService.Message message : chatService.getMessages(0L).getMessages()) {
            long time = message.getDate().getTime();
            if (time > since) {
                builder.add("chatMessage", true, new Attribute("username", message.getUsername()),
                        new Attribute("time", time), new Attribute("message", message.getContent()));
            }
        }
        builder.endAll();
        response.getWriter().print(builder);
    }

    public void addChatMessage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        try {
            chatService.doAddMessage(ServletRequestUtils.getRequiredStringParameter(request, "message"), request);
            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        }
    }

    public void getLyrics(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        String artist = request.getParameter("artist");
        String title = request.getParameter("title");
        LyricsInfo lyrics = lyricsService.getLyrics(artist, title);

        XMLBuilder builder = createXMLBuilder(request, response, true);
        AttributeSet attributes = new AttributeSet();
        if (lyrics.getArtist() != null) {
            attributes.add("artist", lyrics.getArtist());
        }
        if (lyrics.getTitle() != null) {
            attributes.add("title", lyrics.getTitle());
        }
        builder.add("lyrics", attributes, lyrics.getLyrics(), true);

        builder.endAll();
        response.getWriter().print(builder);
    }

    public void setRating(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request = wrapRequest(request);
        try {
            Integer rating = ServletRequestUtils.getRequiredIntParameter(request, "rating");
            if (rating == 0) {
                rating = null;
            }

            String path = StringUtil.utf8HexDecode(ServletRequestUtils.getRequiredStringParameter(request, "id"));
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            String username = securityService.getCurrentUsername(request);
            musicInfoService.setRatingForUser(username, mediaFile, rating);

            XMLBuilder builder = createXMLBuilder(request, response, true).endAll();
            response.getWriter().print(builder);
        } catch (ServletRequestBindingException x) {
            error(request, response, ErrorCode.MISSING_PARAMETER, getErrorMessage(x));
        }
    }

    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        return wrapRequest(request, false);
    }

    private HttpServletRequest wrapRequest(final HttpServletRequest request, boolean jukebox) {
        final String playerId = createPlayerIfNecessary(request, jukebox);
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {

                // Renames "id" request parameter to "path".
                if ("path".equals(name)) {
                    try {
                        return StringUtil.utf8HexDecode(request.getParameter("id"));
                    } catch (Exception e) {
                        return null;
                    }
                }

                // Returns the correct player to be used in PlayerService.getPlayer()
                else if ("player".equals(name)) {
                    return playerId;
                }

                return super.getParameter(name);
            }
        };
    }

    private String getErrorMessage(Exception x) {
        if (x.getMessage() != null) {
            return x.getMessage();
        }
        return x.getClass().getSimpleName();
    }

    private void error(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message) throws IOException {
        XMLBuilder builder = createXMLBuilder(request, response, false);
        builder.add("error", true,
                new XMLBuilder.Attribute("code", code.getCode()),
                new XMLBuilder.Attribute("message", message));
        builder.end();
        response.getWriter().print(builder);
    }

    private XMLBuilder createXMLBuilder(HttpServletRequest request, HttpServletResponse response, boolean ok) throws IOException {
        String format = ServletRequestUtils.getStringParameter(request, "f", "xml");
        boolean json = "json".equals(format);
        boolean jsonp = "jsonp".equals(format);
        XMLBuilder builder;

        response.setCharacterEncoding(StringUtil.ENCODING_UTF8);

        if (json) {
            builder = XMLBuilder.createJSONBuilder();
            response.setContentType("application/json");
        } else if (jsonp) {
            builder = XMLBuilder.createJSONPBuilder(request.getParameter("callback"));
            response.setContentType("text/javascript");
        } else {
        	builder = XMLBuilder.createXMLBuilder();
            response.setContentType("text/xml");
        }
        
        builder.preamble(StringUtil.ENCODING_UTF8);
        builder.add("subsonic-response", false,
                    new Attribute("xmlns", "http://subsonic.org/restapi"),
                    new Attribute("status", ok ? "ok" : "failed"),
                    new Attribute("version", StringUtil.getRESTProtocolVersion()));
        return builder;
    }

    private String createPlayerIfNecessary(HttpServletRequest request, boolean jukebox) {
        String username = request.getRemoteUser();
        String clientId = request.getParameter("c");
        if (jukebox) {
            clientId += "-jukebox";
        }

        List<Player> players = playerService.getPlayersForUserAndClientId(username, clientId);

        // If not found, create it.
        if (players.isEmpty()) {
            Player player = new Player();
            player.setIpAddress(request.getRemoteAddr());
            player.setUsername(username);
            player.setClientId(clientId);
            player.setName(clientId);
            player.setTechnology(jukebox ? PlayerTechnology.JUKEBOX : PlayerTechnology.EXTERNAL_WITH_PLAYLIST);
            playerService.createPlayer(player);
            players = playerService.getPlayersForUserAndClientId(username, clientId);
        }

        // Return the player ID.
        return !players.isEmpty() ? players.get(0).getId() : null;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setDownloadController(DownloadController downloadController) {
        this.downloadController = downloadController;
    }

    public void setCoverArtController(CoverArtController coverArtController) {
        this.coverArtController = coverArtController;
    }

    public void setUserSettingsController(UserSettingsController userSettingsController) {
        this.userSettingsController = userSettingsController;
    }

    public void setLeftController(LeftController leftController) {
        this.leftController = leftController;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setStreamController(StreamController streamController) {
        this.streamController = streamController;
    }

    public void setChatService(ChatService chatService) {
        this.chatService = chatService;
    }

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    public void setLyricsService(LyricsService lyricsService) {
        this.lyricsService = lyricsService;
    }

    public void setPlaylistControlService(net.sourceforge.subsonic.ajax.PlaylistService playlistControlService) {
        this.playlistControlService = playlistControlService;
    }

    public void setJukeboxService(JukeboxService jukeboxService) {
        this.jukeboxService = jukeboxService;
    }

    public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
        this.audioScrobblerService = audioScrobblerService;
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    public void setMusicInfoService(MusicInfoService musicInfoService) {
        this.musicInfoService = musicInfoService;
    }

    public void setShareService(ShareService shareService) {
        this.shareService = shareService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public static enum ErrorCode {

        GENERIC(0, "A generic error."),
        MISSING_PARAMETER(10, "Required parameter is missing."),
        PROTOCOL_MISMATCH_CLIENT_TOO_OLD(20, "Incompatible Subsonic REST protocol version. Client must upgrade."),
        PROTOCOL_MISMATCH_SERVER_TOO_OLD(30, "Incompatible Subsonic REST protocol version. Server must upgrade."),
        NOT_AUTHENTICATED(40, "Wrong username or password."),
        NOT_AUTHORIZED(50, "User is not authorized for the given operation."),
        NOT_LICENSED(60, "The trial period for the Subsonic server is over. Please donate to get a license key. Visit subsonic.org for details."),
        NOT_FOUND(70, "Requested data was not found.");

        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
