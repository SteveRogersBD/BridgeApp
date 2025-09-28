package com.example.bridge.utils;

import android.media.MediaRecorder;
import android.os.Environment;

import java.io.IOException;

public class AudioRecorder {
    private MediaRecorder mediaRecorder;
    private String outputFile;

    public void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recorded_audio.3gp";
        mediaRecorder.setOutputFile(outputFile);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}
