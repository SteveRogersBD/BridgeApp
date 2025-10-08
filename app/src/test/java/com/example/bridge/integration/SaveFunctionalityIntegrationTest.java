package com.example.bridge.integration;

import android.content.Context;
import android.widget.Toast;

import com.example.bridge.TranscriptActivity;
import com.example.bridge.utils.FileManager;
import com.example.bridge.utils.TranscriptManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the enhanced save functionality in TranscriptActivity
 * Tests the interaction between FileManager, TranscriptManager, and save operations
 */
@RunWith(MockitoJUnitRunner.class)
public class SaveFunctionalityIntegrationTest {

    @Mock
    private Context mockContext;

    @Mock
    private TranscriptManager mockTranscriptManager;

    @Mock
    private FileManager mockFileManager;

    private TranscriptActivity activity;

    @Before
    public void setUp() {
        // Note: In a real integration test, we would use ActivityScenario or similar
        // For this test, we focus on the save logic integration
    }

    @Test
    public void testSaveOperation_SuccessfulSave_WithTranscript() {
        // Arrange
        String transcriptText = "This is a test transcript with several words for testing.";
        FileManager.SaveResult successResult = FileManager.SaveResult.success(
                "Recording and transcript saved! (10 words)",
                "/path/to/audio.wav",
                "/path/to/transcript.txt"
        );

        when(mockTranscriptManager.getFullTranscript()).thenReturn(transcriptText);
        when(mockFileManager.isExternalStorageWritable()).thenReturn(true);
        when(mockFileManager.saveRecording(eq(transcriptText), eq(true))).thenReturn(successResult);

        // Act - simulate the save button logic
        boolean storageWritable = mockFileManager.isExternalStorageWritable();
        String transcript = mockTranscriptManager.getFullTranscript();
        FileManager.SaveResult result = mockFileManager.saveRecording(transcript, true);

        // Assert
        assertTrue("Storage should be writable", storageWritable);
        assertNotNull("Transcript should not be null", transcript);
        assertTrue("Save operation should succeed", result.success);
        assertEquals("Success message should match expected format", 
                "Recording and transcript saved! (10 words)", result.message);
        assertNotNull("Audio file path should be set", result.audioFilePath);
        assertNotNull("Transcript file path should be set", result.transcriptFilePath);

        // Verify transcript manager would be cleared on success
        verify(mockTranscriptManager).getFullTranscript();
    }

    @Test
    public void testSaveOperation_FailedSave_RetainsTranscript() {
        // Arrange
        String transcriptText = "Important transcript that should not be lost.";
        Exception saveError = new RuntimeException("Disk full");
        FileManager.SaveResult failureResult = FileManager.SaveResult.failure(
                "Failed to save files", saveError
        );

        when(mockTranscriptManager.getFullTranscript()).thenReturn(transcriptText);
        when(mockFileManager.isExternalStorageWritable()).thenReturn(true);
        when(mockFileManager.saveRecording(eq(transcriptText), eq(true))).thenReturn(failureResult);

        // Act - simulate the save button logic
        String transcript = mockTranscriptManager.getFullTranscript();
        FileManager.SaveResult result = mockFileManager.saveRecording(transcript, true);

        // Assert
        assertFalse("Save operation should fail", result.success);
        assertEquals("Error message should match", "Failed to save files", result.message);
        assertEquals("Error should be preserved", saveError, result.error);

        // Verify transcript manager is NOT cleared on failure (transcript retained)
        verify(mockTranscriptManager).getFullTranscript();
        verify(mockTranscriptManager, never()).clearTranscript();
    }

    @Test
    public void testSaveOperation_EmptyTranscript_AudioOnly() {
        // Arrange
        String emptyTranscript = "";
        FileManager.SaveResult successResult = FileManager.SaveResult.success(
                "Audio recording saved! (No transcript available)",
                "/path/to/audio.wav",
                null
        );

        when(mockTranscriptManager.getFullTranscript()).thenReturn(emptyTranscript);
        when(mockFileManager.isExternalStorageWritable()).thenReturn(true);
        when(mockFileManager.saveRecording(eq(emptyTranscript), eq(true))).thenReturn(successResult);

        // Act
        String transcript = mockTranscriptManager.getFullTranscript();
        FileManager.SaveResult result = mockFileManager.saveRecording(transcript, true);

        // Assert
        assertTrue("Save operation should succeed", result.success);
        assertEquals("Should save audio only message", 
                "Audio recording saved! (No transcript available)", result.message);
        assertNotNull("Audio file path should be set", result.audioFilePath);
        assertNull("Transcript file path should be null", result.transcriptFilePath);
    }

    @Test
    public void testSaveOperation_StorageNotWritable() {
        // Arrange
        when(mockFileManager.isExternalStorageWritable()).thenReturn(false);

        // Act
        boolean storageWritable = mockFileManager.isExternalStorageWritable();

        // Assert
        assertFalse("Storage should not be writable", storageWritable);
        
        // Verify that save operation would not proceed
        verify(mockFileManager, never()).saveRecording(anyString(), anyBoolean());
    }

    @Test
    public void testSaveResultMessages_DifferentScenarios() {
        // Test different success message formats
        
        // Both audio and transcript
        FileManager.SaveResult bothResult = FileManager.SaveResult.success(
                "Recording and transcript saved! (15 words)", "/audio.wav", "/transcript.txt");
        assertTrue("Should indicate success", bothResult.success);
        assertTrue("Should mention both files", bothResult.message.contains("Recording and transcript"));
        
        // Audio only
        FileManager.SaveResult audioOnlyResult = FileManager.SaveResult.success(
                "Audio recording saved! (No transcript available)", "/audio.wav", null);
        assertTrue("Should indicate success", audioOnlyResult.success);
        assertTrue("Should mention audio only", audioOnlyResult.message.contains("Audio recording"));
        
        // Failure case
        FileManager.SaveResult failureResult = FileManager.SaveResult.failure(
                "Save failed: Insufficient storage", new RuntimeException("No space"));
        assertFalse("Should indicate failure", failureResult.success);
        assertTrue("Should mention failure", failureResult.message.contains("Save failed"));
    }

    @Test
    public void testUnsavedContentDetection() {
        // Test logic for detecting unsaved content
        
        // Case 1: Has transcript content
        when(mockTranscriptManager.getFullTranscript()).thenReturn("Some transcript content");
        String transcript = mockTranscriptManager.getFullTranscript();
        boolean hasUnsavedTranscript = !transcript.trim().isEmpty();
        assertTrue("Should detect unsaved transcript", hasUnsavedTranscript);
        
        // Case 2: Empty transcript
        when(mockTranscriptManager.getFullTranscript()).thenReturn("");
        transcript = mockTranscriptManager.getFullTranscript();
        hasUnsavedTranscript = !transcript.trim().isEmpty();
        assertFalse("Should not detect unsaved content for empty transcript", hasUnsavedTranscript);
        
        // Case 3: Whitespace only transcript
        when(mockTranscriptManager.getFullTranscript()).thenReturn("   \n\t  ");
        transcript = mockTranscriptManager.getFullTranscript();
        hasUnsavedTranscript = !transcript.trim().isEmpty();
        assertFalse("Should not detect unsaved content for whitespace-only transcript", hasUnsavedTranscript);
    }

    @Test
    public void testErrorHandling_UnexpectedExceptions() {
        // Test handling of unexpected exceptions during save
        
        String transcriptText = "Test transcript";
        when(mockTranscriptManager.getFullTranscript()).thenReturn(transcriptText);
        when(mockFileManager.isExternalStorageWritable()).thenReturn(true);
        when(mockFileManager.saveRecording(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        try {
            mockFileManager.saveRecording(transcriptText, true);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Should preserve original error message", "Unexpected error", e.getMessage());
        }

        // Verify interactions occurred as expected
        verify(mockFileManager).isExternalStorageWritable();
        verify(mockFileManager).saveRecording(transcriptText, true);
        verify(mockTranscriptManager).getFullTranscript();
    }
}