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
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.domain.MusicFileInfo;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.RandomSearchCriteria;
import net.sourceforge.subsonic.domain.SearchCriteria;
import net.sourceforge.subsonic.domain.SearchResult;
import net.sourceforge.subsonic.util.StringUtil;

import static net.sourceforge.subsonic.service.LuceneSearchService.IndexType.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Provides services for searching for music.
 *
 * @author Sindre Mehus
 */
public class SearchService {

    private static final int INDEX_VERSION = 14;
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final Logger LOG = Logger.getLogger(SearchService.class);

    private Map<File, Line> cachedIndex;
    private List<Line> cachedSongs;
    private List<Line> cachedArtists;
    private SortedSet<Line> cachedAlbums;  // Sorted chronologically.
    private SortedSet<String> cachedGenres;
    private MediaLibraryStatistics statistics;

    private boolean creatingIndex;
    private Timer timer;
    private SettingsService settingsService;
    private SecurityService securityService;
    private MusicFileService musicFileService;
    private MusicInfoService musicInfoService;
    private LuceneSearchService luceneSearchService;

    /**
     * Returns whether the search index exists.
     *
     * @return Whether the search index exists.
     */
    private synchronized boolean isIndexCreated() {
        return getIndexFile().exists();
    }

    /**
     * Returns whether the search index is currently being created.
     *
     * @return Whether the search index is currently being created.
     */
    public synchronized boolean isIndexBeingCreated() {
        return creatingIndex;
    }

    /**
     * Generates the search index.  If the index already exists it will be
     * overwritten.  The index is created asynchronously, i.e., this method returns
     * before the index is created.
     */
    public synchronized void createIndex() {
        if (isIndexBeingCreated()) {
            return;
        }
        creatingIndex = true;

        Thread thread = new Thread("Search Index Generator") {
            @Override
            public void run() {
                doCreateIndex();
            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void doCreateIndex() {
        deleteOldIndexFiles();
        LOG.info("Starting to create search index.");
        PrintWriter writer = null;

        try {

            // Get existing index.
            Map<File, Line> oldIndex = getIndex();

            writer = new PrintWriter(getIndexFile(), StringUtil.ENCODING_UTF8);

            // Create a scanner for visiting all music files.
            Scanner scanner = new Scanner(writer, oldIndex, settingsService.getAllMusicFolders());

            // Read entire music directory.
            for (MusicFolder musicFolder : settingsService.getAllMusicFolders()) {
                MusicFile root = musicFileService.getMusicFile(musicFolder.getPath());
                root.accept(scanner);
            }

            // Clear memory cache.
            writer.flush();
            writer.close();
            synchronized (this) {
                cachedIndex = null;
                cachedSongs = null;
                cachedAlbums = null;
                cachedGenres = null;
                statistics = null;
                getIndex();
            }

            // Now, clean up music_file_info table.
            cleanMusicFileInfo();

            // Update Lucene search index.
            LOG.info("Updating Lucene search index.");
            luceneSearchService.createIndex(SONG, cachedSongs);
            luceneSearchService.createIndex(ALBUM, cachedAlbums);
            luceneSearchService.createIndex(ARTIST, cachedArtists);

            // Don't need this any longer.
            cachedArtists.clear();

            LOG.info("Created search index with " + scanner.getCount() + " entries.");

        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        } finally {
            creatingIndex = false;
            IOUtils.closeQuietly(writer);
        }
    }

    private void cleanMusicFileInfo() {

        // Create sorted set of albums.
        SortedSet<String> albums = new TreeSet<String>();
        for (Line line : cachedAlbums) {
            albums.add(line.file.getPath());
        }

        // Page through music_file_info table.
        int offset = 0;
        int count = 100;
        while (true) {
            List<MusicFileInfo> infos = musicInfoService.getAllMusicFileInfos(offset, count);
            if (infos.isEmpty()) {
                break;
            }
            offset += infos.size();

            for (MusicFileInfo info : infos) {

                // Disable row if album does not exist on disk any more.
                if (info.isEnabled() && !albums.contains(info.getPath())) {
                    info.setEnabled(false);
                    musicInfoService.updateMusicFileInfo(info);
                    LOG.debug("Logically deleting info for album " + info.getPath() + ". Not found on disk.");
                }

                // Enable row if album has reoccurred on disk.
                else if (!info.isEnabled() && albums.contains(info.getPath())) {
                    info.setEnabled(true);
                    musicInfoService.updateMusicFileInfo(info);
                    LOG.debug("Logically undeleting info for album " + info.getPath() + ". Found on disk.");
                }
            }
        }
    }

    /**
     * Schedule background execution of index creation.
     */
    public synchronized void schedule() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                createIndex();
            }
        };

        long daysBetween = settingsService.getIndexCreationInterval();
        int hour = settingsService.getIndexCreationHour();

        if (daysBetween == -1) {
            LOG.info("Automatic index creation disabled.");
            return;
        }

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTime().before(now)) {
            cal.add(Calendar.DATE, 1);
        }

        Date firstTime = cal.getTime();
        long period = daysBetween * 24L * 3600L * 1000L;
        timer.schedule(task, firstTime, period);

        LOG.info("Automatic index creation scheduled to run every " + daysBetween + " day(s), starting at " + firstTime);

        // In addition, create index immediately if it doesn't exist on disk.
        if (!isIndexCreated()) {
            LOG.info("Search index not found on disk. Creating it.");
            createIndex();
        }
    }

    /**
     * Search for music files fulfilling the given search criteria.
     *
     * @param criteria The search criteria.
     * @param indexType The search index to use.
     * @return The search result.
     * @throws IOException If an I/O error occurs.
     */
    public synchronized SearchResult search(SearchCriteria criteria, LuceneSearchService.IndexType indexType) throws IOException {

        if (!isIndexCreated() || isIndexBeingCreated()) {
            SearchResult empty = new SearchResult();
            empty.setOffset(criteria.getOffset());
            empty.setMusicFiles(Collections.<MusicFile>emptyList());
            return empty;
        }

        return luceneSearchService.search(criteria, indexType);
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     * @throws IOException If an I/O error occurs.
     */
    public MediaLibraryStatistics getStatistics() throws IOException {
        if (!isIndexCreated() || isIndexBeingCreated()) {
            return null;
        }

        // Ensure that index is read to memory.
        getIndex();
        return statistics;
    }

    /**
     * Returns a number of random songs.
     *
     * @param criteria Search criteria.
     * @return Array of random songs.
     * @throws IOException If an I/O error occurs.
     */
    public List<MusicFile> getRandomSongs(RandomSearchCriteria criteria) throws IOException {
        int count = criteria.getCount();
        List<MusicFile> result = new ArrayList<MusicFile>(count);

        if (!isIndexCreated() || isIndexBeingCreated()) {
            return result;
        }

        // Ensure that index is read to memory.
        getIndex();

        if (cachedSongs == null || cachedSongs.isEmpty()) {
            return result;
        }

        String genre = criteria.getGenre();
        Integer fromYear = criteria.getFromYear();
        Integer toYear = criteria.getToYear();
        String musicFolderPath = null;
        if (criteria.getMusicFolderId() != null) {
            MusicFolder musicFolder = settingsService.getMusicFolderById(criteria.getMusicFolderId());
            musicFolderPath = musicFolder.getPath().getPath().toUpperCase() + File.separator;
        }

        // Filter by genre, year and music folder.
        List<Line> songs = new ArrayList<Line>(cachedSongs.size());
        String fromYearString = fromYear == null ? null : String.valueOf(fromYear);
        String toYearString = toYear == null ? null : String.valueOf(toYear);

        for (Line song : cachedSongs) {

            // Skip if wrong genre.
            if (genre != null && !genre.equalsIgnoreCase(song.genre)) {
                continue;
            }

            // Skip podcasts if no genre is given.
            if (genre == null && "podcast".equalsIgnoreCase(song.genre)) {
                continue;
            }

            // Skip if wrong year.
            if (fromYearString != null) {
                if (song.year == null || song.year.compareTo(fromYearString) < 0) {
                    continue;
                }
            }
            if (toYearString != null) {
                if (song.year == null || song.year.compareTo(toYearString) > 0) {
                    continue;
                }
            }

            // Skip if wrong music folder.
            if (musicFolderPath != null) {
                String filePath = song.file.getPath().toUpperCase();
                if (!filePath.startsWith(musicFolderPath)) {
                    continue;
                }
            }

            songs.add(song);
        }

        if (songs.isEmpty()) {
            return result;
        }

        // Note: To avoid duplicates, we iterate over more than the requested number of songs.
        for (int i = 0; i < count * 10; i++) {
            int n = RANDOM.nextInt(songs.size());
            File file = songs.get(n).file;

            if (file.exists() && securityService.isReadAllowed(file)) {
                MusicFile musicFile = musicFileService.getMusicFile(file);
                if (!result.contains(musicFile) && !musicFile.isVideo()) {
                    result.add(musicFile);

                    // Enough songs found?
                    if (result.size() == count) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns all genres in the music collection.
     *
     * @return Sorted set of genres.
     * @throws IOException If an I/O error occurs.
     */
    public Set<String> getGenres() throws IOException {

        if (!isIndexCreated() || isIndexBeingCreated()) {
            return Collections.emptySet();
        }

        // Ensure that index is read to memory.
        getIndex();

        return Collections.unmodifiableSortedSet(cachedGenres);
    }

    /**
     * Returns a number of random albums.
     *
     * @param count Maximum number of albums to return.
     * @return Array of random albums.
     * @throws IOException If an I/O error occurs.
     */
    public List<MusicFile> getRandomAlbums(int count) throws IOException {
        List<MusicFile> result = new ArrayList<MusicFile>(count);

        if (!isIndexCreated() || isIndexBeingCreated()) {
            return result;
        }

        // Ensure that index is read to memory.
        getIndex();

        if (cachedSongs == null || cachedSongs.isEmpty()) {
            return result;
        }

        // Note: To avoid duplicates, we iterate over more than the requested number of items.
        for (int i = 0; i < count * 20; i++) {
            int n = RANDOM.nextInt(cachedSongs.size());
            File file = cachedSongs.get(n).file;

            if (file.exists() && securityService.isReadAllowed(file)) {
                MusicFile album = musicFileService.getMusicFile(file.getParentFile());
                if (!album.isRoot() && !result.contains(album)) {
                    result.add(album);

                    // Enough items found?
                    if (result.size() == count) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns a number of least recently modified music files. Only directories (albums) are returned.
     *
     * @param offset Number of music files to skip.
     * @param count  Maximum number of music files to return.
     * @return Array of new music files.
     * @throws IOException If an I/O error occurs.
     */
    public List<MusicFile> getNewestAlbums(int offset, int count) throws IOException {
        List<MusicFile> result = new ArrayList<MusicFile>(count);

        if (!isIndexCreated() || isIndexBeingCreated()) {
            return result;
        }

        // Ensure that index is read to memory.
        getIndex();

        int n = 0;
        for (Line line : cachedAlbums) {
            if (n == count + offset) {
                break;
            }
            if (line.file.exists() && securityService.isReadAllowed(line.file)) {
                if (n >= offset) {
                    result.add(musicFileService.getMusicFile(line.file));
                }
                n++;
            }
        }

        return result;
    }

    /**
     * Returns the creation date for the given file.
     *
     * @param musicFile The file in question.
     * @return The creation date, or {@code null} if not found.
     */
    public Date getCreationDate(MusicFile musicFile) throws IOException {
        if (!isIndexCreated() || isIndexBeingCreated()) {
            return null;
        }

        Line line = getIndex().get(musicFile.getFile());
        return line == null ? null : new Date(line.created);
    }

    /**
     * Returns the search index as a map from files to {@link Line} instances.
     *
     * @return The search index.
     * @throws IOException If an I/O error occurs.
     */
    private synchronized Map<File, Line> getIndex() throws IOException {
        if (!isIndexCreated()) {
            return new TreeMap<File, Line>();
        }

        if (cachedIndex != null) {
            return cachedIndex;
        }

        cachedIndex = new TreeMap<File, Line>();

        // Statistics.
        int songCount = 0;
        long totalLength = 0;
        Set<String> artists = new HashSet<String>();
        Set<String> albums = new HashSet<String>();

        cachedSongs = new ArrayList<Line>();
        cachedArtists = new ArrayList<Line>();
        cachedGenres = new TreeSet<String>();
        cachedAlbums = new TreeSet<Line>(new Comparator<Line>() {
            public int compare(Line line1, Line line2) {
                if (line2.created < line1.created) {
                    return -1;
                }
                if (line1.created < line2.created) {
                    return 1;
                }
                return 0;
            }
        });

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(getIndexFile()), StringUtil.ENCODING_UTF8));

        // TODO: Calculate artist/album count from cachedArtists/cachedAlbums.

        try {

            for (String s = reader.readLine(); s != null; s = reader.readLine()) {

                try {

                    Line line = Line.parse(s);
                    cachedIndex.put(line.file, line);

                    if (line.isAlbum) {
                        cachedAlbums.add(line);
                    } else if (line.isArtist) {
                        cachedArtists.add(line);
                    } else if (line.isFile) {
                        songCount++;
                        totalLength += line.length;
                        artists.add(line.artist);
                        albums.add(line.album);
                        cachedSongs.add(line);
                        if (line.genre != null) {
                            cachedGenres.add(line.genre);
                        }
                    }

                } catch (Exception x) {
                    LOG.error("An error occurred while reading index entry '" + s + "'.", x);
                }
            }
        } finally {
            reader.close();
        }

        statistics = new MediaLibraryStatistics(artists.size(), albums.size(), songCount, totalLength);

        return cachedIndex;
    }

    /**
     * Returns the file containing the index.
     *
     * @return The file containing the index.
     */
    private File getIndexFile() {
        return getIndexFile(INDEX_VERSION);
    }

    /**
     * Returns the index file for the given index version.
     *
     * @param version The index version.
     * @return The index file for the given index version.
     */
    private File getIndexFile(int version) {
        File home = SettingsService.getSubsonicHome();
        return new File(home, "subsonic" + version + ".index");
    }

    /**
     * Deletes old versions of the index file.
     */
    private void deleteOldIndexFiles() {
        for (int i = 2; i < INDEX_VERSION; i++) {
            File file = getIndexFile(i);
            try {
                if (file.exists()) {
                    if (file.delete()) {
                        LOG.info("Deleted old index file: " + file.getPath());
                    }
                }
            } catch (Exception x) {
                LOG.warn("Failed to delete old index file: " + file.getPath(), x);
            }
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMusicFileService(MusicFileService musicFileService) {
        this.musicFileService = musicFileService;
    }

    public void setMusicInfoService(MusicInfoService musicInfoService) {
        this.musicInfoService = musicInfoService;
    }

    public void setLuceneSearchService(LuceneSearchService luceneSearchService) {
        this.luceneSearchService = luceneSearchService;
    }

    /**
     * Contains the content of a single line in the index file.
     */
    static class Line {

        /**
         * Column separator.
         */
        static final String SEPARATOR = " ixYxi ";

        // TODO: Replace isFile, isAlbum, isDirectory with one char.

        public boolean isFile;
        public boolean isAlbum;
        private boolean isArtist;
        private boolean isDirectory;
        public long created;
        private long lastModified;
        public File file;
        private long length;
        public String artist;
        public String album;
        public String title;
        private String year;
        private String genre;

        private Line() {
        }

        /**
         * Creates a line instance by parsing the given string.
         *
         * @param s The string to parse.
         * @return The line created by parsing the string.
         */
        public static Line parse(String s) {
            Line line = new Line();

            String[] tokens = s.split(SEPARATOR, -1);
            line.isFile = "F".equals(tokens[0]);
            line.isArtist = "R".equals(tokens[0]);
            line.isAlbum = "A".equals(tokens[0]);
            line.isDirectory = "D".equals(tokens[0]);
            line.created = Long.parseLong(tokens[1]);
            line.lastModified = Long.parseLong(tokens[2]);
            line.file = new File(tokens[3]);
            line.artist = tokens[5].length() == 0 ? null : tokens[5];
            line.album = tokens[6].length() == 0 ? null : tokens[6];
            if (line.isFile) {
                line.length = Long.parseLong(tokens[4]);
                line.title = tokens[7].length() == 0 ? null : tokens[7];
                line.year = tokens[8].length() == 0 ? null : tokens[8];
                line.genre = tokens[9].length() == 0 ? null : tokens[9];
            }

            return line;
        }

        /**
         * Creates a line instance representing the given music file.
         *
         * @param file         The music file.
         * @param index        The existing search index. Used to avoid parsing metadata if the file has not changed
         *                     since the last time the search index was created.
         * @param musicFolders The set of configured music folders.
         * @return A line instance representing the given music file.
         */
        public static Line forFile(MusicFile file, Map<File, Line> index, Set<File> musicFolders) {
            // Look in existing index first.
            Line existingLine = index.get(file.getFile());

            // Found up-to-date line?
            if (existingLine != null && file.lastModified() == existingLine.lastModified) {
                return existingLine;
            }

            // Otherwise, construct meta data.
            Line line = new Line();

            MusicFile.MetaData metaData = file.getMetaData();
            line.isFile = file.isFile();
            line.isDirectory = file.isDirectory();
            if (line.isDirectory && !musicFolders.contains(file.getFile())) {
                try {
                    line.isAlbum = file.isAlbum();
                } catch (Exception x) {
                    LOG.warn("Failed to determine if " + file + " is an album.", x);
                }
                line.isArtist = !line.isAlbum;
            }
            line.lastModified = file.lastModified();
            line.created = existingLine != null ? existingLine.created : line.lastModified;
            line.file = file.getFile();
            if (line.isFile) {
                line.length = file.length();
                line.artist = StringUtils.upperCase(metaData.getArtist());
                line.album = StringUtils.upperCase(metaData.getAlbum());
                line.title = StringUtils.upperCase(metaData.getTitle());
                line.year = metaData.getYear();
                line.genre = StringUtils.capitalize(StringUtils.lowerCase(metaData.getGenre()));
            } else if (line.isAlbum) {
                resolveArtistAndAlbum(file, line);
            } else if (line.isArtist) {
                line.artist = StringUtils.upperCase(file.getName());
            }

            return line;
        }

        private static void resolveArtistAndAlbum(MusicFile file, Line line) {

            // If directory, find artist from metadata in child.
            if (file.isDirectory()) {
                try {
                    file = file.getFirstChild();
                } catch (IOException e) {
                    return;
                }
                if (file == null) {
                    return;
                }
            }
            line.artist = StringUtils.upperCase(file.getMetaData().getArtist());
            line.album = StringUtils.upperCase(file.getMetaData().getAlbum());
        }

        /**
         * Returns the content of this line as a string.
         *
         * @return The content of this line as a string.
         */
        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer(256);

            if (isFile) {
                buf.append('F').append(SEPARATOR);
            } else if (isAlbum) {
                buf.append('A').append(SEPARATOR);
            } else if (isArtist) {
                buf.append('R').append(SEPARATOR);
            } else {
                buf.append('D').append(SEPARATOR);
            }

            buf.append(created).append(SEPARATOR);
            buf.append(lastModified).append(SEPARATOR);
            buf.append(file.getPath()).append(SEPARATOR);
            buf.append(length).append(SEPARATOR);
            buf.append(artist == null ? "" : artist).append(SEPARATOR);
            buf.append(album == null ? "" : album).append(SEPARATOR);
            buf.append(title == null ? "" : title).append(SEPARATOR);
            buf.append(year == null ? "" : year).append(SEPARATOR);
            buf.append(genre == null ? "" : genre);

            return buf.toString();
        }
    }

    private static class Scanner implements MusicFile.Visitor {
        private final PrintWriter writer;
        private final Map<File, Line> oldIndex;
        private final Set<File> musicFolders;
        private int count;

        Scanner(PrintWriter writer, Map<File, Line> oldIndex, List<MusicFolder> musicFolders) {
            this.writer = writer;
            this.oldIndex = oldIndex;
            this.musicFolders = new HashSet<File>();
            for (MusicFolder musicFolder : musicFolders) {
                this.musicFolders.add(musicFolder.getPath());
            }
        }

        public void visit(MusicFile musicFile) {
            writer.println(Line.forFile(musicFile, oldIndex, musicFolders));
            count++;
            if (count % 1000 == 0) {
                LOG.info("Created search index with " + count + " entries.");
            }
        }

        public boolean includeDirectories() {
            return true;
        }

        public boolean sorted() {
            return false;
        }

        public int getCount() {
            return count;
        }
    }
}
