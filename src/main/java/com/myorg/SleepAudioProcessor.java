package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sleep Audio Processor Lambda Function
 * 
 * This is a basic Lambda function skeleton that serves as a placeholder for future
 * audio processing, metadata enrichment, or validation logic.
 * 
 * Current functionality:
 * - Receives input from Step Functions (S3 event details, audioId)
 * - Logs the input for debugging and monitoring
 * - Performs basic validation
 * - Checks file extensions for supported audio formats
 * - Returns success response with metadata
 * - Uses structured JSON logging for improved observability (Issue #10)
 * 
 * Future enhancements:
 * - Extract audio metadata (duration, format, bitrate)
 * - Validate audio file format and quality
 * - Enrich metadata with additional information
 * - Update DynamoDB with enriched data
 * - Perform audio transformations
 * 
 * Environment Variables:
 * - TABLE_NAME: DynamoDB table name for storing metadata
 */
public class SleepAudioProcessor implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String tableName;
    private final ObjectMapper objectMapper;

    /**
     * Constructor - initializes with environment variables
     */
    public SleepAudioProcessor() {
        this.tableName = System.getenv("TABLE_NAME");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lambda handler method
     * 
     * @param input Input from Step Functions containing S3 event details
     * @param context Lambda execution context
     * @return Response map with processing status and metadata
     */
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        long startTime = System.currentTimeMillis();
        String requestId = context.getRequestId();
        
        try {
            // Structured logging - request started
            logStructured(logger, "INFO", "Processing started", Map.of(
                "requestId", requestId,
                "input", input,
                "tableName", tableName,
                "timestamp", System.currentTimeMillis()
            ));

            // Extract input parameters
            String bucket = (String) input.get("bucket");
            String key = (String) input.get("key");
            String audioId = (String) input.get("audioId");
            String eventTime = (String) input.get("eventTime");

            // Structured logging - parameters extracted
            logStructured(logger, "INFO", "Parameters extracted", Map.of(
                "requestId", requestId,
                "bucket", bucket != null ? bucket : "null",
                "key", key != null ? key : "null",
                "audioId", audioId != null ? audioId : "null"
            ));

            if (bucket == null || bucket.isEmpty()) {
                logStructured(logger, "ERROR", "Validation failed", Map.of(
                    "requestId", requestId,
                    "error", "Bucket name is required",
                    "timestamp", System.currentTimeMillis()
                ));
                throw new IllegalArgumentException("Bucket name is required");
            }

            if (key == null || key.isEmpty()) {
                logStructured(logger, "ERROR", "Validation failed", Map.of(
                    "requestId", requestId,
                    "error", "Object key is required",
                    "timestamp", System.currentTimeMillis()
                ));
                throw new IllegalArgumentException("Object key is required");
            }

            
            // Validate file extension
            boolean validExt = key.endsWith(".mp3") || key.endsWith(".wav") || key.endsWith(".m4a") || key.endsWith(".flac") || key.endsWith(".ogg");
            if (!validExt) {
                String error = "Unsupported file type. Use .mp3, .wav, .m4a, .flac, or .ogg";
                logStructured(logger, "ERROR", "Validation failed", Map.of(
                    "requestId", requestId,
                    "error", error,
                    "key", key,
                    "timestamp", System.currentTimeMillis()
                ));
                throw new IllegalArgumentException(error);
            }
            
            // Structured logging - validation passed
            logStructured(logger, "INFO", "Validation passed", Map.of(
                "requestId", requestId,
                "s3Location", String.format("s3://%s/%s", bucket, key),
                "audioId", audioId
            ));

            // Placeholder for future processing logic:
            // - Validate audio file format
            // - Extract metadata (duration, bitrate, etc.)
            // - Perform transformations
            // - Update DynamoDB with enriched metadata

            // Build success response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("audioId", audioId);
            response.put("bucket", bucket);
            response.put("key", key);
            response.put("processedAt", System.currentTimeMillis());
            response.put("message", "Audio file processed successfully");

            long duration = System.currentTimeMillis() - startTime;
            
            // Structured logging - success
            logStructured(logger, "INFO", "Processing completed successfully", Map.of(
                "requestId", requestId,
                "status", "SUCCESS",
                "audioId", audioId,
                "durationMs", duration,
                "timestamp", System.currentTimeMillis()
            ));
            
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Structured logging - error
            logStructured(logger, "ERROR", "Processing failed", Map.of(
                "requestId", requestId,
                "error", e.getMessage(),
                "errorType", e.getClass().getSimpleName(),
                "durationMs", duration,
                "timestamp", System.currentTimeMillis()
            ));
            
            throw new RuntimeException("Audio processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method for structured JSON logging
     */
    private void logStructured(LambdaLogger logger, String level, String message, Map<String, Object> data) {
        try {
            Map<String, Object> logEntry = new HashMap<>(data);
            logEntry.put("level", level);
            logEntry.put("message", message);
            logger.log(objectMapper.writeValueAsString(logEntry) + "\n");
        } catch (JsonProcessingException e) {
            // Fallback to simple logging if JSON serialization fails
            logger.log(String.format("[%s] %s: %s\n", level, message, data.toString()));
        }
    }
}
