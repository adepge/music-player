package com.adepge.tmplayer;
import com.adepge.tmplayer.ui.library.Song;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import android.Manifest;
import android.support.v4.media.MediaMetadataCompat;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.adepge.tmplayer.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.Arrays;

/**
 * The main activity which hosts the {@link com.adepge.tmplayer.ui.library.LibraryFragment} and
 * {@link com.adepge.tmplayer.ui.playing.PlayingFragment} and targets runtime permissions
 *
 * @version 1.0
 * @author Adam George
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Prompt for runtime permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                        if (!isGranted.containsValue(false)) {
                            Toast.makeText(MainActivity.this, "This app needs to access your media and audio files to function properly.", Toast.LENGTH_LONG).show();
                        }
                    });

            requestPermissionLauncher.launch(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            // Prompt for READ_EXTERNAL_STORAGE permission (Android 9-12)
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                        if (isGranted.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)) {
                            // READ_EXTERNAL_STORAGE permission is granted, do nothing
                        } else {
                            Toast.makeText(MainActivity.this, "This app needs to access your media and audio files to function properly.", Toast.LENGTH_LONG).show();
                        }
                    });

            requestPermissionLauncher.launch(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }

    /**
     * Set the bottom navigation view to switch between fragments
     */
    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }
}