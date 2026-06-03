package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.EventBridgeConfiguration;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.targets.CloudWatchLogGroup;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.List;
import java.util.Map;

public class CdkBaseStack extends Stack {
    
    private final Bucket inputBucket;
    private final Bucket outputBucket;
    private final Rule eventBridgeRule;
    
    public CdkBaseStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkBaseStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Input S3 Bucket - receives raw audio files
        inputBucket = Bucket.Builder.create(this, "SleepAudioInputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .eventBridgeEnabled(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.DESTROY) // For dev - should be configurable
                .autoDeleteObjects(true) // For dev - cleanup on stack deletion
                .build();

        // Output S3 Bucket - stores processed audio files
        outputBucket = Bucket.Builder.create(this, "SleepAudioOutputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.DESTROY) // For dev - should be configurable
                .autoDeleteObjects(true) // For dev - cleanup on stack deletion
                .build();

        // CloudWatch Log Group for EventBridge rule (placeholder target)
        LogGroup logGroup = LogGroup.Builder.create(this, "SleepAudioEventLogGroup")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // EventBridge Rule - triggers on S3 ObjectCreated events from input bucket
        eventBridgeRule = Rule.Builder.create(this, "SleepAudioS3ObjectCreatedRule")
                .eventPattern(EventPattern.builder()
                        .source(List.of("aws.s3"))
                        .detailType(List.of("Object Created"))
                        .detail(Map.of(
                                "bucket", Map.of(
                                        "name", List.of(inputBucket.getBucketName())
                                )
                        ))
                        .build())
                .description("Triggers on object creation in Sleep Audio input bucket")
                .build();
        
        // Add CloudWatch Logs as a placeholder target
        eventBridgeRule.addTarget(CloudWatchLogGroup.Builder.create(logGroup)
                .build());
    }
}
