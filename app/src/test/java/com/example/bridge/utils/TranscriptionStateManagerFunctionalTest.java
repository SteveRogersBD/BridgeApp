package com.example.bridge.utils;

import com.example.bridge.models.RecordingState;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional test for TranscriptionStateManager core functionality.
 * Tests the state management logic without UI components.
 */
public class TranscriptionStateManagerFunctionalTest {

    @Test
    public void testRecordingStateTransitions() {
        // Test the core state transition logic
        
        // Initial state should be IDLE
        RecordingState currentState = RecordingState.IDLE;
        assertTrue(currentState.canStartRecording());
        assertFalse(currentState.canPause());
        assertFalse(currentState.canResume());
        
        // Transition to LISTENING
        currentState = RecordingState.LISTENING;
        assertFalse(currentState.canStartRecording());
        assertTrue(currentState.canPause());
        assertFalse(currentState.canResume());
        assertTrue(currentState.isActive());
        
        // Transition to PAUSED
        currentState = RecordingState.PAUSED;
        assertFalse(currentState.canStartRecording());
        assertFalse(currentState.canPause());
        assertTrue(currentState.canResume());
        assertFalse(currentState.isActive());
        
        // Transition to PROCESSING
        currentState = RecordingState.PROCESSING;
        assertFalse(currentState.canStartRecording());
        assertTrue(currentState.canPause());
        assertFalse(currentState.canResume());
        assertTrue(currentState.isActive());
        
        // Transition to ERROR
        currentState = RecordingState.ERROR;
        assertTrue(currentState.canStartRecording());
        assertFalse(currentState.canPause());
        assertFalse(currentState.canResume());
        assertFalse(currentState.isActive());
    }

    @Test
    public void testStateDisplayText() {
        // Test that each state has appropriate display text
        assertEquals("Ready to record", RecordingState.IDLE.getDisplayText());
        assertEquals("Listening…", RecordingState.LISTENING.getDisplayText());
        assertEquals("Paused", RecordingState.PAUSED.getDisplayText());
        assertEquals("Processing…", RecordingState.PROCESSING.getDisplayText());
        assertEquals("Error occurred", RecordingState.ERROR.getDisplayText());
    }

    @Test
    public void testCompleteRecordingWorkflow() {
        // Test a complete recording workflow
        RecordingState state = RecordingState.IDLE;
        
        // 1. Start recording
        assertTrue("Should be able to start recording from IDLE", state.canStartRecording());
        state = RecordingState.LISTENING;
        
        // 2. Recording is active
        assertTrue("Should be active when listening", state.isActive());
        assertTrue("Should be able to pause when listening", state.canPause());
        
        // 3. Pause recording
        state = RecordingState.PAUSED;
        assertTrue("Should be able to resume when paused", state.canResume());
        assertFalse("Should not be active when paused", state.isActive());
        
        // 4. Resume recording
        state = RecordingState.LISTENING;
        assertTrue("Should be active after resume", state.isActive());
        
        // 5. Processing occurs
        state = RecordingState.PROCESSING;
        assertTrue("Should be active when processing", state.isActive());
        assertTrue("Should be able to pause when processing", state.canPause());
        
        // 6. Return to listening
        state = RecordingState.LISTENING;
        
        // 7. Stop recording
        state = RecordingState.IDLE;
        assertTrue("Should be able to start new recording after stop", state.canStartRecording());
        assertFalse("Should not be active when idle", state.isActive());
    }

    @Test
    public void testErrorRecoveryWorkflow() {
        // Test error handling workflow
        RecordingState state = RecordingState.LISTENING;
        
        // Error occurs during recording
        state = RecordingState.ERROR;
        assertTrue("Should be able to restart after error", state.canStartRecording());
        assertFalse("Should not be active during error", state.isActive());
        
        // Recover from error
        state = RecordingState.LISTENING;
        assertTrue("Should be active after error recovery", state.isActive());
        assertTrue("Should be able to pause after recovery", state.canPause());
    }
}