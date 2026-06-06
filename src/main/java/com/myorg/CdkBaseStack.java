package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.EventBridgeConfiguration;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.TableEncryption;
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
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoPutItem;
import software.amazon.awscdk.services.stepfunctions.Fail;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoUpdateItem;
import software.amazon.awscdk.services.stepfunctions.tasks.SnsPublish;
import software.amazon.awscdk.services.stepfunctions.Catch;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoAttributeValue;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Effect;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CdkBaseStack extends Stack {
    
    private final Bucket inputBucket;
    private final Bucket outputBucket;
    private final Rule eventBridgeRule;
    private final Table metadataTable;
    private final StateMachine stateMachine;
    private final Topic completedTopic;
    private final Topic failedTopic;
    
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

        // DynamoDB Table - stores audio pipeline metadata
        metadataTable = Table.Builder.create(this, "SleepAudioMetadataTable")
                .partitionKey(Attribute.builder()
                        .name("audioId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .encryption(TableEncryption.AWS_MANAGED)
                .pointInTimeRecovery(true)
                .removalPolicy(RemovalPolicy.DESTROY) // For dev - should be configurable
                .build();

        // KMS Key for SNS encryption
        Key snsKmsKey = Key.Builder.create(this, "SleepAudioSNSKey")
                .description("KMS key for Sleep Audio SNS topic encryption")
                .enableKeyRotation(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // SNS Topic for pipeline completion notifications
        completedTopic = Topic.Builder.create(this, "SleepAudioPipelineCompleted")
                .displayName("Sleep Audio Pipeline Completed")
                .masterKey(snsKmsKey)
                .build();

        // SNS Topic for pipeline failure notifications
        failedTopic = Topic.Builder.create(this, "SleepAudioPipelineFailed")
                .displayName("Sleep Audio Pipeline Failed")
                .masterKey(snsKmsKey)
                .build();

        // CloudWatch Log Group for EventBridge rule (placeholder target)
        // CloudWatch Log Group for Step Functions state machine
        LogGroup stateMachineLogGroup = LogGroup.Builder.create(this, "SleepAudioStateMachineLogGroup")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // EventBridge Rule - triggers on S3 ObjectCreated events from input bucket
        // Step Functions State Machine - Orchestrates the audio processing pipeline
        // Now includes DynamoDB metadata storage, SNS notifications, and error handling (Issue #5, #6)
        
        // Task 1: Write initial metadata to DynamoDB
        DynamoPutItem putMetadataTask = DynamoPutItem.Builder.create(this, "WriteInitialMetadata")
                .table(metadataTable)
                .item(Map.of(
                    "audioId", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key")),
                    "status", DynamoAttributeValue.fromString("PROCESSING"),
                    "inputBucket", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.bucket")),
                    "inputKey", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key")),
                    "createdAt", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.eventTime"))
                ))
                .resultPath("$.dynamoResult")
                .comment("Write initial metadata record when pipeline starts")
                .build();
        
        // Task 2: Polly task using CallAwsService for SynthesizeSpeech
        // Error handling with Catch block will be added below
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

        // Task 3: Update DynamoDB status to COMPLETED
        DynamoUpdateItem updateStatusCompleted = DynamoUpdateItem.Builder.create(this, "UpdateStatusCompleted")
                .table(metadataTable)
                .key(Map.of(
                    "audioId", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"))
                ))
                .updateExpression("SET #status = :completed, #updatedAt = :updatedAt")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#updatedAt", "updatedAt"
                ))
                .expressionAttributeValues(Map.of(
                    ":completed", DynamoAttributeValue.fromString("COMPLETED"),
                    ":updatedAt", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$$.State.EnteredTime"))
                ))
                .resultPath("$.updateResult")
                .comment("Update status to COMPLETED after successful processing")
                .build();

        // Task 4: Publish success notification to SNS
        SnsPublish publishSuccessNotification = SnsPublish.Builder.create(this, "PublishSuccessNotification")
                .topic(completedTopic)
                .message(software.amazon.awscdk.services.stepfunctions.TaskInput.fromJsonPathAt("$"))
                .subject("Sleep Audio Pipeline Completed")
                .resultPath("$.snsResult")
                .comment("Notify via SNS that pipeline completed successfully")
                .build();

        // Task 5: Update DynamoDB status to FAILED (for error path)
        DynamoUpdateItem updateStatusFailed = DynamoUpdateItem.Builder.create(this, "UpdateStatusFailed")
                .table(metadataTable)
                .key(Map.of(
                    "audioId", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"))
                ))
                .updateExpression("SET #status = :failed, #updatedAt = :updatedAt, #errorInfo = :errorInfo")
                .expressionAttributeNames(Map.of(
                    "#status", "status",
                    "#updatedAt", "updatedAt",
                    "#errorInfo", "errorInfo"
                ))
                .expressionAttributeValues(Map.of(
                    ":failed", DynamoAttributeValue.fromString("FAILED"),
                    ":updatedAt", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$$.State.EnteredTime")),
                    ":errorInfo", DynamoAttributeValue.fromString(software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.errorMessage"))
                ))
                .resultPath("$.updateResult")
                .comment("Update status to FAILED after error")
                .build();

        // Task 6: Publish failure notification to SNS
        SnsPublish publishFailureNotification = SnsPublish.Builder.create(this, "PublishFailureNotification")
                .topic(failedTopic)
                .message(software.amazon.awscdk.services.stepfunctions.TaskInput.fromJsonPathAt("$"))
                .subject("Sleep Audio Pipeline Failed")
                .resultPath("$.snsResult")
                .comment("Notify via SNS that pipeline failed")
                .build();
        
        // Final success state
        Succeed successState = Succeed.Builder.create(this, "PipelineSucceeded")
                .comment("Pipeline completed successfully")
                .build();

        // Final failure state
        Fail failureState = Fail.Builder.create(this, "PipelineFailed")
                .comment("Pipeline failed")
                .cause("Processing error occurred")
                .error("PipelineError")
                .build();

        // Define error handling: Polly task catches all errors
        pollyTask.addCatch(
            updateStatusFailed.next(publishFailureNotification).next(failureState),
            Catch.Builder.builder()
                .errors(List.of("States.ALL"))
                .resultPath("$.errorMessage")
                .build()
        );

        // Define the success workflow chain
        Chain successChain = Chain.start(updateStatusCompleted)
                .next(publishSuccessNotification)
                .next(successState);

        // Define the complete workflow
        Chain definition = Chain.start(putMetadataTask)
                .next(pollyTask)
                .next(successChain);

        stateMachine = StateMachine.Builder.create(this, "SleepAudioPipelineStateMachine")
                .definition(definition)
                .stateMachineType(StateMachineType.STANDARD)
                .logs(LogOptions.builder()
                        .destination(stateMachineLogGroup)
                        .level(LogLevel.ALL)
                .comment("Orchestrates sleep audio processing with DynamoDB metadata, Polly integration, SNS notifications, and error handling")
                .build();
                .comment("Orchestrates sleep audio processing with Polly integration")
                .comment("Orchestrates sleep audio processing with DynamoDB metadata and Polly integration")
        
        // Add explicit Polly permissions to the state machine role (least privilege)
        stateMachine.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("polly:SynthesizeSpeech"))
                .resources(List.of("*"))
        // Add SNS publish permissions to the state machine role
        stateMachine.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("sns:Publish"))
                .resources(List.of(completedTopic.getTopicArn(), failedTopic.getTopicArn()))
                .build());
        
                .build());
        
        
        // Grant state machine read/write access to DynamoDB table
        metadataTable.grantReadWriteData(stateMachine);
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
