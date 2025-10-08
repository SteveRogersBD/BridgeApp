package com.example.bridge.models;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the TranscriptSegment class.
 */
public class TranscriptSegmentTest {

    @Test
    public void testConstructorWithAllParameters() {
        long timestamp = System.currentTimeMillis();
        TranscriptSegment segment = new TranscriptSegment("Hello world", timestamp, true, 0.95f);
        
        assertEquals("Hello world", segment.getText());
        assertEquals(timestamp, segment.getTimestamp());
        assertTrue(segment.isFinal());
        assertEquals(0.95f, segment.getConfidence(), 0.001f);
    }

    @Test
    public void testConstructorWithoutTimestamp() {
        long beforeCreation = System.currentTimeMillis();
        TranscriptSegment segment = new TranscriptSegment("Test text", false, 0.8f);
        long afterCreation = System.currentTimeMillis();
        
        assertEquals("Test text", segment.getText());
        assertTrue("Timestamp should be between before and after creation", 
                   segment.getTimestamp() >= beforeCreation && segment.getTimestamp() <= afterCreation);
        assertFalse(segment.isFinal());
        assertEquals(0.8f, segment.getConfidence(), 0.001f);
    }

    @Test
    public void testSimpleConstructor() {
        TranscriptSegment segment = new TranscriptSegment("Simple text");
        
        assertEquals("Simple text", segment.getText());
        assertTrue(segment.isFinal());
        assertEquals(1.0f, segment.getConfidence(), 0.001f);
    }

    @Test
    public void testNullTextHandling() {
        TranscriptSegment segment = new TranscriptSegment(null, true, 0.9f);
        assertEquals("", segment.getText());
        
        segment.setText(null);
        assertEquals("", segment.getText());
    }

    @Test
    public void testConfidenceClampingInConstructor() {
        TranscriptSegment highSegment = new TranscriptSegment("text", true, 1.5f);
        assertEquals(1.0f, highSegment.getConfidence(), 0.001f);
        
        TranscriptSegment lowSegment = new TranscriptSegment("text", true, -0.5f);
        assertEquals(0.0f, lowSegment.getConfidence(), 0.001f);
    }

    @Test
    public void testConfidenceClampingInSetter() {
        TranscriptSegment segment = new TranscriptSegment("text");
        
        segment.setConfidence(1.5f);
        assertEquals(1.0f, segment.getConfidence(), 0.001f);
        
        segment.setConfidence(-0.5f);
        assertEquals(0.0f, segment.getConfidence(), 0.001f);
        
        segment.setConfidence(0.75f);
        assertEquals(0.75f, segment.getConfidence(), 0.001f);
    }

    @Test
    public void testHasContent() {
        assertTrue(new TranscriptSegment("Hello").hasContent());
        assertTrue(new TranscriptSegment("  Hello  ").hasContent());
        
        assertFalse(new TranscriptSegment("").hasContent());
        assertFalse(new TranscriptSegment("   ").hasContent());
        assertFalse(new TranscriptSegment(null).hasContent());
    }

    @Test
    public void testGetLength() {
        assertEquals(5, new TranscriptSegment("Hello").getLength());
        assertEquals(0, new TranscriptSegment("").getLength());
        assertEquals(0, new TranscriptSegment(null).getLength());
        assertEquals(3, new TranscriptSegment("   ").getLength());
    }

    @Test
    public void testHasHighConfidence() {
        assertTrue(new TranscriptSegment("text", true, 0.8f).hasHighConfidence());
        assertTrue(new TranscriptSegment("text", true, 0.95f).hasHighConfidence());
        assertTrue(new TranscriptSegment("text", true, 1.0f).hasHighConfidence());
        
        assertFalse(new TranscriptSegment("text", true, 0.79f).hasHighConfidence());
        assertFalse(new TranscriptSegment("text", true, 0.5f).hasHighConfidence());
        assertFalse(new TranscriptSegment("text", true, 0.0f).hasHighConfidence());
    }

    @Test
    public void testSetters() {
        TranscriptSegment segment = new TranscriptSegment("original");
        
        segment.setText("updated");
        assertEquals("updated", segment.getText());
        
        long newTimestamp = System.currentTimeMillis() + 1000;
        segment.setTimestamp(newTimestamp);
        assertEquals(newTimestamp, segment.getTimestamp());
        
        segment.setFinal(false);
        assertFalse(segment.isFinal());
        
        segment.setConfidence(0.6f);
        assertEquals(0.6f, segment.getConfidence(), 0.001f);
    }

    @Test
    public void testEquals() {
        long timestamp = System.currentTimeMillis();
        TranscriptSegment segment1 = new TranscriptSegment("Hello", timestamp, true, 0.9f);
        TranscriptSegment segment2 = new TranscriptSegment("Hello", timestamp, true, 0.9f);
        TranscriptSegment segment3 = new TranscriptSegment("World", timestamp, true, 0.9f);
        
        assertEquals(segment1, segment2);
        assertNotEquals(segment1, segment3);
        assertNotEquals(segment1, null);
        assertNotEquals(segment1, "not a segment");
    }

    @Test
    public void testHashCode() {
        long timestamp = System.currentTimeMillis();
        TranscriptSegment segment1 = new TranscriptSegment("Hello", timestamp, true, 0.9f);
        TranscriptSegment segment2 = new TranscriptSegment("Hello", timestamp, true, 0.9f);
        
        assertEquals(segment1.hashCode(), segment2.hashCode());
    }

    @Test
    public void testToString() {
        TranscriptSegment segment = new TranscriptSegment("Hello", 12345L, true, 0.9f);
        String result = segment.toString();
        
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("12345"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("0.9"));
    }
}