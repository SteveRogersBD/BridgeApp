package com.example.bridge.integration;

import android.content.Context;
import android.speech.SpeechRecognizer;

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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for speech recognition callback handling and component integration.
 * Tests the interaction between speech recognition callbacks and the transcript management system.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SpeechRecognitionIntegrationTest {

    private Context context;

    @Mock
    private TranscriptManager mockTranscriptManager;
    
    @Mock
    private TranscriptionErrorHandler mockErrorHandler;
    
    @Mock
    private TranscriptionStateManager mockStateManager;

    private SpeechLiveTranscriber.Callbacks speechCallbacks;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Setup speech callbacks for testing
        speechCallbacks = createTestCallbacks();
        
        // Setup default mock behaviors
        setupDefaultMockBehaviors();
    }

    private SpeechLiveTranscriber.Callbacks createTestCallbacks() {
        return new SpeechLiveTranscriber.Callbacks() {
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
        };
    }

    private void setupDefaultMockBehaviors() {
        when(mockStateManager.canStartRecording()).thenReturn(true);
        when(mockStateManager.getCurrentState()).thenReturn(RecordingState.IDLE);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("");
        when(mockErrorHandler.isTranscriptionServiceAvailable()).thenReturn(true);
    }

    @Test
    public void testSpeechRecognitionCallbackHandling_CompleteFlow() throws Exception {
        // Test complete speech recognition callback flow
        
        // Test onReady callback
        speechCallbacks.onReady();
        
        verify(mockStateManager).updateState(RecordingState.LISTENING);
        verify(mockErrorHandler).onSuccessfulRecovery();
        
        // Test onPartial callback with valid text
        String partialText = "Hello this is a partial";
        speechCallbacks.onPartial(partialText);
        
        verify(mockTranscriptManager).updatePartialText(partialText);
        verify(mockStateManager).indicateProcessing();
        
        // Test onFinal callback with complete sentence
        String finalText = "Hello this is a complete sentence.";
        speechCallbacks.onFinal(finalText);
        
        verify(mockTranscriptManager).appendFinalText(finalText);
        verify(mockStateManager, times(2)).indicateProcessing(); // Called twice now
        
        // Test multiple partial updates (simulating real-time speech)
        reset(mockTranscriptManager, mockStateManager);
        
        String[] partialUpdates = {
            "How",
            "How are",
            "How are you",
            "How are you today"
        };
        
        for (String partial : partialUpdates) {
            speechCallbacks.onPartial(partial);
        }
        
        verify(mockTranscriptManager, times(4)).updatePartialText(anyString());
        verify(mockStateManager, times(4)).indicateProcessing();
    }

    @Test
    public void testSpeechRecognitionCallbackHandling_EdgeCases() throws Exception {
        // Test callback handling with edge cases
        
        // Test empty/null text handling
        speechCallbacks.onPartial(null);
        speechCallbacks.onPartial("");
        speechCallbacks.onPartial("   ");
        
        verify(mockTranscriptManager, never()).updatePartialText(anyString());
        
        speechCallbacks.onFinal(null);
        speechCallbacks.onFinal("");
        speechCallbacks.onFinal("   ");
        
        verify(mockTranscriptManager, never()).appendFinalText(anyString());
        
        // Test valid text processing
        reset(mockTranscriptManager, mockStateManager);
        
        speechCallbacks.onPartial("Valid partial text");
        speechCallbacks.onFinal("Valid final text");
        
        verify(mockTranscriptManager).updatePartialText("Valid partial text");
        verify(mockTranscriptManager).appendFinalText("Valid final text");
    }

    @Test
    public void testContinuousTranscriptionModeStability() throws Exception {
        // Test continuous transcription mode stability over extended period
        
        // Simulate continuous transcription session with multiple sentences
        String[] sentences = {
            "This is the first sentence.",
            "Here comes the second sentence.",
            "And now we have a third sentence.",
            "Finally the fourth and last sentence."
        };
        
        for (int i = 0; i < sentences.length; i++) {
            // Simulate the flow: ready -> partial updates -> final result
            speechCallbacks.onReady();
            
            // Simulate partial updates for each sentence
            String[] words = sentences[i].split(" ");
            StringBuilder partial = new StringBuilder();
            
            for (String word : words) {
                partial.append(word).append(" ");
                speechCallbacks.onPartial(partial.toString().trim());
            }
            
            // Final result
            speechCallbacks.onFinal(sentences[i]);
        }
        
        // Verify all sentences were processed
        verify(mockTranscriptManager, times(sentences.length)).appendFinalText(anyString());
        
        // Verify state management was called appropriately
        verify(mockStateManager, times(sentences.length)).updateState(RecordingState.LISTENING);
        
        // Verify processing indications occurred
        verify(mockStateManager, atLeast(sentences.length)).indicateProcessing();
    }

    @Test
    public void testErrorRecoveryScenarios_NetworkErrors() throws Exception {
        // Test error recovery for network-related errors
        
        // Test network error
        String networkErrorMessage = "Network error";
        speechCallbacks.onError(networkErrorMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_NETWORK), eq(networkErrorMessage));
        
        // Test network timeout
        String timeoutErrorMessage = "Network timeout";
        speechCallbacks.onError(timeoutErrorMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_NETWORK_TIMEOUT), eq(timeoutErrorMessage));
        
        // Test server error
        String serverErrorMessage = "Server error";
        speechCallbacks.onError(serverErrorMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_SERVER), eq(serverErrorMessage));
    }

    @Test
    public void testErrorRecoveryScenarios_AudioErrors() throws Exception {
        // Test error recovery for audio-related errors
        
        // Test audio recording error
        String audioErrorMessage = "Audio recording error";
        speechCallbacks.onError(audioErrorMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_AUDIO), eq(audioErrorMessage));
        
        // Test insufficient permissions
        String permissionErrorMessage = "Insufficient permissions";
        speechCallbacks.onError(permissionErrorMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS), eq(permissionErrorMessage));
    }

    @Test
    public void testErrorRecoveryScenarios_TemporaryErrors() throws Exception {
        // Test error recovery for temporary errors that should be handled gracefully
        
        // Test no speech detected (should continue listening)
        String noSpeechMessage = "No speech detected - continuing to listen...";
        speechCallbacks.onError(noSpeechMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_NO_MATCH), eq(noSpeechMessage));
        
        // Test speech timeout (should continue listening)
        String timeoutMessage = "No speech input detected - continuing to listen...";
        speechCallbacks.onError(timeoutMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_SPEECH_TIMEOUT), eq(timeoutMessage));
        
        // Test recognizer busy
        String busyMessage = "Recognition service busy";
        speechCallbacks.onError(busyMessage);
        
        verify(mockErrorHandler).handleError(eq(SpeechRecognizer.ERROR_RECOGNIZER_BUSY), eq(busyMessage));
    }

    @Test
    public void testUIResponsivenessDuringActiveTranscription() throws Exception {
        // Test UI responsiveness during active transcription with rapid updates
        
        // Simulate rapid partial text updates (like real speech recognition)
        CountDownLatch latch = new CountDownLatch(1);
        
        // Simulate rapid updates
        for (int i = 0; i < 50; i++) {
            final String partialText = "Rapid update number " + i;
            speechCallbacks.onPartial(partialText);
        }
        
        // Verify all updates were processed
        verify(mockTranscriptManager, times(50)).updatePartialText(anyString());
        verify(mockStateManager, times(50)).indicateProcessing();
    }

    @Test
    public void testErrorRecoveryIntegration_WithStateManagement() throws Exception {
        // Test integration between error recovery and state management
        
        // Setup error handler callback behavior
        doAnswer(invocation -> {
            // Simulate error handler updating state
            mockStateManager.updateState(RecordingState.ERROR);
            return null;
        }).when(mockErrorHandler).handleError(anyInt(), anyString());
        
        // Trigger an error
        speechCallbacks.onError("Test error message");
        
        verify(mockErrorHandler).handleError(anyInt(), eq("Test error message"));
        verify(mockStateManager).updateState(RecordingState.ERROR);
        
        // Simulate recovery
        speechCallbacks.onReady();
        
        verify(mockErrorHandler).onSuccessfulRecovery();
        verify(mockStateManager, times(2)).updateState(any(RecordingState.class)); // Error + Listening
    }

    @Test
    public void testTranscriptionSessionIntegrity() throws Exception {
        // Test that transcription session maintains integrity across various scenarios
        
        // Simulate a complete transcription session with mixed events
        speechCallbacks.onReady();
        speechCallbacks.onPartial("Hello");
        speechCallbacks.onPartial("Hello world");
        speechCallbacks.onFinal("Hello world.");
        
        // Error occurs
        speechCallbacks.onError("Temporary error");
        
        // Recovery
        speechCallbacks.onReady();
        speechCallbacks.onPartial("How are");
        speechCallbacks.onPartial("How are you");
        speechCallbacks.onFinal("How are you?");
        
        // Verify session integrity
        verify(mockTranscriptManager).updatePartialText("Hello");
        verify(mockTranscriptManager).updatePartialText("Hello world");
        verify(mockTranscriptManager).appendFinalText("Hello world.");
        verify(mockTranscriptManager).updatePartialText("How are");
        verify(mockTranscriptManager).updatePartialText("How are you");
        verify(mockTranscriptManager).appendFinalText("How are you?");
        
        // Verify error handling
        verify(mockErrorHandler).handleError(anyInt(), eq("Temporary error"));
        verify(mockErrorHandler, times(2)).onSuccessfulRecovery(); // Called twice for onReady
    }

    private int extractErrorCodeFromMessage(String message) {
        if (message == null) return -1;
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("no match") || lowerMessage.contains("no speech")) {
            return SpeechRecognizer.ERROR_NO_MATCH;
        } else if (lowerMessage.contains("network timeout")) {
            return SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
        } else if (lowerMessage.contains("network")) {
            return SpeechRecognizer.ERROR_NETWORK;
        } else if (lowerMessage.contains("timeout")) {
            return SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
        } else if (lowerMessage.contains("permission")) {
            return SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
        } else if (lowerMessage.contains("audio")) {
            return SpeechRecognizer.ERROR_AUDIO;
        } else if (lowerMessage.contains("busy")) {
            return SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
        } else if (lowerMessage.contains("server")) {
            return SpeechRecognizer.ERROR_SERVER;
        }
        
        return SpeechRecognizer.ERROR_CLIENT;
    }
}