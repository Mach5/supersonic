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
package net.sourceforge.subsonic.service.metadata;

import junit.framework.*;
import net.sourceforge.subsonic.domain.MusicFile;

/**
 * Unit test of {@link MusicFile.MetaData}.
 *
 * @author Sindre Mehus
 */
public class MetaDataTestCase extends TestCase {

    public void testGetDurationAsString() throws Exception {
        doTestGetDurationAsString(0, "0:00");
        doTestGetDurationAsString(1, "0:01");
        doTestGetDurationAsString(10, "0:10");
        doTestGetDurationAsString(33, "0:33");
        doTestGetDurationAsString(59, "0:59");
        doTestGetDurationAsString(60, "1:00");
        doTestGetDurationAsString(61, "1:01");
        doTestGetDurationAsString(70, "1:10");
        doTestGetDurationAsString(119, "1:59");
        doTestGetDurationAsString(120, "2:00");
        doTestGetDurationAsString(1200, "20:00");
        doTestGetDurationAsString(1201, "20:01");
        doTestGetDurationAsString(3599, "59:59");
        doTestGetDurationAsString(3600, "1:00:00");
        doTestGetDurationAsString(3601, "1:00:01");
        doTestGetDurationAsString(3661, "1:01:01");
        doTestGetDurationAsString(4200, "1:10:00");
        doTestGetDurationAsString(4201, "1:10:01");
        doTestGetDurationAsString(4210, "1:10:10");
        doTestGetDurationAsString(36000, "10:00:00");
        doTestGetDurationAsString(360000, "100:00:00");
    }

    private void doTestGetDurationAsString(int seconds, String expected) {
        MusicFile.MetaData metaData = new MusicFile.MetaData();
        metaData.setDuration(seconds);
        assertEquals("Error in getDurationAsString().", expected, metaData.getDurationAsString());
    }

    public void testGetYearAsInteger() {
        doTestGetYearAsInteger(null, null);
        doTestGetYearAsInteger("", null);
        doTestGetYearAsInteger(" ", null);
        doTestGetYearAsInteger("    ", null);
        doTestGetYearAsInteger("abc", null);
        doTestGetYearAsInteger("abcd", null);
        doTestGetYearAsInteger("abcde", null);
        doTestGetYearAsInteger("12", null);
        doTestGetYearAsInteger("123", null);
        doTestGetYearAsInteger("1234", 1234);
        doTestGetYearAsInteger("12345", 1234);
        doTestGetYearAsInteger("2010-06-01", 2010);
        doTestGetYearAsInteger("2010 06 01", 2010);
        doTestGetYearAsInteger("2010abc", 2010);
    }

    private void doTestGetYearAsInteger(String yearString, Integer expected) {
        MusicFile.MetaData metaData = new MusicFile.MetaData();
        metaData.setYear(yearString);
        assertEquals("Error in getYearAsInteger().", expected, metaData.getYearAsInteger());
    }
}