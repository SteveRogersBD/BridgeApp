package com.example.bridge.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a complete transcript recording session.
 * Manages the full transcript text, individual segments, and session metadata.
 */
public class TranscriptSession {
    private String sessionId;
    private long startTime;
    private long duration;
    private StringBuilder fullTranscript;
    private String currentPartialText;
    private boolean isActive;
    private List<TranscriptSegment> segments;

    /**
     * Creates a new transcript session with a unique ID and current timestamp.
     */
    public TranscriptSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        this.duration = 0;
        this.fullTranscript = new StringBuilder();
        this.currentPartialText = "";
        this.isActive = false;
        this.segments = new ArrayList<>();
    }

    /**
     * Creates a new transcript session with a specific session ID.
     * @param sessionId The unique identifier for this session
     */
    public TranscriptSession(String sessionId) {
        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        this.duration = 0;
        this.fullTranscript = new StringBuilder();
        this.currentPartialText = "";
        this.isActive = false;
        this.segments = new ArrayList<>();
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public String getFullTranscript() {
        return fullTranscript.toString();
    }

    public String getCurrentPartialText() {
        return currentPartialText;
    }

    public boolean isActive() {
        return isActive;
    }

    public List<TranscriptSegment> getSegments() {
        return new ArrayList<>(segments); // Return a copy to prevent external modification
    }

    // Setters
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId != null ? sessionId : this.sessionId;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setDuration(long duration) {
        this.duration = Math.max(0, duration);
    }

    public void setCurrentPartialText(String currentPartialText) {
        this.currentPartialText = currentPartialText != null ? currentPartialText : "";
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (active && duration == 0) {
            // Reset start time when activating a new session
            this.startTime = System.currentTimeMillis();
        }
    }

    /**
     * Starts the transcript session.
     */
    public void start() {
        this.isActive = true;
        this.startTime = System.currentTimeMillis();
        this.duration = 0;
    }

    /**
     * Stops the transcript session and calculates the final duration.
     */
    public void stop() {
        if (isActive) {
            this.duration = System.currentTimeMillis() - startTime;
            this.isActive = false;
            this.currentPartialText = "";
        }
    }

    /**
     * Pauses the transcript session, preserving current state.
     */
    public void pause() {
        if (isActive) {
            this.duration += System.currentTimeMillis() - startTime;
            this.isActive = false;
        }
    }

    /**
     * Resumes the transcript session from a paused state.
     */
    public void resume() {
        if (!isActive) {
            this.startTime = System.currentTimeMillis();
            this.isActive = true;
        }
    }

    /**
     * Adds a new transcript segment to the session.
     * @param segment The segment to add
     */
    public void addSegment(TranscriptSegment segment) {
        if (segment != null) {
            segments.add(segment);
            if (segment.isFinal() && segment.hasContent()) {
                appendToFullTranscript(segment.getText());
            }
        }
    }

    /**
     * Appends text to the full transcript with proper spacing.
     * @param text The text to append
     */
    public void appendToFullTranscript(String text) {
        if (text != null && !text.trim().isEmpty()) {
            if (fullTranscript.length() > 0 && !fullTranscript.toString().endsWith(" ")) {
                fullTranscript.append(" ");
            }
            fullTranscript.append(text.trim());
        }
    }

    /**
     * Clears the full transcript and all segments.
     */
    public void clearTranscript() {
        fullTranscript.setLength(0);
        segments.clear();
        currentPartialText = "";
    }

    /**
     * Gets the current elapsed time of the session.
     * @return The elapsed time in milliseconds
     */
    public long getElapsedTime() {
        if (isActive) {
            return duration + (System.currentTimeMillis() - startTime);
        }
        return duration;
    }

    /**
     * Gets the total number of segments in this session.
     * @return The segment count
     */
    public int getSegmentCount() {
        return segments.size();
    }

    /**
     * Gets the total number of final segments in this session.
     * @return The final segment count
     */
    public int getFinalSegmentCount() {
        return (int) segments.stream().filter(TranscriptSegment::isFinal).count();
    }

    /**
     * Checks if the session has any transcript content.
     * @return true if there is transcript content, false otherwise
     */
    public boolean hasContent() {
        return fullTranscript.length() > 0 || 
               (currentPartialText != null && !currentPartialText.trim().isEmpty()) ||
               segments.stream().anyMatch(TranscriptSegment::hasContent);
    }

    /**
     * Gets the total character count of the full transcript.
     * @return The character count
     */
    public int getCharacterCount() {
        return fullTranscript.length();
    }

    /**
     * Gets the estimated word count of the full transcript.
     * @return The estimated word count
     */
    public int getWordCount() {
        String text = fullTranscript.toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    @Override
    public String toString() {
        return "TranscriptSession{" +
                "sessionId='" + sessionId + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", isActive=" + isActive +
                ", segmentCount=" + segments.size() +
                ", characterCount=" + fullTranscript.length() +
                '}';
    }
}