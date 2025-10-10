package com.example.bridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bridge.adapters.ChatAdapter;
import com.example.bridge.models.ChatModel;
import com.example.bridge.utils.SimpleSpeechRecognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FloatingChatOverlay {
    
    private static final String TAG = "FloatingChatOverlay";
    private Context context;
    private WindowManager windowManager;
    private View floatingChatView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;
    
    // Chat components
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView micButton;
    private ImageView closeButton;
    private View micOverlay;
    
    private ChatAdapter adapter;
    private List<ChatModel> messages = new ArrayList<>();
    private TextToSpeech tts;
    private SimpleSpeechRecognizer speechRecognizer;
    
    public FloatingChatOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        createFloatingChat();
        initializeChat();
    }
    
    private void createFloatingChat() {
        try {
            // Create a themed context for Material components
            Context themedContext = new androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_Bridge);
            LayoutInflater inflater = LayoutInflater.from(themedContext);
            floatingChatView = inflater.inflate(R.layout.floating_chat_overlay, null);
        } catch (Exception e) {
            // Fallback to simple layout
            LayoutInflater inflater = LayoutInflater.from(context);
            floatingChatView = inflater.inflate(R.layout.simple_floating_chat, null);
        }
        
        // Set up window parameters
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
            
        params = new WindowManager.LayoutParams(
            (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95),
            (int) (context.getResources().getDisplayMetrics().heightPixels * 0.8),
            layoutType,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.CENTER;
        
        setupViews();
    }
    
    private void setupViews() {
        // Find views
        recyclerView = floatingChatView.findViewById(R.id.chat_recycler);
        messageInput = floatingChatView.findViewById(R.id.message_input);
        sendButton = floatingChatView.findViewById(R.id.send_btn);
        micButton = floatingChatView.findViewById(R.id.mic_btn);
        closeButton = floatingChatView.findViewById(R.id.close_btn);
        micOverlay = floatingChatView.findViewById(R.id.mic_overlay);
        
        // Close button
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hide());
        }
        
        // Send button
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> sendMessage());
        }
        
        // Setup message input
        if (messageInput != null) {
            messageInput.setOnClickListener(v -> {
                // Request focus and show keyboard
                messageInput.requestFocus();
                showKeyboard();
            });
            
            messageInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showKeyboard();
                } else {
                    hideKeyboard();
                }
            });
            
            // Handle Enter key to send message
            messageInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND || 
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && 
                     event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                    sendMessage();
                    return true;
                }
                return false;
            });
        }
        
        // Setup mic button
        setupMicButton();
        
        // Click outside to close
        floatingChatView.setOnClickListener(v -> hide());
        
        // Prevent clicks on the chat area from closing
        View chatContainer = floatingChatView.findViewById(R.id.chat_container);
        if (chatContainer != null) {
            chatContainer.setOnClickListener(v -> {
                // Do nothing - prevent closing when clicking inside chat
            });
        }
    }
    
    private void initializeChat() {
        // Setup RecyclerView
        if (recyclerView != null) {
            adapter = new ChatAdapter(context, messages);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        }
        
        // Initialize TTS
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS language not supported");
                }
            }
        });
        
        // Initialize speech recognizer
        speechRecognizer = new SimpleSpeechRecognizer(context, new SimpleSpeechRecognizer.SpeechListener() {
            @Override
            public void onSpeechReady() {
                Log.d(TAG, "Speech ready");
                if (micOverlay != null) {
                    micOverlay.setVisibility(View.VISIBLE);
                }
                Toast.makeText(context, "Listening... Speak now!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSpeechResult(String text) {
                Log.d(TAG, "Speech result: " + text);
                if (micOverlay != null) {
                    micOverlay.setVisibility(View.GONE);
                }
                
                if (!text.trim().isEmpty()) {
                    addMessage(text, ChatModel.SENT_BY_OTHER);
                    speak(text);
                }
            }

            @Override
            public void onSpeechError(String error) {
                Log.e(TAG, "Speech error: " + error);
                if (micOverlay != null) {
                    micOverlay.setVisibility(View.GONE);
                }
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private void setupMicButton() {
        if (micButton == null) return;
        
        // Check permission
        boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
        micButton.setEnabled(hasPermission);
        
        if (!hasPermission) {
            micButton.setAlpha(0.5f);
            micButton.setOnClickListener(v -> 
                Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show());
            return;
        }
        
        micButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "Mic button pressed");
                    speechRecognizer.startListening();
                    v.setPressed(true);
                    return true;
                    
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "Mic button released");
                    speechRecognizer.stopListening();
                    v.setPressed(false);
                    return true;
                    
                case MotionEvent.ACTION_CANCEL:
                    Log.d(TAG, "Mic button cancelled");
                    speechRecognizer.cancel();
                    v.setPressed(false);
                    return true;
            }
            return false;
        });
    }
    
    private void sendMessage() {
        if (messageInput == null) return;
        
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            messageInput.setError("Type a message");
            return;
        }
        
        addMessage(text, ChatModel.SENT_BY_ME);
        speak(text);
        messageInput.setText("");
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private void addMessage(String text, int sentBy) {
        ChatModel msg = new ChatModel(text, sentBy);
        messages.add(msg);
        if (adapter != null) {
            adapter.notifyItemInserted(messages.size() - 1);
            if (recyclerView != null) {
                recyclerView.scrollToPosition(messages.size() - 1);
            }
        }
    }
    
    private void speak(String text) {
        if (tts != null) {
            tts.setPitch(1.0f);
            tts.setSpeechRate(1.0f);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chat_msg");
        }
    }
    
    private void showKeyboard() {
        if (messageInput != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                messageInput.requestFocus();
                imm.showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    
    private void hideKeyboard() {
        if (messageInput != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
            }
        }
    }
    
    public void show() {
        if (!isShowing && floatingChatView != null) {
            try {
                windowManager.addView(floatingChatView, params);
                isShowing = true;
            } catch (Exception e) {
                Log.e(TAG, "Error showing floating chat", e);
            }
        }
    }
    
    public void hide() {
        if (isShowing && floatingChatView != null) {
            try {
                // Hide keyboard before removing view
                hideKeyboard();
                windowManager.removeView(floatingChatView);
                isShowing = false;
            } catch (Exception e) {
                Log.e(TAG, "Error hiding floating chat", e);
            }
        }
    }
    
    public boolean isShowing() {
        return isShowing;
    }
    
    public void destroy() {
        hide();
        
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        floatingChatView = null;
    }
}