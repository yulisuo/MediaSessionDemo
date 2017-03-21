package com.yls.msd.mediasessiondemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
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
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayService extends MediaBrowserServiceCompat {

    private MediaSessionCompat session;
    private PlaybackStateCompat.Builder stateBuilder;      //PlaybackStateCompat.Builder()     should bu reused
//    private MediaMetadataCompat mmd;        //MediaMetadataCompat.Builder()
    private static final String MY_MEDIA_ROOT_ID = "PlayService";
    private AudioManager am;
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    FileDescriptor fd;

    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };

    private BroadcastReceiver myNoisyAudioStreamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

//    private MediaStyleNotification myPlayerNotification;
    MediaPlayer player;

    public PlayService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(Utils.LOG_TAG,"play service onCreate");
        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
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
        Log.i(Utils.LOG_TAG,"play service initSession");
        session = new MediaSessionCompat(PlayService.this,"yls_player_session");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        session.setPlaybackState(stateBuilder.build());
        session.setCallback(sessionCb);
        setSessionToken(session.getSessionToken());
        initPlayer();
    }


    /**
     * MediaSession Callback impl.
     */
    private MediaSessionCompat.Callback sessionCb = new MediaSessionCompat.Callback(){

        @Override
        public void onPlay() {
            super.onPlay();
            Log.i(Utils.LOG_TAG,"MediaSession Callback onPlay");
            int result = am.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                if (! session.isActive()) {
                    session.setActive(true);
                }
//                try {
//                    player.prepare();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                player.start();
                stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,player.getCurrentPosition(),
                        SystemClock.elapsedRealtime());
                session.setPlaybackState(stateBuilder.build());
            } else {
                Log.e(Utils.LOG_TAG,"MediaSession Callback onPlay,requestAudioFocus not granted");
            }
            //start service
            /*put the service in the foreground(Android considers the service
              in the foreground for purposes of process management) */
            //build the notification
//            buildNotification(PlayService.this,session);
        }

        @Override
        public void onPause() {
            super.onPause();
            if(player.isPlaying()) {
                player.pause();
                stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,player.getCurrentPosition(),
                        SystemClock.elapsedRealtime());
                session.setPlaybackState(stateBuilder.build());
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            //TODO stop service
            player.stop();
            player.release();
            player = null;
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }
    };

    private void initPlayer() {
        try {
            fd = getAssets().openFd("m1.mp3").getFileDescriptor();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        try {
            player.setDataSource(fd);
            player.prepare();
        } catch (Exception e) {
            Log.e(Utils.LOG_TAG,"initPlayer error.");
            e.printStackTrace();
        }
    }

    private void buildNotification(Context context, MediaSessionCompat mediaSession) {
        //metadata
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat matadata = controller.getMetadata();
        //TODO matadata is null
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
        Log.i(Utils.LOG_TAG,"play service onBind");
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(Utils.LOG_TAG,"play service onDestroy");
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
//        if(allowBrowsing(clientPackageName, clientUid)){
            // Returns a root ID, so clients can use onLoadChildren() to retrieve the content hierarchy
            return new BrowserRoot(MY_MEDIA_ROOT_ID,null);
//        } else {
//            // Clients can connect, but since the BrowserRoot is an empty string,
//            // onLoadChildren will return nothing. This disables the ability to browse for content.
//            return new BrowserRoot("",null);
//        }
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
            //TODO add item
//            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(..,..);
        } else {

        }
        result.sendResult(mediaItems);
    }
}
