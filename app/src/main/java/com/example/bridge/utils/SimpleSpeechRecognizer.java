package com.example.bridge.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SimpleSpeechRecognizer {
    
    public interface SpeechListener {
        void onSpeechReady();
        void onSpeechResult(String text);
        void onSpeechError(String error);
    }
    
    private static final String TAG = "SimpleSpeechRecognizer";
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private SpeechListener listener;
    private boolean isListening = false;
    
    public SimpleSpeechRecognizer(Context context, SpeechListener listener) {
        this.context = context;
        this.listener = listener;
        setupSpeechRecognizer();
    }
    
    private void setupSpeechRecognizer() {
        Log.d(TAG, "Setting up speech recognizer");
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available");
            if (listener != null) {
                listener.onSpeechError("Speech recognition not available on this device");
            }
            return;
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
                if (listener != null) {
                    listener.onSpeechReady();
                }
            }
            
            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech started");
            }
            
            @Override
            public void onRmsChanged(float rmsdB) {
                // Volume level changed
            }
            
            @Override
            public void onBufferReceived(byte[] buffer) {
                // Audio buffer received
            }
            
            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
                isListening = false;
            }
            
            @Override
            public void onError(int error) {
                Log.e(TAG, "Speech error: " + error);
                isListening = false;
                
                String errorMsg = "Speech recognition failed";
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMsg = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMsg = "Client side error";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        errorMsg = "Insufficient permissions";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMsg = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMsg = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMsg = "No speech recognized. Speak clearly and try again.";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMsg = "Speech recognizer busy";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        errorMsg = "Server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMsg = "No speech input detected";
                        break;
                }
                
                if (listener != null) {
                    listener.onSpeechError(errorMsg);
                }
            }
            
            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "Speech results received");
                isListening = false;
                
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String result = matches.get(0);
                    Log.d(TAG, "Recognized: " + result);
                    if (listener != null) {
                        listener.onSpeechResult(result);
                    }
                } else {
                    if (listener != null) {
                        listener.onSpeechError("No speech detected");
                    }
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {
                // Handle partial results if needed
            }
            
            @Override
            public void onEvent(int eventType, Bundle params) {
                // Handle events if needed
            }
        });
    }
    
    public void startListening() {
        if (speechRecognizer != null && !isListening) {
            Log.d(TAG, "Starting to listen");
            isListening = true;
            
            // Cancel any previous session
            speechRecognizer.cancel();
            
            // Start listening after a short delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isListening && speechRecognizer != null) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }, 100);
        }
    }
    
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            Log.d(TAG, "Stopping listening");
            speechRecognizer.stopListening();
        }
    }
    
    public void cancel() {
        if (speechRecognizer != null) {
            Log.d(TAG, "Cancelling speech recognition");
            isListening = false;
            speechRecognizer.cancel();
        }
    }
    
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
    }
    
    public boolean isListening() {
        return isListening;
    }
}