package com.example.bridge.models;

import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for the TranscriptSession class.
 */
public class TranscriptSessionTest {

    private TranscriptSession session;

    @Before
    public void setUp() {
        session = new TranscriptSession();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull("Session ID should not be null", session.getSessionId());
        assertFalse("Session ID should not be empty", session.getSessionId().isEmpty());
        assertTrue("Start time should be recent", 
                   System.currentTimeMillis() - session.getStartTime() < 1000);
        assertEquals(0, session.getDuration());
        assertEquals("", session.getFullTranscript());
        assertEquals("", session.getCurrentPartialText());
        assertFalse(session.isActive());
        assertTrue(session.getSegments().isEmpty());
    }

    @Test
    public void testConstructorWithSessionId() {
        String customId = "test-session-123";
        TranscriptSession customSession = new TranscriptSession(customId);
        
        assertEquals(customId, customSession.getSessionId());
        assertFalse(customSession.isActive());
    }

    @Test
    public void testConstructorWithNullSessionId() {
        TranscriptSession nullIdSession = new TranscriptSession(null);
        
        assertNotNull("Should generate ID when null provided", nullIdSession.getSessionId());
        assertFalse("Generated ID should not be empty", nullIdSession.getSessionId().isEmpty());
    }

    @Test
    public void testStartSession() {
        long beforeStart = System.currentTimeMillis();
        session.start();
        long afterStart = System.currentTimeMillis();
        
        assertTrue("Session should be active after start", session.isActive());
        assertTrue("Start time should be updated", 
                   session.getStartTime() >= beforeStart && session.getStartTime() <= afterStart);
        assertEquals("Duration should be reset to 0", 0, session.getDuration());
    }

    @Test
    public void testStopSession() throws InterruptedException {
        session.start();
        Thread.sleep(10); // Small delay to ensure duration > 0
        session.stop();
        
        assertFalse("Session should not be active after stop", session.isActive());
        assertTrue("Duration should be greater than 0", session.getDuration() > 0);
        assertEquals("Partial text should be cleared", "", session.getCurrentPartialText());
    }

    @Test
    public void testPauseAndResume() throws InterruptedException {
        session.start();
        Thread.sleep(10);
        
        session.pause();
        assertFalse("Session should not be active when paused", session.isActive());
        long pausedDuration = session.getDuration();
        assertTrue("Duration should be recorded when paused", pausedDuration > 0);
        
        Thread.sleep(10);
        session.resume();
        assertTrue("Session should be active when resumed", session.isActive());
        assertEquals("Duration should be preserved when resumed", pausedDuration, session.getDuration());
    }

    @Test
    public void testAddSegment() {
        TranscriptSegment segment1 = new TranscriptSegment("Hello", true, 0.9f);
        TranscriptSegment segment2 = new TranscriptSegment("world", false, 0.8f);
        
        session.addSegment(segment1);
        session.addSegment(segment2);
        
        assertEquals(2, session.getSegmentCount());
        assertEquals("Hello", session.getFullTranscript()); // Only final segments added to full transcript
        
        session.addSegment(null); // Should handle null gracefully
        assertEquals(2, session.getSegmentCount());
    }

    @Test
    public void testAppendToFullTranscript() {
        session.appendToFullTranscript("Hello");
        assertEquals("Hello", session.getFullTranscript());
        
        session.appendToFullTranscript("world");
        assertEquals("Hello world", session.getFullTranscript());
        
        session.appendToFullTranscript("  test  ");
        assertEquals("Hello world test", session.getFullTranscript());
        
        session.appendToFullTranscript(null); // Should handle null gracefully
        assertEquals("Hello world test", session.getFullTranscript());
        
        session.appendToFullTranscript(""); // Should handle empty string gracefully
        assertEquals("Hello world test", session.getFullTranscript());
    }

    @Test
    public void testClearTranscript() {
        session.appendToFullTranscript("Some text");
        session.setCurrentPartialText("Partial text");
        session.addSegment(new TranscriptSegment("Segment text"));
        
        session.clearTranscript();
        
        assertEquals("", session.getFullTranscript());
        assertEquals("", session.getCurrentPartialText());
        assertEquals(0, session.getSegmentCount());
    }

    @Test
    public void testGetElapsedTime() throws InterruptedException {
        // Test when not active
        assertEquals(0, session.getElapsedTime());
        
        // Test when active
        session.start();
        Thread.sleep(10);
        long elapsedWhileActive = session.getElapsedTime();
        assertTrue("Elapsed time should be positive when active", elapsedWhileActive > 0);
        
        // Test after pause
        session.pause();
        long elapsedAfterPause = session.getElapsedTime();
        assertEquals("Elapsed time should equal duration when paused", 
                     session.getDuration(), elapsedAfterPause);
    }

    @Test
    public void testGetFinalSegmentCount() {
        session.addSegment(new TranscriptSegment("Final 1", true, 0.9f));
        session.addSegment(new TranscriptSegment("Partial", false, 0.8f));
        session.addSegment(new TranscriptSegment("Final 2", true, 0.95f));
        
        assertEquals(3, session.getSegmentCount());
        assertEquals(2, session.getFinalSegmentCount());
    }

    @Test
    public void testHasContent() {
        assertFalse("Empty session should not have content", session.hasContent());
        
        session.appendToFullTranscript("Some text");
        assertTrue("Session with transcript should have content", session.hasContent());
        
        session.clearTranscript();
        session.setCurrentPartialText("Partial");
        assertTrue("Session with partial text should have content", session.hasContent());
        
        session.setCurrentPartialText("");
        session.addSegment(new TranscriptSegment("Segment text"));
        assertTrue("Session with segments should have content", session.hasContent());
    }

    @Test
    public void testGetCharacterCount() {
        assertEquals(0, session.getCharacterCount());
        
        session.appendToFullTranscript("Hello");
        assertEquals(5, session.getCharacterCount());
        
        session.appendToFullTranscript("world");
        assertEquals(11, session.getCharacterCount()); // "Hello world"
    }

    @Test
    public void testGetWordCount() {
        assertEquals(0, session.getWordCount());
        
        session.appendToFullTranscript("Hello world");
        assertEquals(2, session.getWordCount());
        
        session.appendToFullTranscript("this is a test");
        assertEquals(6, session.getWordCount()); // "Hello world this is a test"
    }

    @Test
    public void testSetters() {
        session.setSessionId("new-id");
        assertEquals("new-id", session.getSessionId());
        
        session.setSessionId(null); // Should not change when null
        assertEquals("new-id", session.getSessionId());
        
        long newTime = System.currentTimeMillis() + 5000;
        session.setStartTime(newTime);
        assertEquals(newTime, session.getStartTime());
        
        session.setDuration(1000);
        assertEquals(1000, session.getDuration());
        
        session.setDuration(-500); // Should clamp to 0
        assertEquals(0, session.getDuration());
        
        session.setCurrentPartialText("Partial");
        assertEquals("Partial", session.getCurrentPartialText());
        
        session.setCurrentPartialText(null); // Should handle null
        assertEquals("", session.getCurrentPartialText());
    }

    @Test
    public void testSetActive() {
        assertFalse(session.isActive());
        
        session.setActive(true);
        assertTrue(session.isActive());
        assertTrue("Start time should be updated when activating", 
                   System.currentTimeMillis() - session.getStartTime() < 1000);
        
        session.setActive(false);
        assertFalse(session.isActive());
    }

    @Test
    public void testGetSegmentsReturnsDefensiveCopy() {
        TranscriptSegment segment = new TranscriptSegment("Test");
        session.addSegment(segment);
        
        List<TranscriptSegment> segments = session.getSegments();
        segments.clear(); // Try to modify the returned list
        
        assertEquals("Original segments should not be affected", 1, session.getSegmentCount());
    }

    @Test
    public void testToString() {
        session.setSessionId("test-123");
        session.addSegment(new TranscriptSegment("Test"));
        session.appendToFullTranscript("Hello world");
        
        String result = session.toString();
        
        assertTrue("Result should contain session ID", result.contains("test-123"));
        assertTrue("Result should contain segment count", result.contains("segmentCount=1"));
        assertTrue("Result should contain character count", 
                   result.contains("characterCount=" + session.getCharacterCount()));
    }
}