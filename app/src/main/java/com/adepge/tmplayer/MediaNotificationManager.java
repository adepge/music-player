package com.adepge.tmplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

/**
 * The MediaNotificationManager is class used to manage and update the media playback
 * notification (foreground service) whenever a song is playing
 *
 * @version 1.0
 * @author Adam George
 */
public class MediaNotificationManager {

    // Fields for creating notification channel
    private static final String CHANNEL_ID = "media_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    private final Context context;
    private final NotificationManager notificationManager;

    // Field for accessing media session
    private final MediaSessionCompat mediaSession;

    /**
     * Method for creating notification channel (in order to handle notification changes in Android 8.0+)
     * @param context set Context
     * @param mediaSession set mediaSession
     */
    public MediaNotificationManager(Context context, MediaSessionCompat mediaSession) {
        this.context = context;
        this.mediaSession = mediaSession;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Method which builds the notification and sets the style of the notification
     *
     * @param title song title
     * @param artist song artist
     * @param albumArt album cover image URI
     * @param isPlaying whether mediaSession is currently playing
     * @return Notification object
     */
    public Notification showNotification(String title, String artist, Bitmap albumArt, boolean isPlaying) {
        Log.d("MediaNotificationManager","Notification updated");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mp_notification_logo)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(albumArt)
                .setDeleteIntent(getActionIntent("ACTION_STOP")) // When notification is removed / swiped away
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT) // Media Playback type of notification
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(new NotificationCompat.Action(R.drawable.media_previous_small, "Previous", getActionIntent("ACTION_SKIP_TO_PREVIOUS")));

        // Shows play/pause depending on whether the song is playing or not
        if (isPlaying) {
            Log.d("MediaNotificationManager","Rendered pause button");
            builder.addAction(new NotificationCompat.Action(R.drawable.media_pause_small, "Pause", getActionIntent("ACTION_PAUSE")));
        } else {
            Log.d("MediaNotificationManager","Rendered play button");
            builder.addAction(new NotificationCompat.Action(R.drawable.media_play_small, "Play", getActionIntent("ACTION_PLAY")));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder()
                            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                            .build()
            );
        }

        builder.addAction(new NotificationCompat.Action(R.drawable.media_next_small, "Next", getActionIntent("ACTION_SKIP_TO_NEXT")))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2) // Shows media buttons in compact view
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(getActionIntent("ACTION_STOP")));
        Log.d("MediaNotificationManager","Style set");

        return builder.build();
    }

    // Method which provides the Intent actions from the notification buttons
    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(context, MediaPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Method which updates the notification (upon song or playback state changes)
     * @param title song title
     * @param artist song artist
     * @param albumArt album cover image
     * @param isPlaying whether mediaSession is currently playing
     */
    @SuppressLint("MissingPermission")
    public void updateNotification(String title, String artist, Bitmap albumArt, boolean isPlaying) {
        Notification notification = showNotification(title, artist, albumArt, isPlaying);
        NotificationManagerCompat.from(this.context).notify(NOTIFICATION_ID, notification);
        Log.d("MediaSessionToken", "Token: " + mediaSession.getSessionToken().toString());
    }
}

