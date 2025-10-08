package com.example.bridge.utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.bridge.R;
import com.example.bridge.models.RecordingState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TranscriptionStateManager.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TranscriptionStateManagerTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private TextView mockStateTextView;
    
    @Mock
    private ImageView mockMicIcon;
    
    @Mock
    private ImageView mockPauseButton;
    
    @Mock
    private View mockMicGlow;

    private TranscriptionStateManager stateManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock view methods that are called during initialization
        when(mockStateTextView.getCurrentTextColor()).thenReturn(0xFF000000);
        when(mockMicGlow.getAlpha()).thenReturn(0.0f);
        
        // Mock ContextCompat.getColor calls
        try (MockedStatic<ContextCompat> mockedContextCompat = mockStatic(ContextCompat.class)) {
            mockedContextCompat.when(() -> ContextCompat.getColor(any(Context.class), anyInt()))
                    .thenReturn(0xFF000000); // Return black color for all color requests
            
            stateManager = new TranscriptionStateManager(mockContext, mockStateTextView, 
                    mockMicIcon, mockPauseButton, mockMicGlow);
        }
    }

    @Test
    public void testInitialState() {
        assertEquals(RecordingState.IDLE, stateManager.getCurrentState());
        assertTrue(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testStateTransitionToListening() {
        stateManager.updateState(RecordingState.LISTENING);
        
        assertEquals(RecordingState.LISTENING, stateManager.getCurrentState());
        verify(mockStateTextView).setText("Listening…");
        verify(mockPauseButton).setImageResource(R.drawable.pause);
        
        assertFalse(stateManager.canStartRecording());
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testStateTransitionToPaused() {
        stateManager.updateState(RecordingState.PAUSED);
        
        assertEquals(RecordingState.PAUSED, stateManager.getCurrentState());
        verify(mockStateTextView).setText("Paused");
        verify(mockPauseButton).setImageResource(R.drawable.play);
        
        assertFalse(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertTrue(stateManager.canResume());
    }

    @Test
    public void testStateTransitionToProcessing() {
        stateManager.updateState(RecordingState.PROCESSING);
        
        assertEquals(RecordingState.PROCESSING, stateManager.getCurrentState());
        verify(mockStateTextView).setText("Processing…");
        verify(mockPauseButton).setImageResource(R.drawable.pause);
        
        assertFalse(stateManager.canStartRecording());
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testStateTransitionToError() {
        stateManager.updateState(RecordingState.ERROR);
        
        assertEquals(RecordingState.ERROR, stateManager.getCurrentState());
        verify(mockStateTextView).setText("Error occurred");
        verify(mockPauseButton).setImageResource(R.drawable.play);
        
        assertTrue(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testNoStateChangeWhenSameState() {
        stateManager.updateState(RecordingState.IDLE);
        
        // Should not trigger any UI updates since state hasn't changed
        verify(mockStateTextView, never()).setText(anyString());
        verify(mockPauseButton, never()).setImageResource(anyInt());
    }

    @Test
    public void testIndicateProcessingFromListeningState() {
        // First set to listening state
        stateManager.updateState(RecordingState.LISTENING);
        reset(mockStateTextView, mockPauseButton); // Reset mocks to track only processing indication
        
        stateManager.indicateProcessing();
        
        // Should briefly show processing state
        verify(mockStateTextView).setText("Processing…");
        assertEquals(RecordingState.PROCESSING, stateManager.getCurrentState());
    }

    @Test
    public void testIndicateProcessingFromNonListeningState() {
        // Set to idle state
        stateManager.updateState(RecordingState.IDLE);
        reset(mockStateTextView, mockPauseButton);
        
        stateManager.indicateProcessing();
        
        // Should not change state when not in listening mode
        verify(mockStateTextView, never()).setText("Processing…");
        assertEquals(RecordingState.IDLE, stateManager.getCurrentState());
    }

    @Test
    public void testSyncWithAudioLevelInListeningState() {
        stateManager.updateState(RecordingState.LISTENING);
        
        float audioLevel = 0.5f;
        stateManager.syncWithAudioLevel(audioLevel);
        
        // Should modulate glow intensity based on audio level
        verify(mockMicGlow).setAlpha(anyFloat());
    }

    @Test
    public void testSyncWithAudioLevelInIdleState() {
        stateManager.updateState(RecordingState.IDLE);
        reset(mockMicGlow);
        
        float audioLevel = 0.5f;
        stateManager.syncWithAudioLevel(audioLevel);
        
        // Should not modulate glow when not in active state
        verify(mockMicGlow, never()).setAlpha(anyFloat());
    }

    @Test
    public void testStateTransitionSequence() {
        // Test a typical recording session sequence
        
        // Start recording
        stateManager.updateState(RecordingState.LISTENING);
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canStartRecording());
        
        // Pause recording
        stateManager.updateState(RecordingState.PAUSED);
        assertTrue(stateManager.canResume());
        assertFalse(stateManager.canPause());
        
        // Resume recording
        stateManager.updateState(RecordingState.LISTENING);
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canResume());
        
        // Stop recording
        stateManager.updateState(RecordingState.IDLE);
        assertTrue(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testErrorRecoverySequence() {
        // Start in listening state
        stateManager.updateState(RecordingState.LISTENING);
        
        // Error occurs
        stateManager.updateState(RecordingState.ERROR);
        assertTrue(stateManager.canStartRecording());
        
        // Recover to listening
        stateManager.updateState(RecordingState.LISTENING);
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canStartRecording());
    }

    @Test
    public void testCleanup() {
        stateManager.updateState(RecordingState.LISTENING);
        stateManager.cleanup();
        
        assertEquals(RecordingState.IDLE, stateManager.getCurrentState());
    }

    @Test
    public void testButtonStateUpdates() {
        // Test all states that should show play button
        RecordingState[] playButtonStates = {
            RecordingState.IDLE, 
            RecordingState.PAUSED, 
            RecordingState.ERROR
        };
        
        for (RecordingState state : playButtonStates) {
            stateManager.updateState(state);
            verify(mockPauseButton).setImageResource(R.drawable.play);
            reset(mockPauseButton);
        }
        
        // Test states that should show pause button
        RecordingState[] pauseButtonStates = {
            RecordingState.LISTENING, 
            RecordingState.PROCESSING
        };
        
        for (RecordingState state : pauseButtonStates) {
            stateManager.updateState(state);
            verify(mockPauseButton).setImageResource(R.drawable.pause);
            reset(mockPauseButton);
        }
    }
}