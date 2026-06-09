package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class CdkBaseApp {
    public static void main(final String[] args) {
        App app = new App();
        
        // Get environment from context (default to "dev")
        String environment = (String) app.getNode().tryGetContext("environment");
        if (environment == null) {
            environment = "dev";
        }
        
        // Determine stack name based on environment
        String stackName = "CdkBaseStack";
        if (!environment.equals("dev")) {
            stackName = "CdkBaseStack-" + environment;
        }

        new CdkBaseStack(app, stackName, StackProps.builder()
                // Environment-agnostic by default for portability
                // For specific account/region deployment, set CDK_DEFAULT_ACCOUNT and CDK_DEFAULT_REGION
                // or use cdk.json context
                // Account/Region-dependent features and context lookups will not work,
                // but a single synthesized template can be deployed anywhere.

                // Uncomment the next block to specialize this stack for the AWS Account
                // and Region that are implied by the current CLI configuration.
                /*
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                */

                // Uncomment the next block if you know exactly what Account and Region you
                // want to deploy the stack to.
                /*
                .env(Environment.builder()
                        .account("123456789012")
                        .region("us-east-1")
                        .build())
                */

                // For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html
                .build(), 
                environment);

        app.synth();
    }
}

