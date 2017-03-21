package com.yls.msd.mediasessiondemo;

import android.app.Activity;
import android.content.ComponentName;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    Button mPlayPause;
    MediaBrowserCompat mBrowser;
    boolean isPlay;

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {

        /**
         * retrieve the media session token from the MediaBrowserService and
         * use the token to create a MediaControllerCompat.
         */
        @Override
        public void onConnected() {
            Log.i(Utils.LOG_TAG,"MediaBrowser ConnectionCallback onConnected");
            //get the media session token
            MediaSessionCompat.Token token = mBrowser.getSessionToken();
            try {
                //create a media controller
                MediaControllerCompat controller = new MediaControllerCompat(MainActivity.this,
                        token);
                //save the controller
                MediaControllerCompat.setMediaController(MainActivity.this,
                        controller);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            buildTransportControls();
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
            Log.i(Utils.LOG_TAG,"MediaBrowser ConnectionCallback onConnectionSuspended");
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
            Log.i(Utils.LOG_TAG,"MediaBrowser ConnectionCallback onConnectionFailed");
        }
    };

    private void buildTransportControls() {
        Log.i(Utils.LOG_TAG,"buildTransportControls");
        mPlayPause = (Button) findViewById(R.id.play);
        mPlayPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MediaControllerCompat controller = MediaControllerCompat.getMediaController(MainActivity.this);
                PlaybackStateCompat playbackState = controller.getPlaybackState();
                Log.i(Utils.LOG_TAG,"onClick state:"+playbackState.getState());
                if(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING){
                    controller.getTransportControls().pause();
                } else {
                    controller.getTransportControls().play();
                }
            }
        });
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(MainActivity.this);
        controller.registerCallback(controllerCallback);
    }

    private MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            Log.i(Utils.LOG_TAG,"onPlaybackStateChanged state:"+state.getState());
            if(state.getState() == PlaybackStateCompat.STATE_PLAYING){
                mPlayPause.setText(R.string.pause);
            } else {
                mPlayPause.setText(R.string.play);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(Utils.LOG_TAG,"MainActivity onCreate");
        //initViews();
        initBrowser();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBrowser.connect();
        Log.i(Utils.LOG_TAG,"MainActivity onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(controllerCallback);
        }
        mBrowser.disconnect();
    }

    private void initBrowser() {
        Log.i(Utils.LOG_TAG,"MainActivity initBrowser");
        mBrowser = new MediaBrowserCompat(this,
                new ComponentName(this,PlayService.class),mConnectionCallbacks,null/*optional Bundle*/);
    }



}
