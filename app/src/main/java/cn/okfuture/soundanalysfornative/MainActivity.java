package cn.okfuture.soundanalysfornative;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    /**
     * 20分贝以下的声音，一般来说，我们认为它是安静的，当然，一般来说15分贝以下的我们就可以认为它属于"死寂"的了。
     * 、20-40分贝大约是情侣耳边的喃喃细语。40-60分贝属于我们正常的交谈声音。60分贝以上就属于吵闹范围了，70分贝
     * 我们就可以认为它是很吵的，而且开始损害听力神经，90分贝以上就会使听力受损，
     *
     * 暂取正常音乐范围为 30-80
     */

    /**
     * 选取音乐
     */
    private static final int SELECT_MUSIC = 0x00;
    /**
     * 求情读文件权限
     */
    private static final int REQUEST_READ_STORAGE = 0x01;
    /**
     * 请求读取文件
     */
    private static final int REQUEST_RECORD_AUDIO = 0X01;
    private static final int FREQUENCY_LINE = 15;
    private Button btn_select_music;
    private TextView tv_select_music;
    private Button btn_start;
    private MediaPlayer mMediaPlayer;
    private Visualizer visualizer;
    private LinearLayout ll_main;

    private long lastChangeTime;


    /**
     * 当前音量
     */
    private int currentVolume;
    /**
     * 当前频率
     */
    private int currentFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        }
        mMediaPlayer = MediaPlayer.create(this, R.raw.houhuiwuqi);
        visualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                Log.i("xiaozhu", "waveform" + waveform.length);

                long v = 0;
                for (int i = 0; i < waveform.length; i++) {
                    v += Math.pow(waveform[i], 2);
                }

                double volume = 10 * Math.log10(v / (double) waveform.length);

                currentVolume = (int) volume;
                Log.i("xiaozhu", "v=" + v + "volume=" + volume + "    " + waveform.length);

            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {


                int max = 0;
                for (int i = 0; i < fft.length; i++) {
                    if (fft[max] < fft[i]) {
                        max = i;
                    }
                }
                if (Math.abs(max - currentFrequency) > FREQUENCY_LINE) {
                    currentFrequency = max;
                    lastChangeTime = System.currentTimeMillis();
                    ll_main.setBackgroundColor(ColorUtils.COLOR_LIST_140[currentFrequency % 140]);
                } else if (System.currentTimeMillis() - lastChangeTime > 1000) {
                    currentFrequency++;
                    lastChangeTime = System.currentTimeMillis();
                    ll_main.setBackgroundColor(ColorUtils.argb((currentVolume - 30) * 0.02f, Color.red(ColorUtils.COLOR_LIST_140[currentFrequency % 140]), Color.green(ColorUtils.COLOR_LIST_140[currentFrequency % 140]), Color.blue(ColorUtils.COLOR_LIST_140[currentFrequency % 140])));
                }


                tv_select_music.setText("频率： " + max * samplingRate / 1024 + "---- 声音" + currentVolume);


            }
        }, Visualizer.getMaxCaptureRate() / 2, true, true);

        visualizer.setEnabled(true);

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

            }
        });


    }


    private void initView() {
        btn_select_music = (Button) findViewById(R.id.btn_select_music);
        tv_select_music = (TextView) findViewById(R.id.tv_select_music);
        btn_start = (Button) findViewById(R.id.btn_start);

        btn_select_music.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        ll_main = (LinearLayout) findViewById(R.id.ll_main);
        ll_main.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select_music:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, SELECT_MUSIC);
                break;
            case R.id.btn_start:

                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                    } else {
                        mMediaPlayer.start();
                    }
                }
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();


            tv_select_music.setText(uri.getPath());
            try {
                mMediaPlayer.setDataSource(this, uri);
                mMediaPlayer.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish();
            }
        }
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish();
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        visualizer.setEnabled(false);
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

        if (visualizer != null) {
            visualizer.release();
        }


    }
}
