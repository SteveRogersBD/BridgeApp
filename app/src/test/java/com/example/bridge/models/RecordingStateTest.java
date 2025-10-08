package com.example.bridge.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the RecordingState enum.
 */
public class RecordingStateTest {

    @Test
    public void testDisplayText() {
        assertEquals("Ready to record", RecordingState.IDLE.getDisplayText());
        assertEquals("Listening…", RecordingState.LISTENING.getDisplayText());
        assertEquals("Paused", RecordingState.PAUSED.getDisplayText());
        assertEquals("Processing…", RecordingState.PROCESSING.getDisplayText());
        assertEquals("Error occurred", RecordingState.ERROR.getDisplayText());
    }

    @Test
    public void testIsActive() {
        assertTrue("LISTENING should be active", RecordingState.LISTENING.isActive());
        assertTrue("PROCESSING should be active", RecordingState.PROCESSING.isActive());
        
        assertFalse("IDLE should not be active", RecordingState.IDLE.isActive());
        assertFalse("PAUSED should not be active", RecordingState.PAUSED.isActive());
        assertFalse("ERROR should not be active", RecordingState.ERROR.isActive());
    }

    @Test
    public void testCanStartRecording() {
        assertTrue("Should be able to start from IDLE", RecordingState.IDLE.canStartRecording());
        assertTrue("Should be able to start from ERROR", RecordingState.ERROR.canStartRecording());
        
        assertFalse("Should not be able to start from LISTENING", RecordingState.LISTENING.canStartRecording());
        assertFalse("Should not be able to start from PROCESSING", RecordingState.PROCESSING.canStartRecording());
        assertFalse("Should not be able to start from PAUSED", RecordingState.PAUSED.canStartRecording());
    }

    @Test
    public void testCanPause() {
        assertTrue("Should be able to pause from LISTENING", RecordingState.LISTENING.canPause());
        assertTrue("Should be able to pause from PROCESSING", RecordingState.PROCESSING.canPause());
        
        assertFalse("Should not be able to pause from IDLE", RecordingState.IDLE.canPause());
        assertFalse("Should not be able to pause from PAUSED", RecordingState.PAUSED.canPause());
        assertFalse("Should not be able to pause from ERROR", RecordingState.ERROR.canPause());
    }

    @Test
    public void testCanResume() {
        assertTrue("Should be able to resume from PAUSED", RecordingState.PAUSED.canResume());
        
        assertFalse("Should not be able to resume from IDLE", RecordingState.IDLE.canResume());
        assertFalse("Should not be able to resume from LISTENING", RecordingState.LISTENING.canResume());
        assertFalse("Should not be able to resume from PROCESSING", RecordingState.PROCESSING.canResume());
        assertFalse("Should not be able to resume from ERROR", RecordingState.ERROR.canResume());
    }

    @Test
    public void testStateTransitionLogic() {
        // Test typical workflow
        RecordingState currentState = RecordingState.IDLE;
        assertTrue("Should be able to start from initial state", currentState.canStartRecording());
        
        currentState = RecordingState.LISTENING;
        assertTrue("Should be active when listening", currentState.isActive());
        assertTrue("Should be able to pause when listening", currentState.canPause());
        
        currentState = RecordingState.PAUSED;
        assertFalse("Should not be active when paused", currentState.isActive());
        assertTrue("Should be able to resume when paused", currentState.canResume());
        
        currentState = RecordingState.ERROR;
        assertFalse("Should not be active in error state", currentState.isActive());
        assertTrue("Should be able to start new recording after error", currentState.canStartRecording());
    }
}