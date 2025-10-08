package com.example.bridge.integration;

import android.content.Context;
import android.speech.SpeechRecognizer;

import com.example.bridge.utils.TranscriptionErrorHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Integration tests for error recovery scenarios.
 * Tests the complete error handling flow from speech recognition errors
 * through error handler processing to callback responses.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ErrorRecoveryIntegrationTest {

    private Context context;
    private TranscriptionErrorHandler errorHandler;
    private TestErrorCallback errorCallback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Setup test error callback
        errorCallback = new TestErrorCallback();
        
        // Create real error handler with test callback
        errorHandler = new TranscriptionErrorHandler(context, errorCallback);
    }

    @Test
    public void testTemporaryErrorRecovery_NoMatchError() throws Exception {
        // Test recovery from ERROR_NO_MATCH (temporary error)
        
        // Trigger no match error
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected - continuing to listen...");
        
        // Should not trigger user notification for temporary error
        assertFalse("Should not request retry for temporary error", errorCallback.retryRequested.get());
        assertFalse("Should not stop transcription for temporary error", errorCallback.transcriptionStopped.get());
    }

    @Test
    public void testRecoverableErrorRecovery_NetworkError() throws Exception {
        // Test recovery from network errors (recoverable)
        
        // Trigger network error
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error");
        
        // Should request retry with delay
        assertTrue("Should request retry for recoverable error", 
                  errorCallback.retryLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Retry should be requested", errorCallback.retryRequested.get());
        assertTrue("Retry delay should be reasonable", errorCallback.retryDelay > 0);
    }

    @Test
    public void testCriticalErrorRecovery_PermissionError() throws Exception {
        // Test recovery from critical errors (should stop transcription)
        
        // Trigger permission error
        errorHandler.handleError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, "Insufficient permissions");
        
        // Should stop transcription
        assertTrue("Should stop transcription for critical error",
                  errorCallback.transcriptionStoppedLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Transcription should be stopped", errorCallback.transcriptionStopped.get());
    }

    @Test
    public void testErrorRecoveryFlow_MultipleErrors() throws Exception {
        // Test recovery flow with multiple consecutive errors
        
        // First error - temporary
        errorHandler.handleError(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected");
        
        // Second error - recoverable
        errorCallback.reset();
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network timeout");
        
        assertTrue("Should handle second error", 
                  errorCallback.retryLatch.await(5, TimeUnit.SECONDS));
        
        // Third error - critical
        errorCallback.reset();
        errorHandler.handleError(SpeechRecognizer.ERROR_AUDIO, "Audio recording error");
        
        assertTrue("Should handle critical error", 
                  errorCallback.transcriptionStoppedLatch.await(5, TimeUnit.SECONDS));
        
        // Verify error escalation
        assertTrue("Should stop transcription after critical error", errorCallback.transcriptionStopped.get());
    }

    @Test
    public void testErrorRecoveryWithRetryLimits() throws Exception {
        // Test error recovery respects retry limits
        
        // Trigger multiple recoverable errors rapidly
        for (int i = 0; i < 10; i++) {
            errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error " + i);
            Thread.sleep(100);
        }
        
        // Should eventually stop retrying
        Thread.sleep(2000); // Allow time for retry logic
        
        // Verify retry behavior is controlled
        assertTrue("Should have attempted retries", errorCallback.retryCount.get() > 0);
        assertTrue("Should limit retry attempts", errorCallback.retryCount.get() < 10);
    }

    @Test
    public void testGracefulDegradation_ServiceUnavailable() throws Exception {
        // Test graceful degradation when transcription service is unavailable
        
        // Simulate service unavailable
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Service unavailable");
        
        // Should activate graceful degradation
        assertTrue("Should activate graceful degradation",
                  errorCallback.gracefulDegradationLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Graceful degradation should be activated", errorCallback.gracefulDegradationActivated.get());
    }

    @Test
    public void testErrorRecoveryPerformance_HighErrorRate() throws Exception {
        // Test error recovery performance under high error rate
        
        long startTime = System.currentTimeMillis();
        int errorCount = 100;
        
        // Generate high rate of errors
        for (int i = 0; i < errorCount; i++) {
            int errorType = (i % 3 == 0) ? SpeechRecognizer.ERROR_NO_MATCH :
                           (i % 3 == 1) ? SpeechRecognizer.ERROR_NETWORK :
                           SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
            
            errorHandler.handleError(errorType, "Error " + i);
            
            if (i % 10 == 0) {
                Thread.sleep(10); // Brief pause every 10 errors
            }
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Verify performance
        assertTrue("Error processing should be efficient", processingTime < 5000); // Less than 5 seconds
        assertTrue("Should handle multiple errors", errorCallback.retryCount.get() > 0);
    }

    @Test
    public void testErrorRecoveryCleanup() throws Exception {
        // Test error recovery cleanup
        
        // Trigger error with pending retry
        errorHandler.handleError(SpeechRecognizer.ERROR_NETWORK, "Network error");
        
        // Wait for retry to be scheduled
        assertTrue("Should schedule retry", 
                  errorCallback.retryLatch.await(5, TimeUnit.SECONDS));
        
        // Cleanup should be called
        errorHandler.cleanup();
        
        // Verify cleanup occurred (no more retries should be scheduled)
        errorCallback.reset();
        Thread.sleep(1000);
        
        assertFalse("Should not retry after cleanup", errorCallback.retryRequested.get());
    }

    @Test
    public void testErrorRecoveryServiceAvailability() throws Exception {
        // Test error recovery checks service availability
        
        // Initially service is available
        assertTrue("Service should be available initially", 
                  errorHandler.isTranscriptionServiceAvailable());
        
        // Trigger service unavailable error
        errorHandler.handleError(SpeechRecognizer.ERROR_SERVER, "Service unavailable");
        
        // Wait for processing
        assertTrue("Should handle service unavailable", 
                  errorCallback.serviceUnavailableLatch.await(5, TimeUnit.SECONDS));
        
        // Service availability should be updated
        assertTrue("Service unavailable should be detected", errorCallback.serviceUnavailable.get());
    }

    // Test callback implementation
    private class TestErrorCallback implements TranscriptionErrorHandler.ErrorCallback {
        AtomicBoolean retryRequested = new AtomicBoolean(false);
        AtomicBoolean transcriptionStopped = new AtomicBoolean(false);
        AtomicBoolean errorRecovered = new AtomicBoolean(false);
        AtomicBoolean gracefulDegradationActivated = new AtomicBoolean(false);
        AtomicBoolean serviceUnavailable = new AtomicBoolean(false);
        AtomicBoolean criticalErrorOccurred = new AtomicBoolean(false);
        
        AtomicInteger retryCount = new AtomicInteger(0);
        int retryDelay = 0;
        
        CountDownLatch retryLatch = new CountDownLatch(1);
        CountDownLatch transcriptionStoppedLatch = new CountDownLatch(1);
        CountDownLatch errorRecoveredLatch = new CountDownLatch(1);
        CountDownLatch gracefulDegradationLatch = new CountDownLatch(1);
        CountDownLatch serviceUnavailableLatch = new CountDownLatch(1);
        CountDownLatch criticalErrorLatch = new CountDownLatch(1);

        @Override
        public void onRetryRequested(int delayMs) {
            retryRequested.set(true);
            retryDelay = delayMs;
            retryCount.incrementAndGet();
            retryLatch.countDown();
        }

        @Override
        public void onTranscriptionStopped(String reason) {
            transcriptionStopped.set(true);
            transcriptionStoppedLatch.countDown();
        }

        @Override
        public void onErrorRecovered() {
            errorRecovered.set(true);
            errorRecoveredLatch.countDown();
        }

        @Override
        public void onGracefulDegradationActivated(String reason) {
            gracefulDegradationActivated.set(true);
            gracefulDegradationLatch.countDown();
        }

        @Override
        public void onServiceUnavailable(String reason) {
            serviceUnavailable.set(true);
            serviceUnavailableLatch.countDown();
        }

        @Override
        public void onCriticalErrorRequiresUserAction(String message, int errorCode) {
            criticalErrorOccurred.set(true);
            criticalErrorLatch.countDown();
        }
        
        void reset() {
            retryRequested.set(false);
            transcriptionStopped.set(false);
            errorRecovered.set(false);
            gracefulDegradationActivated.set(false);
            serviceUnavailable.set(false);
            criticalErrorOccurred.set(false);
            
            retryLatch = new CountDownLatch(1);
            transcriptionStoppedLatch = new CountDownLatch(1);
            errorRecoveredLatch = new CountDownLatch(1);
            gracefulDegradationLatch = new CountDownLatch(1);
            serviceUnavailableLatch = new CountDownLatch(1);
            criticalErrorLatch = new CountDownLatch(1);
        }
    }
}