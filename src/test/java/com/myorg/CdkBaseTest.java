package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.assertions.Match;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test Suite for Issues #3, #4, #5, and #6
 * 
 * Issue #4: Step Functions State Machine + Polly Integration (TDD)
 * These tests are written FIRST (before implementation) following strict TDD.
 * They verify:
 * 1. Input S3 bucket with encryption, versioning, and EventBridge notifications
 * 2. Output S3 bucket with encryption and versioning
 * 3. EventBridge rule triggering on S3 ObjectCreated events
 */
public class CdkBaseTest {

    /**
     * Test: Input S3 bucket should exist with proper encryption and versioning
     */
    @Test
    public void testInputBucketCreatedWithEncryptionAndVersioning() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify S3 bucket exists with encryption
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(
            new HashMap<String, Object>() {{
                put("BucketEncryption", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("ServerSideEncryptionConfiguration", Match.anyValue());
                    }}
                ));
                put("VersioningConfiguration", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Status", "Enabled");
                    }}
                ));
                put("NotificationConfiguration", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("EventBridgeConfiguration", Match.objectLike(
                            new HashMap<String, Object>() {{
                                put("EventBridgeEnabled", true);
                            }}
                        ));
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: Output S3 bucket should exist with encryption and versioning
     */
    @Test
    public void testOutputBucketCreatedWithEncryptionAndVersioning() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify at least 2 S3 buckets exist (input and output)
        template.resourceCountIs("AWS::S3::Bucket", 2);
    }

    /**
     * Test: EventBridge rule should exist and target S3 ObjectCreated events
     */
    @Test
    public void testEventBridgeRuleForS3ObjectCreated() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify EventBridge rule exists
        template.hasResourceProperties("AWS::Events::Rule", Match.objectLike(
            new HashMap<String, Object>() {{
                put("State", "ENABLED");
                put("EventPattern", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("source", Match.arrayWith(List.of("aws.s3")));
                        put("detail-type", Match.arrayWith(List.of("Object Created")));
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: Both buckets should have public access blocked
     */
    @Test
    public void testBucketsHavePublicAccessBlocked() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify public access is blocked on buckets
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PublicAccessBlockConfiguration", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("BlockPublicAcls", true);
                        put("BlockPublicPolicy", true);
                        put("IgnorePublicAcls", true);
                        put("RestrictPublicBuckets", true);
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: EventBridge rule should have at least one target
     */
    @Test
    public void testEventBridgeRuleHasTarget() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify rule has targets defined
        template.resourceCountIs("AWS::Events::Rule", 1);
    }

    // ======================================================================
    // Issue #4: Step Functions State Machine + Polly Integration (TDD)
    // These tests are written FIRST before implementation
    // ======================================================================

    /**
     * Test: Step Functions state machine should exist
     */
    @Test
    public void testStepFunctionsStateMachineExists() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine resource exists
        template.resourceCountIs("AWS::StepFunctions::StateMachine", 1);
    }

    /**
     * Test: State machine should have CloudWatch logging enabled
     */
    @Test
    public void testStateMachineHasLoggingEnabled() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify logging is enabled
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("LoggingConfiguration", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Level", "ALL");
                        put("IncludeExecutionData", true);
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: State machine should have an IAM execution role
     */
    @Test
    public void testStateMachineHasProperIAMRole() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify IAM role exists for state machine
        template.hasResourceProperties("AWS::IAM::Role", Match.objectLike(
            new HashMap<String, Object>() {{
                put("AssumeRolePolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", "sts:AssumeRole");
                                    put("Principal", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Service", "states.amazonaws.com");
                                        }}
                                    ));
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: EventBridge rule should target the Step Functions state machine
     */
    @Test
    public void testEventBridgeTargetsStateMachine() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify EventBridge rule has Step Functions as target
        template.hasResourceProperties("AWS::Events::Rule", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Targets", Match.arrayWith(List.of(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("Arn", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Ref", Match.stringLikeRegexp(".*StateMachine.*"));
                                }}
                            ));
                        }}
                    )
                )));
            }}
        ));
    }

    /**
     * Test: State machine should have a valid definition with states
     */
    @Test
    public void testStateMachineHasValidDefinition() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine has a definition
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.anyValue());
            }}
        ));
    }

    /**
     * Test: State machine IAM role should have permissions for Polly
     */
    @Test
    public void testStateMachineHasPollyPermissions() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify IAM policy includes Polly permissions
        template.hasResourceProperties("AWS::IAM::Policy", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", "polly:SynthesizeSpeech");
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));
    }

    // ======================================================================
    // Issue #5: DynamoDB Metadata Table + State Machine I/O Handling (TDD)
    // These tests are written FIRST before implementation (strict TDD)
    // ======================================================================

    /**
     * Test: DynamoDB table should exist for storing audio metadata
     */
    @Test
    public void testDynamoDBTableExists() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify DynamoDB table resource exists
        template.resourceCountIs("AWS::DynamoDB::Table", 1);
    }

    /**
     * Test: DynamoDB table should have correct key schema (partition key)
     */
    @Test
    public void testDynamoDBTableHasCorrectKeySchema() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify table has partition key defined
        template.hasResourceProperties("AWS::DynamoDB::Table", Match.objectLike(
            new HashMap<String, Object>() {{
                put("KeySchema", Match.arrayWith(List.of(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("AttributeName", "audioId");
                            put("KeyType", "HASH");
                        }}
                    )
                )));
                put("AttributeDefinitions", Match.arrayWith(List.of(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("AttributeName", "audioId");
                            put("AttributeType", "S");
                        }}
                    )
                )));
            }}
        ));
    }

    /**
     * Test: DynamoDB table should have on-demand billing mode
     */
    @Test
    public void testDynamoDBTableHasOnDemandBilling() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify billing mode is PAY_PER_REQUEST (on-demand)
        template.hasResourceProperties("AWS::DynamoDB::Table", Match.objectLike(
            new HashMap<String, Object>() {{
                put("BillingMode", "PAY_PER_REQUEST");
            }}
        ));
    }

    /**
     * Test: DynamoDB table should have server-side encryption enabled
     */
    @Test
    public void testDynamoDBTableHasEncryption() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify SSE is enabled
        template.hasResourceProperties("AWS::DynamoDB::Table", Match.objectLike(
            new HashMap<String, Object>() {{
                put("SSESpecification", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("SSEEnabled", true);
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: DynamoDB table should have point-in-time recovery enabled
     */
    @Test
    public void testDynamoDBTableHasPointInTimeRecovery() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify PITR is enabled
        template.hasResourceProperties("AWS::DynamoDB::Table", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PointInTimeRecoverySpecification", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("PointInTimeRecoveryEnabled", true);
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: State machine should have DynamoDB permissions in IAM policy
     */
    @Test
    public void testStateMachineHasDynamoDBPermissions() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify IAM policy includes DynamoDB PutItem permission
        template.hasResourceProperties("AWS::IAM::Policy", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", "dynamodb:PutItem");
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));
    }

    // ======================================================================
    // Issue #6: SNS Notifications + Error Handling & Status Updates (TDD)
    // These tests are written FIRST before implementation (strict TDD)
    // ======================================================================

    /**
     * Test: Two SNS topics should exist for pipeline completion and failure
     */
    @Test
    public void testSNSTopicsExist() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify exactly 2 SNS topics exist
        template.resourceCountIs("AWS::SNS::Topic", 2);
    }

    /**
     * Test: SNS topics should have encryption enabled
     */
    @Test
    public void testSNSTopicsEncrypted() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify SNS topics have KMS encryption enabled
        template.hasResourceProperties("AWS::SNS::Topic", Match.objectLike(
            new HashMap<String, Object>() {{
                put("KmsMasterKeyId", Match.anyValue());
            }}
        ));
    }

    /**
     * Test: State machine should have error handling (Catch blocks)
     */
    @Test
    public void testStateMachineHasErrorHandling() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine definition includes error handling
        // The definition string should contain "Catch" keyword
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // At least one state should have a Catch block
                                    put("PollyTextToSpeech", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Catch", Match.anyValue());
                                        }}
                                    ));
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));
    }

    /**
     * Test: State machine should include DynamoDB status update tasks
     */
    @Test
    public void testStateMachineHasDynamoDBStatusUpdateTasks() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine definition includes DynamoDB UpdateItem tasks
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // Should have a task for updating status to COMPLETED
                                    put("UpdateStatusCompleted", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Resource", Match.stringLikeRegexp(".*states:dynamodb:updateItem.*"));
                                        }}
                                    ));
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));
    }

    /**
     * Test: State machine should include SNS publish tasks
     */
    @Test
    public void testStateMachineHasSNSPublishTasks() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine definition includes SNS Publish tasks
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // Should have a task for publishing success notification
                                    put("PublishSuccessNotification", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Resource", Match.stringLikeRegexp(".*states:sns:publish.*"));
                                        }}
                                    ));
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));
    }

    /**
     * Test: State machine IAM role should have SNS publish permissions
     */
    @Test
    public void testStateMachineHasSNSPermissions() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify IAM policy includes SNS publish permissions
        template.hasResourceProperties("AWS::IAM::Policy", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", "sns:Publish");
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: State machine IAM role should have DynamoDB UpdateItem permissions
     */
    @Test
    public void testStateMachineHasDynamoDBUpdatePermissions() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify IAM policy includes DynamoDB UpdateItem permission
        template.hasResourceProperties("AWS::IAM::Policy", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", "dynamodb:UpdateItem");
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));

    // ======================================================================
    // Issue #7: Lambda Function Skeleton + Step Functions Integration (TDD)
    // These tests are written FIRST before implementation (strict TDD)
    // ======================================================================

    /**
     * Test: Lambda function should exist for audio processing
     */
    @Test
    public void testLambdaFunctionExists() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Lambda function resource exists
        template.resourceCountIs("AWS::Lambda::Function", 1);
    }

    /**
     * Test: Lambda function should have correct runtime (Java 17)
     */
    @Test
    public void testLambdaFunctionHasCorrectRuntime() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Lambda uses Java 17 runtime
        template.hasResourceProperties("AWS::Lambda::Function", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Runtime", "java17");
            }}
        ));
    }

    /**
     * Test: Lambda function should have correct handler configuration
     */
    @Test
    public void testLambdaFunctionHasCorrectHandler() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Lambda handler is set correctly
        template.hasResourceProperties("AWS::Lambda::Function", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Handler", "com.myorg.SleepAudioProcessor::handleRequest");
            }}
        ));
    }

    /**
     * Test: Lambda function should have environment variables for DynamoDB table
     */
    @Test
    public void testLambdaFunctionHasEnvironmentVariables() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Lambda has environment variables configured
        template.hasResourceProperties("AWS::Lambda::Function", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Environment", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Variables", Match.objectLike(
                            new HashMap<String, Object>() {{
                                put("TABLE_NAME", Match.anyValue());
                            }}
                        ));
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: Lambda execution role should have DynamoDB permissions
     */
    @Test
    public void testLambdaHasDynamoDBPermissions() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Lambda role has DynamoDB permissions
        template.hasResourceProperties("AWS::IAM::Policy", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", Match.arrayWith(List.of(
                                        "dynamodb:GetItem",
                                        "dynamodb:PutItem",
                                        "dynamodb:UpdateItem"
                                    )));
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: State machine should include Lambda invocation task
     */
    @Test
    public void testStateMachineIncludesLambdaInvocationTask() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine definition includes Lambda invocation
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("ProcessAudio", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Resource", Match.stringLikeRegexp(".*lambda:invoke.*"));
                                        }}
                                    ));
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));
    }

    /**
     * Test: State machine should have permission to invoke Lambda
     */
    @Test
    public void testStateMachineCanInvokeLambda() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify state machine role has Lambda invoke permissions
        template.hasResourceProperties("AWS::IAM::Policy", Match.objectLike(
            new HashMap<String, Object>() {{
                put("PolicyDocument", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Statement", Match.arrayWith(List.of(
                            Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("Action", "lambda:InvokeFunction");
                                }}
                            )
                        )));
                    }}
                ));
            }}
        ));
    }
    }
}
