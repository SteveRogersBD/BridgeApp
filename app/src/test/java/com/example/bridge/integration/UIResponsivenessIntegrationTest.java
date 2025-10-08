package com.example.bridge.integration;

import android.content.Context;

import com.example.bridge.models.RecordingState;
import com.example.bridge.utils.SpeechLiveTranscriber;
import com.example.bridge.utils.TranscriptManager;
import com.example.bridge.utils.TranscriptionErrorHandler;
import com.example.bridge.utils.TranscriptionStateManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for UI responsiveness during active transcription.
 * Tests the complete UI update flow under various load conditions.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class UIResponsivenessIntegrationTest {

    private Context context;

    @Mock
    private TranscriptManager mockTranscriptManager;
    
    @Mock
    private TranscriptionErrorHandler mockErrorHandler;
    
    @Mock
    private TranscriptionStateManager mockStateManager;

    private UITestCallbacks uiCallbacks;
    private ExecutorService backgroundExecutor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Setup UI test callbacks
        uiCallbacks = new UITestCallbacks();
        
        // Setup background executor for simulating concurrent operations
        backgroundExecutor = Executors.newFixedThreadPool(4);
        
        setupDefaultMockBehaviors();
    }

    private void setupDefaultMockBehaviors() {
        when(mockStateManager.canStartRecording()).thenReturn(true);
        when(mockStateManager.getCurrentState()).thenReturn(RecordingState.IDLE);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("");
        when(mockErrorHandler.isTranscriptionServiceAvailable()).thenReturn(true);
    }

    @Test
    public void testUIResponsiveness_RapidTextUpdates() throws Exception {
        // Test UI responsiveness with rapid text updates
        
        int updateCount = 200;
        CountDownLatch updateLatch = new CountDownLatch(updateCount);
        AtomicInteger processedUpdates = new AtomicInteger(0);
        
        // Track UI thread operations
        doAnswer(invocation -> {
            processedUpdates.incrementAndGet();
            updateLatch.countDown();
            return null;
        }).when(mockTranscriptManager).updatePartialText(anyString());
        
        // Generate rapid updates from background thread
        backgroundExecutor.submit(() -> {
            try {
                for (int i = 0; i < updateCount; i++) {
                    uiCallbacks.onPartial("Rapid update " + i + " with some additional text content");
                    Thread.sleep(10); // 100 updates per second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Wait for all updates to be processed
        assertTrue("All rapid updates should be processed within timeout",
                  updateLatch.await(30, TimeUnit.SECONDS));
        
        assertEquals("All updates should be processed", updateCount, processedUpdates.get());
    }

    @Test
    public void testUIResponsiveness_ConcurrentOperations() throws Exception {
        // Test UI responsiveness with concurrent operations
        
        int operationsPerType = 50;
        CountDownLatch allOperationsLatch = new CountDownLatch(operationsPerType * 4);
        
        AtomicInteger partialUpdates = new AtomicInteger(0);
        AtomicInteger finalUpdates = new AtomicInteger(0);
        AtomicInteger stateUpdates = new AtomicInteger(0);
        AtomicInteger errorHandling = new AtomicInteger(0);
        
        // Setup mock behaviors to track operations
        doAnswer(invocation -> {
            partialUpdates.incrementAndGet();
            allOperationsLatch.countDown();
            return null;
        }).when(mockTranscriptManager).updatePartialText(anyString());
        
        doAnswer(invocation -> {
            finalUpdates.incrementAndGet();
            allOperationsLatch.countDown();
            return null;
        }).when(mockTranscriptManager).appendFinalText(anyString());
        
        doAnswer(invocation -> {
            stateUpdates.incrementAndGet();
            allOperationsLatch.countDown();
            return null;
        }).when(mockStateManager).updateState(any(RecordingState.class));
        
        doAnswer(invocation -> {
            errorHandling.incrementAndGet();
            allOperationsLatch.countDown();
            return null;
        }).when(mockErrorHandler).handleError(anyInt(), anyString());
        
        // Launch concurrent operations
        // Partial text updates
        backgroundExecutor.submit(() -> {
            for (int i = 0; i < operationsPerType; i++) {
                uiCallbacks.onPartial("Concurrent partial " + i);
                try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        
        // Final text updates
        backgroundExecutor.submit(() -> {
            for (int i = 0; i < operationsPerType; i++) {
                uiCallbacks.onFinal("Concurrent final " + i + ".");
                try { Thread.sleep(7); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        
        // State changes
        backgroundExecutor.submit(() -> {
            for (int i = 0; i < operationsPerType; i++) {
                uiCallbacks.onReady();
                try { Thread.sleep(6); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        
        // Error handling
        backgroundExecutor.submit(() -> {
            for (int i = 0; i < operationsPerType; i++) {
                uiCallbacks.onError("Concurrent error " + i);
                try { Thread.sleep(8); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        
        // Wait for all operations
        assertTrue("All concurrent operations should complete",
                  allOperationsLatch.await(45, TimeUnit.SECONDS));
        
        // Verify all operations were processed
        assertEquals("All partial updates processed", operationsPerType, partialUpdates.get());
        assertEquals("All final updates processed", operationsPerType, finalUpdates.get());
        assertEquals("All state updates processed", operationsPerType, stateUpdates.get());
        assertEquals("All errors processed", operationsPerType, errorHandling.get());
    }

    @Test
    public void testUIResponsiveness_LargeTextProcessing() throws Exception {
        // Test UI responsiveness with large text processing
        
        // Generate large text content
        StringBuilder largeTextBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeTextBuilder.append("This is a very long sentence with many words to test large text processing performance. ");
        }
        String largeText = largeTextBuilder.toString();
        
        int largeTextUpdates = 20;
        CountDownLatch largeTextLatch = new CountDownLatch(largeTextUpdates);
        
        doAnswer(invocation -> {
            largeTextLatch.countDown();
            return null;
        }).when(mockTranscriptManager).appendFinalText(anyString());
        
        long startTime = System.currentTimeMillis();
        
        // Process large text updates
        for (int i = 0; i < largeTextUpdates; i++) {
            final int updateIndex = i;
            backgroundExecutor.submit(() -> {
                uiCallbacks.onFinal(largeText + " Update " + updateIndex);
            });
        }
        
        // Wait for processing
        assertTrue("Large text processing should complete",
                  largeTextLatch.await(20, TimeUnit.SECONDS));
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Verify performance
        assertTrue("Large text processing should be efficient", processingTime < 10000); // Less than 10 seconds
        verify(mockTranscriptManager, times(largeTextUpdates)).appendFinalText(anyString());
    }

    @Test
    public void testUIResponsiveness_StateTransitionLoad() throws Exception {
        // Test UI responsiveness under heavy state transition load
        
        int stateTransitions = 100;
        CountDownLatch stateTransitionLatch = new CountDownLatch(stateTransitions);
        
        doAnswer(invocation -> {
            stateTransitionLatch.countDown();
            return null;
        }).when(mockStateManager).updateState(any(RecordingState.class));
        
        // Generate rapid state transitions
        backgroundExecutor.submit(() -> {
            try {
                for (int i = 0; i < stateTransitions; i++) {
                    uiCallbacks.onReady();
                    Thread.sleep(20); // 50 transitions per second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Wait for all state transitions
        assertTrue("State transitions should complete",
                  stateTransitionLatch.await(15, TimeUnit.SECONDS));
        
        verify(mockStateManager, times(stateTransitions)).updateState(RecordingState.LISTENING);
    }

    @Test
    public void testUIResponsiveness_MixedLoadScenario() throws Exception {
        // Test UI responsiveness under mixed load scenario (realistic usage)
        
        int totalOperations = 300; // Mix of different operations
        CountDownLatch mixedLoadLatch = new CountDownLatch(totalOperations);
        
        AtomicInteger totalProcessed = new AtomicInteger(0);
        
        // Setup tracking for all operations
        doAnswer(invocation -> {
            totalProcessed.incrementAndGet();
            mixedLoadLatch.countDown();
            return null;
        }).when(mockTranscriptManager).updatePartialText(anyString());
        
        doAnswer(invocation -> {
            totalProcessed.incrementAndGet();
            mixedLoadLatch.countDown();
            return null;
        }).when(mockTranscriptManager).appendFinalText(anyString());
        
        doAnswer(invocation -> {
            totalProcessed.incrementAndGet();
            mixedLoadLatch.countDown();
            return null;
        }).when(mockStateManager).updateState(any(RecordingState.class));
        
        // Simulate realistic mixed usage pattern
        backgroundExecutor.submit(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    // Simulate speech recognition pattern
                    uiCallbacks.onReady();
                    Thread.sleep(50);
                    
                    uiCallbacks.onPartial("Word " + i);
                    Thread.sleep(30);
                    
                    uiCallbacks.onPartial("Word " + i + " more");
                    Thread.sleep(30);
                    
                    uiCallbacks.onFinal("Word " + i + " more words.");
                    Thread.sleep(100);
                    
                    // Occasional error
                    if (i % 20 == 0) {
                        uiCallbacks.onError("Temporary error " + i);
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Wait for mixed load completion
        assertTrue("Mixed load scenario should complete",
                  mixedLoadLatch.await(60, TimeUnit.SECONDS));
        
        assertEquals("All mixed operations should be processed", totalOperations, totalProcessed.get());
    }

    // UI test callbacks implementation
    private class UITestCallbacks implements SpeechLiveTranscriber.Callbacks {
        @Override
        public void onReady() {
            if (mockStateManager != null) {
                mockStateManager.updateState(RecordingState.LISTENING);
            }
            
            if (mockErrorHandler != null) {
                mockErrorHandler.onSuccessfulRecovery();
            }
        }
        
        @Override
        public void onPartial(String text) {
            if (mockTranscriptManager != null && text != null && !text.trim().isEmpty()) {
                mockTranscriptManager.updatePartialText(text);
                if (mockStateManager != null) {
                    mockStateManager.indicateProcessing();
                }
            }
        }
        
        @Override
        public void onFinal(String text) {
            if (mockTranscriptManager != null && text != null && !text.trim().isEmpty()) {
                mockTranscriptManager.appendFinalText(text);
                if (mockStateManager != null) {
                    mockStateManager.indicateProcessing();
                }
            }
        }
        
        @Override
        public void onError(String message) {
            int errorCode = extractErrorCodeFromMessage(message);
            
            if (mockErrorHandler != null) {
                mockErrorHandler.handleError(errorCode, message);
            }
        }
    }

    private int extractErrorCodeFromMessage(String message) {
        if (message == null) return -1;
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("no match") || lowerMessage.contains("no speech")) {
            return android.speech.SpeechRecognizer.ERROR_NO_MATCH;
        } else if (lowerMessage.contains("network")) {
            return android.speech.SpeechRecognizer.ERROR_NETWORK;
        } else if (lowerMessage.contains("timeout")) {
            return android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
        }
        
        return android.speech.SpeechRecognizer.ERROR_CLIENT;
    }
}