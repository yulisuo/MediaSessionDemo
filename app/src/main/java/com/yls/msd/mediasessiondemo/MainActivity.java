package com.yls.msd.mediasessiondemo;

import android.content.ComponentName;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity{


    Button btnPlay;
    SeekBar seekBar;
    boolean isPlay;
    MediaBrowserCompat browser;

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback(){

    };

    private MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initBrowser();
    }

    @Override
    protected void onStart() {
        super.onStart();
        browser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback);
        }
        browser.disconnect();
    }

    private void initBrowser() {
        browser = new MediaBrowserCompat(this,
                new ComponentName(this,PlayService.class),mConnectionCallbacks,null);
    }

    private void init() {
        btnPlay = (Button)findViewById(R.id.play);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
    }


}
