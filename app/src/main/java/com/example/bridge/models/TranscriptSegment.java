package com.example.bridge.models;

/**
 * Represents an individual text segment within a transcript.
 * Each segment contains the transcribed text along with metadata about timing and confidence.
 */
public class TranscriptSegment {
    private String text;
    private long timestamp;
    private boolean isFinal;
    private float confidence;

    /**
     * Creates a new transcript segment.
     * @param text The transcribed text content
     * @param timestamp The timestamp when this segment was created (in milliseconds)
     * @param isFinal Whether this is a final result or partial/interim result
     * @param confidence The confidence score of the transcription (0.0 to 1.0)
     */
    public TranscriptSegment(String text, long timestamp, boolean isFinal, float confidence) {
        this.text = text != null ? text : "";
        this.timestamp = timestamp;
        this.isFinal = isFinal;
        this.confidence = Math.max(0.0f, Math.min(1.0f, confidence)); // Clamp between 0 and 1
    }

    /**
     * Creates a new transcript segment with current timestamp.
     * @param text The transcribed text content
     * @param isFinal Whether this is a final result or partial/interim result
     * @param confidence The confidence score of the transcription (0.0 to 1.0)
     */
    public TranscriptSegment(String text, boolean isFinal, float confidence) {
        this(text, System.currentTimeMillis(), isFinal, confidence);
    }

    /**
     * Creates a new final transcript segment with high confidence.
     * @param text The transcribed text content
     */
    public TranscriptSegment(String text) {
        this(text, System.currentTimeMillis(), true, 1.0f);
    }

    // Getters
    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public float getConfidence() {
        return confidence;
    }

    // Setters
    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void setConfidence(float confidence) {
        this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
    }

    /**
     * Checks if this segment has meaningful content.
     * @return true if the text is not empty or whitespace-only, false otherwise
     */
    public boolean hasContent() {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Gets the length of the text content.
     * @return The number of characters in the text
     */
    public int getLength() {
        return text != null ? text.length() : 0;
    }

    /**
     * Checks if this segment has high confidence (>= 0.8).
     * @return true if confidence is high, false otherwise
     */
    public boolean hasHighConfidence() {
        return confidence >= 0.8f;
    }

    @Override
    public String toString() {
        return "TranscriptSegment{" +
                "text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", isFinal=" + isFinal +
                ", confidence=" + confidence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TranscriptSegment that = (TranscriptSegment) o;

        if (timestamp != that.timestamp) return false;
        if (isFinal != that.isFinal) return false;
        if (Float.compare(that.confidence, confidence) != 0) return false;
        return text != null ? text.equals(that.text) : that.text == null;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (isFinal ? 1 : 0);
        result = 31 * result + (confidence != +0.0f ? Float.floatToIntBits(confidence) : 0);
        return result;
    }
}