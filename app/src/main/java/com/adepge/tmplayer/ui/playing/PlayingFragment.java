package com.adepge.tmplayer.ui.playing;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.adepge.tmplayer.MainActivity;
import com.adepge.tmplayer.MediaPlaybackService;
import com.adepge.tmplayer.R;
import com.adepge.tmplayer.ui.library.Song;
import com.bumptech.glide.Glide;

/**
 * This Fragment displays the NowPlaying activity - which shows
 * the song currently playing and some media controls
 *
 * @version 1.0
 * @author Adam George
 */
public class PlayingFragment extends Fragment {

    private MediaPlaybackService mediaPlaybackService;

    // View resource fields
    private TextView songTitle;
    private TextView songArtist;
    private ImageButton playButton;
    private ImageButton skipForward;
    private ImageButton skipBackward;
    private ImageButton repeatButton;
    private ImageButton shuffleButton;
    private ImageView albumCover;

    // Boolean switches (service connection & media controls)
    private boolean isBound = false;

    private boolean isRepeat = false;
    private boolean isShuffle = false;

    // For PlayingFragment seek bar
    private SeekBar seekBar;
    private Handler handler = new Handler();

    private Runnable seekBarUpdater;

    /**
     * When the Fragment is called to be visible, does the following:
     * <ul>
     *     <li>Updates the seek bar</li>
     *     <li>Binds the {@link MediaPlaybackService}</li>
     * </ul>
     */
    @Override
    public void onStart() {
        super.onStart();
        startSeekBarUpdater();
        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (mediaPlaybackService != null) {
            if (mediaPlaybackService.isPlaying()) {
                playButton.setBackgroundResource(R.drawable.media_pause); // Set pause icon
            } else {
                playButton.setBackgroundResource(R.drawable.media_play); // Set play icon
            }
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            /**
             * Method to seek to different position in song when user moves the seek bar
             *
             * @param seekBar The SeekBar whose progress has changed
             * @param progress The current progress level. This will be in the range min..max where min
             *                 and max were set by {@link ProgressBar#setMin(int)} and
             *                 {@link ProgressBar#setMax(int)}, respectively. (The default values for
             *                 min is 0 and max is 100.)
             * @param fromUser True if the progress change was initiated by the user.
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newPosition = (int) (((float) progress / 100) * mediaPlaybackService.getSongDuration());
                    mediaPlaybackService.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing (no intended implementation needed)
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing (no intended implementation needed)
            }
        });
    }

    /**
     * Method to create PlayingFragment View
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return PlayingFragment View
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing, container, false);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        /**
         * Binds the {@link MediaPlaybackService} to the PlayingFragment
         *
         * @param name The concrete component name of the service that has
         * been connected.
         * @param service The IBinder of the Service's communication channel,
         * which you can now make calls on.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("serviceConnection", "Service has successfully connected");
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaPlaybackService = binder.getService();
            mediaPlaybackService.setOnSongChangedListener(new MediaPlaybackService.OnSongChangedListener() {
                /**
                 * Method which updates the song details (album art, song title, artist) in the fragment
                 * when the song changes
                 * @param song
                 */
                @Override
                public void onSongChanged(Song song) {
                    Song currentSong = mediaPlaybackService.getCurrentSong();
                    if (currentSong != null) {
                        songTitle.setText(currentSong.getTitle());
                        songArtist.setText(currentSong.getArtist());

                        String albumArtUri = currentSong.getAlbumCover();
                        if (albumArtUri != null) {
                            Glide.with(getActivity())
                                    .load(albumArtUri)
                                    .error(R.drawable.album_empty) // Fallback icon when no album art found
                                    .into(albumCover);
                        }
                    }
                    // Set maximum progress as 100%
                    seekBar.setMax(100);
                    updateSeekBar();
                }
            });
            isBound = true;

            // Changes play to pause button (and vice versa) depending on whether song is playing
            if (mediaPlaybackService.isPlaying()) {
                playButton.setBackgroundResource(R.drawable.media_pause); // Set pause icon
            } else {
                playButton.setBackgroundResource(R.drawable.media_play); // Set play icon
            }

            // Method is called upon the first song being played with the MediaPlaybackService
            Song currentSong = mediaPlaybackService.getCurrentSong();
            if (currentSong != null) {
                songTitle.setText(currentSong.getTitle());
                songArtist.setText(currentSong.getArtist());

                String albumArtUri = currentSong.getAlbumCover();
                if (albumArtUri != null) {
                    Glide.with(getActivity())
                            .load(albumArtUri)
                            .error(R.drawable.album_empty)
                            .into(albumCover);
                } else {
                    // Set a default image or hide the ImageView if there's no album cover
                    albumCover.setImageResource(R.drawable.album_empty);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    /**
     * Method to register all the relevant listeners to the buttons / seekbar in View
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise view resources
        albumCover = view.findViewById(R.id.album_cover);
        songTitle = view.findViewById(R.id.song_title);
        songArtist = view.findViewById(R.id.song_album);
        playButton = view.findViewById(R.id.play_button);
        skipForward = view.findViewById(R.id.skip_forward);
        skipBackward = view.findViewById(R.id.skip_backward);
        repeatButton = view.findViewById(R.id.track_repeat);
        shuffleButton = view.findViewById(R.id.shuffle);
        seekBar = view.findViewById(R.id.song_seek_bar);

        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Sends the pause/play Intent to MediaPlaybackService (play/pause button)
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                if (mediaPlaybackService.isPlaying()) {
                    intent.setAction("ACTION_PAUSE");
                    playButton.setBackgroundResource(R.drawable.media_play);
                } else {
                    intent.setAction("ACTION_PLAY");
                    playButton.setBackgroundResource(R.drawable.media_pause);
                }
                getActivity().startService(intent);
            }
        });

        // Sends the skip to next song Intent to MediaPlaybackService
        skipForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                intent.setAction("ACTION_SKIP_TO_NEXT");
                getActivity().startService(intent);
            }
        });

        // Sends the skip to previous song Intent to MediaPlaybackService
        skipBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                intent.setAction("ACTION_SKIP_TO_PREVIOUS");
                getActivity().startService(intent);
            }
        });

        // Sets the song to repeat when repeat button is clicked
        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    isRepeat = !isRepeat;
                    mediaPlaybackService.setRepeat(isRepeat);
                    if (isRepeat) {
                        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#34C95C")); // Change to your desired color
                        repeatButton.setBackgroundTintList(colorStateList);
                    } else {
                        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
                        repeatButton.setBackgroundTintList(inactiveStateList);
                    }
                }
            }
        });

        // When shuffle button is clicked, songs do not play in their listed order / play in random order
        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    isShuffle = !isShuffle;
                    mediaPlaybackService.setShuffle(isShuffle);
                    if (isShuffle) {
                        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#34C95C")); // Change to your desired color
                        shuffleButton.setBackgroundTintList(colorStateList);
                    } else {
                        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
                        shuffleButton.setBackgroundTintList(inactiveStateList);
                    }
                }
            }
        });
    }

    /**
     * Method to unbind {@link MediaPlaybackService} and stop seek bar
     * from updating when the Fragment is no longer visible
     */
    @Override
    public void onStop() {
        super.onStop();
        stopSeekBarUpdater();
        if (isBound) {
            mediaPlaybackService.setOnSongChangedListener(null);
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    /**
     * Detaches Fragment from the {@link MainActivity}
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (isBound) {
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }

    /**
     * Method to update the seekbar from (1-100%) depending on the progress
     * of the song relative to its entire duration (updated every second)
     *
     * <p>Additional info: Longer songs will have a slower seek bar
     * and the seekbar movement will skip a larger proportion of time
     * compared to a shorter song</p>
     */
    private void updateSeekBar() {
        if (mediaPlaybackService != null) {
            int progress = (int) (((float) mediaPlaybackService.getCurrentPosition() / mediaPlaybackService.getSongDuration()) * 100);
            seekBar.setProgress(progress);
            if (mediaPlaybackService.isPlaying()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateSeekBar();
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        }
    }

    /**
     * Method which calls the seek bar updater upon
     * the Fragment becoming visible
     */
    private void startSeekBarUpdater() {
        stopSeekBarUpdater();
        seekBarUpdater = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(seekBarUpdater, 1000);
    }

    /**
     * Detaches the Runnable instance of seek bar updater
     */
    private void stopSeekBarUpdater() {
        if (seekBarUpdater != null) {
            handler.removeCallbacks(seekBarUpdater);
            seekBarUpdater = null;
        }
    }
}