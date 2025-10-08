package com.example.bridge.integration;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.bridge.R;
import com.example.bridge.models.RecordingState;
import com.example.bridge.utils.TranscriptionStateManager;

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
 * Integration test for visual feedback and state management coordination.
 * Tests the complete flow of state transitions and visual feedback updates.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class VisualFeedbackIntegrationTest {

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
        
        // Mock view methods
        when(mockStateTextView.getCurrentTextColor()).thenReturn(0xFF000000);
        when(mockMicGlow.getAlpha()).thenReturn(0.0f);
        
        // Mock ContextCompat.getColor calls
        try (MockedStatic<ContextCompat> mockedContextCompat = mockStatic(ContextCompat.class)) {
            mockedContextCompat.when(() -> ContextCompat.getColor(any(Context.class), anyInt()))
                    .thenReturn(0xFF000000);
            
            stateManager = new TranscriptionStateManager(mockContext, mockStateTextView, 
                    mockMicIcon, mockPauseButton, mockMicGlow);
        }
    }

    @Test
    public void testCompleteRecordingSessionFlow() {
        // Test complete recording session with visual feedback coordination
        
        // 1. Start recording - should transition to listening state
        stateManager.updateState(RecordingState.LISTENING);
        
        verify(mockStateTextView).setText("Listening…");
        verify(mockPauseButton).setImageResource(R.drawable.pause);
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canStartRecording());
        
        // 2. Simulate audio input with processing indication
        stateManager.syncWithAudioLevel(0.7f);
        stateManager.indicateProcessing();
        
        // Should briefly show processing state
        assertEquals(RecordingState.PROCESSING, stateManager.getCurrentState());
        
        // 3. Pause recording
        stateManager.updateState(RecordingState.PAUSED);
        
        verify(mockStateTextView).setText("Paused");
        verify(mockPauseButton, times(2)).setImageResource(R.drawable.play); // Called twice: once for initial, once for paused
        assertTrue(stateManager.canResume());
        assertFalse(stateManager.canPause());
        
        // 4. Resume recording
        stateManager.updateState(RecordingState.LISTENING);
        
        verify(mockStateTextView, times(2)).setText("Listening…"); // Called twice
        verify(mockPauseButton, times(2)).setImageResource(R.drawable.pause); // Called twice
        
        // 5. Error occurs
        stateManager.updateState(RecordingState.ERROR);
        
        verify(mockStateTextView).setText("Error occurred");
        verify(mockPauseButton, times(3)).setImageResource(R.drawable.play); // Called three times now
        assertTrue(stateManager.canStartRecording());
        
        // 6. Recover from error
        stateManager.updateState(RecordingState.LISTENING);
        
        // 7. Stop recording
        stateManager.updateState(RecordingState.IDLE);
        
        verify(mockStateTextView).setText("Ready to record");
        verify(mockPauseButton, times(4)).setImageResource(R.drawable.play); // Called four times now
        assertTrue(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testVisualFeedbackCoordination() {
        // Test that visual elements are coordinated properly
        
        // Start in listening state
        stateManager.updateState(RecordingState.LISTENING);
        
        // Simulate continuous audio input
        for (int i = 0; i < 5; i++) {
            float audioLevel = (float) Math.random();
            stateManager.syncWithAudioLevel(audioLevel);
            
            // Should update glow intensity based on audio level
            verify(mockMicGlow, atLeastOnce()).setAlpha(anyFloat());
        }
        
        // Simulate processing indication multiple times
        for (int i = 0; i < 3; i++) {
            stateManager.indicateProcessing();
            // Should show processing state briefly
            assertEquals(RecordingState.PROCESSING, stateManager.getCurrentState());
        }
    }

    @Test
    public void testStateTransitionValidation() {
        // Test that state transitions follow proper validation rules
        
        // Initially should be able to start recording
        assertTrue(stateManager.canStartRecording());
        
        // Start recording
        stateManager.updateState(RecordingState.LISTENING);
        assertFalse(stateManager.canStartRecording());
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canResume());
        
        // Pause
        stateManager.updateState(RecordingState.PAUSED);
        assertFalse(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertTrue(stateManager.canResume());
        
        // Resume
        stateManager.updateState(RecordingState.LISTENING);
        assertFalse(stateManager.canStartRecording());
        assertTrue(stateManager.canPause());
        assertFalse(stateManager.canResume());
        
        // Error state
        stateManager.updateState(RecordingState.ERROR);
        assertTrue(stateManager.canStartRecording());
        assertFalse(stateManager.canPause());
        assertFalse(stateManager.canResume());
    }

    @Test
    public void testAudioLevelSynchronization() {
        // Test audio level synchronization with visual feedback
        
        // Should not sync when idle
        stateManager.updateState(RecordingState.IDLE);
        reset(mockMicGlow);
        stateManager.syncWithAudioLevel(0.8f);
        verify(mockMicGlow, never()).setAlpha(anyFloat());
        
        // Should sync when listening
        stateManager.updateState(RecordingState.LISTENING);
        stateManager.syncWithAudioLevel(0.5f);
        verify(mockMicGlow, atLeastOnce()).setAlpha(anyFloat());
        
        // Should sync when processing
        stateManager.updateState(RecordingState.PROCESSING);
        reset(mockMicGlow);
        stateManager.syncWithAudioLevel(0.3f);
        verify(mockMicGlow, atLeastOnce()).setAlpha(anyFloat());
        
        // Should not sync when paused
        stateManager.updateState(RecordingState.PAUSED);
        reset(mockMicGlow);
        stateManager.syncWithAudioLevel(0.9f);
        verify(mockMicGlow, never()).setAlpha(anyFloat());
    }

    @Test
    public void testProcessingIndicationBehavior() {
        // Test processing indication only works in appropriate states
        
        // Should not indicate processing when idle
        stateManager.updateState(RecordingState.IDLE);
        RecordingState initialState = stateManager.getCurrentState();
        stateManager.indicateProcessing();
        assertEquals(initialState, stateManager.getCurrentState());
        
        // Should not indicate processing when paused
        stateManager.updateState(RecordingState.PAUSED);
        initialState = stateManager.getCurrentState();
        stateManager.indicateProcessing();
        assertEquals(initialState, stateManager.getCurrentState());
        
        // Should indicate processing when listening
        stateManager.updateState(RecordingState.LISTENING);
        stateManager.indicateProcessing();
        assertEquals(RecordingState.PROCESSING, stateManager.getCurrentState());
    }
}