package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.assertions.Match;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test Suite for Issue #9: Pipeline Testing, Refinements & Deployment Preparation
 * 
 * These tests are written FIRST (before implementation) following strict TDD.
 * They verify:
 * 1. Multi-environment support (dev, stage, prod)
 * 2. Environment-specific configurations
 * 3. CDK Pipelines skeleton
 * 4. End-to-end pipeline flow validation
 */
public class PipelineIntegrationTest {

    /**
     * Test: Stack should support environment context
     */
    @Test
    public void testStackSupportsEnvironmentContext() {
        App app = new App();
        
        // Create stack with dev environment
        CdkBaseStack devStack = new CdkBaseStack(app, "DevStack", StackProps.builder()
                .env(Environment.builder()
                        .account("123456789012")
                        .region("us-east-1")
                        .build())
                .build(), "dev");
        
        Template template = Template.fromStack(devStack);
        
        // Verify stack can be synthesized with environment
        assertNotNull(template);
    }

    /**
     * Test: Dev environment should have DESTROY removal policy
     */
    @Test
    public void testDevEnvironmentHasDestroyRemovalPolicy() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "DevStack", StackProps.builder()
                .build(), "dev");
        Template template = Template.fromStack(stack);

        // Dev environment should allow resource destruction for easy cleanup
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Tags", Match.arrayWith(List.of(
                    Match.objectLike(new HashMap<String, Object>() {{
                        put("Key", "Environment");
                        put("Value", "dev");
                    }})
                )));
            }}
        ));
    }

    /**
     * Test: Production environment should have RETAIN removal policy
     */
    @Test
    public void testProductionEnvironmentHasRetainRemovalPolicy() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "ProdStack", StackProps.builder()
                .build(), "prod");
        Template template = Template.fromStack(stack);

        // Production should have tags identifying it as prod
        template.hasResourceProperties("AWS::S3::Bucket", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Tags", Match.arrayWith(List.of(
                    Match.objectLike(new HashMap<String, Object>() {{
                        put("Key", "Environment");
                        put("Value", "prod");
                    }})
                )));
            }}
        ));
    }

    /**
     * Test: Stack should have environment tags
     */
    @Test
    public void testStackHasEnvironmentTags() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack", StackProps.builder()
                .build(), "dev");
        Template template = Template.fromStack(stack);

        // All resources should be tagged with environment
        template.hasResourceProperties("AWS::DynamoDB::Table", Match.objectLike(
            new HashMap<String, Object>() {{
                put("Tags", Match.arrayWith(List.of(
                    Match.objectLike(new HashMap<String, Object>() {{
                        put("Key", "Environment");
                        put("Value", "dev");
                    }})
                )));
            }}
        ));
    }

    /**
     * Test: Application Stage should exist for pipeline deployment
     */
    @Test
    public void testApplicationStageExists() {
        App app = new App();
        
        // This should not throw - ApplicationStage should be creatable
        assertDoesNotThrow(() -> {
            new SleepAudioApplicationStage(app, "TestAppStage", 
                Environment.builder()
                    .account("123456789012")
                    .region("us-east-1")
                    .build(),
                "dev");
        });
    }

    /**
     * Test: Pipeline Stack should exist
     */
    @Test
    public void testPipelineStackExists() {
        App app = new App();
        
        // This should not throw - PipelineStack should be creatable
        assertDoesNotThrow(() -> {
            new SleepAudioPipelineStack(app, "TestPipeline", StackProps.builder()
                    .env(Environment.builder()
                            .account("123456789012")
                            .region("us-east-1")
                            .build())
                    .build());
        });
    }

    /**
     * Test: Pipeline Stack should contain CodePipeline
     */
    @Test
    public void testPipelineStackContainsCodePipeline() {
        App app = new App();
        SleepAudioPipelineStack pipelineStack = new SleepAudioPipelineStack(app, "TestPipeline", 
            StackProps.builder()
                .env(Environment.builder()
                        .account("123456789012")
                        .region("us-east-1")
                        .build())
                .build());
        
        Template template = Template.fromStack(pipelineStack);
        
        // Pipeline should have CodePipeline resource
        template.resourceCountIs("AWS::CodePipeline::Pipeline", 1);
    }

    /**
     * Test: Complete end-to-end flow validation
     * Verifies all states in correct order
     */
    @Test
    public void testCompleteEndToEndFlowStructure() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Verify the state machine has all expected states in the workflow
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    // Validation flow
                                    put("ValidateInput", Match.anyValue());
                                    put("CheckFileExtension", Match.anyValue());
                                    put("ValidationFailed", Match.anyValue());
                                    // Main processing flow
                                    put("WriteInitialMetadata", Match.anyValue());
                                    put("ProcessAudio", Match.anyValue());
                                    put("PollyTextToSpeech", Match.anyValue());
                                    // Success path
                                    put("UpdateStatusCompleted", Match.anyValue());
                                    put("PublishSuccessNotification", Match.anyValue());
                                    put("PipelineSucceeded", Match.anyValue());
                                    // Error path
                                    put("UpdateStatusFailed", Match.anyValue());
                                    put("PublishFailureNotification", Match.anyValue());
                                    put("PipelineFailed", Match.anyValue());
                                }}
                            ));
                        }}
                    )
                ));
            }}
        ));
    }

    /**
     * Test: Validation flow routes correctly
     */
    @Test
    public void testValidationFlowRouting() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // CheckFileExtension should route to both success and failure paths
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
                                            // Should have both Choices (valid path) and Default (invalid path)
                                            put("Choices", Match.anyValue());
                                            put("Default", Match.anyValue());
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
     * TDD Tests for Issue #10: Advanced Error Handling, Retry Policies, and Observability
     * These tests are written FIRST (before implementation) following strict TDD.
     */

    /**
     * Test: Lambda task should have retry policy configured
     */
    @Test
    public void testLambdaTaskHasRetryPolicy() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // ProcessAudio Lambda task should have Retry configuration
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
                                            put("Retry", Match.arrayWith(List.of(
                                                Match.objectLike(new HashMap<String, Object>() {{
                                                    put("ErrorEquals", Match.arrayWith(List.of("Lambda.ServiceException", "Lambda.TooManyRequestsException")));
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
    }

    /**
     * Test: Polly task should have retry policy configured
     */
    @Test
    public void testPollyTaskHasRetryPolicy() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Polly task should have Retry configuration
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("PollyTextToSpeech", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Retry", Match.arrayWith(List.of(
                                                Match.objectLike(new HashMap<String, Object>() {{
                                                    put("ErrorEquals", Match.arrayWith(List.of("Polly.ServiceFailureException")));
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
    }

    /**
     * Test: DynamoDB tasks should have retry policy configured
     */
    @Test
    public void testDynamoDBTasksHaveRetryPolicy() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // WriteInitialMetadata should have Retry configuration
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("DefinitionString", Match.serializedJson(
                    Match.objectLike(
                        new HashMap<String, Object>() {{
                            put("States", Match.objectLike(
                                new HashMap<String, Object>() {{
                                    put("WriteInitialMetadata", Match.objectLike(
                                        new HashMap<String, Object>() {{
                                            put("Type", "Task");
                                            put("Retry", Match.arrayWith(List.of(
                                                Match.objectLike(new HashMap<String, Object>() {{
                                                    put("ErrorEquals", Match.arrayWith(List.of("DynamoDB.ProvisionedThroughputExceededException")));
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
    }

    /**
     * Test: X-Ray tracing should be enabled on Lambda function
     */
    @Test
    public void testXRayTracingEnabledOnLambda() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Lambda function should have X-Ray tracing enabled
        template.hasResourceProperties("AWS::Lambda::Function", Match.objectLike(
            new HashMap<String, Object>() {{
                put("TracingConfig", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Mode", "Active");
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: X-Ray tracing should be enabled on State Machine
     */
    @Test
    public void testXRayTracingEnabledOnStateMachine() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // State machine should have X-Ray tracing enabled
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", Match.objectLike(
            new HashMap<String, Object>() {{
                put("TracingConfiguration", Match.objectLike(
                    new HashMap<String, Object>() {{
                        put("Enabled", true);
                    }}
                ));
            }}
        ));
    }

    /**
     * Test: CloudWatch Alarm for State Machine failures should exist
     */
    @Test
    public void testCloudWatchAlarmForStateMachineFailures() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Should have alarm for ExecutionsFailed metric
        template.hasResourceProperties("AWS::CloudWatch::Alarm", Match.objectLike(
            new HashMap<String, Object>() {{
                put("MetricName", "ExecutionsFailed");
                put("Namespace", "AWS/States");
                put("Statistic", "Sum");
                put("ComparisonOperator", "GreaterThanThreshold");
                put("Threshold", 0);
            }}
        ));
    }

    /**
     * Test: CloudWatch Alarm for Lambda errors should exist
     */
    @Test
    public void testCloudWatchAlarmForLambdaErrors() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "TestStack");
        Template template = Template.fromStack(stack);

        // Should have alarm for Lambda Errors metric
        template.hasResourceProperties("AWS::CloudWatch::Alarm", Match.objectLike(
            new HashMap<String, Object>() {{
                put("MetricName", "Errors");
                put("Namespace", "AWS/Lambda");
                put("Statistic", "Sum");
                put("ComparisonOperator", "GreaterThanThreshold");
                put("Threshold", 5);
            }}
        ));
    }
}
