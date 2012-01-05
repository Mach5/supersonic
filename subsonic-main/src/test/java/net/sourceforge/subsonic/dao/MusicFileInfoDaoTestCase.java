package net.sourceforge.subsonic.dao;

import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFileInfo;
import net.sourceforge.subsonic.domain.User;
import org.springframework.dao.DataAccessException;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Unit test of {@link MusicFileInfoDao}.
 *
 * @author Sindre Mehus
 */
public class MusicFileInfoDaoTestCase extends DaoTestCaseBase {

    protected void setUp() throws Exception {
        getJdbcTemplate().execute("delete from music_file_info");
        getJdbcTemplate().execute("delete from user_role");
        getJdbcTemplate().execute("delete from user");
    }

    public void testCreateMusicFileInfo() {
        MusicFileInfo info = new MusicFileInfo(null, "path", "comment", 123, new Date());
        musicFileInfoDao.createMusicFileInfo(info);

        MusicFileInfo newInfo = musicFileInfoDao.getMusicFileInfoForPath("path");
        assertMusicFileInfoEquals(info, newInfo);
    }

    public void testCaseInsensitivity() {
        MusicFileInfo info = new MusicFileInfo(null, "aBcDeFgH", "comment", 123, new Date());
        musicFileInfoDao.createMusicFileInfo(info);

        MusicFileInfo newInfo = musicFileInfoDao.getMusicFileInfoForPath("AbcdefGH");
        assertMusicFileInfoEquals(info, newInfo);
    }

    public void testUpdateMusicFileInfo() {
        MusicFileInfo info = new MusicFileInfo(null, "path", "comment", 123, new Date());
        musicFileInfoDao.createMusicFileInfo(info);
        info = musicFileInfoDao.getMusicFileInfoForPath("path");

        info.setPath("newPath");
        info.setPlayCount(5);
        info.setLastPlayed(new Date());
        info.setComment("newComment");
        musicFileInfoDao.updateMusicFileInfo(info);

        MusicFileInfo newInfo = musicFileInfoDao.getMusicFileInfoForPath("newPath");
        assertMusicFileInfoEquals(info, newInfo);
    }

    public void testGetHighestRated() {
        userDao.createUser(new User("sindre", "secret", null));

        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "a", null, 0, new Date()));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "b", null, 0, new Date()));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "d", null, 0, new Date()));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "e", null, 0, new Date()));

        musicFileInfoDao.setRatingForUser("sindre", new TestMediaFile("f"), 5);
        musicFileInfoDao.setRatingForUser("sindre", new TestMediaFile("b"), 4);
        musicFileInfoDao.setRatingForUser("sindre", new TestMediaFile("d"), 3);
        musicFileInfoDao.setRatingForUser("sindre", new TestMediaFile("a"), 2);
        musicFileInfoDao.setRatingForUser("sindre", new TestMediaFile("e"), 1);

        List<String> highestRated = musicFileInfoDao.getHighestRated(0, 0);
        assertEquals("Error in getHighestRated().", 0, highestRated.size());

        highestRated = musicFileInfoDao.getHighestRated(0, 1);
        assertEquals("Error in getHighestRated().", 1, highestRated.size());
        assertEquals("Error in getHighestRated().", "f", highestRated.get(0));

        highestRated = musicFileInfoDao.getHighestRated(0, 2);
        assertEquals("Error in getHighestRated().", 2, highestRated.size());
        assertEquals("Error in getHighestRated().", "f", highestRated.get(0));
        assertEquals("Error in getHighestRated().", "b", highestRated.get(1));

        highestRated = musicFileInfoDao.getHighestRated(0, 5);
        assertEquals("Error in getHighestRated().", 5, highestRated.size());
        assertEquals("Error in getHighestRated().", "f", highestRated.get(0));
        assertEquals("Error in getHighestRated().", "b", highestRated.get(1));
        assertEquals("Error in getHighestRated().", "d", highestRated.get(2));
        assertEquals("Error in getHighestRated().", "a", highestRated.get(3));
        assertEquals("Error in getHighestRated().", "e", highestRated.get(4));

        highestRated = musicFileInfoDao.getHighestRated(0, 6);
        assertEquals("Error in getHighestRated().", 5, highestRated.size());

        highestRated = musicFileInfoDao.getHighestRated(1, 0);
        assertEquals("Error in getHighestRated().", 0, highestRated.size());

        highestRated = musicFileInfoDao.getHighestRated(1, 1);
        assertEquals("Error in getHighestRated().", 1, highestRated.size());
        assertEquals("Error in getHighestRated().", "b", highestRated.get(0));

        highestRated = musicFileInfoDao.getHighestRated(3, 2);
        assertEquals("Error in getHighestRated().", 2, highestRated.size());
        assertEquals("Error in getHighestRated().", "a", highestRated.get(0));
        assertEquals("Error in getHighestRated().", "e", highestRated.get(1));

        highestRated = musicFileInfoDao.getHighestRated(4, 10);
        assertEquals("Error in getHighestRated().", 1, highestRated.size());
        assertEquals("Error in getHighestRated().", "e", highestRated.get(0));

        highestRated = musicFileInfoDao.getHighestRated(5, 10);
        assertEquals("Error in getHighestRated().", 0, highestRated.size());

        highestRated = musicFileInfoDao.getHighestRated(6, 10);
        assertEquals("Error in getHighestRated().", 0, highestRated.size());
    }

    public void testGetMostFrequentlyPlayed() {
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "f", null, 5, null));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "b", null, 4, null));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "d", null, 3, null));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "a", null, 2, null));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "e", null, 1, null));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "c", null, 0, null));  // Not included in query.

        List<MusicFileInfo> mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(0, 0);
        assertEquals("Error in getMostFrequentlyPlayed().", 0, mostFrequent.size());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(0, 1);
        assertEquals("Error in getMostFrequentlyPlayed().", 1, mostFrequent.size());
        assertEquals("Error in getMostFrequentlyPlayed().", "f", mostFrequent.get(0).getPath());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(0, 2);
        assertEquals("Error in getMostFrequentlyPlayed().", 2, mostFrequent.size());
        assertEquals("Error in getMostFrequentlyPlayed().", "f", mostFrequent.get(0).getPath());
        assertEquals("Error in getMostFrequentlyPlayed().", "b", mostFrequent.get(1).getPath());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(0, 5);
        assertEquals("Error in getMostFrequentlyPlayed().", 5, mostFrequent.size());
        assertEquals("Error in getMostFrequentlyPlayed().", "f", mostFrequent.get(0).getPath());
        assertEquals("Error in getMostFrequentlyPlayed().", "b", mostFrequent.get(1).getPath());
        assertEquals("Error in getMostFrequentlyPlayed().", "d", mostFrequent.get(2).getPath());
        assertEquals("Error in getMostFrequentlyPlayed().", "a", mostFrequent.get(3).getPath());
        assertEquals("Error in getMostFrequentlyPlayed().", "e", mostFrequent.get(4).getPath());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(0, 6);
        assertEquals("Error in getMostFrequentlyPlayed().", 5, mostFrequent.size());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(1, 0);
        assertEquals("Error in getMostFrequentlyPlayed().", 0, mostFrequent.size());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(1, 1);
        assertEquals("Error in getMostFrequentlyPlayed().", 1, mostFrequent.size());
        assertEquals("Error in getMostFrequentlyPlayed().", "b", mostFrequent.get(0).getPath());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(3, 2);
        assertEquals("Error in getMostFrequentlyPlayed().", 2, mostFrequent.size());
        assertEquals("Error in getMostFrequentlyPlayed().", "a", mostFrequent.get(0).getPath());
        assertEquals("Error in getMostFrequentlyPlayed().", "e", mostFrequent.get(1).getPath());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(4, 10);
        assertEquals("Error in getMostFrequentlyPlayed().", 1, mostFrequent.size());
        assertEquals("Error in getMostFrequentlyPlayed().", "e", mostFrequent.get(0).getPath());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(5, 10);
        assertEquals("Error in getMostFrequentlyPlayed().", 0, mostFrequent.size());

        mostFrequent = musicFileInfoDao.getMostFrequentlyPlayed(6, 10);
        assertEquals("Error in getMostFrequentlyPlayed().", 0, mostFrequent.size());
    }

    public void testGetMostRecentlyPlayed() {
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "f", null, 0, new Date(5)));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "b", null, 0, new Date(4)));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "d", null, 0, new Date(3)));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "a", null, 0, new Date(2)));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "e", null, 0, new Date(1)));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo(null, "c", null, 0, null));  // Not included in query.

        List<MusicFileInfo> mostRecent = musicFileInfoDao.getMostRecentlyPlayed(0, 0);
        assertEquals("Error in getMostRecentlyPlayed().", 0, mostRecent.size());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(0, 1);
        assertEquals("Error in getMostRecentlyPlayed().", 1, mostRecent.size());
        assertEquals("Error in getMostRecentlyPlayed().", "f", mostRecent.get(0).getPath());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(0, 2);
        assertEquals("Error in getMostRecentlyPlayed().", 2, mostRecent.size());
        assertEquals("Error in getMostRecentlyPlayed().", "f", mostRecent.get(0).getPath());
        assertEquals("Error in getMostRecentlyPlayed().", "b", mostRecent.get(1).getPath());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(0, 5);
        assertEquals("Error in getMostRecentlyPlayed().", 5, mostRecent.size());
        assertEquals("Error in getMostRecentlyPlayed().", "f", mostRecent.get(0).getPath());
        assertEquals("Error in getMostRecentlyPlayed().", "b", mostRecent.get(1).getPath());
        assertEquals("Error in getMostRecentlyPlayed().", "d", mostRecent.get(2).getPath());
        assertEquals("Error in getMostRecentlyPlayed().", "a", mostRecent.get(3).getPath());
        assertEquals("Error in getMostRecentlyPlayed().", "e", mostRecent.get(4).getPath());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(0, 6);
        assertEquals("Error in getMostRecentlyPlayed().", 5, mostRecent.size());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(1, 0);
        assertEquals("Error in getMostRecentlyPlayed().", 0, mostRecent.size());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(1, 1);
        assertEquals("Error in getMostRecentlyPlayed().", 1, mostRecent.size());
        assertEquals("Error in getMostRecentlyPlayed().", "b", mostRecent.get(0).getPath());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(3, 2);
        assertEquals("Error in getMostRecentlyPlayed().", 2, mostRecent.size());
        assertEquals("Error in getMostRecentlyPlayed().", "a", mostRecent.get(0).getPath());
        assertEquals("Error in getMostRecentlyPlayed().", "e", mostRecent.get(1).getPath());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(4, 10);
        assertEquals("Error in getMostRecentlyPlayed().", 1, mostRecent.size());
        assertEquals("Error in getMostRecentlyPlayed().", "e", mostRecent.get(0).getPath());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(5, 10);
        assertEquals("Error in getMostRecentlyPlayed().", 0, mostRecent.size());

        mostRecent = musicFileInfoDao.getMostRecentlyPlayed(6, 10);
        assertEquals("Error in getMostRecentlyPlayed().", 0, mostRecent.size());
    }

    public void testRating() {
        MediaFile musicFile = new TestMediaFile("foo");
        assertNull("Error in getRatingForUser().", musicFileInfoDao.getRatingForUser("sindre", musicFile));
        assertNull("Error in getAverageRating().", musicFileInfoDao.getAverageRating(musicFile));

        try {
            musicFileInfoDao.setRatingForUser("sindre", musicFile, 1);
            fail("Expected exception.");
        } catch (DataAccessException x) {
        }

        userDao.createUser(new User("sindre", "secret", null));
        userDao.createUser(new User("bente", "secret", null));

        musicFileInfoDao.setRatingForUser("sindre", musicFile, 1);
        assertEquals("Error in getAverageRating().", 1.0D, musicFileInfoDao.getAverageRating(musicFile));
        assertEquals("Error in getRatingForUser().", new Integer(1), musicFileInfoDao.getRatingForUser("sindre", musicFile));
        assertNull("Error in getRatingForUser().", musicFileInfoDao.getRatingForUser("bente", musicFile));

        musicFileInfoDao.setRatingForUser("sindre", musicFile, 2);
        assertEquals("Error in getAverageRating().", 2.0D, musicFileInfoDao.getAverageRating(musicFile));
        assertEquals("Error in getRatingForUser().", new Integer(2), musicFileInfoDao.getRatingForUser("sindre", musicFile));

        musicFileInfoDao.setRatingForUser("sindre", musicFile, 3);
        assertEquals("Error in getAverageRating().", 3.0D, musicFileInfoDao.getAverageRating(musicFile));
        assertEquals("Error in getRatingForUser().", new Integer(3), musicFileInfoDao.getRatingForUser("sindre", musicFile));

        musicFileInfoDao.setRatingForUser("sindre", musicFile, 4);
        assertEquals("Error in getAverageRating().", 4.0D, musicFileInfoDao.getAverageRating(musicFile));
        assertEquals("Error in getRatingForUser().", new Integer(4), musicFileInfoDao.getRatingForUser("sindre", musicFile));

        musicFileInfoDao.setRatingForUser("sindre", musicFile, 5);
        assertEquals("Error in getAverageRating().", 5.0D, musicFileInfoDao.getAverageRating(musicFile));
        assertEquals("Error in getRatingForUser().", new Integer(5), musicFileInfoDao.getRatingForUser("sindre", musicFile));

        musicFileInfoDao.setRatingForUser("bente", musicFile, 2);
        assertEquals("Error in getRatingForUser().", new Integer(2), musicFileInfoDao.getRatingForUser("bente", musicFile));
        assertEquals("Error in getAverageRating().", 3.5D, musicFileInfoDao.getAverageRating(musicFile));

        userDao.deleteUser("bente");
        assertEquals("Error in getAverageRating().", 5.0D, musicFileInfoDao.getAverageRating(musicFile));
        assertEquals("Error in getRatingForUser().", new Integer(5), musicFileInfoDao.getRatingForUser("sindre", musicFile));
        assertNull("Error in getRatingForUser().", musicFileInfoDao.getRatingForUser("bente", musicFile));

        musicFileInfoDao.setRatingForUser("sindre", musicFile, null);
        assertNull("Error in getRatingForUser().", musicFileInfoDao.getRatingForUser("sindre", musicFile));
        assertNull("Error in getAverageRating().", musicFileInfoDao.getAverageRating(musicFile));
    }

    public void testGetAllMusicFileInfos() {
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo("a"));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo("b"));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo("c"));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo("d"));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo("e"));
        musicFileInfoDao.createMusicFileInfo(new MusicFileInfo("f"));

        MusicFileInfo d = musicFileInfoDao.getMusicFileInfoForPath("d");
        d.setEnabled(false);
        musicFileInfoDao.updateMusicFileInfo(d);

        List<MusicFileInfo> list = musicFileInfoDao.getAllMusicFileInfos(0, 0);
        assertTrue("Error in getAllMusicFileInfos().", list.isEmpty());

        list = musicFileInfoDao.getAllMusicFileInfos(0, 3);
        assertEquals("Error in getAllMusicFileInfos().", 3, list.size());
        assertEquals("Error in getAllMusicFileInfos().", "a", list.get(0).getPath());
        assertEquals("Error in getAllMusicFileInfos().", "b", list.get(1).getPath());
        assertEquals("Error in getAllMusicFileInfos().", "c", list.get(2).getPath());

        list = musicFileInfoDao.getAllMusicFileInfos(3, 2);
        assertEquals("Error in getAllMusicFileInfos().", 2, list.size());
        assertEquals("Error in getAllMusicFileInfos().", "d", list.get(0).getPath());
        assertEquals("Error in getAllMusicFileInfos().", "e", list.get(1).getPath());

        list = musicFileInfoDao.getAllMusicFileInfos(4, 100);
        assertEquals("Error in getAllMusicFileInfos().", 2, list.size());
        assertEquals("Error in getAllMusicFileInfos().", "e", list.get(0).getPath());
        assertEquals("Error in getAllMusicFileInfos().", "f", list.get(1).getPath());

        list = musicFileInfoDao.getAllMusicFileInfos(100, 1);
        assertTrue("Error in getAllMusicFileInfos().", list.isEmpty());
    }


    private void assertMusicFileInfoEquals(MusicFileInfo expected, MusicFileInfo actual) {
        assertEquals("Wrong path.", expected.getPath(), actual.getPath());
        assertEquals("Wrong comment.", expected.getComment(), actual.getComment());
        assertEquals("Wrong last played date.", expected.getLastPlayed(), actual.getLastPlayed());
        assertEquals("Wrong play count.", expected.getPlayCount(), actual.getPlayCount());
    }

    private static class TestMediaFile extends MediaFile {

        public TestMediaFile(String path) {
            setPath(path);
        }
    }
}
