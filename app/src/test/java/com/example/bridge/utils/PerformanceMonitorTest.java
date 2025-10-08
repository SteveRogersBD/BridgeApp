package com.example.bridge.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class PerformanceMonitorTest {

    @Mock
    private PerformanceMonitor.PerformanceCallback mockCallback;
    
    private PerformanceMonitor performanceMonitor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        performanceMonitor = new PerformanceMonitor(mockCallback);
    }

    @Test
    public void testStartSession() {
        performanceMonitor.startSession();
        
        // Verify initial state
        assertEquals(0.0, performanceMonitor.getAverageLatencyMs(), 0.01);
        assertEquals(0, performanceMonitor.getLatencyPercentile(95));
    }

    @Test
    public void testLatencyRecording() {
        performanceMonitor.startSession();
        
        // Record some latency measurements
        long startTime1 = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime1 - 100, false); // 100ms latency
        
        long startTime2 = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime2 - 200, false); // 200ms latency
        
        // Verify average latency calculation
        double avgLatency = performanceMonitor.getAverageLatencyMs();
        assertEquals(150.0, avgLatency, 5.0); // Allow some tolerance
    }

    @Test
    public void testHighLatencyDetection() {
        performanceMonitor.startSession();
        
        // Record high latency measurements
        for (int i = 0; i < 10; i++) {
            long startTime = performanceMonitor.recordTranscriptionStart();
            performanceMonitor.recordTranscriptionEnd(startTime - 1500, false); // 1.5 second latency
        }
        
        // Verify high latency callback is triggered
        verify(mockCallback, atLeastOnce()).onHighLatencyDetected(anyLong());
    }

    @Test
    public void testPerformanceStats() {
        performanceMonitor.startSession();
        
        // Record some measurements
        long startTime = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime - 300, false);
        
        PerformanceMonitor.PerformanceStats stats = performanceMonitor.getStats();
        
        assertNotNull(stats);
        assertTrue(stats.averageLatencyMs > 0);
        assertTrue(stats.sessionDurationMs > 0);
        assertEquals(1, stats.transcriptionEvents);
    }

    @Test
    public void testLatencyPercentileCalculation() {
        performanceMonitor.startSession();
        
        // Record measurements with known values
        for (int i = 1; i <= 100; i++) {
            long startTime = performanceMonitor.recordTranscriptionStart();
            performanceMonitor.recordTranscriptionEnd(startTime - (i * 10), false); // 10ms, 20ms, ..., 1000ms
        }
        
        // Test percentile calculations
        long p50 = performanceMonitor.getLatencyPercentile(50);
        long p95 = performanceMonitor.getLatencyPercentile(95);
        
        assertTrue("P50 should be around 500ms", Math.abs(p50 - 500) < 100);
        assertTrue("P95 should be around 950ms", Math.abs(p95 - 950) < 100);
    }

    @Test
    public void testMemoryUsageTracking() {
        performanceMonitor.startSession();
        
        // Get current memory usage
        long memoryUsage = performanceMonitor.getCurrentMemoryUsageMb();
        
        assertTrue("Memory usage should be positive", memoryUsage > 0);
        assertTrue("Memory usage should be reasonable", memoryUsage < 1000); // Less than 1GB
    }

    @Test
    public void testClearMetrics() {
        performanceMonitor.startSession();
        
        // Record some measurements
        long startTime = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime - 100, false);
        
        // Verify measurements exist
        assertTrue(performanceMonitor.getAverageLatencyMs() > 0);
        
        // Clear metrics
        performanceMonitor.clearMetrics();
        
        // Verify metrics are cleared
        assertEquals(0.0, performanceMonitor.getAverageLatencyMs(), 0.01);
    }

    @Test
    public void testStopSession() {
        performanceMonitor.startSession();
        
        // Record some measurements
        long startTime = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime - 100, false);
        
        // Stop session
        performanceMonitor.stopSession();
        
        // Verify metrics are cleared after stop
        assertEquals(0.0, performanceMonitor.getAverageLatencyMs(), 0.01);
    }

    @Test
    public void testPartialVsFinalTranscriptionTracking() {
        performanceMonitor.startSession();
        
        // Record partial transcription
        long startTime1 = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime1 - 50, true);
        
        // Record final transcription
        long startTime2 = performanceMonitor.recordTranscriptionStart();
        performanceMonitor.recordTranscriptionEnd(startTime2 - 100, false);
        
        PerformanceMonitor.PerformanceStats stats = performanceMonitor.getStats();
        
        // Should track both events
        assertEquals(2, stats.transcriptionEvents);
        // Only final transcription should count toward total time
        assertTrue(stats.totalTranscriptionTimeMs >= 100);
    }

    @Test
    public void testPerformanceOptimizationSuggestions() {
        performanceMonitor.startSession();
        
        // Simulate many high-latency events to trigger optimization suggestions
        for (int i = 0; i < 60; i++) {
            long startTime = performanceMonitor.recordTranscriptionStart();
            performanceMonitor.recordTranscriptionEnd(startTime - 800, false); // 800ms latency
        }
        
        // Verify optimization suggestion callback is triggered
        verify(mockCallback, atLeastOnce()).onPerformanceOptimizationSuggested(anyString());
    }
}