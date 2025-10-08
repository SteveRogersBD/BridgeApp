package com.example.bridge.models;

/**
 * Enum representing the different states of a recording session with display text mapping.
 * Used to track and display the current status of transcription and recording operations.
 */
public enum RecordingState {
    IDLE("Ready to record"),
    LISTENING("Listening…"),
    PAUSED("Paused"),
    PROCESSING("Processing…"),
    ERROR("Error occurred");

    private final String displayText;

    RecordingState(String displayText) {
        this.displayText = displayText;
    }

    /**
     * Gets the user-friendly display text for this recording state.
     * @return The display text to show to users
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * Checks if the current state represents an active recording session.
     * @return true if recording is active (listening or processing), false otherwise
     */
    public boolean isActive() {
        return this == LISTENING || this == PROCESSING;
    }

    /**
     * Checks if the current state allows starting a new recording.
     * @return true if recording can be started from this state, false otherwise
     */
    public boolean canStartRecording() {
        return this == IDLE || this == ERROR;
    }

    /**
     * Checks if the current state allows pausing the recording.
     * @return true if recording can be paused from this state, false otherwise
     */
    public boolean canPause() {
        return this == LISTENING || this == PROCESSING;
    }

    /**
     * Checks if the current state allows resuming the recording.
     * @return true if recording can be resumed from this state, false otherwise
     */
    public boolean canResume() {
        return this == PAUSED;
    }
}