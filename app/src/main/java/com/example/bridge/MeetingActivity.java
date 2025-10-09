package com.example.bridge;

import static android.widget.Toast.LENGTH_SHORT;

import android.Manifest;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMeetingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                    }
                }
        );

        askPermission();

        binding.pauseBtn.setOnClickListener(v -> {
            ensureMicPermissionThen(() -> {
                if (!autoMode) {
                    // start auto mode
                    autoMode = true;
                    //binding.pauseBtn.setText("Stop");
                    binding.stateTv.setText("listening…");
                    launchGoogleSpeech();
                } else {
                    // user pressed Pause: stop *future* relaunches
                    autoMode = false;
                    mainHandler.removeCallbacks(relaunchTask); // <— key line
                    //binding.pauseBtn.setText("Start");
                    binding.stateTv.setText("paused");
                }
            });
        });


        binding.saveBtn.setOnClickListener(v ->
                Toast.makeText(this, "saved (stub)", Toast.LENGTH_SHORT).show());

        //binding.pauseBtn.setText("Start");
        binding.stateTv.setText("paused");
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
    protected void onStop() {
        // safety: stop auto loop when leaving
        autoMode = false;
        //binding.pauseBtn.setText("Start");
        super.onStop();
    }
}


