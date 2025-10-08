package com.example.bridge;

import android.content.Context;

import com.example.bridge.utils.TranscriptManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for lifecycle management functionality in TranscriptActivity
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LifecycleManagementTest {

    private TranscriptActivity activity;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        activity = new TranscriptActivity();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void callPrivateMethod(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
            // Handle primitive types
            if (paramTypes[i] == Boolean.class) paramTypes[i] = boolean.class;
        }
        
        Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    @Test
    public void testLifecycleStateTracking() throws Exception {
        // Test initial state
        assertFalse((Boolean) getPrivateField(activity, "isInBackground"));
        assertFalse((Boolean) getPrivateField(activity, "wasAutoPaused"));

        // Test setting background state directly
        setPrivateField(activity, "isInBackground", true);
        assertTrue((Boolean) getPrivateField(activity, "isInBackground"));

        // Test setting foreground state directly
        setPrivateField(activity, "isInBackground", false);
        assertFalse((Boolean) getPrivateField(activity, "isInBackground"));
    }

    @Test
    public void testSetAutoPausedFlag_UpdatesFlag() throws Exception {
        // Act
        callPrivateMethod(activity, "setAutoPausedFlag", true);

        // Assert
        assertTrue((Boolean) getPrivateField(activity, "wasAutoPaused"));
        
        // Test clearing the flag
        callPrivateMethod(activity, "setAutoPausedFlag", false);
        assertFalse((Boolean) getPrivateField(activity, "wasAutoPaused"));
    }

    @Test
    public void testHasUnsavedContent_WithActiveRecording_ReturnsTrue() throws Exception {
        // Arrange
        setPrivateField(activity, "isRecording", true);

        // Act
        boolean result = (Boolean) callPrivateMethodWithReturn(activity, "hasUnsavedContent");

        // Assert
        assertTrue(result);
    }

    @Test
    public void testHasUnsavedContent_WithAutoPausedRecording_ReturnsTrue() throws Exception {
        // Arrange
        setPrivateField(activity, "wasAutoPaused", true);

        // Act
        boolean result = (Boolean) callPrivateMethodWithReturn(activity, "hasUnsavedContent");

        // Assert
        assertTrue(result);
    }

    @Test
    public void testHasUnsavedContent_WithTranscriptText_ReturnsTrue() throws Exception {
        // Arrange - Create a mock transcript manager that returns content
        TranscriptManager mockTranscriptManager = mock(TranscriptManager.class);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("Some transcript text");
        setPrivateField(activity, "transcriptManager", mockTranscriptManager);

        // Act
        boolean result = (Boolean) callPrivateMethodWithReturn(activity, "hasUnsavedContent");

        // Assert
        assertTrue(result);
    }

    @Test
    public void testHasUnsavedContent_WithNoContent_ReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(activity, "isRecording", false);
        setPrivateField(activity, "wasAutoPaused", false);
        
        // Create a mock transcript manager that returns empty content
        TranscriptManager mockTranscriptManager = mock(TranscriptManager.class);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("");
        setPrivateField(activity, "transcriptManager", mockTranscriptManager);

        // Act
        boolean result = (Boolean) callPrivateMethodWithReturn(activity, "hasUnsavedContent");

        // Assert
        assertFalse(result);
    }

    @Test
    public void testGetUnsavedContentMessage_WithRecordingAndTranscript() throws Exception {
        // Arrange
        setPrivateField(activity, "isRecording", true);
        TranscriptManager mockTranscriptManager = mock(TranscriptManager.class);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("Some text");
        setPrivateField(activity, "transcriptManager", mockTranscriptManager);

        // Act
        String message = (String) callPrivateMethodWithReturn(activity, "getUnsavedContentMessage");

        // Assert
        assertEquals("You have an unsaved recording and transcript.", message);
    }

    @Test
    public void testGetUnsavedContentMessage_WithRecordingOnly() throws Exception {
        // Arrange
        setPrivateField(activity, "isRecording", true);
        TranscriptManager mockTranscriptManager = mock(TranscriptManager.class);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("");
        setPrivateField(activity, "transcriptManager", mockTranscriptManager);

        // Act
        String message = (String) callPrivateMethodWithReturn(activity, "getUnsavedContentMessage");

        // Assert
        assertEquals("You have an unsaved recording.", message);
    }

    @Test
    public void testGetUnsavedContentMessage_WithTranscriptOnly() throws Exception {
        // Arrange
        setPrivateField(activity, "isRecording", false);
        setPrivateField(activity, "wasAutoPaused", false);
        TranscriptManager mockTranscriptManager = mock(TranscriptManager.class);
        when(mockTranscriptManager.getFullTranscript()).thenReturn("Some text");
        setPrivateField(activity, "transcriptManager", mockTranscriptManager);

        // Act
        String message = (String) callPrivateMethodWithReturn(activity, "getUnsavedContentMessage");

        // Assert
        assertEquals("You have an unsaved transcript.", message);
    }

    @Test
    public void testPerformResourceCleanup_CleansAllResources() throws Exception {
        // Arrange - Set up some resources
        setPrivateField(activity, "timerHandler", mock(android.os.Handler.class));

        // Act - Should not throw exception
        callPrivateMethod(activity, "performResourceCleanup");

        // Assert - Verify cleanup completed without errors
        assertNull(getPrivateField(activity, "timerHandler"));
        assertNull(getPrivateField(activity, "binding"));
    }

    // Helper method to call private methods that return values
    private Object callPrivateMethodWithReturn(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
            if (paramTypes[i] == Boolean.class) paramTypes[i] = boolean.class;
        }
        
        Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}