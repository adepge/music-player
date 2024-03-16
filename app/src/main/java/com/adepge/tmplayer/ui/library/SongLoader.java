package com.adepge.tmplayer.ui.library;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * The SongLoader class contains the {@link SongLoader#loadSongs(Context)} method
 * to load in song files from the content provider ({@link MediaStore} API)
 *
 * @version 1.0
 * @author Adam George
 */
public class SongLoader {

    /**
     * Method to query the {@link MediaStore} API and load all songs
     * found on the device in a list
     *
     * @param context
     * @return a list of {@link Song} objects
     */
    public static List<Song> loadSongs(Context context) {
        List<Song> songs = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songAlbumId = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            // Construct a Song object for each file returned
            do {
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                long currentAlbumId = songCursor.getLong(songAlbumId);

                @SuppressLint("Range") long id = songCursor.getLong(songCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                Log.d("SongLoader", "Song ID: " + id);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Log.d("SongLoader", "Song Uri: " + contentUri);

                // Query the URI for the AlbumArt (Obtaining the real file path has been restricted after Android 10.0)
                String currentAlbumArt = String.valueOf(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),currentAlbumId));
                songs.add(new Song(currentAlbumArt, currentTitle, currentArtist, contentUri));
            } while (songCursor.moveToNext());
        }
        // Close songCursor when reaching the end of list
        if (songCursor != null) {
            songCursor.close();
        }
        return songs;
    }
}
