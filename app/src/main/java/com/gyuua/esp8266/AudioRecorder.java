package com.gyuua.esp8266;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder {
    private static final int mAudioSource = MediaRecorder.AudioSource.MIC;
    private static final int mSampleRateInHz = 16000;
    private static final int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord = null;
    private int recordBufSize = 0;

    private boolean isRecording = false;

    private String saveFilePath;
    private File mRecordingFile;

    public void init(String filePath) {
        saveFilePath = filePath;
        recordBufSize = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
        audioRecord = new AudioRecord(mAudioSource, mSampleRateInHz, mChannelConfig,
                mAudioFormat, recordBufSize);

        // create steam
        mRecordingFile = new File(saveFilePath);
        if (mRecordingFile.exists()) {
            mRecordingFile.delete();
        }
        try {
            mRecordingFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecording(String filePath) {
        init(filePath);
        if (audioRecord == null || audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                isRecording = true;
                try {
                    //获取到文件的数据流
                    DataOutputStream mDataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mRecordingFile, true)));
                    byte[] buffer = new byte[recordBufSize];
                    audioRecord.startRecording();//开始录音
                    while (isRecording && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        int bufferReadResult = audioRecord.read(buffer, 0, recordBufSize);
                        for (int i = 0; i < bufferReadResult; i++) {
                            mDataOutputStream.write(buffer[i]);
                        }
                    }
                    mDataOutputStream.close();
                } catch (Throwable t) {
                    stopRecording();
                }
            }
        }).start();
    }

    public void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.release();
            }
        }
    }
}
