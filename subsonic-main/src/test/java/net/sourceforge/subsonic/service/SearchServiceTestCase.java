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

import junit.framework.*;
import net.sourceforge.subsonic.domain.*;

import org.apache.commons.lang.*;

import java.io.*;
import java.util.*;

/**
 * Unit test of {@link SearchService}.
 *
 * @author Sindre Mehus
 */
public class SearchServiceTestCase extends TestCase {

    public void testLine() {
        doTestLine("myArtist", "myAlbum", "myTitle", 2011, "foo.mp3", "rock", 12345678, 2394872834L);
        doTestLine("myArtist", "myAlbum", "myTitle", null, "foo.mp3", "rock", 12345678, 2394872834L);
        doTestLine("myArtist", "myAlbum", "myTitle", null, "foo.mp3", "", 12345678, 2394872834L);
        doTestLine("",         "myAlbum", "myTitle", null, "foo.mp3", "", 12345678, 2394872834L);
        doTestLine("",         "",        "myTitle", null, "foo.mp3", "", 12345678, 2394872834L);
        doTestLine("",         "",        "",        null, "foo.mp3", "", 12345678, 2394872834L);
        doTestLine("",         "",        "",        null, "foo.mp3", "", 12345678, 2394872834L);
    }

    private void doTestLine(final String artist, final String album, final String title, final Integer year,
            final String path, final String genre, final long lastModified, final long length) {

        MusicFile file = new MusicFile() {
            public synchronized net.sourceforge.subsonic.service.metadata.MetaData getMetaData() {
                net.sourceforge.subsonic.service.metadata.MetaData metaData = new net.sourceforge.subsonic.service.metadata.MetaData();
                metaData.setArtist(artist);
                metaData.setAlbumName(album);
                metaData.setTitle(title);
                metaData.setYear(year);
                metaData.setGenre(genre);
                return metaData;
            }
            public File getFile() {
                return new File(path);
            }
            public boolean isFile() {
                return true;
            }
            public boolean isDirectory() {
                return false;
            }
            public long lastModified() {
                return lastModified;
            }
            public long length() {
                return length;
            }
        };

        SearchService.Line line = SearchService.Line.forFile(file, Collections.<File, SearchService.Line>emptyMap(), Collections.<File>emptySet());
        String yearString = year == null ? "" : year.toString();
        String expected = 'F' + SearchService.Line.SEPARATOR +
                          lastModified + SearchService.Line.SEPARATOR +
                          lastModified + SearchService.Line.SEPARATOR +
                          path + SearchService.Line.SEPARATOR +
                          length + SearchService.Line.SEPARATOR +
                          StringUtils.upperCase(artist) + SearchService.Line.SEPARATOR +
                          StringUtils.upperCase(album) + SearchService.Line.SEPARATOR +
                          StringUtils.upperCase(title) + SearchService.Line.SEPARATOR +
                          yearString + SearchService.Line.SEPARATOR +
                          StringUtils.capitalize(genre);

        assertEquals("Error in toString().", expected, line.toString());
        assertEquals("Error in forFile().",  expected, SearchService.Line.forFile(file, Collections.<File, SearchService.Line>emptyMap(), Collections.<File>emptySet()).toString());
        assertEquals("Error in parse().",    expected, SearchService.Line.parse(expected).toString());
    }
}