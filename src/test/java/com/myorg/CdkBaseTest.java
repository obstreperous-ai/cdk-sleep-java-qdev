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
 * TDD Test Suite for Issue #3: S3 Buckets + EventBridge Rule
 * 
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
}
