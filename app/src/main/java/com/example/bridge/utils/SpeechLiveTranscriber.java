package com.example.bridge.utils;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;

public class SpeechLiveTranscriber {

    public interface Callbacks {
        // fired when recognizer is ready (mic open)
        void onReady();
        // interim words (real-time updates)
        void onPartial(String text);
        // final sentence
        void onFinal(String text);
        // if anything fails
        void onError(String message);
    }

    private final Context context;
    private final Callbacks cb;
    private SpeechRecognizer recognizer;
    private Intent intent;
    private boolean isListening = false;
    private boolean continuous = false;

    public SpeechLiveTranscriber(Context ctx, Callbacks callbacks) {
        this.context = ctx.getApplicationContext();
        this.cb = callbacks;
        setup();
    }

    private void setup() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (cb != null) cb.onError("Speech recognition not available on this device");
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // try offline first (falls back to online if unavailable)
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        // Add speech timeout settings to prevent error code 7
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);
        // optional: add your app package for better attribution
        // intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (cb != null) cb.onReady();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override public void onError(int error) {
                isListening = false;
                String errorMessage = getErrorMessage(error);
                if (cb != null) cb.onError(errorMessage);
                
                // For error code 7 (NO_MATCH), automatically restart if in continuous mode
                if (continuous && (error == SpeechRecognizer.ERROR_NO_MATCH || 
                                  error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    // Add a small delay before restarting to prevent rapid restarts
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (continuous) restart();
                    }, 500);
                } else if (continuous) {
                    restart();
                }
            }

            @Override public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> texts = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts != null && !texts.isEmpty() && cb != null) {
                    cb.onFinal(texts.get(0));
                }
                if (continuous) restart();
            }

            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> texts = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts != null && !texts.isEmpty() && cb != null) {
                    cb.onPartial(texts.get(0));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void start(boolean continuousMode) {
        this.continuous = continuousMode;
        if (!isListening && recognizer != null) {
            isListening = true;
            recognizer.startListening(intent);
        }
    }

    public void stop() {
        continuous = false;
        if (recognizer != null && isListening) {
            recognizer.stopListening();
        }
        isListening = false;
    }

    public void cancel() {
        continuous = false;
        if (recognizer != null) recognizer.cancel();
        isListening = false;
    }

    private void restart() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.startListening(intent);
            isListening = true;
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech detected - continuing to listen...";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input detected - continuing to listen...";
            default:
                return "Recognition error (code: " + error + ")";
        }
    }

    public void release() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }
}
