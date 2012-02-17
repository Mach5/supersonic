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
package net.sourceforge.subsonic.androidapp.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import net.sourceforge.subsonic.androidapp.audiofx.EqualizerController;
import net.sourceforge.subsonic.androidapp.audiofx.VisualizerController;
import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.domain.PlayerState;
import net.sourceforge.subsonic.androidapp.domain.RepeatMode;
import net.sourceforge.subsonic.androidapp.util.CancellableTask;
import net.sourceforge.subsonic.androidapp.util.LRUCache;
import net.sourceforge.subsonic.androidapp.util.ShufflePlayBuffer;
import net.sourceforge.subsonic.androidapp.util.SimpleServiceBinder;
import net.sourceforge.subsonic.androidapp.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static net.sourceforge.subsonic.androidapp.domain.PlayerState.*;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class DownloadServiceImpl extends Service implements DownloadService {

    private static final String TAG = DownloadServiceImpl.class.getSimpleName();

    public static final String CMD_PLAY = "net.sourceforge.subsonic.androidapp.CMD_PLAY";
    public static final String CMD_TOGGLEPAUSE = "net.sourceforge.subsonic.androidapp.CMD_TOGGLEPAUSE";
    public static final String CMD_PAUSE = "net.sourceforge.subsonic.androidapp.CMD_PAUSE";
    public static final String CMD_STOP = "net.sourceforge.subsonic.androidapp.CMD_STOP";
    public static final String CMD_PREVIOUS = "net.sourceforge.subsonic.androidapp.CMD_PREVIOUS";
    public static final String CMD_NEXT = "net.sourceforge.subsonic.androidapp.CMD_NEXT";

    private final IBinder binder = new SimpleServiceBinder<DownloadService>(this);
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusListener audioFocusListener;
    private final List<DownloadFile> downloadList = new ArrayList<DownloadFile>();
    private final Handler handler = new Handler();
    private final DownloadServiceLifecycleSupport lifecycleSupport = new DownloadServiceLifecycleSupport(this);
    private final ShufflePlayBuffer shufflePlayBuffer = new ShufflePlayBuffer(this);

    private final LRUCache<MusicDirectory.Entry, DownloadFile> downloadFileCache = new LRUCache<MusicDirectory.Entry, DownloadFile>(100);
    private final List<DownloadFile> cleanupCandidates = new ArrayList<DownloadFile>();
    private final Scrobbler scrobbler = new Scrobbler();
    private final JukeboxService jukeboxService = new JukeboxService(this);
    private DownloadFile currentPlaying;
    private DownloadFile currentDownloading;
    private CancellableTask bufferTask;
    private PlayerState playerState = IDLE;
    private boolean shufflePlay;
    private long revision;
    private static DownloadService instance;
    private String suggestedPlaylistName;
    private PowerManager.WakeLock wakeLock;
    private boolean keepScreenOn = false;

    private static boolean equalizerAvailable;
    private static boolean visualizerAvailable;
    private EqualizerController equalizerController;
    private VisualizerController visualizerController;
    private boolean showVisualization;
    private boolean jukeboxEnabled;

    static {
        try {
            EqualizerController.checkAvailable();
            equalizerAvailable = true;
        } catch (Throwable t) {
            equalizerAvailable = false;
        }
    }
    static {
        try {
            VisualizerController.checkAvailable();
            visualizerAvailable = true;
        } catch (Throwable t) {
            visualizerAvailable = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int more) {
                handleError(new Exception("MediaPlayer error: " + what + " (" + more + ")"));
                return false;
            }
        });

        if (equalizerAvailable) {
            equalizerController = new EqualizerController(this, mediaPlayer);
            if (!equalizerController.isAvailable()) {
                equalizerController = null;
            } else {
                equalizerController.loadSettings();
            }
        }
        if (visualizerAvailable) {
            visualizerController = new VisualizerController(this, mediaPlayer);
            if (!visualizerController.isAvailable()) {
                visualizerController = null;
            }
        }

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        wakeLock.setReferenceCounted(false);
        
        audioFocusListener = new AudioFocusListener(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        instance = this;
        lifecycleSupport.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        lifecycleSupport.onStart(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycleSupport.onDestroy();
        mediaPlayer.release();
        shufflePlayBuffer.shutdown();
        if (equalizerController != null) {
            equalizerController.release();
        }
        if (visualizerController != null) {
            visualizerController.release();
        }

        instance = null;
    }

    public static DownloadService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public synchronized void download(List<MusicDirectory.Entry> songs, boolean save, boolean autoplay, boolean playNext) {
        shufflePlay = false;
        int offset = 1;

        if (songs.isEmpty()) {
            return;
        }
        if (playNext) {
            if (autoplay && getCurrentPlayingIndex() >= 0) {
                offset = 0;
            }
            for (MusicDirectory.Entry song : songs) {
                DownloadFile downloadFile = new DownloadFile(this, song, save);
                downloadList.add(getCurrentPlayingIndex() + offset, downloadFile);
                offset++;
            }
            revision++;
        } else {
            for (MusicDirectory.Entry song : songs) {
                DownloadFile downloadFile = new DownloadFile(this, song, save);
                downloadList.add(downloadFile);
            }
            revision++;
        }
        updateJukeboxPlaylist();

        if (autoplay) {
            play(0);
        } else {
            if (currentPlaying == null) {
                currentPlaying = downloadList.get(0);
            }
            checkDownloads();
        }
        lifecycleSupport.serializeDownloadQueue();
    }

    private void updateJukeboxPlaylist() {
        if (jukeboxEnabled) {
            jukeboxService.updatePlaylist();
        }
    }

    public void restore(List<MusicDirectory.Entry> songs, int currentPlayingIndex, int currentPlayingPosition) {
        download(songs, false, false, false);
        if (currentPlayingIndex != -1) {
            play(currentPlayingIndex, false);
            if (currentPlaying.isCompleteFileAvailable()) {
                doPlay(currentPlaying, currentPlayingPosition, false);
            }
        }
    }

    @Override
    public synchronized void setShufflePlayEnabled(boolean enabled) {
        if (shufflePlay == enabled) {
            return;
        }

        shufflePlay = enabled;
        if (shufflePlay) {
            clear();
            checkDownloads();
        }
    }

    @Override
    public synchronized boolean isShufflePlayEnabled() {
        return shufflePlay;
    }

    @Override
    public synchronized void shuffle() {
        Collections.shuffle(downloadList);
        if (currentPlaying != null) {
            downloadList.remove(getCurrentPlayingIndex());
            downloadList.add(0, currentPlaying);
        }
        revision++;
        lifecycleSupport.serializeDownloadQueue();
        updateJukeboxPlaylist();
    }

    @Override
    public RepeatMode getRepeatMode() {
        return Util.getRepeatMode(this);
    }

    @Override
    public void setRepeatMode(RepeatMode repeatMode) {
        Util.setRepeatMode(this, repeatMode);
    }

    @Override
    public boolean getKeepScreenOn() {
    	return keepScreenOn;
    }

    @Override
    public void setKeepScreenOn(boolean keepScreenOn) {
    	this.keepScreenOn = keepScreenOn;
    }

    @Override
    public boolean getShowVisualization() {
        return showVisualization;
    }

    @Override
    public void setShowVisualization(boolean showVisualization) {
        this.showVisualization = showVisualization;
    }

    @Override
    public synchronized DownloadFile forSong(MusicDirectory.Entry song) {
        for (DownloadFile downloadFile : downloadList) {
            if (downloadFile.getSong().equals(song)) {
                return downloadFile;
            }
        }

        DownloadFile downloadFile = downloadFileCache.get(song);
        if (downloadFile == null) {
            downloadFile = new DownloadFile(this, song, false);
            downloadFileCache.put(song, downloadFile);
        }
        return downloadFile;
    }

    @Override
    public synchronized void clear() {
        clear(true);
    }

    @Override
    public synchronized void clearIncomplete() {
        reset();
        Iterator<DownloadFile> iterator = downloadList.iterator();
        while (iterator.hasNext()) {
            DownloadFile downloadFile = iterator.next();
            if (!downloadFile.isCompleteFileAvailable()) {
                iterator.remove();
            }
        }
        lifecycleSupport.serializeDownloadQueue();
        updateJukeboxPlaylist();
    }

    @Override
    public synchronized int size() {
        return downloadList.size();
    }

    public synchronized void clear(boolean serialize) {
        reset();
        downloadList.clear();
        revision++;
        if (currentDownloading != null) {
            currentDownloading.cancelDownload();
            currentDownloading = null;
        }
        setCurrentPlaying(null, false);

        if (serialize) {
            lifecycleSupport.serializeDownloadQueue();
        }
        updateJukeboxPlaylist();
    }

    @Override
    public synchronized void remove(DownloadFile downloadFile) {
        if (downloadFile == currentDownloading) {
            currentDownloading.cancelDownload();
            currentDownloading = null;
        }
        if (downloadFile == currentPlaying) {
            reset();
            setCurrentPlaying(null, false);
        }
        downloadList.remove(downloadFile);
        revision++;
        lifecycleSupport.serializeDownloadQueue();
        updateJukeboxPlaylist();
    }

    @Override
    public synchronized void delete(List<MusicDirectory.Entry> songs) {
        for (MusicDirectory.Entry song : songs) {
            forSong(song).delete();
        }
    }

    @Override
    public synchronized void unpin(List<MusicDirectory.Entry> songs) {
        for (MusicDirectory.Entry song : songs) {
            forSong(song).unpin();
        }
    }

    synchronized void setCurrentPlaying(int currentPlayingIndex, boolean showNotification) {
        try {
            setCurrentPlaying(downloadList.get(currentPlayingIndex), showNotification);
        } catch (IndexOutOfBoundsException x) {
            // Ignored
        }
    }

    synchronized void setCurrentPlaying(DownloadFile currentPlaying, boolean showNotification) {
        this.currentPlaying = currentPlaying;

        if (currentPlaying != null) {
        	Util.broadcastNewTrackInfo(this, currentPlaying.getSong());
        } else {
            Util.broadcastNewTrackInfo(this, null);
        }

        if (currentPlaying != null && showNotification) {
            Util.showPlayingNotification(this, this, handler, currentPlaying.getSong());
        } else {
            Util.hidePlayingNotification(this, this, handler);
        }
    }

    @Override
    public synchronized int getCurrentPlayingIndex() {
        return downloadList.indexOf(currentPlaying);
    }

    @Override
    public DownloadFile getCurrentPlaying() {
        return currentPlaying;
    }

    @Override
    public DownloadFile getCurrentDownloading() {
        return currentDownloading;
    }

    @Override
    public synchronized List<DownloadFile> getDownloads() {
        return new ArrayList<DownloadFile>(downloadList);
    }

    /** Plays either the current song (resume) or the first/next one in queue. */
    public synchronized void play()
    {
        int current = getCurrentPlayingIndex();
        if (current == -1) {
            play(0);
        } else {
            play(current);
        }
    }

    @Override
    public synchronized void play(int index) {
        play(index, true);
    }

    private synchronized void play(int index, boolean start) {
        if (index < 0 || index >= size()) {
            reset();
            setCurrentPlaying(null, false);
        } else {
            setCurrentPlaying(index, start);
            checkDownloads();
            if (start) {
                if (jukeboxEnabled) {
                    jukeboxService.skip(getCurrentPlayingIndex(), 0);
                    setPlayerState(STARTED);
                } else {
                    if (audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    	Log.e(TAG, "Couldn't get audio focus.");
                    	return;
                    }
                    bufferAndPlay();
                }
            }
        }
    }

    /** Plays or resumes the playback, depending on the current player state. */
    public synchronized void togglePlayPause()
    {
        if (playerState == PAUSED || playerState == COMPLETED) {
            start();
        } else if (playerState == STOPPED || playerState == IDLE) {
        	play();
        } else if (playerState == STARTED) {
            pause();
        }
    }

    @Override
    public synchronized void seekTo(int position) {
        try {
            if (jukeboxEnabled) {
                jukeboxService.skip(getCurrentPlayingIndex(), position / 1000);
            } else {
                mediaPlayer.seekTo(position);
            }
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public synchronized void previous() {
        int index = getCurrentPlayingIndex();
        if (index == -1) {
            return;
        }

        // Restart song if played more than five seconds.
        if (getPlayerPosition() > 5000 || index == 0) {
            play(index);
        } else {
            play(index - 1);
        }
    }

    @Override
    public synchronized void next() {
        int index = getCurrentPlayingIndex();
        if (index != -1) {
            play(index + 1);
        }
    }

    private void onSongCompleted() {
        int index = getCurrentPlayingIndex();
        if (index != -1) {
            switch (getRepeatMode()) {
                case OFF:
                    play(index + 1);
                    break;
                case ALL:
                    play((index + 1) % size());
                    break;
                case SINGLE:
                    play(index);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public synchronized void pause() {
    	if (!jukeboxEnabled) audioManager.abandonAudioFocus(audioFocusListener);
    	pauseMedia();
    }
    
    @Override
    public synchronized void pauseAndRetainFocus() {
    		pauseMedia();
    }
    
    private synchronized void pauseMedia() {
    	try {
            if (playerState == STARTED) {
                if (jukeboxEnabled) {
                    jukeboxService.stop();
                } else {
                    mediaPlayer.pause();
                }
                setPlayerState(PAUSED);
            }
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public synchronized void start() {
        try {
            if (jukeboxEnabled) {
                jukeboxService.start();
            } else {
            	if (audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                	Log.e(TAG, "Couldn't get audio focus.");
                	return;
                }
                mediaPlayer.start();
            }
            setPlayerState(STARTED);
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public synchronized void reset() {
        if (bufferTask != null) {
            bufferTask.cancel();
        }
        try {
            mediaPlayer.reset();
            setPlayerState(IDLE);
            audioManager.abandonAudioFocus(audioFocusListener);
        } catch (Exception x) {
            handleError(x);
        }
    }

    @Override
    public synchronized int getPlayerPosition() {
        try {
            if (playerState == IDLE || playerState == DOWNLOADING || playerState == PREPARING) {
                return 0;
            }
            if (jukeboxEnabled) {
                return jukeboxService.getPositionSeconds() * 1000;
            } else {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (Exception x) {
            handleError(x);
            return 0;
        }
    }

    @Override
    public synchronized int getPlayerDuration() {
        if (currentPlaying != null) {
            Integer duration = currentPlaying.getSong().getDuration();
            if (duration != null) {
                return duration * 1000;
            }
        }
        if (playerState != IDLE && playerState != DOWNLOADING && playerState != PlayerState.PREPARING) {
            try {
                return mediaPlayer.getDuration();
            } catch (Exception x) {
                handleError(x);
            }
        }
        return 0;
    }

    @Override
    public PlayerState getPlayerState() {
        return playerState;
    }

    synchronized void setPlayerState(PlayerState playerState) {
        Log.i(TAG, this.playerState.name() + " -> " + playerState.name() + " (" + currentPlaying + ")");

        if (playerState == PAUSED) {
            lifecycleSupport.serializeDownloadQueue();
        }

        boolean show = this.playerState == PAUSED && playerState == PlayerState.STARTED;
        boolean hide = this.playerState == STARTED && playerState == PlayerState.PAUSED;
        Util.broadcastPlaybackStatusChange(this, playerState);

        this.playerState = playerState;
        if (show) {
            Util.showPlayingNotification(this, this, handler, currentPlaying.getSong());
        } else if (hide) {
            Util.hidePlayingNotification(this, this, handler);
        }

        if (playerState == STARTED) {
            scrobbler.scrobble(this, currentPlaying, false);
        } else if (playerState == COMPLETED) {
            scrobbler.scrobble(this, currentPlaying, true);
        }
    }

    @Override
    public void setSuggestedPlaylistName(String name) {
        this.suggestedPlaylistName = name;
    }

    @Override
    public String getSuggestedPlaylistName() {
        return suggestedPlaylistName;
    }

    @Override
    public EqualizerController getEqualizerController() {
        return equalizerController;
    }

    @Override
    public VisualizerController getVisualizerController() {
        return visualizerController;
    }

    @Override
    public boolean isJukeboxEnabled() {
        return jukeboxEnabled;
    }

    @Override
    public void setJukeboxEnabled(boolean jukeboxEnabled) {
        this.jukeboxEnabled = jukeboxEnabled;
        jukeboxService.setEnabled(jukeboxEnabled);
        if (jukeboxEnabled) {
            reset();
            
            // Cancel current download, if necessary.
            if (currentDownloading != null) {
                currentDownloading.cancelDownload();
            }
        }
    }

    @Override
    public void adjustJukeboxVolume(boolean up) {
        jukeboxService.adjustVolume(up);
    }
    
    private synchronized void setVolume(float volume) {
    	if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(volume, volume);
    }

    private synchronized void bufferAndPlay() {
        reset();

        bufferTask = new BufferTask(currentPlaying, 0);
        bufferTask.start();
    }

    private synchronized void doPlay(final DownloadFile downloadFile, int position, boolean start) {
        try {
            final File file = downloadFile.isCompleteFileAvailable() ? downloadFile.getCompleteFile() : downloadFile.getPartialFile();
            downloadFile.updateModificationDate();
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.reset();
            setPlayerState(IDLE);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(file.getPath());
            setPlayerState(PREPARING);
            mediaPlayer.prepare();
            setPlayerState(PREPARED);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {

                    // Acquire a temporary wakelock, since when we return from
                    // this callback the MediaPlayer will release its wakelock
                    // and allow the device to go to sleep.
                    wakeLock.acquire(60000);

                    setPlayerState(COMPLETED);

                    // If COMPLETED and not playing partial file, we are *really" finished
                    // with the song and can move on to the next.
                    if (!file.equals(downloadFile.getPartialFile())) {
                        onSongCompleted();
                        return;
                    }

                    // If file is not completely downloaded, restart the playback from the current position.
                    int pos = mediaPlayer.getCurrentPosition();
                    synchronized (DownloadServiceImpl.this) {

                        // Work-around for apparent bug on certain phones: If close (less than ten seconds) to the end
                        // of the song, skip to the next rather than restarting it.
                        Integer duration = downloadFile.getSong().getDuration() == null ? null : downloadFile.getSong().getDuration() * 1000;
                        if (duration != null) {
                            if (Math.abs(duration - pos) < 10000) {
                                Log.i(TAG, "Skipping restart from " + pos  + " of " + duration);
                                onSongCompleted();
                                return;
                            }
                        }

                        Log.i(TAG, "Requesting restart from " + pos  + " of " + duration);
                        reset();
                        bufferTask = new BufferTask(downloadFile, pos);
                        bufferTask.start();
                    }
                }
            });

            if (position != 0) {
                Log.i(TAG, "Restarting player from position " + position);
                mediaPlayer.seekTo(position);
            }

            if (start) {
            	if (audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                	Log.e(TAG, "Couldn't get audio focus.");
                	return;
                }
                mediaPlayer.start();
                setPlayerState(STARTED);
            } else {
                setPlayerState(PAUSED);
            }
            lifecycleSupport.serializeDownloadQueue();

        } catch (Exception x) {
            handleError(x);
        }
    }

    private void handleError(Exception x) {
        Log.w(TAG, "Media player error: " + x, x);
        mediaPlayer.reset();
        setPlayerState(IDLE);
        audioManager.abandonAudioFocus(audioFocusListener);
    }

    protected synchronized void checkDownloads() {

        if (!Util.isExternalStoragePresent() || !lifecycleSupport.isExternalStorageAvailable()) {
            return;
        }

        if (shufflePlay) {
            checkShufflePlay();
        }

        if (jukeboxEnabled || !Util.isNetworkConnected(this)) {
            return;
        }

        if (downloadList.isEmpty()) {
            return;
        }

        // Need to download current playing?
        if (currentPlaying != null &&
                currentPlaying != currentDownloading &&
                !currentPlaying.isCompleteFileAvailable()) {

            // Cancel current download, if necessary.
            if (currentDownloading != null) {
                currentDownloading.cancelDownload();
            }

            currentDownloading = currentPlaying;
            currentDownloading.download();
            cleanupCandidates.add(currentDownloading);
        }

        // Find a suitable target for download.
        else if (currentDownloading == null || currentDownloading.isWorkDone() || currentDownloading.isFailed()) {

            int n = size();
            if (n == 0) {
                return;
            }

            int preloaded = 0;

            int start = currentPlaying == null ? 0 : getCurrentPlayingIndex();
            int i = start;
            do {
                DownloadFile downloadFile = downloadList.get(i);
                if (!downloadFile.isWorkDone()) {
                    if (downloadFile.shouldSave() || preloaded < Util.getPreloadCount(this)) {
                        currentDownloading = downloadFile;
                        currentDownloading.download();
                        cleanupCandidates.add(currentDownloading);
                        break;
                    }
                } else if (currentPlaying != downloadFile) {
                    preloaded++;
                }

                i = (i + 1) % n;
            } while (i != start);
        }

        // Delete obsolete .partial and .complete files.
        cleanup();
    }

    private synchronized void checkShufflePlay() {

        final int listSize = 20;
        boolean wasEmpty = downloadList.isEmpty();

        long revisionBefore = revision;

        // First, ensure that list is at least 20 songs long.
        int size = size();
        if (size < listSize) {
            for (MusicDirectory.Entry song : shufflePlayBuffer.get(listSize - size)) {
                DownloadFile downloadFile = new DownloadFile(this, song, false);
                downloadList.add(downloadFile);
                revision++;
            }
        }

        int currIndex = currentPlaying == null ? 0 : getCurrentPlayingIndex();

        // Only shift playlist if playing song #5 or later.
        if (currIndex > 4) {
            int songsToShift = currIndex - 2;
            for (MusicDirectory.Entry song : shufflePlayBuffer.get(songsToShift)) {
                downloadList.add(new DownloadFile(this, song, false));
                downloadList.get(0).cancelDownload();
                downloadList.remove(0);
                revision++;
            }
        }

        if (revisionBefore != revision) {
            updateJukeboxPlaylist();
        }

        if (wasEmpty && !downloadList.isEmpty()) {
            play(0);
        }
    }

    public long getDownloadListUpdateRevision() {
        return revision;
    }

    private synchronized void cleanup() {
        Iterator<DownloadFile> iterator = cleanupCandidates.iterator();
        while (iterator.hasNext()) {
            DownloadFile downloadFile = iterator.next();
            if (downloadFile != currentPlaying && downloadFile != currentDownloading) {
                if (downloadFile.cleanup()) {
                    iterator.remove();
                }
            }
        }
    }

    private class BufferTask extends CancellableTask {

        private static final int BUFFER_LENGTH_SECONDS = 5;

        private final DownloadFile downloadFile;
        private final int position;
        private final long expectedFileSize;
        private final File partialFile;

        public BufferTask(DownloadFile downloadFile, int position) {
            this.downloadFile = downloadFile;
            this.position = position;
            partialFile = downloadFile.getPartialFile();

            // Calculate roughly how many bytes BUFFER_LENGTH_SECONDS corresponds to.
            int bitRate = downloadFile.getBitRate();
            long byteCount = Math.max(100000, bitRate * 1024 / 8 * BUFFER_LENGTH_SECONDS);

            // Find out how large the file should grow before resuming playback.
            expectedFileSize = partialFile.length() + byteCount;
        }

        @Override
        public void execute() {
            setPlayerState(DOWNLOADING);

            while (!bufferComplete()) {
                Util.sleepQuietly(1000L);
                if (isCancelled()) {
                    return;
                }
            }
            doPlay(downloadFile, position, true);
        }

        private boolean bufferComplete() {
            boolean completeFileAvailable = downloadFile.isCompleteFileAvailable();
            long size = partialFile.length();

            Log.i(TAG, "Buffering " + partialFile + " (" + size + "/" + expectedFileSize + ", " + completeFileAvailable + ")");
            return completeFileAvailable || size >= expectedFileSize;
        }

        @Override
        public String toString() {
            return "BufferTask (" + downloadFile + ")";
        }
    }
    
    private class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {
    	
    	private final DownloadServiceImpl downloadService;
    	
    	public AudioFocusListener(DownloadServiceImpl downloadService) {
    		this.downloadService = downloadService;
    	}
    	
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					if (!downloadService.isJukeboxEnabled()) {
						downloadService.setVolume(1.0f);
						downloadService.start();
					}
					break;
					
				case AudioManager.AUDIOFOCUS_LOSS:
					if (!downloadService.isJukeboxEnabled()) {
						downloadService.pause();
					}
					break;
				
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					if (!downloadService.isJukeboxEnabled()) {
						downloadService.pauseAndRetainFocus();
					}
					break;
				
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					if (!downloadService.isJukeboxEnabled()) {
						downloadService.setVolume(0.1f);
					}
					break;
			}
		}
    }
}
