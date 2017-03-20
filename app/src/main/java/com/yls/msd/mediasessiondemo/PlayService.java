package com.yls.msd.mediasessiondemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class PlayService extends MediaBrowserServiceCompat {

    MediaSessionCompat session;
    MediaPlayer player;
    PlaybackStateCompat.Builder stateBuilder;      //PlaybackStateCompat.Builder()     should bu reused
    MediaMetadataCompat mmd;        //MediaMetadataCompat.Builder()
    private static final String MY_MEDIA_ROOT_ID = "PlayService";

    public PlayService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initSession();
    }

    /**
     * init MediaSession:
     * 1) init(new) and set flags
     * 2) set playback state
     * 2) set callback
     * 3) set token
     */
    private void initSession() {
        session = new MediaSessionCompat(PlayService.this,"player session");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        session.setPlaybackState(stateBuilder.build());
        session.setCallback(sessionCb);
        setSessionToken(session.getSessionToken());
    }


    /**
     * MediaSession Callback impl.
     */
    private MediaSessionCompat.Callback sessionCb = new MediaSessionCompat.Callback(){
        @Override
        public void onPlay() {
            super.onPlay();
            //start service
            /*put the service in the foreground(Android considers the service
              in the foreground for purposes of process management) */
            //build the notification
            buildNotification(PlayService.this,session);
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onStop() {
            super.onStop();
            //TODO stop service
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }
    };

    private void buildNotification(Context context, MediaSessionCompat mediaSession) {
        //metadata
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat matadata = controller.getMetadata();
        MediaDescriptionCompat description = matadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Add the metadata for the currently playing track
        builder.setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())

                // Enable launching the player by clicking the notification
                .setContentIntent(controller.getSessionActivity())

                // Stop the service when the notification is swiped away
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))

                // Make the transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Add an app icon and set its accent color
                // Be careful about the color
                .setSmallIcon(R.mipmap.ic_launcher)         //// TODO: 17-3-20
                .setColor(ContextCompat.getColor(this, android.R.color.holo_green_light))  //// TODO: 17-3-20

                // Add a pause button
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_pause, getString(R.string.pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))

                // Take advantage of MediaStyle features
                .setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)
                        // Add a cancel button
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP)));
        // Display the notification and place the service in the foreground
        startForeground(0, builder.build());
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * from MediaBrowserServiceCompat,handle client connections, controls access to the service
     * @param clientPackageName
     * @param clientUid
     * @param rootHints
     * @return
     */
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(allowBrowsing(clientPackageName, clientUid)){
            // Returns a root ID, so clients can use onLoadChildren() to retrieve the content hierarchy
            return new BrowserRoot(MY_MEDIA_ROOT_ID,null);
        } else {
            // Clients can connect, but since the BrowserRoot is an empty string,
            // onLoadChildren will return nothing. This disables the ability to browse for content.
            return new BrowserRoot("",null);
        }
    }

    private boolean allowBrowsing(String clientPackageName, int clientUid) {
        return true;
    }

    /**
     * from MediaBrowserServiceCompat,provides the ability for a client
     * to build and display a menu of the MediaBrowserService's content hierarchy
     * @param parentId
     * @param result
     */
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if(TextUtils.isEmpty(parentId)){
            result.sendResult(null);
            return;
        }
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        if(parentId.equals(MY_MEDIA_ROOT_ID)){
//            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(..,..);
        } else {

        }
        result.sendResult(mediaItems);
    }
}
