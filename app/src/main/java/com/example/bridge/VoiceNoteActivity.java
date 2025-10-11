package com.example.bridge;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.example.bridge.databinding.ActivityVoiceNoteBinding;
import com.example.bridge.utils.GeminiHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class VoiceNoteActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    ActivityVoiceNoteBinding binding;
    private TextToSpeech textToSpeech;
    private File audioFile;
    private AlertDialog loadingDialog;
    private GeminiHelper geminiHelper;
    private AlertDialog aiDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityVoiceNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle system window insets
        setupWindowInsets();
        
        // Setup toolbar buttons
        setupToolbarButtons();

        setupTextToSpeech();
        setupClickListeners();
        
        // Initialize GeminiHelper
        geminiHelper = new GeminiHelper();
    }

    private void initializeViews() {
        // Views are now accessed through binding
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
    }

    private void setupClickListeners() {
        binding.buttonClear.setOnClickListener(v -> {
            binding.editTextMessage.setText("");
            Toast.makeText(this, "Text cleared", Toast.LENGTH_SHORT).show();
        });

        binding.buttonCreateNote.setOnClickListener(v -> {
            String message = binding.editTextMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show();
                return;
            }
            createVoiceNote(message);
        });

        // AI button - show prompt dialog
        binding.buttonAI.setOnClickListener(v -> {
            showAIPromptDialog();
        });
    }

    private void createVoiceNote(String text) {
        if (textToSpeech != null) {
            // Show loading dialog
            showLoadingDialog();
            
            // Create audio file with MP3 extension
            audioFile = new File(getExternalFilesDir(null), "voice_note_" + System.currentTimeMillis() + ".wav");
            
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "voice_note");
            
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Loading dialog is already shown
                }

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        hideLoadingDialog();
                        showShareConfirmationDialog();
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        hideLoadingDialog();
                        Toast.makeText(VoiceNoteActivity.this, "Error creating voice note", Toast.LENGTH_SHORT).show();
                    });
                }
            });

            int result = textToSpeech.synthesizeToFile(text, params, audioFile.getAbsolutePath());
            if (result != TextToSpeech.SUCCESS) {
                hideLoadingDialog();
                Toast.makeText(this, "Failed to create voice note", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_loading, null));
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showShareConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Voice Note Created!")
                .setMessage("Your voice note has been successfully created. Would you like to share it?")
                .setPositiveButton("Share", (dialog, which) -> showSharingDialog())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void showSharingDialog() {
        String[] platforms = {"WhatsApp", "Messenger", "Email", "Other Apps"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share Voice Note")
                .setItems(platforms, (dialog, which) -> {
                    switch (which) {
                        case 0: // WhatsApp
                            shareToWhatsApp();
                            break;
                        case 1: // Messenger
                            shareToMessenger();
                            break;
                        case 2: // Email
                            shareToEmail();
                            break;
                        case 3: // Other Apps
                            shareToOtherApps();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareToWhatsApp() {
        try {
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.putExtra(Intent.EXTRA_TEXT, "Voice note created with Bridge");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed or error sharing", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToMessenger() {
        try {
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.setPackage("com.facebook.orca");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Messenger not installed or error sharing", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToEmail() {
        try {
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Voice Note from Bridge");
            intent.putExtra(Intent.EXTRA_TEXT, "Please find attached voice note created with Bridge app.\n\nMessage content: \"" + binding.editTextMessage.getText().toString().trim() + "\"");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Share via Email"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing via email", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToOtherApps() {
        try {
            Uri audioUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", audioFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_STREAM, audioUri);
            intent.putExtra(Intent.EXTRA_TEXT, "Voice note created with Bridge");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Share Voice Note"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing voice note", Toast.LENGTH_SHORT).show();
        }
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
        String currentMessage = binding.editTextMessage.getText().toString().trim();
        
        if (currentMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a message first, then use AI to enhance it", Toast.LENGTH_LONG).show();
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
        String fullPrompt = "Please enhance the following message based on this instruction: \"" + prompt + "\"\n\n" +
                           "Original message: \"" + currentMessage + "\"\n\n" +
                           "Please return only the enhanced message without any additional explanation or formatting.";
        
        // Call Gemini API
        geminiHelper.callGemini(fullPrompt, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    // Hide progress and update the main EditText
                    progressLayout.clearAnimation();
                    progressLayout.setVisibility(android.view.View.GONE);
                    
                    // Clean up the result (remove any extra formatting)
                    String enhancedMessage = result.trim();
                    if (enhancedMessage.startsWith("\"") && enhancedMessage.endsWith("\"")) {
                        enhancedMessage = enhancedMessage.substring(1, enhancedMessage.length() - 1);
                    }
                    
                    // Update the main message EditText
                    binding.editTextMessage.setText(enhancedMessage);
                    
                    // Close the dialog
                    aiDialog.dismiss();
                    
                    // Show success message
                    Toast.makeText(VoiceNoteActivity.this, "Message enhanced successfully!", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(VoiceNoteActivity.this, "AI processing failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        hideLoadingDialog();
        
        // Dismiss AI dialog if showing
        if (aiDialog != null && aiDialog.isShowing()) {
            aiDialog.dismiss();
        }
        
        super.onDestroy();
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
}