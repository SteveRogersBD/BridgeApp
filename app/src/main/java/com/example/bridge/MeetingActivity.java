package com.example.bridge;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bridge.databinding.ActivityMeetingBinding;
import com.example.bridge.utils.AudioLevelSampler;
import com.example.bridge.utils.GlowPulseController;
import com.example.bridge.utils.SimpleSpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;


public class MeetingActivity extends AppCompatActivity {

    private ActivityMeetingBinding binding;
    private static final int MIC_PERMISSION = 101;

    private ActivityResultLauncher<Intent> speechLauncher;

    // NEW: loop control
    private boolean autoMode = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable relaunchTask = this::launchGoogleSpeech; // scheduled relaunch
    private static final long RELAUNCH_DELAY_MS = 300L; // small pause between sessions

    // Timer functionality
    private long startTimeMs = 0L;
    private long pausedTimeMs = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Animation and glow controllers
    private GlowPulseController glowCtl;
    private AudioLevelSampler sampler;
    private ObjectAnimator micPulseAnimator;
    private ObjectAnimator micRotateAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeetingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize components
        setupAnimations();
        setupTimer();

        // Set up glow controller
        glowCtl = new GlowPulseController(binding.micGlow);
        sampler = new AudioLevelSampler();

        // init launcher
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    boolean gotText = false;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spoken = matches.get(0).trim();
                            if (!spoken.isEmpty()) {
                                appendToTranscript(spoken);
                                gotText = true;
                            }
                        }
                    }
                    binding.stateTv.setText(gotText ? "appended" : "no speech");

                    // if auto mode still on, relaunch after a short delay
                    if (autoMode) {
                        // schedule, don't launch immediately
                        mainHandler.postDelayed(relaunchTask, RELAUNCH_DELAY_MS);
                    } else {
                        // show idle state when not looping
                        binding.stateTv.setText("paused");
                        stopRecordingAnimations();
                        stopMicLevelAndPulse();
                        pauseTimer();
                    }
                }
        );

        askPermission();

        binding.pauseBtn.setOnClickListener(v -> {
            ensureMicPermissionThen(() -> {
                if (!autoMode) {
                    // start auto mode
                    autoMode = true;
                    startTimeMs = System.currentTimeMillis();
                    pausedTimeMs = 0L;
                    
                    // Start timer and animations
                    startTimer();
                    startRecordingAnimations();
                    startMicLevelAndPulse();
                    
                    binding.pauseBtn.setImageResource(R.drawable.pause);
                    binding.stateTv.setText("listening…");
                    launchGoogleSpeech();
                } else {
                    // user pressed Pause: stop *future* relaunches
                    autoMode = false;
                    mainHandler.removeCallbacks(relaunchTask); // <— key line
                    
                    // Stop timer and animations
                    pauseTimer();
                    stopRecordingAnimations();
                    stopMicLevelAndPulse();
                    
                    binding.pauseBtn.setImageResource(R.drawable.play);
                    binding.stateTv.setText("paused");
                }
            });
        });


        binding.saveBtn.setOnClickListener(v -> {
            if (autoMode) {
                // Stop recording when saving
                autoMode = false;
                mainHandler.removeCallbacks(relaunchTask);
                pauseTimer();
                stopRecordingAnimations();
                stopMicLevelAndPulse();
                binding.pauseBtn.setImageResource(R.drawable.play);
                binding.stateTv.setText("saved");
                
                // Reset timer display
                binding.timerTv.setText("00:00:00");
                startTimeMs = 0L;
                pausedTimeMs = 0L;
            }
            Toast.makeText(this, "saved (stub)", Toast.LENGTH_SHORT).show();
        });

        binding.pauseBtn.setImageResource(R.drawable.play);
        binding.stateTv.setText("paused");
        binding.timerTv.setText("00:00:00");
    }

    private void launchGoogleSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Google speech not available. Install/update the Google app.",
                    Toast.LENGTH_LONG).show();
            autoMode = false;
            //binding.pauseBtn.setText("Start");
            binding.stateTv.setText("paused");
        }
    }

    private void appendToTranscript(String text) {
        String existing = binding.transcriptTv.getText() == null
                ? "" : binding.transcriptTv.getText().toString();
        binding.transcriptTv.setText(existing.isEmpty() ? text : (existing + " " + text));
    }

    // --- permissions ---
    private void askPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
        } else {
            onMicPermissionGranted();
        }
    }
    private void ensureMicPermissionThen(Runnable action) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            action.run();
        } else {
            askPermission();
        }
    }
    private void onMicPermissionGranted() {
        binding.pauseBtn.setEnabled(true);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            binding.pauseBtn.setEnabled(granted);
            if (!granted) Toast.makeText(this, "microphone permission is required.", Toast.LENGTH_SHORT).show();
        }
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
                if (autoMode) {
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
    private void startMicLevelAndPulse() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED) {
            glowCtl.onRecordingStart();
            sampler.start(level -> runOnUiThread(() -> {
                if (autoMode) glowCtl.onLevel(level); // gate by recording flag
            }));
        }
    }

    private void stopMicLevelAndPulse() {
        sampler.stop();
        glowCtl.onRecordingPauseOrStop();
    }

    @Override
    protected void onStop() {
        // safety: stop auto loop when leaving
        autoMode = false;
        mainHandler.removeCallbacks(relaunchTask);
        pauseTimer();
        stopRecordingAnimations();
        stopMicLevelAndPulse();
        super.onStop();
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
    }
}


