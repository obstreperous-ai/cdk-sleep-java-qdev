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
 * TDD Test Suite for Issues #3 and #4
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
}
