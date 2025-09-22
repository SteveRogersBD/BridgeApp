package com.example.bridge.utils;
import android.Manifest;
import android.media.*;

import androidx.annotation.RequiresPermission;

public class AudioLevelSampler {
    public interface Listener { void onLevel(float level01); }

    private final int sr = 44100;
    private final int ch = AudioFormat.CHANNEL_IN_MONO;
    private final int fmt = AudioFormat.ENCODING_PCM_16BIT;
    private final int min = AudioRecord.getMinBufferSize(sr, ch, fmt);

    private AudioRecord rec;
    private Thread th;
    private volatile boolean running;

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void start(Listener l) {
        if (running) return;
        rec = new AudioRecord(MediaRecorder.AudioSource.MIC, sr, ch, fmt, min);
        rec.startRecording();
        running = true;
        th = new Thread(() -> {
            short[] buf = new short[min];
            float smooth = 0f;
            while (running) {
                int n = rec.read(buf, 0, buf.length);
                if (n > 0) {
                    double sum = 0;
                    for (int i = 0; i < n; i++) sum += buf[i] * (double) buf[i];
                    double rms = Math.sqrt(sum / n);
                    double db = 20 * Math.log10(rms / 32767.0 + 1e-7); // ~ -50..-10 dB
                    float level = (float) ((db + 50) / 40.0);
                    level = Math.max(0f, Math.min(1f, level));
                    smooth = 0.85f * smooth + 0.15f * level;
                    l.onLevel(smooth);
                }
            }
        }, "audio-level");
        th.start();
    }

    public void stop() {
        running = false;
        if (th != null) try { th.join(200); } catch (InterruptedException ignored) {}
        if (rec != null) { rec.stop(); rec.release(); rec = null; }
    }
}
