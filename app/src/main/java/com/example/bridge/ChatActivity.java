package com.example.bridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bridge.adapters.ChatAdapter;
import com.example.bridge.databinding.ActivityChatBinding;
import com.example.bridge.models.ChatModel;
import com.example.bridge.utils.ActivityLogger;
import com.example.bridge.utils.GeminiHelper;
import android.speech.RecognizerIntent;
import com.example.bridge.utils.SpeechCaptureManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "AlternativeActivity";
    ActivityChatBinding binding;
    ChatAdapter adapter;
    GeminiHelper gm;
    List<ChatModel> messages = new ArrayList<>();
    List<ChatModel> engMessages = new ArrayList<>();
    TextToSpeech tts;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1001;
    private static final int SPEECH_REQUEST_CODE = 1002;
    private String fromLang = "English";
    private String toLang = "English";

    
    // Language arrays for translation settings
    private String[] languages = {
        "Afrikaans", "Albanian", "Amharic", "Arabic", "Armenian", "Azerbaijani",
        "Basque", "Belarusian", "Bengali", "Bosnian", "Bulgarian", "Catalan",
        "Cebuano", "Chinese (Simplified)", "Chinese (Traditional)", "Corsican", "Croatian", "Czech",
        "Danish", "Dutch", "English", "Esperanto", "Estonian", "Filipino",
        "Finnish", "French", "Frisian", "Galician", "Georgian", "German",
        "Greek", "Gujarati", "Haitian Creole", "Hausa", "Hawaiian", "Hebrew",
        "Hindi", "Hmong", "Hungarian", "Icelandic", "Igbo", "Indonesian",
        "Irish", "Italian", "Japanese", "Javanese", "Kannada", "Kazakh",
        "Khmer", "Korean", "Kurdish", "Kyrgyz", "Lao", "Latin",
        "Latvian", "Lithuanian", "Luxembourgish", "Macedonian", "Malagasy", "Malay",
        "Malayalam", "Maltese", "Maori", "Marathi", "Mongolian", "Myanmar",
        "Nepali", "Norwegian", "Pashto", "Persian", "Polish", "Portuguese",
        "Punjabi", "Romanian", "Russian", "Samoan", "Scots Gaelic", "Serbian",
        "Sesotho", "Shona", "Sindhi", "Sinhala", "Slovak", "Slovenian",
        "Somali", "Spanish", "Sundanese", "Swahili", "Swedish", "Tajik",
        "Tamil", "Telugu", "Thai", "Turkish", "Ukrainian", "Urdu",
        "Uzbek", "Vietnamese", "Welsh", "Xhosa", "Yiddish", "Yoruba", "Zulu"
    };


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Handle system window insets
        setupWindowInsets();
        
        //get permission for voice record
        getPermission();
        adapter = new ChatAdapter(this,messages);
        binding.mainRecycler.setAdapter(adapter);
        binding.mainRecycler.setLayoutManager(new LinearLayoutManager(this));
        gm = new GeminiHelper();


        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(Locale.UK);
                    if(result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Toast.makeText(ChatActivity.this,
                                "Language not supported", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        // do something else
                    }

                }

            }
        });

        binding.sendBtn.setOnClickListener(onClickListener);
        binding.aiBtn.setOnClickListener(aiClickListener);
        
        // Set up toolbar buttons
        setupToolbarButtons();
        
        // Set up mic button with Google speech recognition
        setupMicButton();


    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Show explanation to user
                Toast.makeText(this, "Microphone permission is needed for voice input",
                        Toast.LENGTH_LONG).show();
            }
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE);
        } else {
            // Permission already granted
            Log.d(TAG, "Microphone permission already granted");
            binding.fabMic.setEnabled(true);
            setupMicButton(); // Ensure mic button is properly set up
        }
    }
    View.OnClickListener onClickListener = v->{
        if(v.getId() == binding.sendBtn.getId())
        {
            String text = binding.textEt.getText().toString().trim();
            if(text.isEmpty())
            {
                binding.textEt.setError("First type your message");
                return;
            }
            // Add user message to chat
            addMessage(text, ChatModel.SENT_BY_ME);
            // Speak the message
            speak(text);
            // Clear input field
            binding.textEt.setText("");


            String prompt = translate(text, fromLang);

            gm.callGemini(prompt, new GeminiHelper.GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    speak(result);
                    result = "Translation: " + result;
                    ChatModel msg = new ChatModel(result,ChatModel.SENT_BY_ME);
                    runOnUiThread(()->{

                        messages.add(msg);
                        adapter.notifyItemInserted(messages.size() - 1);
                        binding.mainRecycler.smoothScrollToPosition(messages.size());
                    });
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }


    };

    View.OnClickListener aiClickListener = v -> {
        if (v.getId() == binding.aiBtn.getId()) {
            generateAIReply();
        }
    };

    private void generateAIReply() {
        // Build context from chat history
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("You are a helpful AI assistant in a chat conversation. ");
        
        if (messages.isEmpty()) {
            // No conversation history - generate a greeting
            contextBuilder.append("Generate a friendly greeting to start a conversation. ");
            contextBuilder.append("Keep it warm, welcoming, and encourage the user to share what's on their mind.");
        } else {
            contextBuilder.append("Based on the following conversation history, generate a natural and relevant reply. ");
            contextBuilder.append("Keep your response conversational, helpful, and contextually appropriate.\n\n");
            contextBuilder.append("Conversation History:\n");
            
            // Add recent messages for context (last 10 messages to avoid token limits)
            int startIndex = Math.max(0, messages.size() - 10);
            for (int i = startIndex; i < messages.size(); i++) {
                ChatModel msg = messages.get(i);
                String sender = (msg.getSendBy() == ChatModel.SENT_BY_ME) ? "User" : "Other";
                contextBuilder.append(sender).append(": ").append(msg.getMessage()).append("\n");
            }
            
            contextBuilder.append("\nGenerate a helpful and relevant reply as the AI assistant. ");
        }
        
        contextBuilder.append("Keep it concise and natural. Do not include any labels or prefixes in your response.");
        
        String prompt = contextBuilder.toString();
        
        // Show loading state
        binding.aiBtn.setEnabled(false);
        Toast.makeText(ChatActivity.this, "Generating AI reply...", Toast.LENGTH_SHORT).show();
        
        gm.callGemini(prompt, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    // Set the AI-generated reply in the text field
                    binding.textEt.setText(result.trim());
                    binding.aiBtn.setEnabled(true);
                    Toast.makeText(ChatActivity.this, "AI reply generated! Edit if needed or send as is.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    binding.aiBtn.setEnabled(true);
                    Toast.makeText(ChatActivity.this, "Failed to generate AI reply: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void speak(String text) {
        translate(text, toLang);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u1");
    }

    @SuppressLint("NotifyDataSetChanged")
    private void addMessage(String text, int sentBy) {
        ChatModel msg = new ChatModel(text,sentBy);
        messages.add(msg);
        adapter.notifyItemInserted(messages.size() - 1);
        binding.mainRecycler.scrollToPosition(messages.size()-1);



    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d(TAG, "Audio permission granted");
                binding.fabMic.setEnabled(true);
                
                // Re-setup the mic button with proper listeners
                setupMicButton();
                
                Toast.makeText(this, "Voice input ready! Press and hold mic to record", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Log.w(TAG, "Audio permission denied");
                binding.fabMic.setEnabled(false);
                Toast.makeText(this, "Microphone permission denied. Voice input disabled.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupMicButton() {
        Log.d(TAG, "Setting up mic button with Google speech recognition");
        
        // Enable mic button only if permission is granted
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        
        binding.fabMic.setEnabled(hasPermission);
        
        if (!hasPermission) {
            Log.w(TAG, "Microphone permission not granted");
            return;
        }

        // Click listener to start GameActivity
        binding.fabMic.setOnClickListener(v -> {
            Log.d(TAG, "FabMic button clicked - starting GameActivity");
            
            // Navigate to GameActivity
            Intent gameIntent = new Intent(ChatActivity.this, GameActivity.class);
            startActivity(gameIntent);
        });
        
        Log.d(TAG, "Mic button setup complete");
    }
    
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            // Get system bars insets
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            // Apply top padding to toolbar to avoid status bar overlap
            binding.toolBar.setPadding(
                binding.toolBar.getPaddingLeft(),
                topInset,
                binding.toolBar.getPaddingRight(),
                binding.toolBar.getPaddingBottom()
            );
            
            // Apply bottom margin to send card to avoid navigation bar overlap
            android.view.ViewGroup.MarginLayoutParams sendCardParams = 
                (android.view.ViewGroup.MarginLayoutParams) binding.sendCard.getLayoutParams();
            sendCardParams.bottomMargin = bottomInset + 16; // 16dp additional margin
            binding.sendCard.setLayoutParams(sendCardParams);
            
            // Apply bottom margin to FAB to avoid navigation bar overlap
            android.view.ViewGroup.MarginLayoutParams fabParams = 
                (android.view.ViewGroup.MarginLayoutParams) binding.fabMic.getLayoutParams();
            fabParams.bottomMargin = bottomInset + 80; // 80dp to account for send card height
            binding.fabMic.setLayoutParams(fabParams);
            
            return insets;
        });
    }

    private void setupToolbarButtons() {
        // Back button click listener
        binding.backBtn.setOnClickListener(v -> {
            finish(); // Close the activity and go back
        });
        
        // Settings button click listener
        binding.settingsBtn.setOnClickListener(v -> {
            showTranslationSettingsDialog();
        });
    }
    
    private void showTranslationSettingsDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_translation_settings, null);
        
        // Get references to the dropdown views
        AutoCompleteTextView fromLanguageSpinner = dialogView.findViewById(R.id.from_language_spinner);
        AutoCompleteTextView toLanguageSpinner = dialogView.findViewById(R.id.to_language_spinner);
        
        // Create adapters for the dropdowns
        ArrayAdapter<String> fromAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        ArrayAdapter<String> toAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        
        // Set adapters to the dropdowns
        fromLanguageSpinner.setAdapter(fromAdapter);
        toLanguageSpinner.setAdapter(toAdapter);
        
        // Set default selections
        fromLanguageSpinner.setText("English", false);
        toLanguageSpinner.setText("English", false);
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Set up the Done button
        dialogView.findViewById(R.id.done_btn).setOnClickListener(v -> {
            fromLang = fromLanguageSpinner.getText().toString();
            toLang = toLanguageSpinner.getText().toString();

            
            // TODO: Save the selected languages (will be implemented later)
            Toast.makeText(this, "Translation settings saved: " + fromLang + " → "
                    + toLang, Toast.LENGTH_SHORT).show();
            
            dialog.dismiss();
        });
        
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                Log.d(TAG, "Speech recognition result: " + spokenText);
                
                if (!spokenText.trim().isEmpty()) {
                    // Translate the transcribed text first, then add combined message
                    String prompt = translate(spokenText, toLang);
                    
                    gm.callGemini(prompt, new GeminiHelper.GeminiCallback() {
                        @Override
                        public void onSuccess(String result) {
                            runOnUiThread(() -> {
                                // Combine original text and translation in one message on left side
                                String combinedMessage = spokenText + "\n\nTranslation: " + result;
                                addMessage(combinedMessage, ChatModel.SENT_BY_OTHER);
                                Toast.makeText(ChatActivity.this, "Message with translation added", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            runOnUiThread(() -> {
                                // If translation fails, just add the original text
                                addMessage(spokenText, ChatModel.SENT_BY_OTHER);
                                Toast.makeText(ChatActivity.this, "Translation failed, original text added", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    Toast.makeText(this, "No speech detected", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == SPEECH_REQUEST_CODE) {
            Toast.makeText(this, "Speech recognition cancelled or failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Log chat activity before destroying
        logChatActivity();
        
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
    
    private void logChatActivity() {
        if (messages != null && !messages.isEmpty()) {
            // Create a summary of the chat content
            StringBuilder chatContent = new StringBuilder();
            String title = "Chat Session";
            
            // Get the first few messages to create context
            int messagesToInclude = Math.min(5, messages.size());
            for (int i = 0; i < messagesToInclude; i++) {
                ChatModel msg = messages.get(i);
                if (msg.getSendBy() == ChatModel.SENT_BY_ME) {
                    chatContent.append("User: ");
                } else {
                    chatContent.append("AI: ");
                }
                chatContent.append(msg.getMessage()).append(" ");
            }
            
            // If there are more messages, indicate that
            if (messages.size() > messagesToInclude) {
                chatContent.append("... (").append(messages.size()).append(" total messages)");
            }
            
            // Log the activity
            ActivityLogger.logActivity(this, ActivityLogger.ActivityType.CHAT, title, chatContent.toString());
        }
    }



    private String translate(String inputText, String to) {
        // template: from → to, and return ONLY the translated text
        String prompt = "You are a professional translator.\n"
                + "Translate the following text to %s.\n"
                + "Keep the tone, context, and meaning intact.\n\n"
                + "Text:\n"
                + "%s\n\n"
                + "Only return the translated text.\n"
                + "Do not include any explanations, notes, or labels.";

        // IMPORTANT: return the formatted string (don’t drop it!)
        return String.format(prompt, to, inputText);
    }

}
