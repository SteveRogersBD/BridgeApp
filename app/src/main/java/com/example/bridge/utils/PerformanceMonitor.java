package com.example.bridge.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PerformanceMonitor tracks transcription latency, memory usage, and system performance
 * to optimize the user experience during long transcription sessions.
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    // Latency tracking
    private final Queue<Long> latencyMeasurements = new ArrayDeque<>();
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicLong measurementCount = new AtomicLong(0);
    private static final int MAX_LATENCY_SAMPLES = 100;
    
    // Memory tracking
    private final Queue<Long> memoryUsageSamples = new ArrayDeque<>();
    private static final int MAX_MEMORY_SAMPLES = 50;
    private long lastMemoryCheck = 0;
    private static final long MEMORY_CHECK_INTERVAL_MS = 5000; // Check every 5 seconds
    
    // Performance thresholds
    private static final long HIGH_LATENCY_THRESHOLD_MS = 1000; // 1 second
    private static final long MEMORY_WARNING_THRESHOLD_MB = 100; // 100 MB
    private static final long MEMORY_CRITICAL_THRESHOLD_MB = 200; // 200 MB
    
    // Callbacks for performance events
    public interface PerformanceCallback {
        void onHighLatencyDetected(long averageLatencyMs);
        void onMemoryWarning(long memoryUsageMb);
        void onMemoryCritical(long memoryUsageMb);
        void onPerformanceOptimizationSuggested(String suggestion);
    }
    
    private PerformanceCallback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Session tracking
    private long sessionStartTime = 0;
    private long totalTranscriptionTime = 0;
    private int transcriptionEvents = 0;
    
    public PerformanceMonitor(PerformanceCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Starts monitoring a new transcription session
     */
    public void startSession() {
        sessionStartTime = System.currentTimeMillis();
        totalTranscriptionTime = 0;
        transcriptionEvents = 0;
        clearMetrics();
        Log.d(TAG, "Performance monitoring started");
    }
    
    /**
     * Records the start of a transcription operation
     */
    public long recordTranscriptionStart() {
        return System.currentTimeMillis();
    }
    
    /**
     * Records the completion of a transcription operation and calculates latency
     */
    public void recordTranscriptionEnd(long startTime, boolean isPartial) {
        long endTime = System.currentTimeMillis();
        long latency = endTime - startTime;
        
        recordLatency(latency);
        transcriptionEvents++;
        
        if (!isPartial) {
            totalTranscriptionTime += latency;
        }
        
        // Check memory usage periodically
        if (endTime - lastMemoryCheck > MEMORY_CHECK_INTERVAL_MS) {
            checkMemoryUsage();
            lastMemoryCheck = endTime;
        }
        
        // Analyze performance and provide suggestions
        analyzePerformance();
    }
    
    /**
     * Records latency measurement and maintains rolling average
     */
    private void recordLatency(long latencyMs) {
        synchronized (latencyMeasurements) {
            latencyMeasurements.offer(latencyMs);
            totalLatencyMs.addAndGet(latencyMs);
            measurementCount.incrementAndGet();
            
            // Maintain maximum sample size
            if (latencyMeasurements.size() > MAX_LATENCY_SAMPLES) {
                Long removedLatency = latencyMeasurements.poll();
                if (removedLatency != null) {
                    totalLatencyMs.addAndGet(-removedLatency);
                    measurementCount.decrementAndGet();
                }
            }
        }
    }
    
    /**
     * Gets the current average latency
     */
    public double getAverageLatencyMs() {
        long count = measurementCount.get();
        if (count == 0) return 0.0;
        return (double) totalLatencyMs.get() / count;
    }
    
    /**
     * Gets the current latency percentile
     */
    public long getLatencyPercentile(int percentile) {
        synchronized (latencyMeasurements) {
            if (latencyMeasurements.isEmpty()) return 0;
            
            Long[] sortedLatencies = latencyMeasurements.toArray(new Long[0]);
            java.util.Arrays.sort(sortedLatencies);
            
            int index = (int) Math.ceil(percentile / 100.0 * sortedLatencies.length) - 1;
            index = Math.max(0, Math.min(index, sortedLatencies.length - 1));
            
            return sortedLatencies[index];
        }
    }
    
    /**
     * Checks current memory usage and triggers warnings if necessary
     */
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemoryMb = usedMemory / (1024 * 1024);
        
        synchronized (memoryUsageSamples) {
            memoryUsageSamples.offer(usedMemoryMb);
            if (memoryUsageSamples.size() > MAX_MEMORY_SAMPLES) {
                memoryUsageSamples.poll();
            }
        }
        
        // Check thresholds and notify callback
        if (usedMemoryMb > MEMORY_CRITICAL_THRESHOLD_MB) {
            notifyCallback(() -> callback.onMemoryCritical(usedMemoryMb));
        } else if (usedMemoryMb > MEMORY_WARNING_THRESHOLD_MB) {
            notifyCallback(() -> callback.onMemoryWarning(usedMemoryMb));
        }
        
        Log.d(TAG, "Memory usage: " + usedMemoryMb + " MB");
    }
    
    /**
     * Analyzes current performance metrics and provides optimization suggestions
     */
    private void analyzePerformance() {
        double avgLatency = getAverageLatencyMs();
        
        // Check for high latency
        if (avgLatency > HIGH_LATENCY_THRESHOLD_MS) {
            notifyCallback(() -> callback.onHighLatencyDetected((long) avgLatency));
        }
        
        // Provide optimization suggestions based on patterns
        if (transcriptionEvents > 50) { // After sufficient data
            if (avgLatency > 500) {
                notifyCallback(() -> callback.onPerformanceOptimizationSuggested(
                    "Consider reducing transcription frequency or using offline mode"));
            }
            
            long memoryTrend = getMemoryTrend();
            if (memoryTrend > 5) { // Memory increasing by >5MB per sample
                notifyCallback(() -> callback.onPerformanceOptimizationSuggested(
                    "Memory usage increasing - consider clearing old transcript data"));
            }
        }
    }
    
    /**
     * Calculates memory usage trend (MB per sample)
     */
    private long getMemoryTrend() {
        synchronized (memoryUsageSamples) {
            if (memoryUsageSamples.size() < 5) return 0;
            
            Long[] samples = memoryUsageSamples.toArray(new Long[0]);
            int n = samples.length;
            
            // Simple linear regression to detect trend
            long sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < n; i++) {
                sumX += i;
                sumY += samples[i];
                sumXY += i * samples[i];
                sumX2 += i * i;
            }
            
            // Calculate slope (trend)
            long denominator = n * sumX2 - sumX * sumX;
            if (denominator == 0) return 0;
            
            return (n * sumXY - sumX * sumY) / denominator;
        }
    }
    
    /**
     * Gets comprehensive performance statistics
     */
    public PerformanceStats getStats() {
        long sessionDuration = sessionStartTime > 0 ? 
            System.currentTimeMillis() - sessionStartTime : 0;
        
        return new PerformanceStats(
            getAverageLatencyMs(),
            getLatencyPercentile(95),
            getCurrentMemoryUsageMb(),
            getAverageMemoryUsageMb(),
            sessionDuration,
            transcriptionEvents,
            totalTranscriptionTime
        );
    }
    
    /**
     * Gets current memory usage in MB
     */
    public long getCurrentMemoryUsageMb() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }
    
    /**
     * Gets average memory usage in MB
     */
    private long getAverageMemoryUsageMb() {
        synchronized (memoryUsageSamples) {
            if (memoryUsageSamples.isEmpty()) return 0;
            return memoryUsageSamples.stream().mapToLong(Long::longValue).sum() / memoryUsageSamples.size();
        }
    }
    
    /**
     * Clears all performance metrics
     */
    public void clearMetrics() {
        synchronized (latencyMeasurements) {
            latencyMeasurements.clear();
            totalLatencyMs.set(0);
            measurementCount.set(0);
        }
        
        synchronized (memoryUsageSamples) {
            memoryUsageSamples.clear();
        }
        
        Log.d(TAG, "Performance metrics cleared");
    }
    
    /**
     * Stops performance monitoring
     */
    public void stopSession() {
        PerformanceStats finalStats = getStats();
        Log.d(TAG, "Performance monitoring stopped. Final stats: " + finalStats);
        clearMetrics();
    }
    
    /**
     * Safely notifies callback on main thread
     */
    private void notifyCallback(Runnable action) {
        if (callback != null) {
            mainHandler.post(action);
        }
    }
    
    /**
     * Performance statistics data class
     */
    public static class PerformanceStats {
        public final double averageLatencyMs;
        public final long p95LatencyMs;
        public final long currentMemoryMb;
        public final long averageMemoryMb;
        public final long sessionDurationMs;
        public final int transcriptionEvents;
        public final long totalTranscriptionTimeMs;
        
        public PerformanceStats(double averageLatencyMs, long p95LatencyMs, 
                              long currentMemoryMb, long averageMemoryMb,
                              long sessionDurationMs, int transcriptionEvents,
                              long totalTranscriptionTimeMs) {
            this.averageLatencyMs = averageLatencyMs;
            this.p95LatencyMs = p95LatencyMs;
            this.currentMemoryMb = currentMemoryMb;
            this.averageMemoryMb = averageMemoryMb;
            this.sessionDurationMs = sessionDurationMs;
            this.transcriptionEvents = transcriptionEvents;
            this.totalTranscriptionTimeMs = totalTranscriptionTimeMs;
        }
        
        @Override
        public String toString() {
            return String.format("PerformanceStats{avgLatency=%.1fms, p95Latency=%dms, " +
                    "currentMem=%dMB, avgMem=%dMB, sessionDuration=%dms, events=%d}",
                    averageLatencyMs, p95LatencyMs, currentMemoryMb, averageMemoryMb,
                    sessionDurationMs, transcriptionEvents);
        }
    }
}