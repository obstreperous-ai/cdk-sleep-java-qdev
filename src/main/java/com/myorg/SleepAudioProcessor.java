package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

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

    /**
     * Constructor - initializes with environment variables
     */
    public SleepAudioProcessor() {
        this.tableName = System.getenv("TABLE_NAME");
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
        
        try {
            // Log incoming request
            logger.log("Processing audio file with input: " + input.toString());
            logger.log("DynamoDB table name: " + tableName);

            // Extract input parameters
            String bucket = (String) input.get("bucket");
            String key = (String) input.get("key");
            String audioId = (String) input.get("audioId");
            String eventTime = (String) input.get("eventTime");

            // Basic validation
            // Validate required fields
                throw new IllegalArgumentException("Bucket name is required");
                String error = "Bucket name is required";
                logger.log("ERROR: " + error);
                throw new IllegalArgumentException(error);
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Object key is required");
                String error = "Object key is required";
                logger.log("ERROR: " + error);
                throw new IllegalArgumentException(error);

            
            // Validate file extension
            boolean validExt = key.endsWith(".mp3") || key.endsWith(".wav") || key.endsWith(".m4a") || key.endsWith(".flac") || key.endsWith(".ogg");
            if (!validExt) {
                String error = "Unsupported file type. Use .mp3, .wav, .m4a, .flac, or .ogg";
                logger.log("ERROR: " + error);
                throw new IllegalArgumentException(error);
            }
            
            logger.log("Validation passed for key: " + key);
            logger.log(String.format("Processing file: s3://%s/%s (audioId: %s)", bucket, key, audioId));

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

            logger.log("Processing completed successfully");
            return response;

        } catch (Exception e) {
            logger.log("Error processing audio file: " + e.getMessage());
            throw new RuntimeException("Audio processing failed: " + e.getMessage(), e);
        }
    }
}
