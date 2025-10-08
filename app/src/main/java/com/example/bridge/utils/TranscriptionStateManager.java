package com.example.bridge.utils;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.bridge.R;
import com.example.bridge.models.RecordingState;

/**
 * Manages visual feedback and state coordination for transcription activity.
 * Handles state transitions, animations, and visual indicators based on transcription status.
 */
public class TranscriptionStateManager {
    
    private final Context context;
    private final TextView stateTextView;
    private final ImageView micIcon;
    private final ImageView pauseButton;
    private final View micGlow;
    
    private RecordingState currentState = RecordingState.IDLE;
    private ObjectAnimator stateColorAnimator;
    private ObjectAnimator micIconAnimator;
    private ValueAnimator glowIntensityAnimator;
    
    // Animation durations
    private static final int STATE_TRANSITION_DURATION = 300;
    private static final int MIC_PULSE_DURATION = 1000;
    private static final int GLOW_FADE_DURATION = 500;
    
    public TranscriptionStateManager(Context context, TextView stateTextView, 
                                   ImageView micIcon, ImageView pauseButton, View micGlow) {
        this.context = context;
        this.stateTextView = stateTextView;
        this.micIcon = micIcon;
        this.pauseButton = pauseButton;
        this.micGlow = micGlow;
        
        try {
            initializeAnimators();
        } catch (Exception e) {
            // Animators may fail in test environment, continue without them
        }
    }
    
    private void initializeAnimators() {
        // State color transition animator
        stateColorAnimator = ObjectAnimator.ofObject(stateTextView, "textColor", 
                new ArgbEvaluator(), 0, 0);
        stateColorAnimator.setDuration(STATE_TRANSITION_DURATION);
        stateColorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Mic icon pulse animator for processing state
        micIconAnimator = ObjectAnimator.ofFloat(micIcon, "alpha", 1.0f, 0.6f);
        micIconAnimator.setDuration(MIC_PULSE_DURATION);
        micIconAnimator.setRepeatMode(ValueAnimator.REVERSE);
        micIconAnimator.setRepeatCount(ValueAnimator.INFINITE);
        micIconAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Glow intensity animator
        glowIntensityAnimator = ValueAnimator.ofFloat(0f, 1f);
        glowIntensityAnimator.setDuration(GLOW_FADE_DURATION);
        glowIntensityAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowIntensityAnimator.addUpdateListener(animation -> {
            float intensity = (float) animation.getAnimatedValue();
            updateGlowIntensity(intensity);
        });
    }
    
    /**
     * Updates the transcription state and triggers appropriate visual feedback.
     */
    public void updateState(RecordingState newState) {
        if (currentState == newState) {
            return; // No change needed
        }
        
        RecordingState previousState = currentState;
        currentState = newState;
        
        // Update state text and color
        updateStateDisplay();
        
        // Update button state
        updateButtonState();
        
        // Update microphone visual feedback
        updateMicrophoneVisuals(previousState);
        
        // Update glow effects
        updateGlowEffects();
    }
    
    /**
     * Updates state text and color with smooth transition.
     */
    private void updateStateDisplay() {
        // Update text immediately
        stateTextView.setText(currentState.getDisplayText());
        
        // Animate color change
        int targetColor = getStateColor(currentState);
        
        if (stateColorAnimator != null) {
            int currentColor = stateTextView.getCurrentTextColor();
            
            if (stateColorAnimator.isRunning()) {
                stateColorAnimator.cancel();
            }
            
            stateColorAnimator.setObjectValues(currentColor, targetColor);
            stateColorAnimator.start();
        } else {
            // Fallback for test environment - set color directly
            stateTextView.setTextColor(targetColor);
        }
    }
    
    /**
     * Gets the appropriate color for the given recording state.
     */
    private int getStateColor(RecordingState state) {
        switch (state) {
            case IDLE:
                return ContextCompat.getColor(context, R.color.on_surface_variant);
            case LISTENING:
                return ContextCompat.getColor(context, R.color.primary);
            case PROCESSING:
                return ContextCompat.getColor(context, R.color.tertiary);
            case PAUSED:
                return ContextCompat.getColor(context, R.color.stroke_orange);
            case ERROR:
                return ContextCompat.getColor(context, R.color.stroke_red);
            default:
                return ContextCompat.getColor(context, R.color.on_surface_variant);
        }
    }
    
    /**
     * Updates the pause/play button based on current state.
     */
    private void updateButtonState() {
        int iconResource;
        
        if (currentState == RecordingState.IDLE || currentState == RecordingState.PAUSED || 
            currentState == RecordingState.ERROR) {
            iconResource = R.drawable.play;
        } else {
            iconResource = R.drawable.pause;
        }
        
        pauseButton.setImageResource(iconResource);
    }
    
    /**
     * Updates microphone visual feedback based on state transition.
     */
    private void updateMicrophoneVisuals(RecordingState previousState) {
        // Stop any running mic animations
        if (micIconAnimator != null && micIconAnimator.isRunning()) {
            micIconAnimator.cancel();
            micIcon.setAlpha(1.0f);
        }
        
        switch (currentState) {
            case IDLE:
                // Reset to default state
                micIcon.animate()
                        .alpha(0.7f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(STATE_TRANSITION_DURATION)
                        .start();
                break;
                
            case LISTENING:
                // Active listening state - steady glow
                micIcon.animate()
                        .alpha(1.0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(STATE_TRANSITION_DURATION)
                        .start();
                break;
                
            case PROCESSING:
                // Processing state - pulsing animation
                micIcon.setAlpha(1.0f);
                micIcon.setScaleX(1.0f);
                micIcon.setScaleY(1.0f);
                if (micIconAnimator != null) {
                    micIconAnimator.start();
                }
                break;
                
            case PAUSED:
                // Paused state - dimmed
                micIcon.animate()
                        .alpha(0.5f)
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(STATE_TRANSITION_DURATION)
                        .start();
                break;
                
            case ERROR:
                // Error state - subtle red tint effect
                micIcon.animate()
                        .alpha(0.8f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(STATE_TRANSITION_DURATION)
                        .start();
                break;
        }
    }
    
    /**
     * Updates glow effects based on current state.
     */
    private void updateGlowEffects() {
        if (glowIntensityAnimator != null && glowIntensityAnimator.isRunning()) {
            glowIntensityAnimator.cancel();
        }
        
        float targetIntensity;
        
        switch (currentState) {
            case LISTENING:
                targetIntensity = 0.8f;
                break;
            case PROCESSING:
                targetIntensity = 1.0f;
                break;
            case PAUSED:
                targetIntensity = 0.3f;
                break;
            case ERROR:
                targetIntensity = 0.4f;
                break;
            case IDLE:
            default:
                targetIntensity = 0.0f;
                break;
        }
        
        if (glowIntensityAnimator != null) {
            float currentIntensity = micGlow.getAlpha();
            glowIntensityAnimator.setFloatValues(currentIntensity, targetIntensity);
            glowIntensityAnimator.start();
        } else {
            // Fallback for test environment - set intensity directly
            updateGlowIntensity(targetIntensity);
        }
    }
    
    /**
     * Updates glow intensity during animation.
     */
    private void updateGlowIntensity(float intensity) {
        micGlow.setAlpha(intensity);
        
        // Adjust glow color based on current state
        int glowColor = getGlowColor(currentState);
        micGlow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(glowColor));
    }
    
    /**
     * Gets the appropriate glow color for the given state.
     */
    private int getGlowColor(RecordingState state) {
        switch (state) {
            case LISTENING:
                return ContextCompat.getColor(context, R.color.primary);
            case PROCESSING:
                return ContextCompat.getColor(context, R.color.tertiary);
            case PAUSED:
                return ContextCompat.getColor(context, R.color.stroke_orange);
            case ERROR:
                return ContextCompat.getColor(context, R.color.stroke_red);
            case IDLE:
            default:
                return ContextCompat.getColor(context, R.color.primary);
        }
    }
    
    /**
     * Triggers a brief processing indication (e.g., when receiving partial results).
     */
    public void indicateProcessing() {
        if (currentState == RecordingState.LISTENING) {
            // Brief processing state indication
            RecordingState originalState = currentState;
            updateState(RecordingState.PROCESSING);
            
            // Return to listening state after brief delay
            micIcon.postDelayed(() -> {
                if (currentState == RecordingState.PROCESSING) {
                    updateState(originalState);
                }
            }, 200);
        }
    }
    
    /**
     * Synchronizes glow effects with audio level input.
     */
    public void syncWithAudioLevel(float audioLevel) {
        if (currentState == RecordingState.LISTENING || currentState == RecordingState.PROCESSING) {
            // Modulate glow intensity based on audio level
            float baseIntensity = currentState == RecordingState.LISTENING ? 0.8f : 1.0f;
            float modulatedIntensity = Math.min(1.0f, baseIntensity + (audioLevel * 0.3f));
            
            micGlow.setAlpha(modulatedIntensity);
        }
    }
    
    /**
     * Gets the current recording state.
     */
    public RecordingState getCurrentState() {
        return currentState;
    }
    
    /**
     * Checks if the current state allows starting recording.
     */
    public boolean canStartRecording() {
        return currentState.canStartRecording();
    }
    
    /**
     * Checks if the current state allows pausing.
     */
    public boolean canPause() {
        return currentState.canPause();
    }
    
    /**
     * Checks if the current state allows resuming.
     */
    public boolean canResume() {
        return currentState.canResume();
    }
    
    /**
     * Cleanup method to stop all animations and reset state.
     */
    public void cleanup() {
        if (stateColorAnimator != null && stateColorAnimator.isRunning()) {
            stateColorAnimator.cancel();
        }
        if (micIconAnimator != null && micIconAnimator.isRunning()) {
            micIconAnimator.cancel();
        }
        if (glowIntensityAnimator != null && glowIntensityAnimator.isRunning()) {
            glowIntensityAnimator.cancel();
        }
        
        currentState = RecordingState.IDLE;
    }
}