# Architecture Documentation

## Project Overview

The **cdk-sleep-java-qdev** project is a production-grade, event-driven, serverless sleep audio processing pipeline built on AWS Cloud Development Kit (CDK) using Java. This architecture demonstrates a modern, scalable approach to audio file processing with automated event handling, AI-powered audio generation and enhancement, storage management, metadata persistence, and notification capabilities.

The pipeline enables users to upload raw audio files or text content, which are then automatically processed using AWS Step Functions orchestration, enhanced with Amazon Polly for text-to-speech conversion and Amazon Bedrock for AI-generated sleep sounds, and delivered as high-quality processed audio files optimized for sleep and relaxation applications.

## Architectural Philosophy

This project follows AWS Well-Architected Framework principles:
- **Operational Excellence**: Infrastructure as Code (IaC) with AWS CDK for repeatable deployments
- **Security**: Least privilege IAM policies, encryption at rest and in transit
- **Reliability**: Event-driven architecture with automatic retries and dead-letter queues
- **Performance Efficiency**: Serverless compute with Lambda, automatic scaling
- **Cost Optimization**: Pay-per-use serverless components, no idle resources
- **Sustainability**: Serverless architecture minimizes resource waste

## Current Implementation Status

**Issue #4 Completed**: Step Functions State Machine with Polly Integration implemented.

The following components have been implemented:
- ✅ Input S3 Bucket with encryption, versioning, and EventBridge notifications
- ✅ Output S3 Bucket with encryption and versioning
- ✅ EventBridge Rule triggering on S3 Object Created events
- ✅ AWS Step Functions State Machine orchestrating the workflow
- ✅ Amazon Polly integration for text-to-speech synthesis

**Next Steps**: Issue #5 will add DynamoDB metadata table and enhance state machine input/output handling.

## Event-Driven Sleep Audio Pipeline

### Future Architecture Vision
### High-Level Flow
### High-Level Overview

The pipeline processes sleep audio content through a fully automated, event-driven workflow. Users can upload either raw audio files or text content to an input S3 bucket. The system automatically detects uploads via EventBridge, orchestrates complex processing workflows using AWS Step Functions, leverages AI services (Amazon Polly and Bedrock) for audio generation and enhancement, stores results in an output S3 bucket, persists metadata in DynamoDB, and notifies stakeholders via SNS.

**Key Differentiators:**
- **Step Functions Orchestration**: Provides visual workflow management, automatic retries, error handling, and long-running task coordination superior to individual Lambda functions
- **AI-Powered Audio Generation**: Integrates Amazon Polly for natural-sounding voice narration and Amazon Bedrock for generative AI sleep soundscapes
- **Scalable and Cost-Effective**: Serverless architecture scales automatically from zero to thousands of requests with pay-per-use pricing
- **Multi-Environment Ready**: Designed for dev/stage/prod environments with CDK context-based configuration

### Data Flow Overview

**User Upload** → **S3 Input Bucket** → **EventBridge** → **Step Functions Workflow** → (**Polly TTS** + **Bedrock AI** + **Lambda Processing**) → **S3 Output Bucket** + **DynamoDB Metadata** → **SNS Notifications** → **Email/SQS/Lambda Subscribers**

### Detailed Pipeline Stages

The pipeline is designed to process sleep audio files uploaded to Amazon S3 through a fully automated, event-driven workflow. 

### Currently Implemented (Issue #3 and #4)

#### 1. **Input S3 Bucket** (`SleepAudioInputBucket`)
- **Encryption**: SSE-S3 (AWS-managed encryption)
- **Versioning**: Enabled to prevent accidental data loss
- **EventBridge Integration**: Enabled to emit S3 ObjectCreated events
- **Public Access**: Fully blocked (BlockPublicAccess.BLOCK_ALL)
- **Removal Policy**: DESTROY (for dev environment - should be configurable per environment)
- **Purpose**: Entry point for raw audio files or text content for processing

#### 2. **Output S3 Bucket** (`SleepAudioOutputBucket`)
- **Encryption**: SSE-S3 (AWS-managed encryption)
- **Versioning**: Enabled
- **Public Access**: Fully blocked
- **Removal Policy**: DESTROY (for dev environment)
- **Purpose**: Storage for processed audio files (to be implemented)

#### 3. **EventBridge Rule** (`SleepAudioS3ObjectCreatedRule`)
- **Event Pattern**: Matches S3 ObjectCreated events from the input bucket
- **Source**: aws.s3
- **Detail Type**: "Object Created"
- **Filter**: Only triggers for events from the specific input bucket
- **Target**: AWS Step Functions State Machine (triggers workflow execution)
- **Input Transformation**: Passes S3 bucket name, object key, event time, and details to state machine
- **Description**: "Triggers on object creation in Sleep Audio input bucket"

#### 4. **AWS Step Functions State Machine** (`SleepAudioPipelineStateMachine`)
- **Type**: STANDARD (for long-running workflows and visual tracking)
- **Logging**: CloudWatch Logs with ALL level and execution data included
- **X-Ray Tracing**: Enabled for distributed tracing
- **IAM Role**: Least privilege with scoped permissions for Polly, S3 read/write
- **Workflow Definition**:
  - **Polly Task** (`PollyTextToSpeech`): Invokes Amazon Polly `SynthesizeSpeech` API
    - Placeholder text (to be replaced with dynamic content)
    - Output format: MP3
    - Voice: Joanna (Neural engine for natural sound)
  - **Success State** (`ProcessingComplete`): Marks workflow completion
- **Future Enhancements**: Will add S3 object retrieval, DynamoDB writes, and error handling

### Future Components (Not Yet Implemented)

The following components are planned for future issues:

#### Planned: Amazon Polly Integration
- Current: Basic Polly task with placeholder text
- Future enhancements:
  - Dynamic text extraction from S3 objects
  - SSML support for advanced voice control
  - Voice selection based on content type
- Text-to-speech generation for sleep stories and meditations
- Neural TTS voices for natural narration

#### Planned: Amazon Bedrock Integration
- AI-generated ambient sleep sounds
- Audio enhancement capabilities

#### Planned: DynamoDB Table
- **Table Name**: `SleepAudioMetadata`
- Metadata storage for audio files and processing status

#### Planned: SNS Topic
- **Topic Name**: `SleepAudioProcessingNotifications`
- Notifications for processing completion/failure
- **Metrics**: Custom CloudWatch metrics for business KPIs (files processed, processing time, error rates)

## Current Architecture Diagram (Issue #4 Implementation)
The following Mermaid diagram illustrates the current Event-Driven Sleep Audio Pipeline with AWS Step Functions orchestrating the workflow and Amazon Polly for text-to-speech.


graph LR
    User[👤 User/Application] -->|"1. Upload Audio/Text"| InputBucket
    
    InputBucket["🪣 S3 Input Bucket<br/><b>SleepAudioInputBucket</b><br/>━━━━━━━━━━━<br/>✅ Encrypted (SSE-S3)<br/>✅ Versioned<br/>✅ EventBridge Enabled<br/>✅ Public Access Blocked"]
    Bedrock -->|Enhanced Audio| StepFunctions
    InputBucket -->|"2. S3 ObjectCreated Event"| EventBridge
    Lambda2 -->|Processed Audio| StepFunctions
    EventBridge["⚡ Amazon EventBridge<br/><b>SleepAudioS3ObjectCreatedRule</b><br/>━━━━━━━━━━━<br/>✅ Matches S3 Object Created<br/>✅ Filters by Input Bucket<br/>✅ State: ENABLED"]
    
    EventBridge -->|"3. Start Execution<br/>(with S3 details)"| StepFunctions["🔀 Step Functions<br/><b>SleepAudioPipelineStateMachine</b><br/>━━━━━━━━━━━<br/>✅ Standard Workflow<br/>✅ CloudWatch Logging<br/>✅ X-Ray Tracing<br/>✅ Polly Integration"]
    
    StepFunctions -->|"4. Synthesize Speech"| Polly["🎙️ Amazon Polly<br/><b>Neural TTS</b><br/>━━━━━━━━━━━<br/>• Voice: Joanna<br/>• Format: MP3<br/>• Engine: Neural"]
    
    Polly -->|"5. Audio Stream"| StepFunctions
    
    StepFunctions -->|"6. Processing Complete"| SuccessState["✅ Success State<br/>(ProcessingComplete)"]
    
    OutputBucket["🪣 S3 Output Bucket<br/><b>SleepAudioOutputBucket</b><br/>━━━━━━━━━━━<br/>✅ Encrypted (SSE-S3)<br/>✅ Versioned<br/>✅ Public Access Blocked<br/><i>(Ready for processed files)</i>"]
    
    CloudWatch["☁️ CloudWatch Logs<br/><b>StateMachineLogGroup</b><br/>━━━━━━━━━━━<br/>✅ Retention: 7 days<br/>✅ Execution Data<br/>✅ Log Level: ALL"]
    
    StepFunctions -.->|"Logs"| CloudWatch
    
    %% Future components (dashed)
    StepFunctions -.->|"🔮 Future: Store"| OutputBucket
    StepFunctions -.->|"🔮 Future: Metadata"| DynamoDB[("💾 DynamoDB<br/><i>(Planned)</i>")]
    StepFunctions -.->|"🔮 Future: Notify"| SNS["📢 SNS<br/><i>(Planned)</i>"]
    
    style InputBucket fill:#27ae60,stroke:#1e8449,stroke-width:3px,color:#fff
    style OutputBucket fill:#27ae60,stroke:#1e8449,stroke-width:3px,color:#fff
    style EventBridge fill:#9b59b6,stroke:#8e44ad,stroke-width:3px,color:#fff
    style StepFunctions fill:#e67e22,stroke:#d35400,stroke-width:3px,color:#fff
    style Polly fill:#3498db,stroke:#2980b9,stroke-width:3px,color:#fff
    style SuccessState fill:#27ae60,stroke:#1e8449,stroke-width:2px,color:#fff
    style CloudWatch fill:#95a5a6,stroke:#7f8c8d,stroke-width:2px,color:#fff
    style DynamoDB fill:#bdc3c7,stroke:#95a5a6,stroke-width:2px,stroke-dasharray: 5 5
    style SNS fill:#bdc3c7,stroke:#95a5a6,stroke-width:2px,stroke-dasharray: 5 5
    
    classDef implemented fill:#27ae60,stroke:#1e8449,stroke-width:3px,color:#fff
    classDef future fill:#bdc3c7,stroke:#95a5a6,stroke-width:2px,stroke-dasharray: 5 5

**Legend:**
- ✅ **Solid boxes with green**: Currently implemented and tested
- 🔮 **Dashed boxes**: Planned for future issues

## Implementation Details

### Security Best Practices (Implemented)

#### Encryption at Rest
- All S3 buckets use SSE-S3 encryption (AWS-managed keys)
- Future: Option to use KMS CMK for additional control

#### Least Privilege Access
- S3 buckets have public access completely blocked
- EventBridge rule has minimal permissions (CDK automatically creates least-privilege IAM roles)

#### Data Protection
- Versioning enabled on both buckets to prevent accidental data loss
- Removal policy set to DESTROY for dev (should be RETAIN for production)

### Testing Approach (TDD)

All infrastructure is developed following strict Test-Driven Development:

1. **Tests Written First**: All assertions written before implementation
2. **CDK Assertions**: Using @aws-cdk/assertions library for CloudFormation template validation
3. **Test Coverage**:
   - Input bucket encryption, versioning, and EventBridge configuration
   - Output bucket encryption and versioning
   - EventBridge rule event pattern and state
   - Public access blocking on all buckets
   - Resource counts and properties
   - Step Functions state machine existence and properties
   - State machine logging configuration
   - IAM roles and Polly permissions
   - EventBridge targeting Step Functions
**Test File**: `src/test/java/com/myorg/CdkBaseTest.java`

### Code Organization

**Main Stack**: `src/main/java/com/myorg/CdkBaseStack.java`
- Input bucket: `SleepAudioInputBucket`
- Output bucket: `SleepAudioOutputBucket`
- EventBridge rule: `SleepAudioS3ObjectCreatedRule`
- State machine log group: `SleepAudioStateMachineLogGroup`
- State machine: `SleepAudioPipelineStateMachine`
  - Polly task: `PollyTextToSpeech`
  - Success state: `ProcessingComplete`
- Placeholder target: CloudWatch Log Group
  - Target: Step Functions state machine with input transformation

**State Machine Workflow**:
1. Receives S3 event details (bucket, key, eventTime, eventDetail)
2. Invokes Polly `SynthesizeSpeech` with placeholder text
3. Completes successfully (future: will write to S3 and DynamoDB)
```
## AWS Services Rationale

### Why Step Functions?
- **Visual Workflow**: Auto-generated state machine diagrams for documentation
- **Error Handling**: Built-in retries, exponential backoff, catch blocks
- **Service Integrations**: Direct AWS service calls (Polly, Bedrock, S3, DynamoDB) without Lambda glue code
- **Long-Running**: Supports executions up to 1 year
- **Cost**: Pay per state transition (~$25 per 1M transitions)

### Why Amazon Polly?
- State-of-the-art neural TTS voices (remarkably natural)
- 60+ voices across 20+ languages
- SSML support for fine-grained voice control
- **Implemented**: Decouples event producers from consumers
- Managed service with automatic scaling

### Why Amazon Bedrock?
- Access to multiple foundation models via single API
- Generative AI for novel sleep soundscapes
- Serverless and fully managed (no ML infrastructure)
- Data privacy: Training data doesn't leave AWS
- Pay-per-token pricing

### Why EventBridge vs. Direct S3→Lambda?
- Decouples producers from consumers
- Multiple workflows can subscribe to same event
- Event replay for debugging
- Schema registry for event documentation

### Why DynamoDB On-Demand?
- Serverless with automatic scaling
- Single-digit millisecond latency
- No capacity planning required
- Pay only for actual read/write requests
        "account": "<account>",
## Multi-Environment Support

The infrastructure supports deployment to multiple environments (dev, stage, prod) using AWS CDK context values.

        "removalPolicy": "RETAIN",
        "logRetentionDays": 30,
        "enableXRayTracing": true
      },
      "prod": {
        "account": "333333333333",
**Note**: Currently configured for dev environment with DESTROY removal policy for easy cleanup during development.
# Deploy to development
cdk deploy --context environment=dev

# Deploy to production
- ✅ Step Functions execution role with scoped access to:
  - `polly:SynthesizeSpeech` (for TTS generation)
  - S3 read access to input bucket
  - S3 write access to output bucket
- ✅ EventBridge rule uses CDK-managed IAM roles with minimal permissions
- 🔮 Future: Step Functions execution role with scoped access
- **Development**: Fast iteration, resources destroyed on stack deletion, minimal alarms
- **Staging**: Pre-production validation, retained resources, moderate alarms
- ✅ **At Rest**: S3 buckets use SSE-S3 encryption
- ✅ **State Machine Logs**: Encrypted at rest in CloudWatch Logs
- ✅ **Private Buckets**: BlockPublicAccess enabled on all buckets
- ✅ **In Transit**: AWS SDK enforces TLS 1.2+ for all API calls

### IAM Least Privilege
- ✅ Step Functions execution role follows least privilege:
  - Only necessary Polly actions
  - Scoped S3 permissions (read input, write output)
- ✅ S3 bucket policies restrict access to IAM roles only (enforced by BlockPublicAccess)
- ✅ CDK automatically enforces secure access patterns
- 🔮 Future: VPC endpoints for private connectivity
- Step Functions execution role has scoped access to Polly, Bedrock, S3, DynamoDB
**Current Infrastructure Cost (Issue #4):**
### Encryption
**Current Infrastructure Cost (Issue #3):**
- **Step Functions**: ~$0.025 per 1K state transitions (Standard workflow)
- **Polly**: Pay per character (~$16 per 1M characters for Neural voices)
- **S3**: Minimal storage costs + requests (~$0.50/month for light usage)
- **EventBridge**: ~$0.01/month (first 1M events free, then $1 per million)
**Total Current: ~$1-2/month for basic infrastructure (light usage)**

**Note**: Actual costs depend on usage. Polly charges apply when state machine executes.


**Total Current: <$2/month for basic infrastructure**
- Lambda, Bedrock, DynamoDB will add costs based on usage
**Future Costs** (when processing is added):
- Step Functions, Lambda, Polly, Bedrock, DynamoDB will add ~$65-70/month for 10K files
- **SNS**: ~$0.01
- **CloudWatch**: ~$19 (logs, metrics, alarms)

- ✅ CloudWatch Logs for Step Functions state machine (ALL level, execution data included)
- ✅ AWS X-Ray tracing enabled on state machine
**Currently Implemented:**
- ✅ CloudWatch Logs for EventBridge rule (via placeholder Log Group target)

**Future Observability (not yet implemented):**
- 🔮 Structured JSON logging for Lambda functions
- 🔮 CloudWatch Alarms for errors and throttling
- 🔮 AWS X-Ray distributed tracing
- 🔮 CloudWatch Dashboards
- Step Functions Express Workflows for short-duration tasks (if applicable)
- DynamoDB On-Demand (no wasted provisioned capacity)
- CloudWatch log retention tuning (7 days dev, 90 days prod)

## Observability

4. **CloudFront CDN**: Global content delivery
- **CloudWatch Metrics**: Custom metrics for files processed, Polly/Bedrock requests, processing duration
- **CloudWatch Alarms**: Alarms for execution failures, throttling, errors, DLQ depth
- **AWS X-Ray**: End-to-end distributed tracing across Step Functions and Lambda
- **CloudWatch Dashboards**: Real-time operational visibility (future)

## Future Extensibility
1. **Content Marketplace**: Public catalog with search/browse
The architecture is designed for evolution:

### Short-Term (3-6 months)
1. **Multi-Format Output**: Generate multiple quality variants for adaptive streaming
2. **Advanced Audio Analysis**: ML-based quality scoring, silence detection
3. **API Gateway**: RESTful API for programmatic access
- **DynamoDB**: NoSQL schema allows schema evolution

### Mid-Term (6-12 months)
1. **Real-Time Processing**: WebSocket API for live upload progress
2. **SageMaker Models**: Custom ML models for personalized sleep audio
3. **Multi-Region Deployment**: Active-active for global availability
**Last Updated**: Issue #4 - TDD Implementation of Step Functions State Machine + Polly Integration  
**Last Updated**: Issue #3 - TDD Implementation of S3 Buckets + EventBridge Rule  
**Next Review**: After Issue #5 (TDD: DynamoDB Metadata Table + State Machine I/O Handling)  
**Next Review**: After Issue #4 (TDD: Step Functions State Machine + Polly Integration)  
2. **Mobile Applications**: iOS/Android apps with offline playback
3. **Personalization Engine**: Amazon Personalize for recommendations
4. **Enterprise Features**: Multi-tenancy, white-label deployments, SLA guarantees

### Extensibility Points
- After each issue completion
- **Step Functions**: Extend state machine with new states non-invasively
- **DynamoDB**: NoSQL schema allows adding attributes without migrations
This document is kept in sync with actual CDK implementation. Every infrastructure change is reflected here. ARCHITECTURE.md is the **single source of truth** for system design decisions.
- **SNS**: Pub/sub pattern allows new subscribers without publisher changes

---
**Next Steps**: Issue #5 will add DynamoDB metadata table and enhance state machine to read S3 objects, process content, write results, and store metadata using strict TDD (tests first, always).
