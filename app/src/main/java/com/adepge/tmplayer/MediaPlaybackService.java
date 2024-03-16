package com.adepge.tmplayer;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Binder;
import android.util.Log;
import android.view.KeyEvent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.adepge.tmplayer.ui.library.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The MediaPlaybackService manages the playback of songs, handles all media action Intents,
 * stores current song information and the current song list
 *
 * @version 1.0
 * @author Adam George
 */
public class MediaPlaybackService extends MediaBrowserServiceCompat {

    // Fields for list of songs
    private List<Song> songList;
    private int currentSongIndex;

    // MediaSession fields
    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private MediaNotificationManager mediaNotificationManager;

    // Current song information
    private String currentTitle;
    private String currentArtist;
    private Bitmap currentAlbumArt;
    private Song currentSong;
    private static final int NOTIFICATION_ID = 1;
    private OnSongChangedListener onSongChangedListener;

    // Boolean switches (for checks and media actions)
    private boolean isRepeat = false;

    private boolean isShuffle = false;
    boolean isSwitchingSongs = false;

    // Fields for shuffle and skipping functionality
    private List<Integer> shuffledIndices;
    private int currentIndex = 0;
    private boolean userHasSkipped = false;

    // SharedPreferences fields
    private static final String PREFS_NAME = "com.adepge.tmplayer";
    private static final String PREF_CURRENT_SONG_INDEX = "currentSongIndex";
    private static final String PREF_SONG_POSITION = "songPosition";

    /**
     * An interface that describes the methods called when the (song) change event
     * is registered
     */
    public interface OnSongChangedListener {
        void onSongChanged(Song song);
    }

    /**
     * Method to save the current song playing and position which persists
     * even after the lifecycle of the app Activities
     */
    private void saveSongInfo() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_CURRENT_SONG_INDEX, currentSongIndex);
        editor.putInt(PREF_SONG_POSITION, mediaPlayer.getCurrentPosition());
        editor.apply();
    }

    // Binder class for MediaPlaybackService
    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }

    // Setter for songChanged listener
    public void setOnSongChangedListener(OnSongChangedListener listener) {
        this.onSongChangedListener = listener;
    }

    // Getter for isPlaying() - whether a song is playing in the media session
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    // Setter for repeat option
    public void setRepeat(boolean isRepeat) {
        this.isRepeat = isRepeat;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, "MusicPlayer");
        mediaPlayer = new MediaPlayer();

        // Set the media session's callback
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            /**
             * Handles the different action Intents with corresponding methods
             * @param command The command name.
             * @param extras Optional parameters for the command, may be null.
             * @param cb A result receiver to which a result may be sent by the command, may be null.
             */
            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                switch (command) {
                    case "ACTION_PLAY":
                        onPlay();
                        break;
                    case "ACTION_PAUSE":
                        onPause();
                        break;
                    case "ACTION_SKIP_TO_NEXT":
                        onSkipToNext();
                        break;
                    case "ACTION_SKIP_TO_PREVIOUS":
                        onSkipToPrevious();
                        break;
                    case "ACTION_STOP":
                        onStop();
                        break;
                    default:
                        super.onCommand(command, extras, cb);
                }
            }

            /**
             * Method which starts the playback of a song and updates the notification
             */
            @Override
            public void onPlay() {
                super.onPlay();
                mediaPlayer.start();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                        .build());
                mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, true);
            }

            /**
             * Method which pauses the playback of a song and updates the notification
             */
            @Override
            public void onPause() {
                super.onPause();
                mediaPlayer.pause();
                saveSongInfo();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                        .build());
                mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, false);
            }

            /**
             * Method which stops the playback of a song and removes the notification service
             */
            @Override
            public void onStop() {
                super.onStop();
                mediaPlayer.stop();
                saveSongInfo();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                        .build());
                stopForeground(true);
            }

            /**
             * Method for skipping to the next song manually
             */
            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                userHasSkipped = true; // Flag to determine whether the song change was manually called
                if (currentSongIndex < songList.size() - 1) {
                    if (isRepeat) {
                        playSong(songList.get(currentSongIndex)); // Continues to play same song
                    } else {
                        if (isShuffle) {
                            currentIndex = (currentIndex + 1) % shuffledIndices.size(); // Use shuffled index when shuffle option is true
                            currentSongIndex = shuffledIndices.get(currentIndex);
                        } else {
                            currentSongIndex++; // Go to next song index (in order)
                        }
                        playSong(songList.get(currentSongIndex));
                    }
                }
            }

            /**
             * Method for skipping to the previous song manually
             */
            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                userHasSkipped = true; // Flag to determine whether the song change was manually called
                if (currentSongIndex > 0) {
                    if (isRepeat) {
                        playSong(songList.get(currentSongIndex)); // Continues to play same song
                    } else {
                        if (isShuffle) {
                            currentIndex = (currentIndex - 1) % shuffledIndices.size(); // Use shuffled index when shuffle option is true
                            currentSongIndex = shuffledIndices.get(currentIndex);
                        } else {
                            currentSongIndex--; // Go to previous song index (in order)
                        }
                        playSong(songList.get(currentSongIndex));
                    }
                }
            }
        });

        // Activate the media session
        mediaSession.setActive(true);
        mediaNotificationManager = new MediaNotificationManager(this, mediaSession);

        // Sets the MediaBrowserService token to the MediaSession's token
        setSessionToken(mediaSession.getSessionToken());

        // If a currently playing song has been stored in SharedPreferences, loads the song and position
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(PREF_CURRENT_SONG_INDEX) && prefs.contains(PREF_SONG_POSITION)) {
            currentSongIndex = prefs.getInt(PREF_CURRENT_SONG_INDEX, 0);
            int songPosition = prefs.getInt(PREF_SONG_POSITION, 0);
            // Checks if songList has been initialised first
            if (songList != null && !songList.isEmpty()) {
                playSong(songList.get(currentSongIndex));
                mediaPlayer.seekTo(songPosition);
            }
        }
    }

    // Helper functions for MediaBrowserService and MediaPlaybackService
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(new ArrayList<>());
    }

    /**
     * Method which takes the given {@link Song} object and plays
     * the song in the app
     *
     * @param song song to play
     */
    public void playSong(Song song) {
        currentTitle = song.getTitle();
        currentArtist = song.getArtist();
        currentAlbumArt = getAlbumArt(song.getAlbumCover());
        currentSong = song;
        try {
            // Flag in order to avoid recursively calling onCompleted event (due to mediaPlayer.reset())
            isSwitchingSongs = true;
            mediaPlayer.reset();
            Log.d("MediaPlaybackService", "Song Uri: " + song.getSongUri());
            mediaPlayer.setDataSource(this, song.getSongUri());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                /**
                 * After the mediaPlayer is initialised, set the playbackState, media actions and start
                 * the notification foreground service
                 * @param mp the MediaPlayer that is ready for playback
                 */
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                            .build());
                    Notification notification = mediaNotificationManager.showNotification(currentTitle, currentArtist, currentAlbumArt, mp.isPlaying());
                    startForeground(NOTIFICATION_ID, notification);
                    isSwitchingSongs = false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                /**
                 * Upon song completed, handles the next song behaviour according to the settings available
                 * @param mp the MediaPlayer that reached the end of the file
                 */
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!isSwitchingSongs && !userHasSkipped) {
                        if (isRepeat) {
                            playSong(song);
                        } else {
                            if (isShuffle) {
                                currentIndex = (currentIndex + 1) % shuffledIndices.size(); // Use shuffled index when shuffle option is true
                                playSong(songList.get(shuffledIndices.get(currentIndex)));
                            } else {
                                if (currentSongIndex < songList.size() - 1) {
                                    currentSongIndex++;
                                    playSong(songList.get(currentSongIndex));
                                } else {
                                    mediaPlayer.stop(); // Upon reaching the end of all songs, stops playing
                                }
                            }
                        }
                    }
                    userHasSkipped = false;
                }
            });
            if (onSongChangedListener != null) {
                onSongChangedListener.onSongChanged(song);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls {@link MediaPlaybackService#playSong(Song)} while specifying
     * the index at which the song being played is in the list
     * @param songs
     * @param startIndex
     *
     * <p>Additional info: startIndex does not compensate for position in song list;
     * If you play the last song in the list (without shuffling),
     * it will stop after that (due to reaching end of the list)</p>
     */
    public void playSongs(List<Song> songs, int startIndex) {
        songList = songs;
        currentSongIndex = startIndex;
        playSong(songList.get(currentSongIndex));
    }

    /**
     * Releases mediaPlayer and mediaSession when service is stopped
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaSession.setActive(false);
        mediaSession.release();
    }

    /**
     * Handles the Media Action Buttons (includes headsets, speakers etc.)
     *
     * @param intent The Intent supplied to {@link android.content.Context#startService},
     * as given.  This may be null if the service is being restarted after
     * its process has gone away, and it had previously returned anything
     * except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     * start.  Use with {@link #stopSelfResult(int)}.
     *
     * @return Service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "ACTION_PLAY":
                        mediaSession.getController().getTransportControls().play();
                        break;
                    case "ACTION_PAUSE":
                        mediaSession.getController().getTransportControls().pause();
                        break;
                    case "ACTION_SKIP_TO_NEXT":
                        mediaSession.getController().getTransportControls().skipToNext();
                        break;
                    case "ACTION_SKIP_TO_PREVIOUS":
                        mediaSession.getController().getTransportControls().skipToPrevious();
                        break;
                    case "ACTION_STOP":
                        mediaSession.getController().getTransportControls().stop();
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Method to load the BitMap from an image URI
     * @param albumArtUri content URI of album cover image
     * @return bitmap of album image
     */
    public Bitmap getAlbumArt(String albumArtUri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(albumArtUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    // Getter method for current song
    public Song getCurrentSong() {
        return currentSong;
    }

    // Setter method for shuffle option
    public void setShuffle(boolean isShuffle) {
        this.isShuffle = isShuffle;
        if (isShuffle) {
            shuffleIndices();
        } else {
            currentIndex = shuffledIndices.indexOf(currentSongIndex);
        }
    }

    // Shuffles the indices (index number) order of the song list
    private void shuffleIndices() {
        shuffledIndices = new ArrayList<>();
        for (int i = 0; i < songList.size(); i++) {
            shuffledIndices.add(i);
        }
        Collections.shuffle(shuffledIndices);
        currentIndex = shuffledIndices.indexOf(currentSongIndex);
    }

    // Getter methods for song information
    public int getSongDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

}
