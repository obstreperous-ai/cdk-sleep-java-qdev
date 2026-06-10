package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.Tags;
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
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.stepfunctions.Retry;
import software.amazon.awscdk.services.stepfunctions.Errors;

import software.amazon.awscdk.services.stepfunctions.Pass;
import software.amazon.awscdk.services.stepfunctions.Choice;
import software.amazon.awscdk.services.stepfunctions.Condition;
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
    private final Function audioProcessorLambda;
    
    private final Alarm stateMachineFailureAlarm;
    private final Alarm lambdaErrorAlarm;
    
    private final String environment;
    
    public CdkBaseStack(final Construct scope, final String id) {
        this(scope, id, null, "dev");
    }

    public CdkBaseStack(final Construct scope, final String id, final StackProps props) {
        this(scope, id, props, "dev");
    }

    public CdkBaseStack(final Construct scope, final String id, final StackProps props, final String environment) {
        super(scope, id, props);
        
        this.environment = environment != null ? environment : "dev";
        
        // Determine removal policy based on environment
        RemovalPolicy removalPolicy = getRemovalPolicyForEnvironment(this.environment);
        boolean autoDeleteObjects = "dev".equals(this.environment);
        
        // Add environment tags to the stack
        Tags.of(this).add("Environment", this.environment);
        Tags.of(this).add("ManagedBy", "CDK");
        Tags.of(this).add("Project", "SleepAudioPipeline");
        
        initializeResources(removalPolicy, autoDeleteObjects);
    }
    
    private void initializeResources(RemovalPolicy removalPolicy, boolean autoDeleteObjects) {
        // Note: 'this' in the following code refers to the CdkBaseStack instance
        // All resources are created in the context of this stack
        
        super(scope, id, props);

        // Input S3 Bucket - receives raw audio files
        inputBucket = Bucket.Builder.create(this, "SleepAudioInputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .eventBridgeEnabled(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(removalPolicy)
                .autoDeleteObjects(autoDeleteObjects)
                .build();

        // Output S3 Bucket - stores processed audio files
        outputBucket = Bucket.Builder.create(this, "SleepAudioOutputBucket")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(removalPolicy)
                .autoDeleteObjects(autoDeleteObjects)
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
                .removalPolicy(removalPolicy)
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

        // Lambda Function - Audio processor (placeholder for future processing logic)
        audioProcessorLambda = Function.Builder.create(this, "SleepAudioProcessorFunction")
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.SleepAudioProcessor::handleRequest")
                .code(Code.fromAsset("target/function.jar"))
                .environment(Map.of(
                    "TABLE_NAME", metadataTable.getTableName(),
                    "ENVIRONMENT", this.environment
                    "OUTPUT_BUCKET", outputBucket.getBucketName(),
                ))
                .description("Processes sleep audio files - placeholder for metadata enrichment and validation")
                .tracing(Tracing.ACTIVE)
                .timeout(Duration.seconds(30))
                .build();
        
        // Grant Lambda permissions to access DynamoDB table
        metadataTable.grantReadWriteData(audioProcessorLambda);

        // Grant Lambda read access to input bucket
        inputBucket.grantRead(audioProcessorLambda);

        // Grant Lambda write access to output bucket
        outputBucket.grantWrite(audioProcessorLambda);

        
        // Grant Lambda permission to use Amazon Polly for text-to-speech (Issue #11)
        audioProcessorLambda.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("polly:SynthesizeSpeech"))
                .resources(List.of("*"))
                .build());
        // CloudWatch Log Group for EventBridge rule (placeholder target)
        // CloudWatch Log Group for Step Functions state machine
        LogGroup stateMachineLogGroup = LogGroup.Builder.create(this, "SleepAudioStateMachineLogGroup")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // EventBridge Rule - triggers on S3 ObjectCreated events from input bucket
        // Step Functions State Machine - Orchestrates the audio processing pipeline
        // Now includes DynamoDB metadata storage, Lambda processing, SNS notifications, and error handling (Issue #5, #6, #7)
        
        // Task 1: Write initial metadata to DynamoDB
        // Task 0: Input Validation - Extract and validate input fields
        Pass validateInput = Pass.Builder.create(this, "ValidateInput")
                .comment("Validate and extract input fields from S3 event")
                .parameters(Map.of(
                    "bucket", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.bucket"),
                    "key", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"),
                    "eventTime", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.eventTime"),
                    "fileExtension", software.amazon.awscdk.services.stepfunctions.JsonPath.stringSplit(
                        software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"), 
                        "."
                    )
                ))
                .resultPath("$.validation")
                .build();
        
        // Pass state for validation failure
        Pass validationFailed = Pass.Builder.create(this, "ValidationFailed")
                .comment("File validation failed - unsupported format")
                .parameters(Map.of(
                    "bucket", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.bucket"),
                    "key", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"),
                    "errorMessage", "Unsupported file format. Supported formats: .mp3, .wav, .m4a, .flac"
                ))
                .resultPath("$")
                .build();
        
        // Choice state for file extension validation
        Choice checkFileExtension = Choice.Builder.create(this, "CheckFileExtension")
                .comment("Check if file has supported extension")
                .build();
        
        // Define supported file extensions
        // Note: Step Functions doesn't have built-in extension checking, so we check for common patterns
        Condition isSupportedFile = Condition.or(
            Condition.stringMatches("$.key", "*.mp3"),
            Condition.stringMatches("$.key", "*.wav"),
            Condition.stringMatches("$.key", "*.m4a"),
            Condition.stringMatches("$.key", "*.flac")
        );
        
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
        
        // Add retry policy for DynamoDB throttling
        putMetadataTask.addRetry(Retry.builder()
                .errors(List.of("DynamoDB.ProvisionedThroughputExceededException"))
                .interval(Duration.seconds(2))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());
        
        // Task 2: Invoke Lambda function for audio processing
        LambdaInvoke processAudioTask = LambdaInvoke.Builder.create(this, "ProcessAudio")
                .lambdaFunction(audioProcessorLambda)
                .payload(software.amazon.awscdk.services.stepfunctions.TaskInput.fromObject(Map.of(
                    "bucket", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.bucket"),
                    "key", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"),
                    "audioId", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.key"),
                    "eventTime", software.amazon.awscdk.services.stepfunctions.JsonPath.stringAt("$.eventTime")
                )))
                .resultPath("$.lambdaResult")
                .comment("Process audio file and enrich metadata")
        
        // Add retry policy for Lambda service errors with exponential backoff
        processAudioTask.addRetry(Retry.builder()
                .errors(List.of("Lambda.ServiceException", "Lambda.TooManyRequestsException"))
                .interval(Duration.seconds(2))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());
                .build();

        // Task 3: Polly task using CallAwsService for SynthesizeSpeech
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
        
        // Add retry policy for Polly service errors
        pollyTask.addRetry(Retry.builder()
                .errors(List.of("Polly.ServiceFailureException", "Polly.ThrottlingException"))
                .interval(Duration.seconds(3))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build());
                .build();

        // Task 4: Update DynamoDB status to COMPLETED
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
        
        // Add retry policy for DynamoDB throttling
        updateStatusCompleted.addRetry(Retry.builder()
                .errors(List.of("DynamoDB.ProvisionedThroughputExceededException"))
                .interval(Duration.seconds(2))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());
                .build();

        // Task 5: Publish success notification to SNS
        SnsPublish publishSuccessNotification = SnsPublish.Builder.create(this, "PublishSuccessNotification")
                .topic(completedTopic)
                .message(software.amazon.awscdk.services.stepfunctions.TaskInput.fromJsonPathAt("$"))
                .subject("Sleep Audio Pipeline Completed")
                .resultPath("$.snsResult")
                .comment("Notify via SNS that pipeline completed successfully")
                .build();

        // Task 6: Update DynamoDB status to FAILED (for error path)
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
        
        // Add retry policy for DynamoDB throttling
        updateStatusFailed.addRetry(Retry.builder()
                .errors(List.of("DynamoDB.ProvisionedThroughputExceededException"))
                .interval(Duration.seconds(2))
                .maxAttempts(3)
                .backoffRate(2.0)
                .build());
                .build();

        // Task 7: Publish failure notification to SNS
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

        // Define error handling: Lambda task catches specific errors with detailed context
        processAudioTask.addCatch(
            updateStatusFailed.next(publishFailureNotification).next(failureState),
            Catch.Builder.builder()
                .errors(List.of("Lambda.ServiceException", "Lambda.AWSLambdaException", "Lambda.SdkClientException"))
                .resultPath("$.errorMessage")
                .build()
        );
        
        // Catch all other errors from Lambda
        processAudioTask.addCatch(
            updateStatusFailed.next(publishFailureNotification).next(failureState),
            Catch.Builder.builder()
                .errors(List.of(Errors.ALL))
                .resultPath("$.errorMessage")
                .build()
        );

        // Define error handling: Polly task catches specific and all errors
        pollyTask.addCatch(
            updateStatusFailed.next(publishFailureNotification).next(failureState),
            Catch.Builder.builder()
                .errors(List.of("Polly.ServiceFailureException", "Polly.InvalidSsmlException", "Polly.ThrottlingException"))
                .resultPath("$.errorMessage")
                .build()
        );
        
        pollyTask.addCatch(
            updateStatusFailed.next(publishFailureNotification).next(failureState),
            Catch.Builder.builder()
                .errors(List.of(Errors.ALL))
                .resultPath("$.errorMessage")
                .build()
        );

        // Define the success workflow chain
        Chain successChain = Chain.start(updateStatusCompleted)
                .next(publishSuccessNotification)
                .next(successState);

        // Define error workflow for validation failures
        Chain validationErrorChain = Chain.start(validationFailed)
                .next(updateStatusFailed)
                .next(publishFailureNotification)
                .next(failureState);
        
        // Define the complete workflow with input validation
        Chain definition = Chain.start(validateInput)
                .next(checkFileExtension.when(isSupportedFile, Chain.start(putMetadataTask)
                .next(processAudioTask)
                .next(pollyTask)
                .next(successChain))
                .otherwise(validationErrorChain));
        
        stateMachine = StateMachine.Builder.create(this, "SleepAudioPipelineStateMachine")
                .definition(definition)
                .stateMachineType(StateMachineType.STANDARD)
                .tracingEnabled(true)
                .logs(LogOptions.builder()
                        .destination(stateMachineLogGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .comment("Orchestrates sleep audio processing with Lambda, DynamoDB metadata, Polly integration, SNS notifications, and error handling")
                .build();
                
        // Add explicit Polly permissions to the state machine role (least privilege)
        stateMachine.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("polly:SynthesizeSpeech"))
                .resources(List.of("*"))
                .build());
        
        // Add SNS publish permissions to the state machine role
                stateMachine.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("sns:Publish"))
                .resources(List.of(completedTopic.getTopicArn(), failedTopic.getTopicArn()))
                .build());
        

        metadataTable.grantReadWriteData(stateMachine);
        // Grant state machine read access to input bucket (for future processing)

        inputBucket.grantRead(stateMachine);
        
        // Grant state machine write access to output bucket (for future processing)
        
        // CloudWatch Alarm for State Machine Execution Failures (Issue #10)
        stateMachineFailureAlarm = Alarm.Builder.create(this, "StateMachineExecutionFailuresAlarm")
                .metric(Metric.Builder.create()
                        .namespace("AWS/States")
                        .metricName("ExecutionsFailed")
                        .dimensionsMap(Map.of(
                            "StateMachineArn", stateMachine.getStateMachineArn()
                        ))
                        .statistic("Sum")
                        .period(Duration.minutes(5))
                        .build())
                .evaluationPeriods(1)
                .threshold(0)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .alarmDescription("Alert when state machine executions fail")
                .alarmName(String.format("SleepAudio-StateMachine-Failures-%s", this.environment))
                .build();
        
        // CloudWatch Alarm for Lambda Function Errors (Issue #10)
        lambdaErrorAlarm = Alarm.Builder.create(this, "LambdaErrorsAlarm")
                .metric(Metric.Builder.create()
                        .namespace("AWS/Lambda")
                        .metricName("Errors")
                        .dimensionsMap(Map.of(
                            "FunctionName", audioProcessorLambda.getFunctionName()
                        ))
                        .statistic("Sum")
                        .period(Duration.minutes(5))
                        .build())
                .evaluationPeriods(1)
                .threshold(5)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .treatMissingData(TreatMissingData.NOT_BREACHING)
                .alarmDescription("Alert when Lambda function error rate is high")
                .alarmName(String.format("SleepAudio-Lambda-Errors-%s", this.environment))
                .build();
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
    
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Determine removal policy based on environment
     * - dev: DESTROY (easy cleanup)
     * - stage/prod: RETAIN (data protection)
     */
    private RemovalPolicy getRemovalPolicyForEnvironment(String env) {
        if ("dev".equals(env)) {
            return RemovalPolicy.DESTROY;
        }
        return RemovalPolicy.RETAIN;
    }
}
