package net.sourceforge.subsonic.androidapp.test;

import com.xtremelabs.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.view.KeyEvent;

import net.sourceforge.subsonic.androidapp.domain.PlayerState;
import net.sourceforge.subsonic.androidapp.service.DownloadServiceImpl;
import net.sourceforge.subsonic.androidapp.service.DownloadServiceLifecycleSupport;

@RunWith(RobolectricTestRunner.class)
public class DownloadServiceLifecycleSupportTest {
	
	@Mock private DownloadServiceImpl mockedDownloadServiceImpl;
	private Intent inputIntent;
	private DownloadServiceLifecycleSupport serviceUnderTest;
	
	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Before
	public void setUp() {
		inputIntent = new Intent();
	}

	@Test
    public void testMediaPlayEventOnPausedPlayer() {
    	when(mockedDownloadServiceImpl.getPlayerState()).thenReturn(PlayerState.PAUSED);
    	serviceUnderTest = new DownloadServiceLifecycleSupport(mockedDownloadServiceImpl);
    	inputIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, 126));	// 126: workaround until API level 11 is set
    	serviceUnderTest.onStart(inputIntent);
    	verify(mockedDownloadServiceImpl).start();
    }

	@Test
    public void testMediaPlayEventOnStoppedPlayer() {
    	when(mockedDownloadServiceImpl.getPlayerState()).thenReturn(PlayerState.STOPPED);
    	serviceUnderTest = new DownloadServiceLifecycleSupport(mockedDownloadServiceImpl);
    	inputIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, 126));	// 126: workaround until API level 11 is set
    	serviceUnderTest.onStart(inputIntent);
    	verify(mockedDownloadServiceImpl).play();
    }
	
	@Test
    public void testMediaPauseEvent() {
    	serviceUnderTest = new DownloadServiceLifecycleSupport(mockedDownloadServiceImpl);
    	inputIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, 127));	// 127: workaround until API level 11 is set
    	serviceUnderTest.onStart(inputIntent);
    	verify(mockedDownloadServiceImpl).pause();
    }	
}