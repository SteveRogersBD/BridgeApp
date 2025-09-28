package com.example.bridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bridge.adapters.ChatAdapter;
import com.example.bridge.databinding.ActivityChatBinding;
import com.example.bridge.models.ChatModel;
import com.example.bridge.utils.GeminiHelper;
import com.example.bridge.utils.SimpleSpeechRecognizer;
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
    TextToSpeech tts;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1001;
    SimpleSpeechRecognizer speechRecognizer;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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

        // Initialize speech recognizer
        speechRecognizer = new SimpleSpeechRecognizer(this, new SimpleSpeechRecognizer.SpeechListener() {
            @Override
            public void onSpeechReady() {
                Log.d(TAG, "Speech ready - showing UI");
                // Show recording UI
                binding.micOverlay.setVisibility(View.VISIBLE);
                binding.micLottie.playAnimation();
                Toast.makeText(ChatActivity.this, "Listening... Speak now!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSpeechResult(String text) {
                Log.d(TAG, "Speech result: " + text);
                // Hide recording UI
                binding.micOverlay.setVisibility(View.GONE);
                binding.micLottie.cancelAnimation();

                // Add message to chat if text is not empty
                if (!text.trim().isEmpty()) {
                    addMessage(text, ChatModel.SENT_BY_OTHER);
                    speak(text);
                    Toast.makeText(ChatActivity.this, "Added: " + text, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Empty text received", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onSpeechError(String error) {
                Log.e(TAG, "Speech error: " + error);
                // Hide recording UI and show error
                binding.micOverlay.setVisibility(View.GONE);
                binding.micLottie.cancelAnimation();
                Toast.makeText(ChatActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Set up mic button
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
            binding.fabMic.setEnabled(true);
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
        }


    };


    private void speak(String text) {
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
                binding.fabMic.setEnabled(true);
                Toast.makeText(this, "Voice input ready! Press and hold mic to record", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                binding.fabMic.setEnabled(false);
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMicButton() {
        // Enable mic button only if permission is granted
        binding.fabMic.setEnabled(
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
        );

        binding.fabMic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (ContextCompat.checkSelfPermission(ChatActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(ChatActivity.this,
                            "Microphone permission required", Toast.LENGTH_SHORT).show();
                    return true;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start listening when button is pressed
                        Log.d(TAG, "Mic button pressed - starting listening");
                        speechRecognizer.startListening();
                        v.setPressed(true);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Stop listening when button is released
                        Log.d(TAG, "Mic button released - stopping listening");
                        speechRecognizer.stopListening();
                        v.setPressed(false);
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        // Cancel listening if touch is cancelled
                        Log.d(TAG, "Mic button cancelled - cancelling listening");
                        speechRecognizer.cancel();
                        v.setPressed(false);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }


}