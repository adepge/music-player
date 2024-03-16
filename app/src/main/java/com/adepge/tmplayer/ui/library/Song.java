package com.adepge.tmplayer.ui.library;

import android.net.Uri;

/**
 * This class defines the Song object and its attributes
 *
 * @version 1.0
 * @author Adam George
 */
public class Song {
    private Uri songUri;
    private String albumCover;
    private String title;
    private String artist;

    // Constructor
    public Song(String albumCover, String title, String artist, Uri songUri) {
        this.albumCover = albumCover;
        this.title = title;
        this.artist = artist;
        this.songUri = songUri;
    }

    // Getter methods
    public String getAlbumCover() {
        return albumCover;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    // Setter methods
    public Uri getSongUri() {
        return songUri;
    }
}