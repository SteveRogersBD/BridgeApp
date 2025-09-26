package com.example.bridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bridge.adapters.ChatAdapter;
import com.example.bridge.databinding.ActivityChatBinding;
import com.example.bridge.models.ChatModel;
import com.example.bridge.utils.GeminiHelper;
import com.example.bridge.utils.SpeechCaptureManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity implements
        SpeechCaptureManager.Listener {

    ActivityChatBinding binding;
    ChatAdapter adapter;
    GeminiHelper gm;
    List<ChatModel> messages;
    private boolean listening = false;
    private boolean micHeld = false;
    TextToSpeech tts;
    SpeechCaptureManager sst;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1001;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //set permission for voice record
        getPermission();
        //initialize
        gm = new GeminiHelper();
        sst = new SpeechCaptureManager(ChatActivity.this,this);

        // Setup RecyclerView
        messages = new ArrayList<>();
        adapter = new ChatAdapter(ChatActivity.this, messages);
        binding.mainRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.mainRecycler.setAdapter(adapter);
        
        // Add a welcome message
        //addMessage("Welcome! You can type a message or use voice input.", ChatModel.SENT_BY_OTHER);
//        binding.fabMic.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                toggleListening();
//            }
//        });


        binding.fabMic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Check permission before starting recording
//                if (ContextCompat.checkSelfPermission(ChatActivity.this,
//                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(ChatActivity.this,
//                            "Please grant microphone permission to use voice input",
//                            Toast.LENGTH_SHORT).show();
//                    return true;
//                }
                //getPermission();
                
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    // when touched
                    //toggleListening();
                    startListening();
                    micHeld = true;
                    //start recording
                    sst.start();

                }
                else if(event.getAction() == MotionEvent.ACTION_UP)
                {
                    //when released
                    //toggleListening();
                    stopListening();
                    micHeld = false;
                    //stop recording
                    sst.stop();
                }
                return true; //means we handled the touch
            }
        });

        tts = new TextToSpeech(ChatActivity.this, new TextToSpeech.OnInitListener() {
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
                        binding.fabMic.setEnabled(true);
                    }

                }

            }
        });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = binding.textEt.getText().toString().trim();
                if(text.isEmpty())
                {
                    binding.textEt.setError("First type your message");
                    return;
                }
                
                // Add user message to chat
                addMessage(text, ChatModel.SENT_BY_ME);
                
                // Clear input field
                binding.textEt.setText("");
                
                // Speak the message
                tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u1");
                
                // Simulate a response (you can replace this with actual AI response)
                simulateResponse(text);
            }
        });
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
            //Toast.makeText(this, "Voice input ready!", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleListening() {
        if(listening) {
            stopListening();
        }
        else startListening();
    }

    private void startListening() {
        listening = true;
        binding.micOverlay.setVisibility(View.VISIBLE);
        binding.micLottie.setProgress(0f);
        binding.micLottie.playAnimation();
        binding.fabMic.setImageResource(R.drawable.block); // or a pause/stop icon
    }

    private void stopListening() {
        listening = false;
        binding.micLottie.cancelAnimation();
        binding.micOverlay.setVisibility(View.GONE);
        binding.fabMic.setImageResource(R.drawable.mic);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // be safe: stop animation if activity goes background
        if (binding.micLottie.isAnimating()) {
            binding.micLottie.cancelAnimation();
        }
        binding.micOverlay.setVisibility(View.GONE);
        listening = false;
        binding.fabMic.setImageResource(R.drawable.mic);
    }

    @Override
    public void onReady() {
        //when the listener is ready and state is LISTENING
    }

    @Override
    public void onFinalText(String text) {
        //got the final text and state is FINALIZING
        if (text != null && !text.trim().isEmpty()) {
            // Add voice message to chat
            addMessage(text.trim(), ChatModel.SENT_BY_ME);
            
            // Speak the recognized text
            tts.setPitch(1.0f);
            tts.setSpeechRate(1.0f);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice1");
            
            // Simulate a response (you can replace this with actual AI response)
            simulateResponse(text.trim());
        }
    }

    @Override
    public void onError(String message) {
        // any error occurred
        Toast.makeText(this, "Voice recognition error: " + message, Toast.LENGTH_SHORT).show();
        stopListening();
    }

    @Override
    public void onStateChanged(SpeechCaptureManager.State state) {
        //when state has changed
    }

    // Helper method to add messages to the chat
    private void addMessage(String message, int sentBy) {
        messages.add(new ChatModel(message, sentBy));
        adapter.notifyItemInserted(messages.size() - 1);
        
        // Scroll to the latest message
        binding.mainRecycler.scrollToPosition(messages.size() - 1);
    }
    
    // Simulate AI response (replace with actual AI integration)
    private void simulateResponse(String userMessage) {
        // Add a delay to simulate processing
        binding.mainRecycler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String response = generateResponse(userMessage);
                addMessage(response, ChatModel.SENT_BY_OTHER);
            }
        }, 1500); // 1.5 second delay
    }
    
    // Generate a simple response (replace with actual AI logic)
    private String generateResponse(String userMessage) {
        String message = userMessage.toLowerCase();
        
        if (message.contains("hello") || message.contains("hi")) {
            return "Hello! How can I help you today?";
        } else if (message.contains("how are you")) {
            return "I'm doing great! Thanks for asking. How are you?";
        } else if (message.contains("weather")) {
            return "I don't have access to weather data, but I hope it's nice where you are!";
        } else if (message.contains("time")) {
            return "I don't have access to the current time, but you can check your device's clock.";
        } else if (message.contains("thank")) {
            return "You're welcome! Is there anything else I can help you with?";
        } else if (message.contains("bye") || message.contains("goodbye")) {
            return "Goodbye! Have a great day!";
        } else {
            return "That's interesting! Tell me more about that.";
        }
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
            } else {
                // Permission denied
                binding.fabMic.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}