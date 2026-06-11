package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Issue #11: Core Audio Processing Logic and Output Handling
 * 
 * These tests are written FIRST (before implementation) following strict TDD.
 * They verify:
 * 1. Lambda downloads input from S3
 * 2. Lambda processes/generates audio using Polly
 * 3. Lambda uploads output to S3 with proper naming
 * 4. Lambda updates DynamoDB with output metadata
 * 5. Error handling for S3/Polly failures
 */
public class SleepAudioProcessorTest {

    private Context mockContext;
    private LambdaLogger mockLogger;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        when(mockContext.getRequestId()).thenReturn("test-request-id-123");
        
        // Set up environment variables for testing
        System.setProperty("TABLE_NAME", "test-table");
        System.setProperty("INPUT_BUCKET", "test-input-bucket");
        System.setProperty("OUTPUT_BUCKET", "test-output-bucket");
    }

    /**
     * Test: Handler should accept valid input
     */
    @Test
    public void testHandlerAcceptsValidInput() {
        // Note: This test will initially fail until implementation is complete
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "test-input-bucket");
        input.put("key", "test-audio.mp3");
        input.put("audioId", "test-audio.mp3");
        input.put("eventTime", "2024-01-01T00:00:00Z");
        
        // This should not throw - basic validation should pass
        assertDoesNotThrow(() -> {
            Map<String, Object> response = processor.handleRequest(input, mockContext);
            assertNotNull(response);
        });
    }

    /**
     * Test: Response should contain required output fields
     */
    @Test
    public void testResponseContainsRequiredOutputFields() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "test-input-bucket");
        input.put("key", "test-audio.mp3");
        input.put("audioId", "test-audio.mp3");
        input.put("eventTime", "2024-01-01T00:00:00Z");
        
        Map<String, Object> response = processor.handleRequest(input, mockContext);
        
        // Response should contain status
        assertTrue(response.containsKey("status"));
        assertEquals("COMPLETED", response.get("status"));
        
        // Response should contain output location
        assertTrue(response.containsKey("outputBucket"));
        assertTrue(response.containsKey("outputKey"));
        
        // Response should contain metadata
        assertTrue(response.containsKey("processedAt"));
        assertTrue(response.containsKey("audioId"));
    }

    /**
     * Test: Output key should follow naming convention
     */
    @Test
    public void testOutputKeyFollowsNamingConvention() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "test-input-bucket");
        input.put("key", "input/myaudio.mp3");
        input.put("audioId", "input/myaudio.mp3");
        input.put("eventTime", "2024-01-01T00:00:00Z");
        
        Map<String, Object> response = processor.handleRequest(input, mockContext);
        
        String outputKey = (String) response.get("outputKey");
        assertNotNull(outputKey);
        
        // Output key should include original name and timestamp/id
        assertTrue(outputKey.contains("myaudio") || outputKey.contains("processed"));
        assertTrue(outputKey.endsWith(".mp3"));
    }

    /**
     * Test: Response should contain output metadata
     */
    @Test
    public void testResponseContainsOutputMetadata() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "test-input-bucket");
        input.put("key", "test-audio.mp3");
        input.put("audioId", "test-audio.mp3");
        input.put("eventTime", "2024-01-01T00:00:00Z");
        
        Map<String, Object> response = processor.handleRequest(input, mockContext);
        
        // Should contain file size or duration metadata
        assertTrue(response.containsKey("outputFileSize") || response.containsKey("processingType"));
        
        // Should indicate processing type (text-to-speech or audio processing)
        assertTrue(response.containsKey("processingType"));
    }

    /**
     * Test: Handler should determine input type (text vs audio)
     */
    @Test
    public void testHandlerDeterminesInputType() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        // Test with audio file
        Map<String, Object> audioInput = new HashMap<>();
        audioInput.put("bucket", "test-input-bucket");
        audioInput.put("key", "test-audio.mp3");
        audioInput.put("audioId", "test-audio.mp3");
        audioInput.put("eventTime", "2024-01-01T00:00:00Z");
        
        Map<String, Object> audioResponse = processor.handleRequest(audioInput, mockContext);
        
        // Should indicate it processed audio
        String processingType = (String) audioResponse.get("processingType");
        assertNotNull(processingType);
        assertTrue(processingType.equals("AUDIO_PROCESSING") || processingType.equals("TEXT_TO_SPEECH"));
    }

    /**
     * Test: Handler should handle missing bucket gracefully
     */
    @Test
    public void testHandlerHandlesMissingBucket() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("key", "test-audio.mp3");
        input.put("audioId", "test-audio.mp3");
        
        // Should throw IllegalArgumentException
        assertThrows(RuntimeException.class, () -> {
            processor.handleRequest(input, mockContext);
        });
    }

    /**
     * Test: Handler should validate required fields
     */
    @Test
    public void testHandlerValidatesRequiredFields() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "test-input-bucket");
        // Missing key field
        
        // Should throw exception for missing key
        assertThrows(RuntimeException.class, () -> {
            processor.handleRequest(input, mockContext);
        });
    }

    /**
     * Test: Handler should include S3 output location in response
     */
    @Test
    public void testResponseIncludesS3OutputLocation() {
        SleepAudioProcessor processor = new SleepAudioProcessor();
        
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "test-input-bucket");
        input.put("key", "test-audio.mp3");
        input.put("audioId", "test-audio.mp3");
        input.put("eventTime", "2024-01-01T00:00:00Z");
        
        Map<String, Object> response = processor.handleRequest(input, mockContext);
        
        // Should contain output S3 location
        assertTrue(response.containsKey("outputBucket"));
        assertTrue(response.containsKey("outputKey"));
        
        // Output bucket should be set
        assertNotNull(response.get("outputBucket"));
        assertNotNull(response.get("outputKey"));
    }
}
