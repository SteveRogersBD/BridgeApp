package com.example.bridge.utils;

import com.example.bridge.models.TranscriptSession;
import com.example.bridge.models.TranscriptSegment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class MemoryManagerTest {

    @Mock
    private MemoryManager.MemoryCallback mockCallback;
    
    private MemoryManager memoryManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        memoryManager = new MemoryManager(mockCallback);
    }

    @Test
    public void testStartStopMonitoring() {
        memoryManager.startMonitoring();
        // Monitoring should be active (no direct way to test, but no exceptions should occur)
        
        memoryManager.stopMonitoring();
        // Monitoring should be stopped
    }

    @Test
    public void testOptimizeSessionWithSmallSession() {
        TranscriptSession session = new TranscriptSession();
        session.appendToFullTranscript("Short transcript");
        
        MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
        
        // Small session should not need optimization
        assertFalse(result.optimizationPerformed);
        assertEquals(0, result.segmentsRemoved);
        assertEquals(0, result.charactersRemoved);
    }

    @Test
    public void testOptimizeSessionWithLargeTranscript() {
        TranscriptSession session = new TranscriptSession();
        
        // Create a large transcript that exceeds the threshold
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("This is a long transcript segment that will exceed memory limits. ");
        }
        session.appendToFullTranscript(largeText.toString());
        
        MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
        
        // Large session should be optimized
        assertTrue(result.optimizationPerformed);
        assertTrue(result.charactersRemoved > 0);
    }

    @Test
    public void testOptimizeSessionWithManySegments() {
        TranscriptSession session = new TranscriptSession();
        
        // Add many segments to exceed the segment limit
        for (int i = 0; i < 600; i++) {
            TranscriptSegment segment = new TranscriptSegment("Segment " + i, System.currentTimeMillis() - (i * 1000), true, 0.9f);
            session.addSegment(segment);
        }
        
        MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
        
        // Should optimize due to too many segments
        assertTrue(result.optimizationPerformed);
    }

    @Test
    public void testOptimizeSessionWithNullSession() {
        MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(null);
        
        assertFalse(result.optimizationPerformed);
        assertEquals(0, result.segmentsRemoved);
        assertEquals(0, result.charactersRemoved);
    }

    @Test
    public void testForceCleanup() {
        // Should not throw any exceptions
        memoryManager.forceCleanup();
    }

    @Test
    public void testGetMemoryStats() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        
        assertNotNull(stats);
        assertTrue("Used memory should be positive", stats.usedMemoryMb > 0);
        assertTrue("Total memory should be positive", stats.totalMemoryMb > 0);
        assertTrue("Max memory should be positive", stats.maxMemoryMb > 0);
        assertTrue("Free memory should be non-negative", stats.freeMemoryMb >= 0);
        assertTrue("Total memory should be >= used memory", stats.totalMemoryMb >= stats.usedMemoryMb);
    }

    @Test
    public void testMemoryWarningCallback() {
        memoryManager.startMonitoring();
        
        // Create a session that might trigger memory warnings
        TranscriptSession session = new TranscriptSession();
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            largeText.append("Large memory consuming text segment. ");
        }
        session.appendToFullTranscript(largeText.toString());
        
        memoryManager.optimizeSession(session);
        
        // Note: Memory warning callback depends on actual system memory usage
        // This test mainly ensures no exceptions are thrown
    }

    @Test
    public void testMemoryOptimizationResult() {
        MemoryManager.MemoryOptimizationResult result = 
            new MemoryManager.MemoryOptimizationResult(5, 1000, true);
        
        assertEquals(5, result.segmentsRemoved);
        assertEquals(1000, result.charactersRemoved);
        assertTrue(result.optimizationPerformed);
    }

    @Test
    public void testMemoryStatsToString() {
        MemoryManager.MemoryStats stats = new MemoryManager.MemoryStats(100, 200, 500, 100, 50);
        
        String statsString = stats.toString();
        
        assertNotNull(statsString);
        assertTrue(statsString.contains("100"));
        assertTrue(statsString.contains("200"));
        assertTrue(statsString.contains("500"));
        assertTrue(statsString.contains("50"));
    }

    @Test
    public void testOptimizationWithArchiving() {
        TranscriptSession session = new TranscriptSession();
        
        // Add old segments that should be archived
        long oldTime = System.currentTimeMillis() - 600000; // 10 minutes ago
        for (int i = 0; i < 100; i++) {
            TranscriptSegment segment = new TranscriptSegment("Old segment " + i, oldTime - (i * 1000), true, 0.9f);
            session.addSegment(segment);
        }
        
        // Add recent segments that should be kept
        long recentTime = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            TranscriptSegment segment = new TranscriptSegment("Recent segment " + i, recentTime - (i * 1000), true, 0.9f);
            session.addSegment(segment);
        }
        
        MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
        
        // Should perform optimization due to many segments
        assertTrue(result.optimizationPerformed);
        
        // Verify archive callback was called if old content was archived
        // Note: This depends on the internal implementation details
    }

    @Test
    public void testContinuousOptimization() {
        memoryManager.startMonitoring();
        
        TranscriptSession session = new TranscriptSession();
        
        // Simulate continuous addition of content
        for (int i = 0; i < 10; i++) {
            // Add content
            session.appendToFullTranscript("Continuous content addition " + i + ". ");
            
            // Optimize periodically
            MemoryManager.MemoryOptimizationResult result = memoryManager.optimizeSession(session);
            
            // Should handle continuous optimization without issues
            assertNotNull(result);
        }
        
        memoryManager.stopMonitoring();
    }
}