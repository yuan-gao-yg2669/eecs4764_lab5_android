package com.gyuua.esp8266;

import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;

public class Record {
    private MediaRecorder recorder;

    public void startRecord(String audioPath) throws IOException {
        // start recording
        if (recorder == null)
            recorder = new MediaRecorder();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS);
            recorder.setAudioSamplingRate(16000);
            recorder.setAudioEncodingBitRate(192000);
            recorder.setAudioChannels(1);
            recorder.setOutputFile(audioPath);
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
        } catch (RuntimeException e) {
            recorder.reset();
            recorder.release();
            recorder = null;
        }
    }
}
