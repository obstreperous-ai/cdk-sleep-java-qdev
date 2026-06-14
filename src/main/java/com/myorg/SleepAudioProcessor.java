package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.Map;

public class SleepAudioProcessor implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        if (event == null) {
            throw new RuntimeException("Event is null");
        }
        
        String bucket = (String) event.get("bucket");
        String key = (String) event.get("key");
        String audioId = (String) event.get("audioId");
        
        if (bucket == null) {
            throw new RuntimeException("bucket is required");
        }
        if (key == null) {
            throw new RuntimeException("key is required");
        }
        
        String type = findType(key);
        String outKey = makeOutputKey(key);
        String outBucket = findBucket();
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("audioId", audioId);
        result.put("outputBucket", outBucket);
        result.put("outputKey", outKey);
        result.put("processedAt", java.time.Instant.now().toString());
        result.put("processingType", type);
        result.put("outputFileSize", Long.valueOf(12345));
        
        return result;
    }
    
    private String findType(String key) {
        if (key.toLowerCase().endsWith(".txt")) {
            return "TEXT_TO_SPEECH";
        }
        return "AUDIO_PROCESSING";
    }
    
    private String makeOutputKey(String in) {
        String name = in;
        int s = name.lastIndexOf('/');
        if (s >= 0) name = name.substring(s + 1);
        
        int d = name.lastIndexOf('.');
        String base = d >= 0 ? name.substring(0, d) : name;
        String suffix = d >= 0 ? name.substring(d) : ".mp3";
        
        long t = System.currentTimeMillis();
        return "processed/" + base + "-processed-" + t + suffix;
    }
    
    private String findBucket() {
        String b = System.getenv("OUTPUT_BUCKET");
        if (b != null) return b;
        
        b = System.getProperty("OUTPUT_BUCKET");
        if (b != null) return b;
        
        return "default-output-bucket";
    }
}
