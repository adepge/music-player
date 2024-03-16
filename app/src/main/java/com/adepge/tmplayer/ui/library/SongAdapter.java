package com.adepge.tmplayer.ui.library;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.adepge.tmplayer.R;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * This adapter generates child Views from the data source (song list) to populate
 * the RecyclerView in {@link LibraryFragment}
 *
 * @version 1.0
 * @author Adam George
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    // Interface for ClickListener on a song
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    // Static class for child ViewHolder
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        TextView title;
        TextView artist;

        // Initialise view resources
        public SongViewHolder(View v) {
            super(v);
            albumCover = v.findViewById(R.id.album_thumbnail);
            title = v.findViewById(R.id.song_list_title);
            artist = v.findViewById(R.id.song_list_artist);
        }
    }

    private List<Song> songs;
    private OnSongClickListener listener;

    // Constructor
    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    /**
     * Creates the ViewHolder
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     *               an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return the SongViewHolder (album art, song title, artist)
     */
    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(v);
    }

    /**
     * Load data into ViewHolder after binding adapter to the RecyclerView
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {
        Song song = songs.get(position);
        Log.d("SongAdapter", "Binding song: " + song.getTitle());
        Glide.with(holder.albumCover.getContext()).load(song.getAlbumCover()).error(R.drawable.album_empty).into(holder.albumCover);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Song song = songs.get(position);
                    Log.d("SongAdapter", "Song clicked: " + song.getTitle());
                    listener.onSongClick(song);
                }
            }
        });
    }

    /**
     * Getter method
     * @return the number of songs in the SongList
     */
    @Override
    public int getItemCount() {
        return songs.size();
    }
}
