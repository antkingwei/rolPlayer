
package com.rolmex.android.rolplayer.ui;

import com.rolmex.android.rolplayer.Api;
import com.rolmex.android.rolplayer.BaseActivity;
import com.rolmex.android.rolplayer.R;
import com.rolmex.android.rolplayer.model.FileItem;
import com.rolmex.android.rolplayer.model.InfoItem;
import com.rolmex.android.rolplayer.model.Result;
import com.rolmex.android.rolplayer.service.WindowPlayer;
import com.rolmex.android.rolplayer.task.Task;
import com.rolmex.android.rolplayer.task.Task.TaskCallback;
import com.rolmex.android.rolplayer.widget.MarqueeText;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.FrameLayout.LayoutParams;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.utils.Log;
import io.vov.vitamio.utils.StringUtils;

import io.vov.vitamio.widget.CenterLayout;
import io.vov.vitamio.widget.VideoView;

public class PlayActivity extends BaseActivity implements OnInfoListener, OnBufferingUpdateListener {

    private RelativeLayout prepare_ly;

    private VideoView player_buffer;

    private MarqueeText player_title;

    private TextView player_btn_back, player_btn_start;

    private TextView player_time;

    private String path = "http://119.167.146.12/fcs126.56.com/flvdownload/28/18/13950338514hd_clear.flv.mp4?t=_vvdH13rtMTryFzRk7W70w&r=47460&e=1395996812&v=1&s=1&tt=344&sz=24137946&vid=109381429";

    // private String path="";
    private Uri uri;

    private boolean isStart;

    private SeekBar player_seekbar;

    private ProgressBar pb;

    private GestureDetector mGestureDetector;

    private RelativeLayout player_top_ly, player_bottom_ly;

    private Button suspend_btn;

    private CenterLayout player_buffer_content;

    private int screen_width, screen_height;

    // 重力感应
    private SensorManager mManager = null;

    private Sensor mSensor = null;

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    player_top_ly.setVisibility(View.GONE);
                    player_bottom_ly.setVisibility(View.GONE);
                    break;
            }
        }

    };
    @SuppressWarnings("deprecation")
    @Override
    protected void init() {
        // TODO Auto-generated method stub
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        Intent intent = getIntent();
        String vid = intent.getStringExtra("vid");

        mManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        initUI();
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            screen_width = dm.widthPixels;
            screen_height = dm.heightPixels;
            player_buffer_content.setLayoutParams(new RelativeLayout.LayoutParams(screen_width,
                    getCenterBufferHeight()));
        } else {
            screen_width = dm.heightPixels;
            screen_height = dm.widthPixels;
        }

        loadData(vid);
        mGestureDetector = new GestureDetector(this, new MyGestureListener());
        myHandler.post(SeekBarUp);
        setScreenBriness(240);

    }
    /**
     * 设置屏幕亮度
     * @param brightness
     */
    private void setScreenBriness(int brightness){
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.screenBrightness = Float.valueOf(brightness/255f);
        this.getWindow().setAttributes(lp);
        
    }

    /**
     * 重力感应改变手机屏幕方向
     */
    private SensorEventListener sensorListner = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            @SuppressWarnings("deprecation")
            float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];

            if (x > 9) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            if (y > 9) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

    };

    private String getPlayerTitle() {
        Intent intent = getIntent();
        return intent.getStringExtra("title");
    }

//    public boolean onTouchEvent(MotionEvent event) {
//
//        if (mGestureDetector.onTouchEvent(event))
//            return true;
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                Log.e("TOUCH_DOWN", "TOUCH_DOWN");
//                player_top_ly.setVisibility(View.VISIBLE);
//                player_bottom_ly.setVisibility(View.VISIBLE);
//                break;
//            case MotionEvent.ACTION_UP:
//                myHandler.sendEmptyMessageDelayed(0, 5000);
//                Log.e("TOUCH_UP", "TOUCH_UP");
//                break;
//        }
//        return super.onTouchEvent(event);
//    }

    private void initUI() {
        player_buffer = (VideoView)this.findViewById(R.id.player_buffer);
        player_top_ly = (RelativeLayout)this.findViewById(R.id.player_top_ly);
        player_bottom_ly = (RelativeLayout)this.findViewById(R.id.player_bottom_ly);
        prepare_ly = (RelativeLayout)this.findViewById(R.id.player_prepare_ly);
        suspend_btn = (Button)this.findViewById(R.id.player_suspend_btn);
        prepare_ly.setVisibility(View.GONE);

        player_title = (MarqueeText)this.findViewById(R.id.player_title);
        player_btn_back = (TextView)this.findViewById(R.id.player_btn_back);
        player_btn_start = (TextView)this.findViewById(R.id.play_btn_start);
        player_title.setText(getPlayerTitle());

        player_seekbar = (SeekBar)this.findViewById(R.id.player_seekbar);

        player_time = (TextView)this.findViewById(R.id.player_time);
        player_buffer_content = (CenterLayout)this.findViewById(R.id.player_buffer_content);

        player_btn_back.setOnClickListener(buttonListener);
        player_btn_start.setOnClickListener(buttonListener);
        suspend_btn.setOnClickListener(buttonListener);
       
        player_buffer.setOnTouchListener(new View.OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:

                      player_top_ly.setVisibility(View.VISIBLE);
                      player_bottom_ly.setVisibility(View.VISIBLE);
                      break;
                  case MotionEvent.ACTION_UP:
                      myHandler.sendEmptyMessageDelayed(0, 5000);

                      break;
                        
                }
                return true;
            }
        });
        player_title.setOnClickListener(new View.OnClickListener() {
            
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                player_buffer.stopPlayback();
                Intent intent = new Intent(PlayActivity.this,WindowPlayer.class);
                intent.putExtra("screen_width", screen_width);
                intent.putExtra("screen_height", screen_height);
                startService(intent);
                
                finish();
            }
        });

        player_seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                // Log.e(progress + "progress", progress + "progress");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                player_buffer.pause();
                isStart = false;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                int value = (int)(seekBar.getProgress() * player_buffer.getDuration() / seekBar
                        .getMax());
                player_buffer.seekTo(value);
                player_buffer.start();
                isStart = true;

            }
        });

    }

    private Runnable SeekBarUp = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            upBarSize();
            upTime();
            myHandler.postDelayed(SeekBarUp, 300);
        }

    };

    private void upTime() {
        player_time.setText(StringUtils.generateTime(player_buffer.getCurrentPosition()) + "/"
                + StringUtils.generateTime(player_buffer.getDuration()));
    }

    private void upBarSize() {

        player_seekbar.setProgress((int)(player_buffer.getCurrentPosition() * 100 / player_buffer
                .getDuration()));
    }

    private View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == player_btn_back) {
                player_buffer.stopPlayback();
                finish();
            }
            if (v == player_btn_start) {
                playOrPause();
                upStartBtnBg(isStart);

            }
            if (v == suspend_btn) {
                playOrPause();
                upStartBtnBg(isStart);
            }

        }
    };

    private void loadData(String vid) {
        Api.getVideoAddr(getApplicationContext(), vid, new TaskCallback() {

            @Override
            public void onTaskComplete(Task task, Result result) {
                // TODO Auto-generated method stub
                bindData(result);
            }

        });
    }

    private void bindData(Result result) {
        if (result.info == null) {
            return;
        }
        Log.e(result.msg, "antking_info");
        InfoItem info = result.info;

        List<FileItem> list = info.rfiles;
        Log.e(list.size() + "", list.size() + "");
        String path = "";
        for (FileItem item : list) {

            if (item.type.equals("qvga")) {
                path = item.url;
                break;
            }

        }
        if (path == "") {
            path = list.get(0).url;
        }
        prepareVideo(path);
    }

    private void prepareVideo(String path) {
        if (path == "") {

        } else {
            uri = Uri.parse(path);

            player_buffer.setVideoURI(uri);
            // player_buffer.setMediaController(new MediaController(this));
            player_buffer.requestFocus();
            player_buffer.setOnInfoListener(this);
            player_buffer.setOnBufferingUpdateListener(this);
            player_buffer.setBufferSize(512 * 1024);
            player_buffer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    mp.setPlaybackSpeed(1.0f);
                }
            });
            prepare_ly.setVisibility(View.GONE);
            playerStart();
            upStartBtnBg(isStart);
            myHandler.sendEmptyMessageDelayed(0, 5000);
        }
    }

    private void playerStart() {
        suspend_btn.setVisibility(View.GONE);
        if (player_buffer.isPlaying())
            return;
        player_buffer.start();
        isStart = true;
    }

    private void playerPause() {
        player_buffer.pause();
        suspend_btn.setVisibility(View.VISIBLE);
        isStart = false;
    }

    private void playOrPause() {
        if (player_buffer.isPlaying()) {
            playerPause();
        } else {
            playerStart();
        }
    }

    private void upStartBtnBg(boolean isStart) {
        if (isStart) {
            player_btn_start.setBackground(this.getResources().getDrawable(
                    R.drawable.play_detail_btn_start));
        } else {
            player_btn_start.setBackground(this.getResources().getDrawable(
                    R.drawable.play_detail_btn_suspend));
        }
    }

    private class MyGestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // TODO Auto-generated method stub
            float oldX = e1.getX(), oldY = e1.getY();
            float newX = e2.getX(), newY = e2.getY();
            // Log.e("oldx:" + oldX + ",oldY" + oldY + "newX:" + newX + "newY:"
            // + newY, "oldx:" + oldX
            // + ",oldY" + oldY + "newX:" + newX + "newY:" + newY);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // TODO Auto-generated method stub
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onDoubleTap(e);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("onPause", "onPause");
        mManager.unregisterListener(sensorListner);
        player_buffer.pause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("onResume", "onResume");
        mManager.registerListener(sensorListner, mSensor, SensorManager.SENSOR_DELAY_GAME);
        player_buffer.start();

    }

    @Override
    protected int getLayout() {
        // TODO Auto-generated method stub
        return R.layout.activity_player;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // TODO Auto-generated method stub

        player_seekbar.setSecondaryProgress(percent);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        // Log.e("" + what + "ex" + extra, "" + what + "ex" + extra);
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("onConfigure", "onConfigure");
        if (player_buffer == null)
            return;
        player_buffer.pause();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            player_buffer_content.setLayoutParams(new RelativeLayout.LayoutParams(screen_height,
                    screen_width));

            Log.e("heng", "heng");
        } else {
            Log.e("shu", "shu");

            player_buffer_content.setLayoutParams(new RelativeLayout.LayoutParams(screen_width,
                    getCenterBufferHeight()));

        }
        player_buffer.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
        if (isStart)
            player_buffer.start();

    }

    private int getCenterBufferHeight() {
        return (screen_width * screen_width) / screen_height;
    }

}
