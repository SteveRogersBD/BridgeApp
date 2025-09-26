package com.example.bridge.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechCaptureManager {

    public interface Listener {
        void onReady();                   // ui can show "listening…"
        void onFinalText(String text);    // final transcript after release
        void onError(String message);     // friendly error
        void onStateChanged(State state); // optional: IDLE/LISTENING/FINALIZING
    }

    public enum State{IDLE,LISTENING,FINALIZING}

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private Intent intent;
    private State state = State.IDLE;

    public SpeechCaptureManager(Context ctx, Listener listener) {
        this.context = ctx.getApplicationContext();
        this.listener = listener;
        setup();
    }

    private void setup() {
        // 1. check if device even supports speech recognition
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (listener != null) listener.onError("speech service not available");
            return; // stop setup, nothing else makes sense
        }

        // 2. create a SpeechRecognizer instance tied to this app
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);

        // 3. register a RecognitionListener to receive callbacks
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (listener != null) listener.onReady();
            }

            @Override public void onBeginningOfSpeech() { /* user started talking */ }

            @Override public void onRmsChanged(float rmsdB) { /* mic volume changes */ }

            @Override public void onBufferReceived(byte[] buffer) { /* raw audio chunks */ }

            @Override public void onEndOfSpeech() { /* silence detected or stopListening() called */ }

            @Override public void onResults(Bundle results) {
                // final transcript arrives here
                setState(State.IDLE);
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (list != null && !list.isEmpty()) ? list.get(0) : "";
                if (text.isEmpty()) {
                    if (listener != null) listener.onError("no speech detected");
                } else {
                    if (listener != null) listener.onFinalText(text);
                }
            }

            @Override public void onPartialResults(Bundle partialResults) { /* ignored now */ }

            @Override public void onError(int error) {
                setState(State.IDLE);
                if (listener != null) listener.onError(friendlyError(error));
            }

            @Override public void onEvent(int eventType, Bundle params) { }
        });

        // 4. prepare the recognizer intent that tells Android "how" we want recognition
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false); // we only want final text
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1); // top 1 guess

        // 5. tweak silence detection windows
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500);
    }

    public void start() {
        if (recognizer == null || state != State.IDLE) return;
        setState(State.LISTENING);
        recognizer.cancel(); // reset any prior session
        recognizer.startListening(intent);
    }

    // call when user releases the button
    public void stop() {
        if (recognizer == null) return;
        if (state == State.LISTENING) setState(State.FINALIZING);
        recognizer.stopListening();
    }

    // use for aborts (slide-to-cancel or onPause)
    public void cancel() {
        if (recognizer == null) return;
        setState(State.IDLE);
        recognizer.cancel();
    }

    public void destroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        setState(State.IDLE);
    }

    private void setState(State s) {
        state = s;
        if (listener != null) listener.onStateChanged(state);
    }

    private String friendlyError(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network error";
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech was recognized";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER: return "Error from server";
            default: return "Unknown speech error: " + code;
        }
    }
}
