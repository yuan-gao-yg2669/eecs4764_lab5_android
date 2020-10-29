package com.gyuua.esp8266;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.auth.oauth2.GoogleCredentials;

public class GoogleSpeech extends Fragment {
    public static final String TAG = "GoogleSpeechFragment";

    private static final String AUDIO_PATH = "AudioPath";

    private static ClientTask clientTask;
    private static ClientCallback callback;
    private static String audioPath;
    private static String speech_text;

    /** Static initializer for GoogleSpeech **/
    public static GoogleSpeech getInstance(FragmentManager fragmentManager, String path) {
        GoogleSpeech googleSpeech = (GoogleSpeech) fragmentManager
                .findFragmentByTag(GoogleSpeech.TAG);
        if (googleSpeech == null) {
            googleSpeech = new GoogleSpeech();
            Bundle args = new Bundle();
            args.putString(AUDIO_PATH, path);
            googleSpeech.setArguments(args);
            fragmentManager.beginTransaction().add(googleSpeech, TAG).commit();
        }
        return googleSpeech;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioPath = getArguments().getString(AUDIO_PATH);
        speech_text = "listening...";
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
        callback = (ClientCallback) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear reference to host Activity to avoid memory leak.
        callback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelClient();
        super.onDestroy();
    }

    public static void startClient() {
        cancelClient();
        clientTask = new GoogleSpeech.ClientTask(callback);
        clientTask.execute(audioPath);
    }

    public static void cancelClient() {
        if (clientTask != null) {
            clientTask.cancel(true);
        }
    }

    private static class ClientTask extends AsyncTask<String, Integer, GoogleSpeech.ClientTask.Result> {
        private ClientCallback callback;

        ClientTask(ClientCallback callback) {
            setCallback(callback);
        }

        void setCallback(ClientCallback callback) {
            this.callback = callback;
        }

        /**
         * Wrapper class that serves as a union of a result value and an exception. When the download
         * task has completed, either the result value or exception can be a non-null value.
         * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
         */
        class Result {
            public String resultValue;
            public Exception exception;
            public Result(String resultValue) {
                this.resultValue = resultValue;
            }
            public Result(Exception exception) {
                this.exception = exception;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected ClientTask.Result doInBackground(String... path) {
            Result result = null;
            String audioPath = path[0];

            InputStream cred = this.getClass().getClassLoader().getResourceAsStream("assets/" + "credential.json");
            List<String> SCOPE = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
            GoogleCredentials credentials = null;
            try {
                credentials = GoogleCredentials.fromStream(cred).createScoped(SCOPE);
                //AccessToken token = credentials.refreshAccessToken();
                CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
                SpeechSettings settings = SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
                SpeechClient speechClient = SpeechClient.create(settings);
                String resultString = speech2text(speechClient, audioPath);
                if (resultString != null) {
                    result = new GoogleSpeech.ClientTask.Result(resultString);
                } else {
                    throw new IOException("No response received.");
                }
            } catch (Exception e) {
                result = new Result(e);
            }
            return result;
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(GoogleSpeech.ClientTask.Result result) {
            if (result != null && callback != null) {
                if (result.exception != null) {
                    callback.updateFromCommunicate(result.exception.toString());
                } else if (result.resultValue != null) {
                    callback.updateFromCommunicate(result.resultValue);
                }
                callback.finishCommunicating();
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(GoogleSpeech.ClientTask.Result result) {
        }

        /** Demonstrates using the Speech API to transcribe an audio file. */
        private String speech2text(SpeechClient speechClient, String audioPath) throws Exception {
            String resultText = null;
            try {
                //InputStream data =  this.getClass().getClassLoader().getResourceAsStream("assets/" + "test.ogg");
                //ByteString audioBytes = ByteString.readFrom(data);

                //Path path = Paths.get(audioPath);
                //byte[] data = Files.readAllBytes(path);
                //ByteString audioBytes = ByteString.copyFrom(data);

                File audioFile = new File(audioPath);
                InputStream data = new FileInputStream(audioFile);
                ByteString audioBytes = ByteString.readFrom(data);

                //InputStream data =  this.getClass().getClassLoader().getResourceAsStream("/data/user/0/com.gyuua.esp8266/files/record.ogg");
                //ByteString audioBytes = ByteString.readFrom(data);

                // Builds the sync recognize request
                RecognitionConfig config =
                        RecognitionConfig.newBuilder()
                                .setEncoding(AudioEncoding.OGG_OPUS)
                                .setSampleRateHertz(16000)
                                .setLanguageCode("en-US")
                                .build();
                RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

                // Performs speech recognition on the audio file
                RecognizeResponse response = speechClient.recognize(config, audio);
                List<SpeechRecognitionResult> results = response.getResultsList();

                //resultText = response.toString();

                for (SpeechRecognitionResult result : results) {
                    // There can be several alternative transcripts for a given chunk of speech. Just use the
                    // first (most likely) one here.
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    //System.out.printf("Transcription: %s%n", alternative.getTranscript());
                    resultText = alternative.getTranscript();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                // Close Stream and disconnect HTTPS connection.
                if (speechClient != null) {
                    speechClient.close();
                }
            }
            return resultText;
        }
    }
}
