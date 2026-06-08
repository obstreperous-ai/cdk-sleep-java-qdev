package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * Application Stage for CDK Pipelines deployment
 * 
 * This stage wraps the CdkBaseStack and allows it to be deployed
 * through CDK Pipelines to different environments (dev, stage, prod).
 */
public class SleepAudioApplicationStage extends Stage {
    
    private final CdkBaseStack appStack;
    
    public SleepAudioApplicationStage(final Construct scope, final String id, 
                                      final Environment environment, final String environmentName) {
        super(scope, id, StageProps.builder()
                .env(environment)
                .build());
        
        // Create the application stack within this stage
        appStack = new CdkBaseStack(this, "SleepAudioStack", 
            StackProps.builder()
                .env(environment)
                .build(),
            environmentName);
    }
    
    public CdkBaseStack getAppStack() {
        return appStack;
    }
}
