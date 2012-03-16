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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.MediaFile;
import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.SearchCriteria;
import net.sourceforge.subsonic.domain.SearchResult;
import net.sourceforge.subsonic.util.FileUtil;

import static net.sourceforge.subsonic.service.SearchService.IndexType.ALBUM;
import static net.sourceforge.subsonic.service.SearchService.IndexType.ARTIST;
import static net.sourceforge.subsonic.service.SearchService.IndexType.SONG;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id$
 * @see MediaScannerService
 */
public class SearchService {

    private static final Logger LOG = Logger.getLogger(SearchService.class);

    private static final String FIELD_PATH = "path";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ALBUM = "album";
    private static final String FIELD_ARTIST = "artist";
    private static final Version LUCENE_VERSION = Version.LUCENE_30;

    private MediaFileService mediaFileService;

    private IndexWriter artistWriter;
    private IndexWriter albumWriter;
    private IndexWriter songWriter;

    public SearchService() {
        removeLocks();
    }


    public void startIndexing() {
        try {
            artistWriter = createIndexWriter(ARTIST);
            albumWriter = createIndexWriter(ALBUM);
            songWriter = createIndexWriter(SONG);
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        }
    }

    public void index(MediaFile mediaFile) {
        try {
            if (mediaFile.isFile()) {
                songWriter.addDocument(SONG.createDocument(mediaFile));
            } else if (mediaFile.isAlbum()) {
                albumWriter.addDocument(ALBUM.createDocument(mediaFile));
            } else {
                artistWriter.addDocument(ARTIST.createDocument(mediaFile));
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    public void stopIndexing() {
        try {
            artistWriter.optimize();
            albumWriter.optimize();
            songWriter.optimize();
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        } finally {
            FileUtil.closeQuietly(artistWriter);
            FileUtil.closeQuietly(albumWriter);
            FileUtil.closeQuietly(songWriter);
        }
    }

    public SearchResult search(SearchCriteria criteria, IndexType indexType) {
        SearchResult result = new SearchResult();
        List<MediaFile> mediaFiles = new ArrayList<MediaFile>();
        int offset = criteria.getOffset();
        int count = criteria.getCount();
        result.setOffset(offset);
        result.setMediaFiles(mediaFiles);

        IndexReader reader = null;
        try {
            reader = createIndexReader(indexType);
            Searcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new SubsonicAnalyzer();

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, indexType.getFields(), analyzer, indexType.getBoosts());
            Query query = queryParser.parse(criteria.getQuery());

            TopDocs topDocs = searcher.search(query, null, offset + count);
            result.setTotalHits(topDocs.totalHits);

            int start = Math.min(offset, topDocs.totalHits);
            int end = Math.min(start + count, topDocs.totalHits);
            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                mediaFiles.add(mediaFileService.getMediaFile(doc.getField(FIELD_PATH).stringValue()));
            }

        } catch (Throwable x) {
            LOG.error("Failed to execute Lucene search.", x);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
    }

    private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        File dir = getIndexDirectory(indexType);
        return new IndexWriter(FSDirectory.open(dir), new SubsonicAnalyzer(), true, new IndexWriter.MaxFieldLength(10));
    }

    private IndexReader createIndexReader(IndexType indexType) throws IOException {
        File dir = getIndexDirectory(indexType);
        return IndexReader.open(FSDirectory.open(dir), true);
    }

    private File getIndexRootDirectory() {
        return new File(SettingsService.getSubsonicHome(), "lucene");
    }

    private File getIndexDirectory(IndexType indexType) {
        return new File(getIndexRootDirectory(), indexType.toString().toLowerCase());
    }

    private void removeLocks() {
        for (IndexType indexType : IndexType.values()) {
            Directory dir = null;
            try {
                dir = FSDirectory.open(getIndexDirectory(indexType));
                if (IndexWriter.isLocked(dir)) {
                    IndexWriter.unlock(dir);
                    LOG.info("Removed Lucene lock file in " + dir);
                }
            } catch (Exception x) {
                LOG.warn("Failed to remove Lucene lock file in " + dir, x);
            } finally {
                FileUtil.closeQuietly(dir);
            }
        }
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public static enum IndexType {

        SONG(new String[]{FIELD_TITLE, FIELD_ARTIST}, FIELD_TITLE) {

            @Override
            public Document createDocument(MediaFile mediaFile) {
                Document doc = new Document();
                doc.add(new Field(FIELD_PATH, mediaFile.getPath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

                if (mediaFile.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, mediaFile.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getTitle() != null) {
                    doc.add(new Field(FIELD_TITLE, mediaFile.getTitle(), Field.Store.YES, Field.Index.ANALYZED));
                }

                return doc;
            }
        },

        ALBUM(new String[]{FIELD_ALBUM, FIELD_ARTIST}, FIELD_ALBUM) {

            @Override
            public Document createDocument(MediaFile mediaFile) {
                Document doc = new Document();
                doc.add(new Field(FIELD_PATH, mediaFile.getPath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

                if (mediaFile.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, mediaFile.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getAlbumName() != null) {
                    doc.add(new Field(FIELD_ALBUM, mediaFile.getAlbumName(), Field.Store.YES, Field.Index.ANALYZED));
                }

                return doc;
            }
        },

        ARTIST(new String[]{FIELD_ARTIST}, null) {

            @Override
            public Document createDocument(MediaFile mediaFile) {
                Document doc = new Document();
                doc.add(new Field(FIELD_PATH, mediaFile.getPath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

                if (mediaFile.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, mediaFile.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }

                return doc;
            }
        };

        private final String[] fields;
        private final Map<String, Float> boosts;

        private IndexType(String[] fields, String boostedField) {
            this.fields = fields;
            boosts = new HashMap<String, Float>();
            if (boostedField != null) {
                boosts.put(boostedField, 2.0F);
            }
        }

        public String[] getFields() {
            return fields;
        }

        public abstract Document createDocument(MediaFile mediaFile);

        public Map<String, Float> getBoosts() {
            return boosts;
        }
    }

    private class SubsonicAnalyzer extends StandardAnalyzer {
        private SubsonicAnalyzer() {
            super(LUCENE_VERSION);
        }

        @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = super.tokenStream(fieldName, reader);
            return new ASCIIFoldingFilter(result);
        }

        @Override
        public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
            class SavedStreams {
                StandardTokenizer tokenStream;
                TokenStream filteredTokenStream;
            }

            SavedStreams streams = (SavedStreams) getPreviousTokenStream();
            if (streams == null) {
                streams = new SavedStreams();
                setPreviousTokenStream(streams);
                streams.tokenStream = new StandardTokenizer(LUCENE_VERSION, reader);
                streams.filteredTokenStream = new StandardFilter(streams.tokenStream);
                streams.filteredTokenStream = new LowerCaseFilter(streams.filteredTokenStream);
                streams.filteredTokenStream = new StopFilter(true, streams.filteredTokenStream, STOP_WORDS_SET);
                streams.filteredTokenStream = new ASCIIFoldingFilter(streams.filteredTokenStream);
            } else {
                streams.tokenStream.reset(reader);
            }
            streams.tokenStream.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);

            return streams.filteredTokenStream;
        }
    }
}


