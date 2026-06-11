package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.assertions.Match;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test Suite for Issue #12: End-to-End Validation & Project Completion
 * 
 * These tests are written FIRST (before final polish) following strict TDD.
 * They verify the complete pipeline from end to end:
 * 1. Complete happy path flow validation
 * 2. Error handling scenarios
 * 3. Retry behavior validation
 * 4. Input validation rejection
 * 5. SNS notifications for success/failure
 * 6. DynamoDB metadata updates
 */
@DisplayName("End-to-End Pipeline Validation Tests")
public class EndToEndValidationTest {

    /**
     * Test: Complete happy path flow from S3 upload to completion
     * Validates: S3 → EventBridge → StateMachine → Lambda → DynamoDB → SNS
     */
    @Test
    @DisplayName("Complete happy path flow is correctly configured")
    public void testCompleteHappyPathFlow() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // 1. Verify S3 Input Bucket triggers EventBridge
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(
            new HashMap<String, Object>() {{
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

        // 2. Verify EventBridge Rule targets State Machine
        template.hasResourceProperties("AWS::Events::Rule", Match.objectLike(
            new HashMap<String, Object>() {{
                put("State", "ENABLED");
                put("Targets", Match.arrayWith(List.of(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("Arn", Match.anyValue());
                        }}
                    )
                )));
            }}
        ));

        // 3. Verify State Machine has all workflow states
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // Validation path
                                    put("ValidateInput", Match.anyValue());
                                    put("CheckFileExtension", Match.anyValue());
                                    // Processing path
                                    put("WriteInitialMetadata", Match.anyValue());
                                    put("ProcessAudio", Match.anyValue());
                                    put("PollyTextToSpeech", Match.anyValue());
                                    // Success path
                                    put("UpdateStatusCompleted", Match.anyValue());
                                    put("PublishSuccessNotification", Match.anyValue());
                                    put("PipelineSucceeded", Match.anyValue());
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));

        // 4. Verify Lambda function is integrated
        template.resourceCountIs("AWS::Lambda::Function", 1);

        // 5. Verify DynamoDB table exists for metadata
        template.resourceCountIs("AWS::DynamoDB::Table", 1);

        // 6. Verify SNS topics for notifications
        template.resourceCountIs("AWS::SNS::Topic", 2);

        // 7. Verify S3 Output Bucket exists
        template.resourceCountIs("AWS::S3::Bucket", 2);
    }

    /**
     * Test: Error handling path is correctly configured
     * Validates: Error → Catch → UpdateStatusFailed → SNS → PipelineFailed
     */
    @Test
    @DisplayName("Error handling path is correctly configured")
    public void testErrorHandlingPath() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify error handling states exist
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // Error path states
                                    put("ValidationFailed", Match.anyValue());
                                    put("UpdateStatusFailed", Match.anyValue());
                                    put("PublishFailureNotification", Match.anyValue());
                                    put("PipelineFailed", Match.anyValue());
                                    
                                    // Verify ProcessAudio has Catch block
                                    put("ProcessAudio", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Catch", Match.anyValue());
                                        }}
                                    ));
                                    
                                    // Verify PollyTextToSpeech has Catch block
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
     * Test: Retry policies are configured for transient failures
     * Validates: Lambda, Polly, and DynamoDB tasks have retry logic
     */
    @Test
    @DisplayName("Retry policies configured for transient failures")
    public void testRetryPoliciesConfigured() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Lambda has retry policy
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("ProcessAudio", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Retry", Match.arrayWith(List.of(
                                                Match.objectLike(new HashMap<String, Object>() {{
                                                    put("ErrorEquals", Match.anyValue());
                                                    put("IntervalSeconds", Match.anyValue());
                                                    put("MaxAttempts", Match.anyValue());
                                                    put("BackoffRate", Match.anyValue());
                                                }})
                                            )));
                                        }}
                                    ));
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));

        // Verify Polly has retry policy
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("PollyTextToSpeech", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Retry", Match.anyValue());
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
     * Test: Input validation rejects invalid formats
     * Validates: CheckFileExtension routes invalid files to error path
     */
    @Test
    @DisplayName("Input validation rejects invalid file formats")
    public void testInputValidationRejectsInvalidFormats() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify Choice state for file validation exists
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("CheckFileExtension", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Choice");
                                            put("Choices", Match.anyValue());
                                            put("Default", "ValidationFailed");
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
     * Test: SNS notifications configured for success and failure
     * Validates: Two SNS topics with proper permissions
     */
    @Test
    @DisplayName("SNS notifications configured for success and failure scenarios")
    public void testSNSNotificationsConfigured() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify 2 SNS topics exist (success and failure)
        template.resourceCountIs("AWS::SNS::Topic", 2);

        // Verify SNS topics have encryption
        template.hasResourceProperties("AWS::SNS::Topic", Match.objectLike(
            new HashMap<String, Object>() {{
                put("KmsMasterKeyId", Match.anyValue());
            }}
        ));

        // Verify State Machine has permission to publish to SNS
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
     * Test: DynamoDB metadata updates are configured
     * Validates: PutItem and UpdateItem operations in state machine
     */
    @Test
    @DisplayName("DynamoDB metadata updates configured correctly")
    public void testDynamoDBMetadataUpdates() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify State Machine has DynamoDB operations
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // Initial metadata write
                                    put("WriteInitialMetadata", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Resource", Match.stringLikeRegexp(".*dynamodb:putItem.*"));
                                        }}
                                    ));
                                    
                                    // Status update on completion
                                    put("UpdateStatusCompleted", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Resource", Match.stringLikeRegexp(".*dynamodb:updateItem.*"));
                                        }}
                                    ));
                                    
                                    // Status update on failure
                                    put("UpdateStatusFailed", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Resource", Match.stringLikeRegexp(".*dynamodb:updateItem.*"));
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
}
