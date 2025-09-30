package com.example.bridge;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bridge.databinding.ActivityTranscriptBinding;
import com.example.bridge.utils.AudioLevelSampler;
import com.example.bridge.utils.GlowPulseController;
import com.example.bridge.utils.SpeechLiveTranscriber;

public class TranscriptActivity extends AppCompatActivity {

    ActivityTranscriptBinding binding;
    private static final int MIC_PERMISSION = 101;

    private boolean isRecording = false;
    private boolean isPaused = false;

    // timer
    private long startTimeMs = 0L;
    private long pausedTimeMs = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // glow + mic level + animations
    private GlowPulseController glowCtl;
    private AudioLevelSampler sampler;
    private ObjectAnimator micPulseAnimator;
    private ObjectAnimator micRotateAnimator;

    private SpeechLiveTranscriber stt;
    private StringBuilder fullTranscript = new StringBuilder();


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTranscriptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize components
        setupAnimations();
        setupTimer();

        // set up glow controller
        glowCtl = new GlowPulseController(binding.micGlow);
        sampler = new AudioLevelSampler();

        // ask mic permission
        askPermission();

        // Button click listeners
        binding.pauseBtn.setOnClickListener(v -> pauseBtnClicked());
        binding.saveBtn.setOnClickListener(v -> saveBtnClicked());

        // Initialize UI state
        updateUIState();
    }

    private void setupAnimations() {
        // Mic pulse animation for recording indication
        micPulseAnimator = ObjectAnimator.ofFloat(binding.micIcon, "scaleX", 1.0f, 1.1f);
        micPulseAnimator.setDuration(800);
        micPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        micPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        micPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        // Subtle rotation animation for active recording
        micRotateAnimator = ObjectAnimator.ofFloat(binding.micIcon, "rotation", 0f, 360f);
        micRotateAnimator.setDuration(3000);
        micRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        micRotateAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    private void setupTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording && !isPaused) {
                    long elapsedMs = System.currentTimeMillis() - startTimeMs - pausedTimeMs;
                    updateTimerDisplay(elapsedMs);
                    timerHandler.postDelayed(this, 100); // Update every 100ms for smooth display
                }
            }
        };
    }

    private void updateTimerDisplay(long elapsedMs) {
        int hours = (int) (elapsedMs / 3600000);
        int minutes = (int) (elapsedMs % 3600000) / 60000;
        int seconds = (int) (elapsedMs % 60000) / 1000;
        binding.timerTv.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void pauseBtnClicked() {
        if (!isRecording) {
            // Start recording
            startRecording();
        } else if (!isPaused) {
            // Pause recording
            pauseRecording();
        } else {
            // Resume recording
            resumeRecording();
        }
    }

    private void saveBtnClicked() {
        if (isRecording) {
            stopRecording();
        }
        // TODO: Implement save functionality
        Toast.makeText(this, "Transcript saved!", Toast.LENGTH_SHORT).show();
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startRecording() {
        isRecording = true;
        isPaused = false;
        startTimeMs = System.currentTimeMillis();
        pausedTimeMs = 0L;

        startTimer();
        startMicLevelAndPulse();
        startRecordingAnimations();

        updateUIState();
        if(stt != null) stt.start(true); // Enable continuous mode
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void pauseRecording() {
        isPaused = true;
        long currentTime = System.currentTimeMillis();
        
        pauseTimer();
        stopMicLevelAndPulse();
        stopRecordingAnimations();

        updateUIState();
        
        // TODO: Pause your STT recorder here
        if (stt != null) stt.stop();
        Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show();
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void resumeRecording() {
        isPaused = false;
        long pauseEndTime = System.currentTimeMillis();
        
        startTimer();
        startMicLevelAndPulse();
        startRecordingAnimations();

        updateUIState();
        
        // TODO: Resume your STT recorder here
        if (stt != null) stt.start(true);   // resume listening with continuous mode
        Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        isRecording = false;
        isPaused = false;
        
        pauseTimer();
        stopMicLevelAndPulse();
        stopRecordingAnimations();
        
        // Reset timer display
        binding.timerTv.setText("00:00:00");
        startTimeMs = 0L;
        pausedTimeMs = 0L;

        updateUIState();
        
        // TODO: Stop your STT recorder here
        if (stt != null) stt.cancel();       // fully end session
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateUIState() {
        if (!isRecording) {
            // Not recording - show play state
            binding.pauseBtn.setImageResource(R.drawable.play);
            binding.stateTv.setText("Ready to record");
            binding.stateTv.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        } else if (isPaused) {
            // Recording but paused
            binding.pauseBtn.setImageResource(R.drawable.play);
            binding.stateTv.setText("Paused");
            binding.stateTv.setTextColor(ContextCompat.getColor(this, R.color.stroke_red));
        } else {
            // Actively recording
            binding.pauseBtn.setImageResource(R.drawable.pause);
            binding.stateTv.setText("Listening…");
            binding.stateTv.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    // ---- timer helpers ----
    private void startTimer() {
        timerHandler.post(timerRunnable);
    }

    private void pauseTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    // ---- animation helpers ----
    private void startRecordingAnimations() {
        // Start mic pulse animation
        if (micPulseAnimator != null && !micPulseAnimator.isRunning()) {
            micPulseAnimator.start();
        }
        
        // Start subtle rotation
        if (micRotateAnimator != null && !micRotateAnimator.isRunning()) {
            micRotateAnimator.start();
        }

        // Animate mic icon color to indicate recording
        binding.micIcon.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start();
    }

    private void stopRecordingAnimations() {
        // Stop animations
        if (micPulseAnimator != null && micPulseAnimator.isRunning()) {
            micPulseAnimator.cancel();
        }
        
        if (micRotateAnimator != null && micRotateAnimator.isRunning()) {
            micRotateAnimator.cancel();
        }

        // Reset mic icon state
        binding.micIcon.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .rotation(0f)
                .alpha(0.7f)
                .setDuration(300)
                .start();
    }

    // ---- mic level + glow pulse ----
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startMicLevelAndPulse() {
        glowCtl.onRecordingStart();
        sampler.start(level -> runOnUiThread(() -> {
            if (isRecording) glowCtl.onLevel(level); // gate by recording flag
        }));
    }

    private void stopMicLevelAndPulse() {
        sampler.stop();
        glowCtl.onRecordingPauseOrStop();
    }

    // ---- permission flow (unchanged, with callback) ----
    private void askPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
        } else {
            onMicPermissionGranted();
        }
    }

    private void onMicPermissionGranted() {
        // create once; reuse
        stt = new SpeechLiveTranscriber(this, new SpeechLiveTranscriber.Callbacks() {
            @Override public void onReady() {
                // optional: show a hint
                runOnUiThread(() -> binding.stateTv.setText("Listening…"));
            }
            @Override public void onPartial(String text) {
                runOnUiThread(() -> binding.transcriptTv.setText(text));
            }
            @Override public void onFinal(String text) {
                runOnUiThread(() -> {
                    // append final sentence to the rolling transcript
                    if (fullTranscript.length() > 0) fullTranscript.append(' ');
                    fullTranscript.append(text);
                    binding.transcriptTv.setText(fullTranscript.toString());
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    // Only show toast for serious errors, not for "no speech detected" messages
                    if (!message.contains("continuing to listen")) {
                        Toast.makeText(TranscriptActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMicPermissionGranted();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    openAppSettings();
                } else {
                    Toast.makeText(this, "Microphone permission is required.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording && !isPaused) {
            // Auto-pause when activity goes to background
            pauseRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (sampler != null) {
            sampler.stop();
        }
        stopRecordingAnimations();
        if (stt != null) stt.release();
    }
}
