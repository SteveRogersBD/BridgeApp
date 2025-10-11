package com.example.bridge;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.databinding.ActivityMeetingBinding;
import com.example.bridge.utils.ActivityLogger;
import com.example.bridge.utils.AudioLevelSampler;
import com.example.bridge.utils.GeminiHelper;
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
    
    // AI functionality
    private GeminiHelper geminiHelper;
    private AlertDialog aiDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityMeetingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle system window insets
        setupWindowInsets();
        
        // Setup toolbar buttons
        setupToolbarButtons();

        // Initialize components
        setupAnimations();
        setupTimer();

        // Set up glow controller
        glowCtl = new GlowPulseController(binding.micGlow);
        sampler = new AudioLevelSampler();
        
        // Initialize GeminiHelper
        geminiHelper = new GeminiHelper();

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

        // AI button click listener
        binding.getRoot().findViewById(R.id.ai_btn1).setOnClickListener(v -> {
            showAIPromptDialog();
        });

//        // Add floating button toggle
//        binding.getRoot().findViewById(R.id.floating_toggle_btn).setOnClickListener(v -> {
//            if (FloatingButtonManager.canDrawOverlays(this)) {
//                FloatingButtonManager.startFloatingButton(this);
//                Toast.makeText(this, "Floating button started", Toast.LENGTH_SHORT).show();
//            } else {
//                FloatingButtonManager.requestOverlayPermission(this);
//            }
//        });

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FloatingButtonManager.isOverlayPermissionResult(requestCode)) {
            if (FloatingButtonManager.canDrawOverlays(this)) {
                FloatingButtonManager.startFloatingButton(this);
                Toast.makeText(this, "Floating button started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
            }
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
        // Log meeting activity before destroying
        logMeetingActivity();
        
        super.onDestroy();
        // Clean up resources
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (sampler != null) {
            sampler.stop();
        }
        stopRecordingAnimations();
        
        // Dismiss AI dialog if showing
        if (aiDialog != null && aiDialog.isShowing()) {
            aiDialog.dismiss();
        }
    }
    
    private void logMeetingActivity() {
        String transcript = binding.transcriptTv.getText() != null ? 
            binding.transcriptTv.getText().toString().trim() : "";
        
        if (!transcript.isEmpty()) {
            String title = "Meeting Session";
            
            // Log the meeting activity with transcript content
            ActivityLogger.logActivity(this, ActivityLogger.ActivityType.MEETING, title, transcript);
        }
    }
    
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            // Get system bars insets
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            
            // Apply top padding to toolbar to avoid status bar overlap
            binding.toolBar.setPadding(
                binding.toolBar.getPaddingLeft(),
                topInset,
                binding.toolBar.getPaddingRight(),
                binding.toolBar.getPaddingBottom()
            );
            
            return insets;
        });
    }
    
    private void setupToolbarButtons() {
        // Back button click listener
        binding.backBtn.setOnClickListener(v -> {
            finish(); // Close the activity and go back
        });
    }
    
    private void showAIPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_ai_prompt, null);
        builder.setView(dialogView);
        
        aiDialog = builder.create();
        aiDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Get references to dialog views
        EditText editTextPrompt = dialogView.findViewById(R.id.editTextPrompt);
        LinearLayout buttonSend = dialogView.findViewById(R.id.buttonSend);
        LinearLayout buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        LinearLayout progressLayout = dialogView.findViewById(R.id.aiProgressLayout);
        
        // Set up button click listeners
        buttonCancel.setOnClickListener(v -> aiDialog.dismiss());
        
        buttonSend.setOnClickListener(v -> {
            String prompt = editTextPrompt.getText().toString().trim();
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Process the AI prompt
            processAIPrompt(prompt, progressLayout, buttonSend, buttonCancel);
        });
        
        aiDialog.show();
    }
    
    private void processAIPrompt(String prompt, LinearLayout progressLayout, LinearLayout buttonSend, LinearLayout buttonCancel) {
        String currentTranscript = binding.transcriptTv.getText() == null ? "" : binding.transcriptTv.getText().toString().trim();
        
        if (currentTranscript.isEmpty()) {
            Toast.makeText(this, "Please record some transcript first, then use AI to enhance it", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Show progress bar and hide buttons
        progressLayout.setVisibility(android.view.View.VISIBLE);
        buttonSend.setVisibility(android.view.View.GONE);
        buttonCancel.setVisibility(android.view.View.GONE);
        
        // Start glowing animation
        android.view.animation.Animation glowAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.glow_pulse);
        progressLayout.startAnimation(glowAnimation);
        
        // Create the full prompt for Gemini
        String fullPrompt = "Please enhance the following transcript based on this instruction: \"" + prompt + "\"\n\n" +
                           "Original transcript: \"" + currentTranscript + "\"\n\n" +
                           "Please return only the enhanced transcript without any additional explanation or formatting.";
        
        // Call Gemini API
        geminiHelper.callGemini(fullPrompt, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    // Hide progress and update the transcript EditText
                    progressLayout.clearAnimation();
                    progressLayout.setVisibility(android.view.View.GONE);
                    
                    // Clean up the result (remove any extra formatting)
                    String enhancedTranscript = result.trim();
                    if (enhancedTranscript.startsWith("\"") && enhancedTranscript.endsWith("\"")) {
                        enhancedTranscript = enhancedTranscript.substring(1, enhancedTranscript.length() - 1);
                    }
                    
                    // Update the transcript EditText
                    binding.transcriptTv.setText(enhancedTranscript);
                    
                    // Close the dialog
                    aiDialog.dismiss();
                    
                    // Show success message
                    Toast.makeText(MeetingActivity.this, "Transcript enhanced successfully!", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    // Hide progress and show buttons again
                    progressLayout.clearAnimation();
                    progressLayout.setVisibility(android.view.View.GONE);
                    buttonSend.setVisibility(android.view.View.VISIBLE);
                    buttonCancel.setVisibility(android.view.View.VISIBLE);
                    
                    // Show error message
                    Toast.makeText(MeetingActivity.this, "AI processing failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}


