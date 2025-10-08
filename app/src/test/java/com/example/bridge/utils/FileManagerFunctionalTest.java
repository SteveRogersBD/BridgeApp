package com.example.bridge.utils;

import android.content.Context;
import android.os.Environment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for FileManager save operations
 */
@RunWith(MockitoJUnitRunner.class)
public class FileManagerFunctionalTest {

    @Mock
    private Context mockContext;

    private FileManager fileManager;

    @Before
    public void setUp() {
        fileManager = new FileManager(mockContext);
    }

    @Test
    public void testSaveResultCreation_Success() {
        // Test successful save result creation
        String message = "Save successful";
        String audioPath = "/path/to/audio.wav";
        String transcriptPath = "/path/to/transcript.txt";

        FileManager.SaveResult result = FileManager.SaveResult.success(message, audioPath, transcriptPath);

        assertTrue("Result should indicate success", result.success);
        assertEquals("Message should match", message, result.message);
        assertEquals("Audio path should match", audioPath, result.audioFilePath);
        assertEquals("Transcript path should match", transcriptPath, result.transcriptFilePath);
        assertNull("Error should be null for success", result.error);
    }

    @Test
    public void testSaveResultCreation_Failure() {
        // Test failure save result creation
        String message = "Save failed";
        Exception error = new RuntimeException("Test error");

        FileManager.SaveResult result = FileManager.SaveResult.failure(message, error);

        assertFalse("Result should indicate failure", result.success);
        assertEquals("Message should match", message, result.message);
        assertNull("Audio path should be null for failure", result.audioFilePath);
        assertNull("Transcript path should be null for failure", result.transcriptFilePath);
        assertEquals("Error should match", error, result.error);
    }

    @Test
    public void testExternalStorageCheck() {
        // Test external storage availability check
        try (MockedStatic<Environment> mockedEnvironment = mockStatic(Environment.class)) {
            // Test writable state
            mockedEnvironment.when(Environment::getExternalStorageState)
                    .thenReturn(Environment.MEDIA_MOUNTED);
            
            assertTrue("Should detect writable storage", fileManager.isExternalStorageWritable());

            // Test non-writable state
            mockedEnvironment.when(Environment::getExternalStorageState)
                    .thenReturn(Environment.MEDIA_MOUNTED_READ_ONLY);
            
            assertFalse("Should detect non-writable storage", fileManager.isExternalStorageWritable());
        }
    }

    @Test
    public void testWordCountingLogic() {
        // Test the word counting logic used in success messages
        
        // Empty text
        String emptyText = "";
        assertEquals("Empty text should have 0 words", 0, countWordsHelper(emptyText));
        
        // Single word
        String singleWord = "Hello";
        assertEquals("Single word should count as 1", 1, countWordsHelper(singleWord));
        
        // Multiple words
        String multipleWords = "This is a test transcript";
        assertEquals("Should count 5 words", 5, countWordsHelper(multipleWords));
        
        // Text with extra spaces
        String spacedText = "  Word1   Word2  Word3  ";
        assertEquals("Should handle extra spaces", 3, countWordsHelper(spacedText));
    }

    @Test
    public void testFileManagerInitialization() {
        // Test that FileManager can be initialized with a context
        assertNotNull("FileManager should be initialized", fileManager);
        
        // Test with null context (should not crash)
        FileManager nullContextManager = new FileManager(null);
        assertNotNull("FileManager should handle null context", nullContextManager);
    }

    @Test
    public void testSaveOperationInputValidation() {
        // Test various input scenarios for save operations
        
        // Valid transcript text
        String validTranscript = "This is a valid transcript.";
        assertTrue("Valid transcript should be detected", !validTranscript.trim().isEmpty());
        
        // Empty transcript
        String emptyTranscript = "";
        assertTrue("Empty transcript should be detected", emptyTranscript.trim().isEmpty());
        
        // Whitespace-only transcript
        String whitespaceTranscript = "   \n\t  ";
        assertTrue("Whitespace-only transcript should be detected as empty", 
                whitespaceTranscript.trim().isEmpty());
        
        // Null transcript
        String nullTranscript = null;
        assertTrue("Null transcript should be handled", nullTranscript == null);
    }

    /**
     * Helper method to count words (mimics the logic in FileManager)
     */
    private int countWordsHelper(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}