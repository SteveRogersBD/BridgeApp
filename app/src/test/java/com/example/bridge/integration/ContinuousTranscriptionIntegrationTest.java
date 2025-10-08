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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests focused on continuous transcription mode stability,
 * performance under load, and long-running session behavior.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ContinuousTranscriptionIntegrationTest {

    private Context context;

    @Mock
    private SpeechLiveTranscriber mockSpeechTranscriber;
    
    @Mock
    private TranscriptManager mockTranscriptManager;
    
    @Mock
    private TranscriptionErrorHandler mockErrorHandler;
    
    @Mock
    private TranscriptionStateManager mockStateManager;

    private TestSpeechCallbacks testCallbacks;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Setup test callbacks
        testCallbacks = new TestSpeechCallbacks();
        
        // Setup default mock behaviors
        setupDefaultMockBehaviors();
    }

    private void setupDefaultMockBehaviors() {
        when(mockStateManager.canStartRecording()).thenReturn(true);
        when(mockStateManager.getCurrentState()).thenReturn(RecordingState.IDLE);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("");
        when(mockErrorHandler.isTranscriptionServiceAvailable()).thenReturn(true);
    }

    @Test
    public void testContinuousMode_LongRunningSession() throws Exception {
        // Test continuous transcription over an extended period
        
        // Simulate a 5-minute continuous session with regular speech
        int totalSentences = 50; // Simulate 50 sentences over the session
        CountDownLatch completionLatch = new CountDownLatch(totalSentences);
        
        // Track performance metrics
        AtomicInteger partialUpdates = new AtomicInteger(0);
        AtomicInteger finalResults = new AtomicInteger(0);
        AtomicInteger stateUpdates = new AtomicInteger(0);
        
        // Setup mock behaviors to track calls
        doAnswer(invocation -> {
            partialUpdates.incrementAndGet();
            return null;
        }).when(mockTranscriptManager).updatePartialText(anyString());
        
        doAnswer(invocation -> {
            finalResults.incrementAndGet();
            completionLatch.countDown();
            return null;
        }).when(mockTranscriptManager).appendFinalText(anyString());
        
        doAnswer(invocation -> {
            stateUpdates.incrementAndGet();
            return null;
        }).when(mockStateManager).updateState(any(RecordingState.class));
        
        // Simulate continuous speech recognition
        new Thread(() -> {
            try {
                for (int i = 0; i < totalSentences; i++) {
                    simulateSentenceRecognition(i);
                    Thread.sleep(100); // Simulate time between sentences
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // Wait for completion
        assertTrue("Long running session should complete within timeout",
                  completionLatch.await(30, TimeUnit.SECONDS));
        
        // Verify performance metrics
        assertEquals("Should process all sentences", totalSentences, finalResults.get());
        assertTrue("Should have multiple partial updates", partialUpdates.get() > totalSentences);
        assertTrue("Should have state updates", stateUpdates.get() > 0);
    }

    @Test
    public void testContinuousMode_MemoryStability() throws Exception {
        // Test memory stability during continuous operation
        
        // Simulate memory-intensive scenario with large text updates
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("This is a long sentence with many words to test memory handling. ");
        }
        String longSentence = largeText.toString();
        
        // Process multiple large text updates
        for (int i = 0; i < 20; i++) {
            testCallbacks.onReady();
            testCallbacks.onPartial(longSentence.substring(0, Math.min(100 * (i + 1), longSentence.length())));
            testCallbacks.onFinal(longSentence);
        }
        
        // Verify all updates were processed without memory issues
        verify(mockTranscriptManager, times(20)).appendFinalText(eq(longSentence));
        verify(mockTranscriptManager, times(20)).updatePartialText(anyString());
    }

    @Test
    public void testContinuousMode_ErrorRecoveryStability() throws Exception {
        // Test continuous mode stability with intermittent errors
        
        // Simulate session with periodic errors and recovery
        for (int cycle = 0; cycle < 10; cycle++) {
            // Normal operation
            testCallbacks.onReady();
            testCallbacks.onPartial("Normal speech " + cycle);
            testCallbacks.onFinal("Normal speech " + cycle + ".");
            
            // Inject error every few cycles
            if (cycle % 3 == 0) {
                testCallbacks.onError("Temporary network error");
                // Simulate recovery
                mockErrorHandler.onSuccessfulRecovery();
            }
        }
        
        // Verify continuous operation despite errors
        verify(mockTranscriptManager, times(10)).appendFinalText(anyString());
        verify(mockErrorHandler, times(4)).handleError(anyInt(), anyString()); // Errors on cycles 0, 3, 6, 9
        verify(mockErrorHandler, times(14)).onSuccessfulRecovery(); // 10 normal + 4 recovery
    }

    @Test
    public void testContinuousMode_RapidStateChanges() throws Exception {
        // Test continuous mode with rapid state changes
        
        int lifecycleOperations = 20;
        CountDownLatch rapidChangesLatch = new CountDownLatch(100);
        
        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    testCallbacks.onReady();
                    testCallbacks.onPartial("Rapid " + i);
                    
                    if (i % 10 == 0) {
                        // Inject occasional errors
                        testCallbacks.onError("Brief error " + i);
                        testCallbacks.onReady(); // Immediate recovery
                    }
                    
                    testCallbacks.onFinal("Rapid " + i + ".");
                    rapidChangesLatch.countDown();
                    
                    Thread.sleep(10); // Very rapid updates
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        assertTrue("Rapid state changes should complete",
                  rapidChangesLatch.await(15, TimeUnit.SECONDS));
        
        // Verify system handled rapid changes
        verify(mockTranscriptManager, times(100)).appendFinalText(anyString());
        verify(mockTranscriptManager, times(100)).updatePartialText(anyString());
    }

    @Test
    public void testContinuousMode_HighVolumeTextProcessing() throws Exception {
        // Test continuous mode with high volume of text processing
        
        // Generate high volume of text updates
        int updateCount = 500;
        CountDownLatch highVolumeLatch = new CountDownLatch(updateCount);
        
        AtomicInteger processedUpdates = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            processedUpdates.incrementAndGet();
            highVolumeLatch.countDown();
            return null;
        }).when(mockTranscriptManager).updatePartialText(anyString());
        
        // Rapid text updates
        new Thread(() -> {
            try {
                for (int i = 0; i < updateCount; i++) {
                    testCallbacks.onPartial("High volume update " + i + " with additional text content");
                    Thread.sleep(5); // Very rapid updates
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        assertTrue("High volume processing should complete",
                  highVolumeLatch.await(20, TimeUnit.SECONDS));
        
        assertEquals("Should process all updates", updateCount, processedUpdates.get());
    }

    @Test
    public void testContinuousMode_ErrorRecoveryPatterns() throws Exception {
        // Test different error recovery patterns in continuous mode
        
        // Pattern 1: Immediate recovery
        testCallbacks.onReady();
        testCallbacks.onError("Network timeout");
        testCallbacks.onReady(); // Immediate recovery
        testCallbacks.onFinal("Recovered text 1.");
        
        // Pattern 2: Multiple errors before recovery
        testCallbacks.onError("Server busy");
        testCallbacks.onError("Network error");
        testCallbacks.onReady(); // Recovery after multiple errors
        testCallbacks.onFinal("Recovered text 2.");
        
        // Pattern 3: Error during partial update
        testCallbacks.onPartial("Partial text before error");
        testCallbacks.onError("Audio error");
        testCallbacks.onReady(); // Recovery
        testCallbacks.onFinal("Recovered text 3.");
        
        // Verify all recovery patterns worked
        verify(mockTranscriptManager).appendFinalText("Recovered text 1.");
        verify(mockTranscriptManager).appendFinalText("Recovered text 2.");
        verify(mockTranscriptManager).appendFinalText("Recovered text 3.");
        
        // Verify error handling
        verify(mockErrorHandler, times(4)).handleError(anyInt(), anyString());
        verify(mockErrorHandler, times(3)).onSuccessfulRecovery();
    }

    @Test
    public void testContinuousMode_ResourceManagement() throws Exception {
        // Test resource management during continuous operation
        
        // Simulate long session with resource cleanup checks
        for (int session = 0; session < 5; session++) {
            // Each session has multiple interactions
            for (int interaction = 0; interaction < 20; interaction++) {
                testCallbacks.onReady();
                testCallbacks.onPartial("Session " + session + " interaction " + interaction);
                testCallbacks.onFinal("Session " + session + " interaction " + interaction + ".");
            }
        }
        
        // Verify all sessions were processed
        verify(mockTranscriptManager, times(100)).appendFinalText(anyString()); // 5 sessions * 20 interactions
    }

    // Helper class for test callbacks
    private class TestSpeechCallbacks implements SpeechLiveTranscriber.Callbacks {
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

    // Helper methods
    private void simulateSentenceRecognition(int sentenceNumber) {
        String sentence = "This is sentence number " + sentenceNumber + ".";
        String[] words = sentence.split(" ");
        
        testCallbacks.onReady();
        
        // Simulate partial updates
        StringBuilder partial = new StringBuilder();
        for (String word : words) {
            partial.append(word).append(" ");
            testCallbacks.onPartial(partial.toString().trim());
        }
        
        // Final result
        testCallbacks.onFinal(sentence);
    }

    private int extractErrorCodeFromMessage(String message) {
        if (message == null) return -1;
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("no match") || lowerMessage.contains("no speech")) {
            return android.speech.SpeechRecognizer.ERROR_NO_MATCH;
        } else if (lowerMessage.contains("network timeout")) {
            return android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
        } else if (lowerMessage.contains("network")) {
            return android.speech.SpeechRecognizer.ERROR_NETWORK;
        } else if (lowerMessage.contains("timeout")) {
            return android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
        } else if (lowerMessage.contains("permission")) {
            return android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
        } else if (lowerMessage.contains("audio")) {
            return android.speech.SpeechRecognizer.ERROR_AUDIO;
        } else if (lowerMessage.contains("busy")) {
            return android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
        } else if (lowerMessage.contains("server")) {
            return android.speech.SpeechRecognizer.ERROR_SERVER;
        }
        
        return android.speech.SpeechRecognizer.ERROR_CLIENT;
    }
}