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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class VoiceNoteActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private EditText editTextMessage;
    private LinearLayout buttonAI, buttonCreateNote, buttonClear;
    private TextToSpeech textToSpeech;
    private File audioFile;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_note);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupTextToSpeech();
        setupClickListeners();
    }

    private void initializeViews() {
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonAI = findViewById(R.id.buttonAI);
        buttonCreateNote = findViewById(R.id.buttonCreateNote);
        buttonClear = findViewById(R.id.buttonClear);
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
    }

    private void setupClickListeners() {
        buttonClear.setOnClickListener(v -> {
            editTextMessage.setText("");
            Toast.makeText(this, "Text cleared", Toast.LENGTH_SHORT).show();
        });

        buttonCreateNote.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show();
                return;
            }
            createVoiceNote(message);
        });

        // AI button - no logic for now as requested
        buttonAI.setOnClickListener(v -> {
            Toast.makeText(this, "AI feature coming soon", Toast.LENGTH_SHORT).show();
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
            intent.putExtra(Intent.EXTRA_TEXT, "Please find attached voice note created with Bridge app.\n\nMessage content: \"" + editTextMessage.getText().toString().trim() + "\"");
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
        super.onDestroy();
    }
}