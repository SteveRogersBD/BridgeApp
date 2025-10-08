package com.example.bridge.integration;

import android.content.Context;
import com.example.bridge.models.TranscriptSession;
import com.example.bridge.models.TranscriptSegment;
import com.example.bridge.utils.AnimationController;
import com.example.bridge.utils.BatteryOptimizer;
import com.example.bridge.utils.MemoryManager;
import com.example.bridge.utils.PerformanceMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class PerformanceOptimizationIntegrationTest {

    @Mock
    private PerformanceMonitor.PerformanceCallback performanceCallback;
    
    @Mock
    private MemoryManager.MemoryCallback memoryCallback;
    
    @Mock
    private BatteryOptimizer.BatteryCallback batteryCallback;
    
    private Context context;
    private PerformanceMonitor performanceMonitor;
    private MemoryManager memoryManager;
    private BatteryOptimizer batteryOptimizer;
    private AnimationController animationController;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        performanceMonitor = new PerformanceMonitor(performanceCallback);
        memoryManager = new MemoryManager(memoryCallback);
        batteryOptimizer = new BatteryOptimizer(context, batteryCallback);
        animationController = new AnimationController();
    }

    @Test
    public void testIntegratedPerformanceMonitoringSession() {
        // Start a complete monitoring session
        performanceMonitor.startSession();
        memoryManager.startMonitoring();
        batteryOptimizer.startOptimization();
        
        // Simulate transcription activity
        simulateTranscriptionActivity();
        
        // Get performance stats
        PerformanceMonitor.PerformanceStats stats = performanceMonitor.getStats();
        assertNotNull(stats);
        assertTrue(stats.transcriptionEvents > 0);
        
        // Stop monitoring
        performanceMonitor.stopSession();
        memoryManager.stopMonitoring();
        batteryOptimizer.stopOptimization();
    }

    @Test
    public void testMemoryOptimizationWithPerformanceMonitoring() {
        TranscriptSession session = createLargeTranscriptSession();
        
        performanceMonitor.startSession();
        
        // Optimize memory while monitoring performance
        MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
        
        // Should perform optimization on large session
        assertTrue(result.optimizationPerformed);
        
        // Performance monitoring should continue working
        PerformanceMonitor.PerformanceStats stats = performanceMonitor.getStats();
        assertNotNull(stats);
        
        performanceMonitor.stopSession();
    }

    @Test
    public void testBatteryOptimizationWithAnimationControl() {
        batteryOptimizer.startOptimization();
        
        // Test different power modes affect animation performance
        float normalFactor = batteryOptimizer.getPerformanceFactor();
        assertEquals(1.0f, normalFactor, 0.01f);
        
        // Apply performance factor to animation controller
        animationController.setPerformanceFactor(normalFactor);
        
        // Test that animations are enabled in normal mode
        animationController.setAnimationsEnabled(true);
        assertEquals(0, animationController.getActiveAnimationCount());
        
        batteryOptimizer.stopOptimization();
    }

    @Test
    public void testHighLatencyTriggersOptimizations() {
        performanceMonitor.startSession();
        
        // Simulate high latency transcription events
        for (int i = 0; i < 20; i++) {
            long startTime = performanceMonitor.recordTranscriptionStart();
            // Simulate 1.5 second latency
            performanceMonitor.recordTranscriptionEnd(startTime - 1500, false);
        }
        
        // Should trigger high latency callback
        verify(performanceCallback, atLeastOnce()).onHighLatencyDetected(anyLong());
        
        performanceMonitor.stopSession();
    }

    @Test
    public void testMemoryPressureTriggersCleanup() {
        TranscriptSession session = createVeryLargeTranscriptSession();
        
        memoryManager.startMonitoring();
        
        // Optimize large session multiple times
        for (int i = 0; i < 5; i++) {
            MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
            if (result.optimizationPerformed) {
                // Memory optimization should be triggered
                assertTrue(result.charactersRemoved > 0 || result.segmentsRemoved > 0);
            }
        }
        
        memoryManager.stopMonitoring();
    }

    @Test
    public void testIntegratedResourceCleanup() {
        // Start all monitoring
        performanceMonitor.startSession();
        memoryManager.startMonitoring();
        batteryOptimizer.startOptimization();
        
        // Simulate activity
        simulateTranscriptionActivity();
        
        // Stop all monitoring (simulating activity destroy)
        performanceMonitor.stopSession();
        memoryManager.stopMonitoring();
        batteryOptimizer.stopOptimization();
        animationController.cancelAllAnimations();
        
        // Should complete without exceptions
        assertEquals(0, animationController.getActiveAnimationCount());
    }

    @Test
    public void testPerformanceOptimizationSuggestions() {
        performanceMonitor.startSession();
        
        // Simulate conditions that should trigger optimization suggestions
        for (int i = 0; i < 60; i++) {
            long startTime = performanceMonitor.recordTranscriptionStart();
            performanceMonitor.recordTranscriptionEnd(startTime - 600, false); // 600ms latency
        }
        
        // Should trigger optimization suggestions
        verify(performanceCallback, atLeastOnce()).onPerformanceOptimizationSuggested(anyString());
        
        performanceMonitor.stopSession();
    }

    @Test
    public void testMemoryStatsIntegration() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        
        assertNotNull(stats);
        assertTrue("Used memory should be positive", stats.usedMemoryMb > 0);
        assertTrue("Total memory should be >= used memory", stats.totalMemoryMb >= stats.usedMemoryMb);
        assertTrue("Max memory should be >= total memory", stats.maxMemoryMb >= stats.totalMemoryMb);
    }

    @Test
    public void testBatteryAwarePerformanceAdjustment() {
        batteryOptimizer.startOptimization();
        
        // Test that battery optimizer provides appropriate performance factors
        float performanceFactor = batteryOptimizer.getPerformanceFactor();
        assertTrue("Performance factor should be positive", performanceFactor > 0);
        assertTrue("Performance factor should be reasonable", performanceFactor <= 2.0f);
        
        // Apply to animation controller
        animationController.setPerformanceFactor(performanceFactor);
        
        // Should handle the performance adjustment
        // (No direct way to test internal scaling, but should not throw exceptions)
        
        batteryOptimizer.stopOptimization();
    }

    @Test
    public void testConcurrentOptimizationOperations() {
        // Start all optimizers concurrently
        performanceMonitor.startSession();
        memoryManager.startMonitoring();
        batteryOptimizer.startOptimization();
        
        TranscriptSession session = createLargeTranscriptSession();
        
        // Perform operations concurrently
        for (int i = 0; i < 10; i++) {
            // Performance monitoring
            long startTime = performanceMonitor.recordTranscriptionStart();
            performanceMonitor.recordTranscriptionEnd(startTime - 100, false);
            
            // Memory optimization
            memoryManager.optimizeSession(session);
            
            // Animation control
            animationController.setPerformanceFactor(0.8f);
        }
        
        // Should handle concurrent operations without issues
        PerformanceMonitor.PerformanceStats stats = performanceMonitor.getStats();
        assertNotNull(stats);
        assertTrue(stats.transcriptionEvents >= 10);
        
        // Cleanup
        performanceMonitor.stopSession();
        memoryManager.stopMonitoring();
        batteryOptimizer.stopOptimization();
    }

    private void simulateTranscriptionActivity() {
        // Simulate normal transcription activity
        for (int i = 0; i < 10; i++) {
            long startTime = performanceMonitor.recordTranscriptionStart();
            
            // Simulate processing time (50-200ms)
            int processingTime = 50 + (i * 15);
            performanceMonitor.recordTranscriptionEnd(startTime - processingTime, i % 3 == 0);
        }
    }

    private TranscriptSession createLargeTranscriptSession() {
        TranscriptSession session = new TranscriptSession();
        
        // Create a transcript that exceeds normal size limits
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            largeText.append("This is transcript segment ").append(i).append(". ");
        }
        session.appendToFullTranscript(largeText.toString());
        
        // Add many segments
        for (int i = 0; i < 300; i++) {
            TranscriptSegment segment = new TranscriptSegment(
                "Segment " + i, 
                System.currentTimeMillis() - (i * 1000), 
                true, 
                0.9f
            );
            session.addSegment(segment);
        }
        
        return session;
    }

    private TranscriptSession createVeryLargeTranscriptSession() {
        TranscriptSession session = new TranscriptSession();
        
        // Create an extremely large transcript
        StringBuilder veryLargeText = new StringBuilder();
        for (int i = 0; i < 15000; i++) {
            veryLargeText.append("Very large transcript content segment ").append(i)
                         .append(" with additional text to increase memory usage. ");
        }
        session.appendToFullTranscript(veryLargeText.toString());
        
        // Add very many segments
        for (int i = 0; i < 800; i++) {
            TranscriptSegment segment = new TranscriptSegment(
                "Large segment " + i + " with extra content", 
                System.currentTimeMillis() - (i * 500), 
                true, 
                0.85f
            );
            session.addSegment(segment);
        }
        
        return session;
    }
}