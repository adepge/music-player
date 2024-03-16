package com.adepge.tmplayer.ui.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adepge.tmplayer.MainActivity;
import com.adepge.tmplayer.MediaPlaybackService;
import com.adepge.tmplayer.R;
import com.adepge.tmplayer.databinding.FragmentLibraryBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This Fragment displays the list of playable songs using a {@link RecyclerView}
 *
 * @version 1.0
 * @author Adam George
 */
public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private MediaPlaybackService mediaPlaybackService;
    private boolean isBound = false;

    /**
     * A method which calls {@link SongLoader#loadSongs(Context)}, loading {@link Song} objects
     * with their ClickListeners setting the song adapter in the RecyclerView
     */
    public void loadSongs() {
        final List<Song> songs = SongLoader.loadSongs(requireActivity());
        songAdapter = new SongAdapter(songs, new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(Song song) {
                if (isBound) {
                    int songIndex = songs.indexOf(song);
                    mediaPlaybackService.playSongs(songs, songIndex);
                }
            }
        });
        recyclerView.setAdapter(songAdapter);
    }

    /**
     * Calls an instance of {@link ServiceConnection} used to bind the {@link MediaPlaybackService} to this Fragment
     * Calls {@link LibraryFragment#loadSongs()} upon service connected.
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("serviceConnection", "Service has successfully connected");
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaPlaybackService = binder.getService();
            isBound = true;
            loadSongs();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    /**
     * Method for creating the Fragment View
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = binding.songRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return root;
    }

    /**
     * Binds the {@link MediaPlaybackService} when Fragment is about to become visible
     */
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().startService(intent); // Start the MediaPlaybackService
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbinds the {@link MediaPlaybackService} when Fragment is no longe visible
     */
    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
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
        binding = null;
    }
}