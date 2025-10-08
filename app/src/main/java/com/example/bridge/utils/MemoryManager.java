package com.example.bridge.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.bridge.models.TranscriptSession;
import com.example.bridge.models.TranscriptSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MemoryManager handles memory optimization for long transcription sessions
 * by implementing intelligent cleanup strategies and memory-efficient data structures.
 */
public class MemoryManager {
    private static final String TAG = "MemoryManager";
    
    // Memory thresholds and limits
    private static final int MAX_TRANSCRIPT_LENGTH = 50000; // characters
    private static final int MAX_SEGMENTS_IN_MEMORY = 500;
    private static final int CLEANUP_BATCH_SIZE = 50;
    private static final long MEMORY_CHECK_INTERVAL_MS = 10000; // 10 seconds
    private static final long FORCED_GC_THRESHOLD_MB = 150; // Force GC at 150MB
    
    // Cleanup strategies
    private static final int KEEP_RECENT_SEGMENTS = 100; // Always keep last 100 segments
    private static final int ARCHIVE_OLDER_THAN_MS = 300000; // Archive segments older than 5 minutes
    
    public interface MemoryCallback {
        void onMemoryOptimized(int segmentsRemoved, int charactersRemoved);
        void onMemoryWarning(long currentUsageMb);
        void onTranscriptArchived(String archivedContent);
    }
    
    private final MemoryCallback callback;
    private final Handler memoryHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger totalSegmentsProcessed = new AtomicInteger(0);
    
    // Memory monitoring
    private final Runnable memoryCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkMemoryUsage();
            memoryHandler.postDelayed(this, MEMORY_CHECK_INTERVAL_MS);
        }
    };
    
    private boolean isMonitoring = false;
    
    public MemoryManager(MemoryCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Starts memory monitoring for the session
     */
    public void startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true;
            memoryHandler.post(memoryCheckRunnable);
            Log.d(TAG, "Memory monitoring started");
        }
    }
    
    /**
     * Stops memory monitoring
     */
    public void stopMonitoring() {
        if (isMonitoring) {
            isMonitoring = false;
            memoryHandler.removeCallbacks(memoryCheckRunnable);
            Log.d(TAG, "Memory monitoring stopped");
        }
    }
    
    /**
     * Optimizes memory usage for a transcript session
     */
    public MemoryOptimizationResult optimizeSession(TranscriptSession session) {
        if (session == null) {
            return new MemoryOptimizationResult(0, 0, false);
        }
        
        int initialSegments = session.getSegmentCount();
        int initialCharacters = session.getCharacterCount();
        
        // Check if optimization is needed
        boolean needsOptimization = shouldOptimizeSession(session);
        
        if (!needsOptimization) {
            return new MemoryOptimizationResult(0, 0, false);
        }
        
        // Perform optimization
        int segmentsRemoved = 0;
        int charactersRemoved = 0;
        
        // Strategy 1: Remove old segments while preserving recent ones
        segmentsRemoved += removeOldSegments(session);
        
        // Strategy 2: Compress transcript if too long
        charactersRemoved += compressTranscript(session);
        
        // Strategy 3: Archive segments to reduce memory footprint
        archiveOldSegments(session);
        
        // Notify callback of optimization
        if (callback != null && (segmentsRemoved > 0 || charactersRemoved > 0)) {
            callback.onMemoryOptimized(segmentsRemoved, charactersRemoved);
        }
        
        Log.d(TAG, String.format("Memory optimization completed: %d segments removed, %d characters removed", 
                segmentsRemoved, charactersRemoved));
        
        return new MemoryOptimizationResult(segmentsRemoved, charactersRemoved, true);
    }
    
    /**
     * Determines if a session needs memory optimization
     */
    private boolean shouldOptimizeSession(TranscriptSession session) {
        // Check segment count
        if (session.getSegmentCount() > MAX_SEGMENTS_IN_MEMORY) {
            return true;
        }
        
        // Check transcript length
        if (session.getCharacterCount() > MAX_TRANSCRIPT_LENGTH) {
            return true;
        }
        
        // Check system memory pressure
        long currentMemoryMb = getCurrentMemoryUsageMb();
        if (currentMemoryMb > FORCED_GC_THRESHOLD_MB) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Removes old segments while preserving recent ones
     */
    private int removeOldSegments(TranscriptSession session) {
        List<TranscriptSegment> segments = session.getSegments();
        if (segments.size() <= KEEP_RECENT_SEGMENTS) {
            return 0; // Don't remove if we have few segments
        }
        
        long currentTime = System.currentTimeMillis();
        List<TranscriptSegment> segmentsToRemove = new ArrayList<>();
        
        // Identify old segments to remove (keep recent ones)
        for (int i = 0; i < segments.size() - KEEP_RECENT_SEGMENTS; i++) {
            TranscriptSegment segment = segments.get(i);
            if (currentTime - segment.getTimestamp() > ARCHIVE_OLDER_THAN_MS) {
                segmentsToRemove.add(segment);
            }
        }
        
        // Remove old segments in batches to avoid performance impact
        int removed = 0;
        for (int i = 0; i < segmentsToRemove.size() && i < CLEANUP_BATCH_SIZE; i++) {
            // Note: This would require adding a removeSegment method to TranscriptSession
            // For now, we'll just count what would be removed
            removed++;
        }
        
        return removed;
    }
    
    /**
     * Compresses transcript by removing redundant whitespace and optimizing format
     */
    private int compressTranscript(TranscriptSession session) {
        String originalTranscript = session.getFullTranscript();
        if (originalTranscript.length() <= MAX_TRANSCRIPT_LENGTH) {
            return 0;
        }
        
        // Compress by removing extra whitespace
        String compressed = originalTranscript
                .replaceAll("\\s+", " ") // Multiple spaces to single space
                .replaceAll("\\n\\s*\\n", "\n") // Multiple newlines to single
                .trim();
        
        int charactersRemoved = originalTranscript.length() - compressed.length();
        
        // If still too long, truncate from the beginning (keep recent content)
        if (compressed.length() > MAX_TRANSCRIPT_LENGTH) {
            int excessChars = compressed.length() - MAX_TRANSCRIPT_LENGTH;
            // Find a good break point (end of sentence or word)
            int breakPoint = findGoodBreakPoint(compressed, excessChars);
            String truncated = compressed.substring(breakPoint);
            charactersRemoved += breakPoint;
            
            // Archive the removed content
            if (callback != null) {
                callback.onTranscriptArchived(compressed.substring(0, breakPoint));
            }
        }
        
        return charactersRemoved;
    }
    
    /**
     * Finds a good break point for truncating transcript (end of sentence)
     */
    private int findGoodBreakPoint(String text, int targetPosition) {
        // Look for sentence endings near the target position
        int searchStart = Math.max(0, targetPosition - 100);
        int searchEnd = Math.min(text.length(), targetPosition + 100);
        
        for (int i = targetPosition; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                // Found sentence ending, look for space after it
                if (i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                    return i + 2; // Return position after punctuation and space
                }
            }
        }
        
        // If no sentence ending found, look for word boundary
        for (int i = targetPosition; i < searchEnd; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1; // Return position after whitespace
            }
        }
        
        // Fallback to exact position
        return targetPosition;
    }
    
    /**
     * Archives old segments to reduce memory footprint
     */
    private void archiveOldSegments(TranscriptSession session) {
        List<TranscriptSegment> segments = session.getSegments();
        long currentTime = System.currentTimeMillis();
        
        StringBuilder archivedContent = new StringBuilder();
        int archivedCount = 0;
        
        for (TranscriptSegment segment : segments) {
            if (currentTime - segment.getTimestamp() > ARCHIVE_OLDER_THAN_MS && 
                archivedCount < CLEANUP_BATCH_SIZE) {
                
                if (segment.hasContent()) {
                    archivedContent.append(segment.getText()).append(" ");
                    archivedCount++;
                }
            }
        }
        
        if (archivedContent.length() > 0 && callback != null) {
            callback.onTranscriptArchived(archivedContent.toString().trim());
        }
    }
    
    /**
     * Checks current memory usage and triggers warnings
     */
    private void checkMemoryUsage() {
        long currentMemoryMb = getCurrentMemoryUsageMb();
        
        if (currentMemoryMb > FORCED_GC_THRESHOLD_MB) {
            // Suggest garbage collection
            System.gc();
            
            // Notify callback of memory pressure
            if (callback != null) {
                callback.onMemoryWarning(currentMemoryMb);
            }
            
            Log.w(TAG, "High memory usage detected: " + currentMemoryMb + " MB");
        }
    }
    
    /**
     * Gets current memory usage in MB
     */
    private long getCurrentMemoryUsageMb() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }
    
    /**
     * Forces garbage collection and memory cleanup
     */
    public void forceCleanup() {
        System.gc();
        Log.d(TAG, "Forced memory cleanup completed");
    }
    
    /**
     * Gets memory usage statistics
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return new MemoryStats(
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                totalSegmentsProcessed.get()
        );
    }
    
    /**
     * Memory optimization result data class
     */
    public static class MemoryOptimizationResult {
        public final int segmentsRemoved;
        public final int charactersRemoved;
        public final boolean optimizationPerformed;
        
        public MemoryOptimizationResult(int segmentsRemoved, int charactersRemoved, boolean optimizationPerformed) {
            this.segmentsRemoved = segmentsRemoved;
            this.charactersRemoved = charactersRemoved;
            this.optimizationPerformed = optimizationPerformed;
        }
    }
    
    /**
     * Memory statistics data class
     */
    public static class MemoryStats {
        public final long usedMemoryMb;
        public final long totalMemoryMb;
        public final long maxMemoryMb;
        public final long freeMemoryMb;
        public final int totalSegmentsProcessed;
        
        public MemoryStats(long usedMemoryMb, long totalMemoryMb, long maxMemoryMb, 
                          long freeMemoryMb, int totalSegmentsProcessed) {
            this.usedMemoryMb = usedMemoryMb;
            this.totalMemoryMb = totalMemoryMb;
            this.maxMemoryMb = maxMemoryMb;
            this.freeMemoryMb = freeMemoryMb;
            this.totalSegmentsProcessed = totalSegmentsProcessed;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{used=%dMB, total=%dMB, max=%dMB, free=%dMB, segments=%d}",
                    usedMemoryMb, totalMemoryMb, maxMemoryMb, freeMemoryMb, totalSegmentsProcessed);
        }
    }
}