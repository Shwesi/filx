package com.virmana.flixa.ui.activities;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.virmana.flixa.R;
import com.virmana.flixa.cast.ExpandedControlsActivity;
import com.virmana.flixa.event.CastSessionEndedEvent;
import com.virmana.flixa.event.CastSessionStartedEvent;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

public class SimpleVideoPlayer extends AppCompatActivity {


    SimpleExoPlayer player;
    ProgressBar progressBar;
    boolean doubleBackToExitPressedOnce = false;
    String url,cookie,did;

    InterstitialAd mInterstitialAd;

    private class SessionManagerListenerImpl implements SessionManagerListener {
        @Override
        public void onSessionStarting(Session session) {
            Log.d("MYAPP","onSessionStarting");
        }

        @Override
        public void onSessionStarted(Session session, String s) {
            Log.d("MYAPP","onSessionStarted");
            invalidateOptionsMenu();
            EventBus.getDefault().post(new CastSessionStartedEvent());
            startActivity(new Intent(SimpleVideoPlayer.this, ExpandedControlsActivity.class));
            finish();
        }

        @Override
        public void onSessionStartFailed(Session session, int i) {
            Log.d("MYAPP","onSessionStartFailed");
        }

        @Override
        public void onSessionEnding(Session session) {
            Log.d("MYAPP","onSessionEnding");
            EventBus.getDefault().post(new CastSessionEndedEvent(session.getSessionRemainingTimeMs()));
        }

        @Override
        public void onSessionEnded(Session session, int i) {
            Log.d("MYAPP","onSessionEnded");
        }

        @Override
        public void onSessionResuming(Session session, String s) {
            Log.d("MYAPP","onSessionResuming");
        }

        @Override
        public void onSessionResumed(Session session, boolean b) {
            Log.d("MYAPP","onSessionResumed");
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumeFailed(Session session, int i) {
            Log.d("MYAPP","onSessionResumeFailed");
        }

        @Override
        public void onSessionSuspended(Session session, int i) {
            Log.d("MYAPP","onSessionSuspended");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        url = getIntent().getStringExtra("url");
        cookie = getIntent().getStringExtra("cookie");


        //    initializePlayer();


    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    public void onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            showInter(did);

            SimpleVideoPlayer.this.finish();
            if (player != null) {
                player.setPlayWhenReady(false);
            }
            super.onBackPressed();
            return;
     //   }

     //   this.doubleBackToExitPressedOnce = true;
     //   Toast.makeText(this, "Click BACK again to EXIT!", Toast.LENGTH_SHORT).show();

      //  new Handler().postDelayed(new Runnable() {
     //       @Override
      //      public void run() {
       //         doubleBackToExitPressedOnce=false;
            }
     //   }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player !=null){
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        super.onDestroy();
    }

    private void initializePlayer(){
        // Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        //Initialize the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        //Initialize simpleExoPlayerView
        SimpleExoPlayerView simpleExoPlayerView = findViewById(R.id.video_view);
        progressBar = findViewById(R.id.progresbar_video_play);

        simpleExoPlayerView.setPlayer(player);

        String userAgent = Util.getUserAgent(SimpleVideoPlayer.this, getResources().getString(R.string.app_name));
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, Util.getUserAgent(this, "CloudinaryExoplayer"));


        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // This is the MediaSource representing the media to be played.
        Uri videoUri = Uri.parse(url);
        if (cookie!=null) {
            DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, null);
            httpDataSourceFactory.getDefaultRequestProperties().set("Cookie", cookie);
            dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(), null, httpDataSourceFactory);
        }

        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url));
        //   MediaSource videoSource = new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null);

        // Prepare the player with the source.
        player.prepare(videoSource);
        player.setPlayWhenReady(true);

        //progressBar.setVisibility(View.GONE);
        player.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playWhenReady) {
                    progressBar.setVisibility(View.GONE);
                }
                super.onPlayerStateChanged(playWhenReady, playbackState);
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                super.onPlayerError(error);
                finish();
                Toast.makeText(SimpleVideoPlayer.this, "Can't play this video!", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton rotate = findViewById(R.id.rotate);
        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                if (rotation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }else{
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        ////////////

        //DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        // player = ExoPlayerFactory.newSimpleInstance(SimpleVideoPlayer.this, trackSelector);
        //  playerView.setPlayer(player);


        // DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(SimpleVideoPlayer.this, userAgent);

        //If google drive you need to set custom cookie



        //   player.prepare(videoSource);





    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    private void showInter(final String did) {
        mInterstitialAd = new InterstitialAd(getApplicationContext());
        mInterstitialAd.setAdUnitId(did);

        AdRequest adRequest1 = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest1);
        //     mInterstitialAd.loadAd(new AdRequest.Builder().build());


        mInterstitialAd.setAdListener(new AdListener(){

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if(mInterstitialAd.isLoaded()){
                    mInterstitialAd.show();
                }
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                //   Toast.makeText(SimpleVideoPlayer.this, "Failed"+i, Toast.LENGTH_SHORT).show();
            }
        });
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            //Log.d("TAG", "The interstitial wasn't loaded yet.");
        }
    }


}
