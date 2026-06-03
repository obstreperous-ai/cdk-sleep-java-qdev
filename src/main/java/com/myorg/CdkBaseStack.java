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
import software.amazon.awscdk.services.events.targets.SfnStateMachine;
import software.amazon.awscdk.services.events.RuleTargetInput;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.StateMachineType;
import software.amazon.awscdk.services.stepfunctions.LogOptions;
import software.amazon.awscdk.services.stepfunctions.LogLevel;
import software.amazon.awscdk.services.stepfunctions.Chain;
import software.amazon.awscdk.services.stepfunctions.tasks.CallAwsService;
import software.amazon.awscdk.services.stepfunctions.Succeed;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Effect;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CdkBaseStack extends Stack {
    
    private final Bucket inputBucket;
    private final Bucket outputBucket;
    private final Rule eventBridgeRule;
    private final StateMachine stateMachine;
    
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
        // CloudWatch Log Group for Step Functions state machine
        LogGroup stateMachineLogGroup = LogGroup.Builder.create(this, "SleepAudioStateMachineLogGroup")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // EventBridge Rule - triggers on S3 ObjectCreated events from input bucket
        // Step Functions State Machine - Orchestrates the audio processing pipeline
        // Start with minimal Polly integration following TDD
        
        // Define Polly task using CallAwsService for SynthesizeSpeech
        CallAwsService pollyTask = CallAwsService.Builder.create(this, "PollyTextToSpeech")
                .service("polly")
                .action("synthesizeSpeech")
                .parameters(Map.of(
                    "Text", "This is a placeholder sleep audio message. Real text will come from S3 object or processing logic.",
                    "OutputFormat", "mp3",
                    "VoiceId", "Joanna",
                    "Engine", "neural"
                ))
                .iamResources(List.of("*"))
                .resultPath("$.pollyResult")
                .build();
        
        // Success state
        Succeed successState = Succeed.Builder.create(this, "ProcessingComplete")
                .comment("Audio processing completed successfully")
                .build();
        
        // Define the workflow chain: Start -> Polly Task -> Success
        Chain definition = Chain.start(pollyTask).next(successState);
        
        // Create the state machine
        stateMachine = StateMachine.Builder.create(this, "SleepAudioPipelineStateMachine")
                .definition(definition)
                .stateMachineType(StateMachineType.STANDARD)
                .logs(LogOptions.builder()
                        .destination(stateMachineLogGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .tracingEnabled(true)
                .comment("Orchestrates sleep audio processing with Polly integration")
                .build();
        
        // Add explicit Polly permissions to the state machine role (least privilege)
        stateMachine.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("polly:SynthesizeSpeech"))
                .resources(List.of("*"))
                .build());
        
        // Grant state machine read access to input bucket (for future processing)
        inputBucket.grantRead(stateMachine);
        
        // Grant state machine write access to output bucket (for future processing)
        outputBucket.grantWrite(stateMachine);

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
        
        // Add Step Functions state machine as target
        // Transform the EventBridge event to pass S3 details to the state machine
        eventBridgeRule.addTarget(SfnStateMachine.Builder.create(stateMachine)
                .input(RuleTargetInput.fromObject(Map.of(
                    "bucket", RuleTargetInput.fromEventPath("$.detail.bucket.name"),
                    "key", RuleTargetInput.fromEventPath("$.detail.object.key"),
                    "eventTime", RuleTargetInput.fromEventPath("$.time"),
                    "eventDetail", RuleTargetInput.fromEventPath("$.detail")
                )))
                .build());
    }
    
    // Getters for testing if needed
    public StateMachine getStateMachine() {
        return stateMachine;
    }
}
