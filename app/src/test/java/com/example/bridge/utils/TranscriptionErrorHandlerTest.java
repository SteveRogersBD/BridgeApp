package com.example.bridge.utils;

import android.content.Context;
import android.speech.SpeechRecognizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TranscriptionErrorHandler
 * Tests error categorization, retry logic, and user notification system
 */
@RunWith(RobolectricTestRunner.class)
public class TranscriptionErrorHandlerTest {

    @Mock
    private TranscriptionErrorHandler.ErrorCallback mockCallback;

    private TranscriptionErrorHandler errorHandler;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        errorHandler = new TranscriptionErrorHandler(context, mockCallback);
    }

    // ---- Error Categorization Tests ----

    @Test
    public void testCategorizeTemporaryErrors() {
        assertEquals(TranscriptionErrorHandler.ErrorCategory.TEMPORARY,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_NO_MATCH));
        assertEquals(TranscriptionErrorHandler.ErrorCategory.TEMPORARY,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT));
    }

    @Test
    public void testCategorizeRecoverableErrors() {
        assertEquals(TranscriptionErrorHandler.ErrorCategory.RECOVERABLE,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_NETWORK));
        assertEquals(TranscriptionErrorHandler.ErrorCategory.RECOVERABLE,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT));
        assertEquals(TranscriptionErrorHandler.ErrorCategory.RECOVERABLE,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_SERVER));
        assertEquals(TranscriptionErrorHandler.ErrorCategory.RECOVERABLE,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY));
    }

    @Test
    public void testCategorizeCriticalErrors() {
        assertEquals(TranscriptionErrorHandler.ErrorCategory.CRITICAL,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));
        assertEquals(TranscriptionErrorHandler.ErrorCategory.CRITICAL,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_AUDIO));
        assertEquals(TranscriptionErrorHandler.ErrorCategory.CRITICAL,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_CLIENT));
    }

    @Test
    public void testCategorizeUnknownErrorsAsCritical() {
        assertEquals(TranscriptionErrorHandler.ErrorCategory.CRITICAL,
                errorHandler.categorizeError(999)); // Unknown error code
    }

    // ---- Error Severity Tests ----

    @Test
    public void testErrorSeverityLow() {
        assertEquals(TranscriptionErrorHandler.ErrorSeverity.LOW,
                errorHandler.getErrorSeverity(SpeechRecognizer.ERROR_NO_MATCH));
        assertEquals(TranscriptionErrorHandler.ErrorSeverity.LOW,
                errorHandler.getErrorSeverity(SpeechRecognizer.ERROR_SPEECH_TIMEOUT));
    }

    @Test
    public void testErrorSeverityMedium() {
        assertEquals(TranscriptionErrorHandler.ErrorSeverity.MEDIUM,
                errorHandler.getErrorSeverity(SpeechRecognizer.ERROR_NETWORK));
        assertEquals(TranscriptionErrorHandler.ErrorSeverity.MEDIUM,
                errorHandler.getErrorSeverity(SpeechRecognizer.ERROR_SERVER));
    }

    @Test
    public void testErrorSeverityHigh() {
        assertEquals(TranscriptionErrorHandler.ErrorSeverity.HIGH,
                errorHandler.getErrorSeverity(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));
        assertEquals(TranscriptionErrorHandler.ErrorSeverity.HIGH,
                errorHandler.getErrorSeverity(SpeechRecognizer.ERROR_AUDIO));
    }

    // ---- Temporary Error Handling Tests ----

    @Test
    public void testTemporaryErrorTriggersRetry() {
        // First verify that the error is categorized correctly
        assertEquals(TranscriptionErrorHandler.ErrorCategory.TEMPORARY,
                errorHandler.categorizeError(SpeechRecognizer.ERROR_NO_MATCH));
        
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected");

        // Verify retry is scheduled with longer timeout to account for delay
        verify(mockCallback, timeout(2000)).onRetryRequested(anyInt());
    }

    @Test
    public void testMultipleTemporaryErrorsEscalateToRecoverable() {
        // Trigger 3 consecutive temporary errors
        for (int i = 0; i < 3; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected");
        }

        // Should escalate and trigger a longer retry delay
        verify(mockCallback, timeout(1000).atLeast(3)).onRetryRequested(anyInt());
    }

    // ---- Recoverable Error Handling Tests ----

    @Test
    public void testRecoverableErrorTriggersRetryWithDelay() {
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error");

        ArgumentCaptor<Integer> delayCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockCallback, timeout(5000)).onRetryRequested(delayCaptor.capture());
        
        // Should use longer delay for recoverable errors
        assertTrue("Recoverable error should have longer delay", 
                delayCaptor.getValue() >= 2000);
    }

    @Test
    public void testMultipleRecoverableErrorsEscalateToCritical() {
        // Trigger 3 consecutive recoverable errors
        for (int i = 0; i < 3; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error");
        }

        // Should trigger service unavailable callback
        verify(mockCallback, timeout(1000)).onServiceUnavailable(anyString());
    }

    // ---- Critical Error Handling Tests ----

    @Test
    public void testCriticalErrorStopsTranscription() {
        errorHandler.handleError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, 
                "Permission denied");

        // Permission errors now trigger critical error requiring user action
        verify(mockCallback).onCriticalErrorRequiresUserAction(anyString(), eq(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));
    }

    @Test
    public void testCriticalServerErrorStillRetriesAfterDelay() {
        // Server errors are actually categorized as recoverable, not critical
        // So a single server error will trigger retry, not graceful degradation
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error");

        // Should trigger retry for recoverable error
        verify(mockCallback).onRetryRequested(anyInt());
    }

    @Test
    public void testCriticalAudioErrorDoesNotRetry() {
        errorHandler.handleError(SpeechRecognizer.ERROR_AUDIO, "Audio error");

        // Should stop transcription but not retry
        verify(mockCallback).onTranscriptionStopped("Audio error");
        verify(mockCallback, never()).onRetryRequested(anyInt());
    }

    // ---- Recovery Tests ----

    @Test
    public void testSuccessfulRecoveryResetsCounters() {
        // Trigger some errors first
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech");
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error");

        // Simulate successful recovery
        errorHandler.onSuccessfulRecovery();

        verify(mockCallback).onErrorRecovered();

        // After recovery, errors should be treated as first occurrence again
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech");
        
        // Should trigger retry, not escalation
        verify(mockCallback, timeout(1000).atLeast(1)).onRetryRequested(anyInt());
    }

    // ---- User-Friendly Message Tests ----

    @Test
    public void testGetUserFriendlyMessages() {
        assertEquals("Audio recording issue detected",
                errorHandler.getUserFriendlyMessage(SpeechRecognizer.ERROR_AUDIO));
        assertEquals("Microphone permission required",
                errorHandler.getUserFriendlyMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));
        assertEquals("Network connection issue",
                errorHandler.getUserFriendlyMessage(SpeechRecognizer.ERROR_NETWORK));
        assertEquals("No speech detected",
                errorHandler.getUserFriendlyMessage(SpeechRecognizer.ERROR_NO_MATCH));
    }

    @Test
    public void testGetUserFriendlyMessageForUnknownError() {
        String message = errorHandler.getUserFriendlyMessage(999);
        assertTrue("Should include error code", message.contains("999"));
        assertTrue("Should indicate it's an error", message.contains("error"));
    }

    // ---- Retry Logic Tests ----

    @Test
    public void testShouldAttemptRetryInitially() {
        assertTrue("Should allow retry initially", errorHandler.shouldAttemptRetry());
    }

    @Test
    public void testShouldAttemptRetryAfterError() {
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech");
        
        // Immediately after error, might not be ready for retry
        // But after some time, should be ready
        try {
            Thread.sleep(600); // Wait longer than TEMPORARY_RETRY_DELAY
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue("Should allow retry after delay", errorHandler.shouldAttemptRetry());
    }

    // ---- Reset and Cleanup Tests ----

    @Test
    public void testResetClearsState() {
        // Trigger some errors
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech");
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error");

        // Reset should clear all state
        errorHandler.reset();

        // After reset, should allow retry
        assertTrue("Should allow retry after reset", errorHandler.shouldAttemptRetry());
    }

    @Test
    public void testCleanupRemovesPendingCallbacks() {
        // Schedule some retries
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech");
        
        // Cleanup should remove pending callbacks
        errorHandler.cleanup();
        
        // Wait to ensure no delayed callbacks execute
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify no additional callbacks after cleanup
        verify(mockCallback, atMost(1)).onRetryRequested(anyInt());
    }

    // ---- Enhanced Error Recovery Tests ----

    @Test
    public void testExponentialBackoffForTemporaryErrors() {
        // First temporary error should use base delay
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech 1");
        ArgumentCaptor<Integer> delayCaptor1 = ArgumentCaptor.forClass(Integer.class);
        verify(mockCallback, timeout(1000)).onRetryRequested(delayCaptor1.capture());
        assertEquals("First retry should use base delay", 500, (int) delayCaptor1.getValue());

        reset(mockCallback);
        
        // Second temporary error should use exponential backoff
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech 2");
        ArgumentCaptor<Integer> delayCaptor2 = ArgumentCaptor.forClass(Integer.class);
        verify(mockCallback, timeout(1000)).onRetryRequested(delayCaptor2.capture());
        assertEquals("Second retry should use exponential backoff", 1000, (int) delayCaptor2.getValue());
    }

    @Test
    public void testGracefulDegradationActivation() {
        // Simulate many consecutive errors to trigger graceful degradation
        for (int i = 0; i < 12; i++) { // Exceed MAX_TOTAL_RETRIES (10)
            errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "Repeated error " + i);
        }

        // Should activate graceful degradation
        verify(mockCallback, atLeastOnce()).onGracefulDegradationActivated(anyString());
        assertTrue("Graceful degradation should be active", errorHandler.isGracefulDegradationActive());
        assertFalse("Service should be marked unavailable", errorHandler.isTranscriptionServiceAvailable());
    }

    @Test
    public void testServiceUnavailabilityHandling() {
        // Simulate multiple recoverable errors to trigger service unavailability
        for (int i = 0; i < 3; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error " + i);
        }

        // Should trigger service unavailable callback
        verify(mockCallback, atLeastOnce()).onServiceUnavailable(anyString());
    }

    @Test
    public void testCriticalErrorRequiresUserAction() {
        // Permission error should require user action
        errorHandler.handleError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, "Permission denied");

        verify(mockCallback).onCriticalErrorRequiresUserAction(anyString(), eq(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS));
    }

    @Test
    public void testServiceRecoveryAttempt() {
        // Activate graceful degradation first
        for (int i = 0; i < 12; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "Error " + i);
        }

        assertTrue("Graceful degradation should be active", errorHandler.isGracefulDegradationActive());

        // Attempt service recovery
        errorHandler.attemptServiceRecovery();

        // Should reset state and request retry
        assertFalse("Graceful degradation should be deactivated", errorHandler.isGracefulDegradationActive());
        assertTrue("Service should be available again", errorHandler.isTranscriptionServiceAvailable());
        verify(mockCallback, atLeastOnce()).onRetryRequested(eq(0)); // Immediate retry
    }

    @Test
    public void testNetworkErrorGracefulDegradation() {
        // Network errors should eventually lead to graceful degradation
        for (int i = 0; i < 12; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error " + i);
        }

        verify(mockCallback, atLeastOnce()).onGracefulDegradationActivated(anyString());
    }

    @Test
    public void testAudioErrorHandling() {
        // Audio errors should be treated as critical and stop transcription
        errorHandler.handleError(SpeechRecognizer.ERROR_AUDIO, "Audio recording error");

        // Audio errors should stop transcription completely
        verify(mockCallback, timeout(1000)).onTranscriptionStopped(anyString());
    }

    @Test
    public void testTranscriptionServiceAvailabilityTracking() {
        // Initially service should be available
        assertTrue("Service should be available initially", errorHandler.isTranscriptionServiceAvailable());
        assertFalse("Graceful degradation should not be active initially", errorHandler.isGracefulDegradationActive());

        // After critical error, service should be unavailable
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error");
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error");
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error");

        assertFalse("Service should be unavailable after critical errors", errorHandler.isTranscriptionServiceAvailable());
    }

    @Test
    public void testMaxRetryDelayLimit() {
        // Test that retry delay doesn't exceed maximum
        for (int i = 0; i < 10; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error " + i);
        }

        ArgumentCaptor<Integer> delayCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockCallback, atLeast(1)).onRetryRequested(delayCaptor.capture());
        
        // All captured delays should be within reasonable limits
        for (Integer delay : delayCaptor.getAllValues()) {
            assertTrue("Retry delay should not exceed maximum", delay <= 30000);
        }
    }

    @Test
    public void testResetClearsEnhancedState() {
        // Activate graceful degradation
        for (int i = 0; i < 12; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "Error " + i);
        }

        assertTrue("Graceful degradation should be active", errorHandler.isGracefulDegradationActive());

        // Reset should clear all enhanced state
        errorHandler.reset();

        assertTrue("Service should be available after reset", errorHandler.isTranscriptionServiceAvailable());
        assertFalse("Graceful degradation should be inactive after reset", errorHandler.isGracefulDegradationActive());
    }

    // ---- Integration Tests ----

    @Test
    public void testErrorHandlingFlow() {
        // Test a complete error handling flow
        
        // 1. Start with temporary error
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech");
        verify(mockCallback).onRetryRequested(anyInt());
        
        // 2. Simulate successful recovery
        errorHandler.onSuccessfulRecovery();
        verify(mockCallback).onErrorRecovered();
        
        // 3. Then a recoverable error
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network issue");
        verify(mockCallback, atLeast(2)).onRetryRequested(anyInt());
        
        // 4. Finally a critical error
        errorHandler.handleError(SpeechRecognizer.ERROR_AUDIO, "Audio failure");
        verify(mockCallback).onTranscriptionStopped(anyString());
    }

    @Test
    public void testRobustErrorRecoveryFlow() {
        // Test the complete robust error recovery flow
        
        // 1. Multiple temporary errors with exponential backoff
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech 1");
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech 2");
        
        // 2. Escalation to recoverable errors
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network issue");
        
        // 3. Service unavailability
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error 1");
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error 2");
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Server error 3");
        
        // 4. Should trigger service unavailable
        verify(mockCallback, atLeastOnce()).onServiceUnavailable(anyString());
        
        // 5. Attempt recovery
        errorHandler.attemptServiceRecovery();
        verify(mockCallback, atLeastOnce()).onRetryRequested(eq(0));
    }
}