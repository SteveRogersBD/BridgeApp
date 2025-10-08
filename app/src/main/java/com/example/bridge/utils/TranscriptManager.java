package com.example.bridge.utils;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * TranscriptManager handles text formatting, combination logic for partial and final transcripts,
 * auto-scroll detection and control mechanisms, and transcript history management.
 * 
 * Requirements addressed: 2.1, 2.2, 2.3
 */
public class TranscriptManager {
    
    public interface ScrollCallback {
        void onScrollToBottom();
    }
    
    private final TextView transcriptTextView;
    private final ScrollView scrollView;
    private final Handler mainHandler;
    
    private StringBuilder fullTranscript;
    private String currentPartialText;
    private ScrollCallback scrollCallback;
    private boolean autoScrollEnabled;
    private boolean userScrolledManually;
    
    // Auto-scroll detection variables
    private int lastScrollY;
    private long lastScrollTime;
    private long lastUserScrollTime;
    private static final long SCROLL_DETECTION_DELAY_MS = 1000;
    private static final long USER_SCROLL_TIMEOUT_MS = 3000; // Resume auto-scroll after 3 seconds of no user interaction
    private static final int SCROLL_THRESHOLD = 30; // pixels - more sensitive detection
    private static final int ANIMATION_DURATION_MS = 300; // Smooth scroll animation duration
    
    // Animation and interaction tracking
    private ObjectAnimator currentScrollAnimator;
    private boolean isAnimatingScroll;
    private final Runnable autoScrollResumeRunnable;
    
    // Memory optimization
    private static final int MAX_TRANSCRIPT_CHARS = 50000;
    private static final int CLEANUP_THRESHOLD_CHARS = 45000;
    private long lastMemoryOptimization = 0;
    private static final long MEMORY_OPTIMIZATION_INTERVAL_MS = 30000; // 30 seconds
    
    public TranscriptManager(TextView transcriptTextView, ScrollView scrollView) {
        this.transcriptTextView = transcriptTextView;
        this.scrollView = scrollView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.fullTranscript = new StringBuilder();
        this.currentPartialText = "";
        this.autoScrollEnabled = true;
        this.userScrolledManually = false;
        this.isAnimatingScroll = false;
        
        // Runnable to resume auto-scroll after user interaction timeout
        this.autoScrollResumeRunnable = () -> {
            if (System.currentTimeMillis() - lastUserScrollTime >= USER_SCROLL_TIMEOUT_MS) {
                if (isAtBottom()) {
                    userScrolledManually = false;
                }
            }
        };
        
        setupScrollDetection();
    }
    
    /**
     * Updates the partial text (temporary transcription results)
     * Requirements: 2.1 - Display partial transcription results immediately
     */
    public void updatePartialText(String partialText) {
        if (partialText == null) {
            partialText = "";
        }
        
        this.currentPartialText = partialText;
        updateDisplay();
    }
    
    /**
     * Appends final text to the full transcript
     * Requirements: 2.2 - Append final text to full transcript
     */
    public void appendFinalText(String finalText) {
        if (finalText == null || finalText.trim().isEmpty()) {
            return;
        }
        
        // Add space if there's existing content
        if (fullTranscript.length() > 0) {
            fullTranscript.append(" ");
        }
        
        fullTranscript.append(finalText.trim());
        
        // Clear partial text since it's now final
        currentPartialText = "";
        
        // Check if memory optimization is needed
        checkMemoryOptimization();
        
        updateDisplay();
        
        // Auto-scroll to show new content if enabled
        if (autoScrollEnabled && !userScrolledManually) {
            scrollToBottomSmooth();
        }
    }
    
    /**
     * Clears the entire transcript
     */
    public void clearTranscript() {
        cancelCurrentAnimation();
        fullTranscript.setLength(0);
        currentPartialText = "";
        userScrolledManually = false;
        mainHandler.removeCallbacks(autoScrollResumeRunnable);
        updateDisplay();
    }
    
    /**
     * Returns the full transcript text without partial text
     */
    public String getFullTranscript() {
        return fullTranscript.toString();
    }
    
    /**
     * Returns the complete display text (full + partial)
     */
    public String getCompleteText() {
        StringBuilder completeText = new StringBuilder(fullTranscript);
        
        if (!currentPartialText.isEmpty()) {
            if (completeText.length() > 0) {
                completeText.append(" ");
            }
            completeText.append(currentPartialText);
        }
        
        return completeText.toString();
    }
    
    /**
     * Sets the callback for scroll events
     */
    public void setScrollCallback(ScrollCallback callback) {
        this.scrollCallback = callback;
    }
    
    /**
     * Enables or disables auto-scrolling
     */
    public void setAutoScrollEnabled(boolean enabled) {
        this.autoScrollEnabled = enabled;
    }
    
    /**
     * Returns whether auto-scroll is currently enabled
     */
    public boolean isAutoScrollEnabled() {
        return autoScrollEnabled && !userScrolledManually;
    }
    
    /**
     * Forces scroll to bottom immediately (no animation)
     */
    public void scrollToBottom() {
        mainHandler.post(() -> {
            cancelCurrentAnimation();
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            if (scrollCallback != null) {
                scrollCallback.onScrollToBottom();
            }
        });
    }
    
    /**
     * Smoothly scrolls to bottom with animation
     * Requirements: 2.3 - Smooth auto-scroll animation when new text is added
     */
    public void scrollToBottomSmooth() {
        mainHandler.post(() -> {
            if (isAnimatingScroll) {
                return; // Don't start new animation if one is already running
            }
            
            int currentScrollY = scrollView.getScrollY();
            int targetScrollY = getMaxScrollY();
            
            if (currentScrollY >= targetScrollY - 10) {
                // Already at bottom, call callback immediately
                if (scrollCallback != null) {
                    scrollCallback.onScrollToBottom();
                }
                return;
            }
            
            cancelCurrentAnimation();
            
            isAnimatingScroll = true;
            currentScrollAnimator = ObjectAnimator.ofInt(scrollView, "scrollY", currentScrollY, targetScrollY);
            currentScrollAnimator.setDuration(ANIMATION_DURATION_MS);
            currentScrollAnimator.setInterpolator(new DecelerateInterpolator());
            
            currentScrollAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    isAnimatingScroll = false;
                    if (scrollCallback != null) {
                        scrollCallback.onScrollToBottom();
                    }
                }
                
                @Override
                public void onAnimationCancel(android.animation.Animator animation) {
                    isAnimatingScroll = false;
                }
            });
            
            currentScrollAnimator.start();
        });
    }
    
    /**
     * Resets manual scroll detection (e.g., when user wants to re-enable auto-scroll)
     */
    public void resetScrollDetection() {
        userScrolledManually = false;
        mainHandler.removeCallbacks(autoScrollResumeRunnable);
    }
    
    /**
     * Returns whether the user has manually scrolled recently
     */
    public boolean hasUserScrolledManually() {
        return userScrolledManually;
    }
    
    /**
     * Cancels any current scroll animation
     */
    private void cancelCurrentAnimation() {
        if (currentScrollAnimator != null && currentScrollAnimator.isRunning()) {
            currentScrollAnimator.cancel();
        }
        isAnimatingScroll = false;
    }
    
    /**
     * Gets the maximum scroll Y position
     */
    private int getMaxScrollY() {
        if (scrollView.getChildCount() == 0) {
            return 0;
        }
        int contentHeight = scrollView.getChildAt(0).getHeight();
        int scrollViewHeight = scrollView.getHeight();
        return Math.max(0, contentHeight - scrollViewHeight);
    }
    
    /**
     * Updates the TextView display with current full + partial text
     * Requirements: 2.3 - Auto-scroll to show latest content
     */
    private void updateDisplay() {
        mainHandler.post(() -> {
            String displayText = getCompleteText();
            transcriptTextView.setText(displayText);
        });
    }
    
    /**
     * Sets up scroll detection to pause auto-scroll when user manually scrolls
     * Requirements: 2.3 - Maintain scrollability and detect user interaction
     */
    private void setupScrollDetection() {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int currentScrollY = scrollView.getScrollY();
            long currentTime = System.currentTimeMillis();
            
            // Ignore scroll changes during our own animations
            if (isAnimatingScroll) {
                lastScrollY = currentScrollY;
                lastScrollTime = currentTime;
                return;
            }
            
            // Check if this is a significant scroll change by user
            if (Math.abs(currentScrollY - lastScrollY) > SCROLL_THRESHOLD) {
                boolean scrolledUp = currentScrollY < lastScrollY;
                boolean scrolledDown = currentScrollY > lastScrollY;
                boolean wasAtBottom = isAtBottomPosition(lastScrollY);
                boolean nowAtBottom = isAtBottom();
                
                // Detect user manual scrolling
                if (scrolledUp && !wasAtBottom) {
                    // User scrolled up from a non-bottom position - definitely manual
                    userScrolledManually = true;
                    lastUserScrollTime = currentTime;
                    mainHandler.removeCallbacks(autoScrollResumeRunnable);
                } else if (scrolledDown && !nowAtBottom) {
                    // User scrolled down but not to bottom - likely manual
                    userScrolledManually = true;
                    lastUserScrollTime = currentTime;
                    mainHandler.removeCallbacks(autoScrollResumeRunnable);
                } else if (nowAtBottom && userScrolledManually) {
                    // User scrolled back to bottom - schedule auto-scroll resume
                    mainHandler.removeCallbacks(autoScrollResumeRunnable);
                    mainHandler.postDelayed(autoScrollResumeRunnable, USER_SCROLL_TIMEOUT_MS);
                }
                
                lastScrollY = currentScrollY;
                lastScrollTime = currentTime;
            }
        });
    }
    
    /**
     * Checks if a given scroll position is at the bottom
     */
    private boolean isAtBottomPosition(int scrollY) {
        if (scrollView.getChildCount() == 0) {
            return true; // No content, consider at bottom
        }
        int scrollViewHeight = scrollView.getHeight();
        int contentHeight = scrollView.getChildAt(0).getHeight();
        return scrollY + scrollViewHeight >= contentHeight - 10;
    }
    
    /**
     * Checks if the ScrollView is currently at the bottom
     */
    private boolean isAtBottom() {
        if (scrollView.getChildCount() == 0) {
            return true; // No content, consider at bottom
        }
        int scrollY = scrollView.getScrollY();
        int scrollViewHeight = scrollView.getHeight();
        int contentHeight = scrollView.getChildAt(0).getHeight();
        
        // Consider "at bottom" if within a small threshold
        return scrollY + scrollViewHeight >= contentHeight - 10;
    }
    
    /**
     * Returns the current transcript statistics
     */
    public TranscriptStats getStats() {
        String fullText = fullTranscript.toString();
        int wordCount = fullText.trim().isEmpty() ? 0 : fullText.trim().split("\\s+").length;
        int characterCount = fullText.length();
        
        return new TranscriptStats(wordCount, characterCount, !currentPartialText.isEmpty());
    }
    
    /**
     * Checks if memory optimization is needed and performs it
     */
    private void checkMemoryOptimization() {
        long currentTime = System.currentTimeMillis();
        
        // Check if we need optimization based on size or time
        boolean sizeThresholdReached = fullTranscript.length() > CLEANUP_THRESHOLD_CHARS;
        boolean timeThresholdReached = currentTime - lastMemoryOptimization > MEMORY_OPTIMIZATION_INTERVAL_MS;
        
        if (sizeThresholdReached || (timeThresholdReached && fullTranscript.length() > 10000)) {
            optimizeMemoryUsage();
            lastMemoryOptimization = currentTime;
        }
    }
    
    /**
     * Optimizes memory usage by compressing transcript content
     */
    private void optimizeMemoryUsage() {
        if (fullTranscript.length() <= MAX_TRANSCRIPT_CHARS) {
            return;
        }
        
        String originalText = fullTranscript.toString();
        
        // Compress whitespace
        String compressed = originalText
                .replaceAll("\\s+", " ")
                .replaceAll("\\n\\s*\\n", "\n")
                .trim();
        
        // If still too long, keep only the most recent content
        if (compressed.length() > MAX_TRANSCRIPT_CHARS) {
            int excessChars = compressed.length() - MAX_TRANSCRIPT_CHARS;
            
            // Find a good break point (sentence or word boundary)
            int breakPoint = findOptimalBreakPoint(compressed, excessChars);
            compressed = compressed.substring(breakPoint);
        }
        
        // Update the transcript with optimized content
        fullTranscript.setLength(0);
        fullTranscript.append(compressed);
        
        android.util.Log.d("TranscriptManager", 
            String.format("Memory optimization: reduced from %d to %d characters", 
                originalText.length(), compressed.length()));
    }
    
    /**
     * Finds an optimal break point for transcript truncation
     */
    private int findOptimalBreakPoint(String text, int targetPosition) {
        // Look for sentence endings near the target position
        int searchStart = Math.max(0, targetPosition - 200);
        int searchEnd = Math.min(text.length(), targetPosition + 200);
        
        // First, try to find sentence endings
        for (int i = targetPosition; i < searchEnd; i++) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (Character.isWhitespace(next)) {
                    return i + 2; // Position after punctuation and space
                }
            }
        }
        
        // If no sentence ending, look for word boundaries
        for (int i = targetPosition; i < searchEnd; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        
        // Fallback to exact position
        return targetPosition;
    }
    
    /**
     * Gets memory usage information for the transcript
     */
    public MemoryInfo getMemoryInfo() {
        int transcriptBytes = fullTranscript.toString().getBytes().length;
        int partialTextBytes = currentPartialText.getBytes().length;
        int totalBytes = transcriptBytes + partialTextBytes;
        
        return new MemoryInfo(
            fullTranscript.length(),
            currentPartialText.length(),
            totalBytes,
            lastMemoryOptimization > 0
        );
    }
    
    /**
     * Memory information data class
     */
    public static class MemoryInfo {
        public final int transcriptCharacters;
        public final int partialTextCharacters;
        public final int totalBytes;
        public final boolean hasBeenOptimized;
        
        public MemoryInfo(int transcriptCharacters, int partialTextCharacters, 
                         int totalBytes, boolean hasBeenOptimized) {
            this.transcriptCharacters = transcriptCharacters;
            this.partialTextCharacters = partialTextCharacters;
            this.totalBytes = totalBytes;
            this.hasBeenOptimized = hasBeenOptimized;
        }
    }
    
    /**
     * Simple data class for transcript statistics
     */
    public static class TranscriptStats {
        public final int wordCount;
        public final int characterCount;
        public final boolean hasPartialText;
        
        public TranscriptStats(int wordCount, int characterCount, boolean hasPartialText) {
            this.wordCount = wordCount;
            this.characterCount = characterCount;
            this.hasPartialText = hasPartialText;
        }
    }
}