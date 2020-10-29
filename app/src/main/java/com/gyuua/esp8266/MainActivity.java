package com.gyuua.esp8266;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends FragmentActivity implements DownloadCallback, ClientCallback {

    private NetworkFragment networkFragment; // a reference to the NetworkFragment, for async task execution
    private boolean downloading = false; // whether a download is in progress

    private GoogleSpeech googleSpeech;
    private boolean communicating = false;

    private Record recorder;
    //private AudioRecorder audioRecorder;

    private TextView weather;
    private TextView speechText;
    private TextView httpText;

    private boolean isRecording;

    //private Context context;
    private File audioFile;
    private String audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File dir = contextWrapper.getDir("audioDir", Context.MODE_PRIVATE);
        audioFile = new File(dir, "record.ogg");
        if(!audioFile.exists()){
            audioFile.mkdirs();
        }
        this.audioPath = audioFile.getPath();


        //networkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "https://857e3149d9e4.ngrok.io/");
        googleSpeech = GoogleSpeech.getInstance(getSupportFragmentManager(), this.audioPath);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        weather = findViewById(R.id.text_weather);
        TextView status = findViewById(R.id.p1_status);
        Button btn_weather = findViewById(R.id.p1_btn_weather);
        btn_weather.setOnClickListener(v -> {
            //startDownload();
            //goToMainActivity2();
        });

        recorder = new Record();
        isRecording = false;
        //audioRecorder = new AudioRecorder();
        speechText = findViewById(R.id.p1_text_speech);
        httpText = findViewById(R.id.http_text);
        Button btn_record = findViewById(R.id.p1_btn_record);
        btn_record.setOnClickListener(v -> {
            if(!isRecording){
                try {
                    recorder.startRecord(audioPath);
                    isRecording = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn_stop = findViewById(R.id.p1_btn_stop);
        btn_stop.setOnClickListener(v ->{
            if(isRecording){
                //audioRecorder.stopRecording();
                recorder.stopRecord();
                startClient();
                isRecording = false;
            }
        });
    }

    private void goToMainActivity2() {
        Intent intent = new Intent(this, MainActivity2.class);
        startActivity(intent);
    }

    /** client service**/
    private void startClient(){
        if (!communicating && googleSpeech != null) {
            // Execute the async download.
            GoogleSpeech.startClient();
            communicating = true;
        }
    }

    @Override
    public void updateFromCommunicate(String result) {
        speechText.setText(result);
        send(result);
    }

    @Override
    public void finishCommunicating() {
        communicating = false;
        if (googleSpeech != null) {
            GoogleSpeech.cancelClient();
        }
    }

    /** internet service **/
    private void startDownload() {
        if (!downloading && networkFragment != null) {
            // Execute the async download.
            NetworkFragment.startDownload();
            downloading = true;
        }
    }

    @Override
    public void updateFromDownload(String result) {
        if(result!= null){
            weather.setText(result);
        }else{
            weather.setText(R.string.weather_error);
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        TextView status = findViewById(R.id.p1_status);
        switch(progressCode) {
            case Progress.ERROR:
            case Progress.CONNECT_SUCCESS:
            case Progress.GET_INPUT_STREAM_SUCCESS:
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                status.setText("" + percentComplete + "%");
                break;
        }
    }

    @Override
    public void finishDownloading() {
        downloading = false;
        if (networkFragment != null) {
            NetworkFragment.cancelDownload();
        }
    }


    private void send(String urlCmd) {
        //开启线程，发送请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpsURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL("https://7da0417c34f5.ngrok.io/");
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    connection.setRequestProperty("Command", urlCmd);
                    connection.setDoInput(true);
                    connection.connect();

                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    show(getBody(result.toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                    show("No response received from Smart Watch");
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            show("No response received from Smart Watch");
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    private String getBody(String res){
        String start = "<h2>";
        String end = "</h2>";
        int s = res.indexOf(start) + start.length();
        int e = res.indexOf(end);
        return res.substring(s, e);
    }

    private void show(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                httpText.setText(result);
            }
        });
    }
}
