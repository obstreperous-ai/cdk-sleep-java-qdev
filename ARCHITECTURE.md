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

## Event-Driven Sleep Audio Pipeline

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

The pipeline is designed to process sleep audio files uploaded to Amazon S3 through a fully automated, event-driven workflow. The architecture consists of the following stages:

#### Stage 1: Ingestion and Event Detection
The pipeline is designed to process sleep audio files uploaded to Amazon S3 through a fully automated, event-driven workflow. The architecture consists of the following stages:
**User Upload** → Users or applications upload content to the S3 input bucket
- Supported formats: Raw audio (MP3, WAV, FLAC, M4A) or text files (TXT, JSON) for TTS generation
#### 1. **Ingestion Stage (S3 Source Bucket)**
- **Purpose**: Receives raw sleep audio files from users or external systems
- **Bucket Name Pattern**: `sleep-audio-source-{accountId}-{region}`
- **File Formats**: MP3, WAV, FLAC, M4A
- **Encryption**: S3 bucket encryption enabled with AWS KMS
- **Versioning**: Enabled to prevent accidental data loss
- **Event Notifications**: Configured to emit S3 ObjectCreated events to EventBridge

#### 2. **Event Bus (Amazon EventBridge)**
- **Purpose**: Decouples event producers from consumers, enables flexible routing
- **Event Pattern**: Captures S3 ObjectCreated events with filtering rules
- **Event Routing**: EventBridge rule matches the S3 event pattern and triggers Step Functions execution
- **Benefits**: 
  - Multiple consumers can subscribe to the same event
  - Easy to add new processing workflows without modifying existing code
  - Built-in retry and error handling capabilities
  - Event replay for debugging and reprocessing

#### Stage 2: Orchestrated Processing (AWS Step Functions)

**Why Step Functions** (This is the KEY architectural choice):
- **Visual Workflow**: Provides graphical representation of processing logic for easier debugging and maintenance
- **Built-in Error Handling**: Automatic retries, catch blocks, and exponential backoff without custom code
- **State Management**: Maintains execution state for long-running processes (up to 1 year)
- **Service Integrations**: Native integrations with Polly, Bedrock, S3, DynamoDB without Lambda glue code
- **Audit Trail**: Complete execution history for compliance and troubleshooting

**Step Functions Workflow States:**

1. **Validation State**: Validates file format, size constraints, content type
2. **Metadata Extraction State**: Lambda function extracts audio properties (duration, bitrate, etc.)
3. **Text-to-Speech Generation State**: Direct Step Functions integration with **Amazon Polly**
   - Converts text content to natural-sounding speech using neural voices
   - Configurable voice parameters: language, voice ID, speaking rate, pitch
   - **Use Cases**: Guided meditations, sleep stories, affirmations, bedtime narratives
4. **AI Audio Enhancement State** (optional): Direct Step Functions integration with **Amazon Bedrock**
   - Uses foundation models (Stability AI, Amazon Titan) for generative audio
   - Generate ambient sleep sounds (rain, ocean waves, white noise, binaural beats)
   - Audio enhancement and noise reduction
   - **Use Cases**: Personalized soundscapes, background ambiance for meditations
5. **Audio Processing State**: Lambda function performs final processing (format conversion, normalization)
6. **Storage State**: Writes processed audio file to S3 output bucket
7. **Metadata Update State**: Updates DynamoDB record with processing results
8. **Notification State**: Publishes success/failure message to SNS topic

**Error Handling in Step Functions:**
- Each state has retry configuration with exponential backoff
- Catch blocks redirect errors to error notification states
- Failed executions logged to CloudWatch with full context

#### 3. **AWS Services Integration**

**Amazon Polly** (Text-to-Speech):
- Neural TTS voices for natural-sounding narration
- 60+ voices across 20+ languages
- SSML support for fine-grained control (pauses, emphasis, prosody)
- Cost: $4 per 1M characters

**Amazon Bedrock** (Generative AI):
- Access to multiple foundation models via single API
- Generate novel sleep soundscapes and ambient audio
- AI-powered audio enhancement and noise reduction
- Pay-per-token pricing

#### 4. **Lambda Functions (Supplemental Processing)**

**3a. Audio Metadata Extractor Lambda**
- **Trigger**: EventBridge rule matching S3 ObjectCreated events
- **Function**: 
  - Downloads audio file from source S3 bucket
  - Extracts metadata (duration, bitrate, sample rate, channels, format)
  - Generates unique audio ID
  - Validates file integrity and format
- **Runtime**: Java 17 with AWS SDK for Java v2
- **Memory**: 512 MB (configurable based on file sizes)
- **Timeout**: 5 minutes
- **Output**: Publishes custom event to EventBridge with metadata

**3b. Audio Transcoding Lambda (Future)**
- **Purpose**: Convert audio files to optimized formats for streaming
- **Triggers**: EventBridge event from metadata extractor
- **Technology**: FFmpeg Lambda Layer or AWS Elemental MediaConvert
- **Output Formats**: Multiple bitrate MP3 versions for adaptive streaming

**3c. Audio Analysis Lambda (Future)**
- **Purpose**: Analyze audio characteristics (volume levels, silence detection, frequency analysis)
- **Triggers**: EventBridge event from metadata extractor
- **Technology**: AWS Rekognition Audio or custom ML models
- **Use Cases**: Quality validation, automatic categorization

#### 4. **Storage Stage**

**4a. Processed Audio Storage (S3 Destination Bucket)**
- **Bucket Name Pattern**: `sleep-audio-processed-{accountId}-{region}`
- **Security**: Private bucket with Block Public Access enabled, SSE-S3 encryption
- **Purpose**: Stores processed/transcoded audio files
- **Lifecycle Policies**: 
  - Transition to S3 Intelligent-Tiering after 30 days
  - Archive to Glacier after 90 days for cost optimization
- **CDN Integration**: CloudFront distribution for global delivery (future)

**4b. Metadata Persistence (Amazon DynamoDB)**
- **Table Name**: `SleepAudioMetadata`
- **Primary Key**: `audioId` (String, UUID format)
- **Attributes**:
  - `audioId`: Unique identifier for the audio file
  - `sourceKey`: Original S3 object key
  - `fileName`: Original file name
  - `uploadTimestamp`: ISO 8601 timestamp
  - `duration`: Duration in seconds
  - `format`: Audio format (mp3, wav, etc.)
  - `bitrate`: Bitrate in kbps
  - `sampleRate`: Sample rate in Hz
  - `channels`: Number of audio channels (1=mono, 2=stereo)
  - `fileSize`: File size in bytes
  - `processingStatus`: Status (uploaded, processing, completed, failed)
  - `processedKey`: S3 key for processed audio (if applicable)
  - `tags`: Map of user-defined tags
  - `pollyVoiceId`: Amazon Polly voice used (if TTS)
  - `bedrockModelId`: Amazon Bedrock model used (if AI-generated)
- **Secondary Indexes**:
  - GSI on `uploadTimestamp` for time-based queries
  - GSI on `processingStatus` for monitoring
- **Point-in-Time Recovery**: Enabled for data protection
- **On-Demand Billing**: Suitable for variable workload patterns
- **Encryption**: AWS-managed keys (SSE-DynamoDB)

#### 5. **Notification Stage (Amazon SNS)**
- **Topic Name**: `SleepAudioProcessingNotifications`
- **Topic Name**: `SleepAudioProcessingNotifications-{environment}`
- **Purpose**: Notify subscribers about processing completion or failures
- **Subscribers**:
  - Email notifications for administrators
  - SQS queue for downstream application integration
  - Lambda function for custom notification logic
- **Message Format**: JSON with audioId, status, metadata, and error details
- **Encryption**: SNS messages encrypted in transit (TLS) and at rest (KMS)
- **Filtering**: SNS subscription filters for status-based routing

### Error Handling and Observability

- **Dead Letter Queues (DLQ)**: SQS DLQs attached to Lambda functions for failed event processing
  - `/aws/lambda/audio-metadata-extractor-{environment}`
  - `/aws/lambda/audio-processor-{environment}`
  - `/aws/states/sleep-audio-pipeline-{environment}` (Step Functions)
- **CloudWatch Logs**: All Lambda functions log to CloudWatch with structured JSON logging
  - Step Functions: ExecutionsFailed, ExecutionThrottled, ExecutionDuration
  - Lambda: Error rate, duration, concurrent executions
  - DynamoDB: SystemErrors, UserErrors
- **CloudWatch Alarms**: Alarms for Lambda errors, DLQ depth, and processing duration
- **X-Ray Tracing**: Distributed tracing enabled for end-to-end request tracking
  - `SleepAudio/FilesProcessed` (count, dimension: status)
  - `SleepAudio/PollyRequests` (count, dimension: voiceId)
  - `SleepAudio/BedrockRequests` (count, dimension: modelId)
- **Metrics**: Custom CloudWatch metrics for business KPIs (files processed, processing time, error rates)

## Architecture Diagram
The following Mermaid diagram illustrates the complete Event-Driven Sleep Audio Pipeline with AWS Step Functions orchestrating the workflow, Amazon Polly for text-to-speech, and Amazon Bedrock for AI-generated sleep sounds.


```mermaid
    User[👤 User/Application] -->|"1. Upload Audio/Text"| S3Source
    User[👤 User/Application] -->|Upload Audio| S3Source
    S3Source[("🪣 S3 Input Bucket<br/>sleep-audio-source<br/>(Raw Audio/Text)")]
    S3Source -->|"2. S3 ObjectCreated Event"| EventBridge
    S3Source -->|ObjectCreated Event| EventBridge
    EventBridge["⚡ Amazon EventBridge<br/>(Event Router)"]
    EventBridge -->|"3. Trigger Workflow"| StepFunctions
    EventBridge -->|Route Event| MetadataLambda
    StepFunctions["🔀 AWS Step Functions<br/>(Orchestrator)<br/>━━━━━━━━━━━<br/>• Validation<br/>• Metadata Extraction<br/>• AI Processing<br/>• Storage<br/>• Notification"]
    MetadataLambda -->|Publish Event| EventBridge
    StepFunctions -->|"4a. Read Source File"| S3Source
    StepFunctions -->|"4b. Extract Metadata"| Lambda1["λ Metadata Extractor<br/>(Java 17)"]
    Lambda1 -->|Metadata| StepFunctions
    ProcessingLambda -->|Send Notification| SNS
    StepFunctions -->|"5a. Text-to-Speech<br/>(if text input)"| Polly["🎤 Amazon Polly<br/>(Neural TTS)<br/>• 60+ Voices<br/>• SSML Support"]
    Polly -->|Synthesized Audio| StepFunctions
    SNS[SNS Topic<br/>Notifications]
    StepFunctions -->|"5b. AI Enhancement<br/>(optional)"| Bedrock["🤖 Amazon Bedrock<br/>(Generative AI)<br/>• Sleep Sounds<br/>• Audio Enhancement"]
    Bedrock -->|Enhanced Audio| StepFunctions
    
    StepFunctions -->|"6. Audio Processing"| Lambda2["λ Audio Processor<br/>(Java 17)<br/>• Format Conversion<br/>• Normalization"]
    Lambda2 -->|Processed Audio| StepFunctions
    
    StepFunctions -->|"7. Write Processed File"| S3Dest[("🪣 S3 Output Bucket<br/>sleep-audio-processed<br/>(Final Audio)")]
    StepFunctions -->|"8. Store Metadata"| DynamoDB[("💾 DynamoDB Table<br/>SleepAudioMetadata<br/>• Audio ID<br/>• Duration<br/>• Status<br/>• Timestamps")]
    
    StepFunctions -->|"9. Send Notification"| SNS["📢 Amazon SNS Topic<br/>(Notifications)"]
    
    SNS -->|"10a. Email Alert"| Email["📧 Email<br/>(DevOps Team)"]
    SNS -->|"10b. Queue Event"| SQS["📦 SQS Queue<br/>(App Integration)"]
    SNS -->|"10c. Custom Webhook"| Lambda3["λ Notification Handler<br/>(Custom Logic)"]
    
    CloudWatch["☁️ CloudWatch<br/>• Logs<br/>• Metrics<br/>• Alarms<br/>• X-Ray Tracing"]
    
    StepFunctions -.->|Logs/Metrics| CloudWatch
    Lambda1 -.->|Logs/Traces| CloudWatch
    Lambda2 -.->|Logs/Traces| CloudWatch
    Lambda3 -.->|Logs/Traces| CloudWatch
    
    DLQ["💀 Dead Letter Queue<br/>(Failed Events)"]
    EventBridge -.->|Failed Events| DLQ
    Lambda1 -.->|Errors| DLQ
    Lambda2 -.->|Errors| DLQ
    SNS -->|Trigger| NotifyLambda[Lambda<br/>Custom Notifications]
    
    style S3Source fill:#e67e22
    style S3Dest fill:#e67e22
    style StepFunctions fill:#2ecc71
    style Polly fill:#3498db
    style Bedrock fill:#f39c12
    style EventBridge fill:#9b59b6
    style DynamoDB fill:#3498db
    style CloudWatch fill:#95a5a6
    style DLQ fill:#c0392b
    style Lambda1 fill:#16a085
    style Lambda2 fill:#16a085
    style Lambda3 fill:#16a085
    style SNS fill:#e74c3c
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
- Cost-effective: $4 per 1M characters
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

## Multi-Environment Support

The infrastructure supports deployment to multiple environments (dev, stage, prod) using AWS CDK context values.

**Environment Configuration Example (cdk.json):**
```json
{
  "context": {
    "environments": {
      "dev": {
        "account": "111111111111",
        "region": "us-east-1",
        "removalPolicy": "DESTROY",
        "logRetentionDays": 7,
        "enableXRayTracing": false
      },
      "stage": {
        "account": "222222222222",
        "region": "us-east-1",
        "removalPolicy": "RETAIN",
        "logRetentionDays": 30,
        "enableXRayTracing": true
      },
      "prod": {
        "account": "333333333333",
        "region": "us-east-1",
        "removalPolicy": "RETAIN",
        "logRetentionDays": 90,
        "enableXRayTracing": true,
        "enablePITR": true
      }
    }
  }
}
```

**Deployment Commands:**
```bash
# Deploy to development
cdk deploy --context environment=dev

# Deploy to production
cdk deploy --context environment=prod
```

**Environment-Specific Behaviors:**
- **Development**: Fast iteration, resources destroyed on stack deletion, minimal alarms
- **Staging**: Pre-production validation, retained resources, moderate alarms
- **Production**: Full observability, PITR enabled, comprehensive alarms, multi-region ready

## Security Considerations

### IAM Least Privilege
- Every resource has a dedicated IAM role with minimum required permissions
- No wildcard (`*`) resource permissions in production
- Service-to-service permissions explicitly defined
- Step Functions execution role has scoped access to Polly, Bedrock, S3, DynamoDB

### Encryption
- **At Rest**: S3 (SSE-S3/AES-256), DynamoDB (AWS-managed keys), SNS (KMS)
- **In Transit**: TLS 1.2+ enforced for all API calls
- **Private Buckets**: Block Public Access enabled on all S3 buckets

### Network Security
- S3 bucket policies restrict access to IAM roles only
- Bucket policies deny non-HTTPS requests
- Future: VPC endpoints for private connectivity (avoid public internet)

## Cost Considerations

**Estimated Cost for 10,000 Audio Files/Month:**
- **S3**: ~$2 (storage + requests)
- **EventBridge**: ~$0.01
- **Step Functions**: ~$2 (80,000 state transitions)
- **Lambda**: ~$6 (compute time)
- **Amazon Polly**: ~$8 (20% using TTS, 1,000 chars each)
- **Amazon Bedrock**: ~$30 (30% using AI, varies by model)
- **DynamoDB**: ~$0.03 (on-demand pricing)
- **SNS**: ~$0.01
- **CloudWatch**: ~$19 (logs, metrics, alarms)

**Total: ~$67/month for 10,000 files (~$0.007 per file)**

**Cost Optimization Strategies:**
- S3 Lifecycle Policies (Intelligent-Tiering, Glacier archival)
- Lambda ARM64 (Graviton2) for 20% cost reduction
- Step Functions Express Workflows for short-duration tasks (if applicable)
- DynamoDB On-Demand (no wasted provisioned capacity)
- CloudWatch log retention tuning (7 days dev, 90 days prod)

## Observability

- **CloudWatch Logs**: Structured JSON logging for all Lambda functions and Step Functions
- **CloudWatch Metrics**: Custom metrics for files processed, Polly/Bedrock requests, processing duration
- **CloudWatch Alarms**: Alarms for execution failures, throttling, errors, DLQ depth
- **AWS X-Ray**: End-to-end distributed tracing across Step Functions and Lambda
- **CloudWatch Dashboards**: Real-time operational visibility (future)

## Future Extensibility

The architecture is designed for evolution:

### Short-Term (3-6 months)
1. **Multi-Format Output**: Generate multiple quality variants for adaptive streaming
2. **Advanced Audio Analysis**: ML-based quality scoring, silence detection
3. **API Gateway**: RESTful API for programmatic access
4. **CloudFront Distribution**: Global CDN for low-latency delivery

### Mid-Term (6-12 months)
1. **Real-Time Processing**: WebSocket API for live upload progress
2. **SageMaker Models**: Custom ML models for personalized sleep audio
3. **Multi-Region Deployment**: Active-active for global availability
4. **Batch Processing**: AWS Batch for large-scale jobs

### Long-Term (12+ months)
1. **Marketplace and Content Library**: Public catalog with search/browse
2. **Mobile Applications**: iOS/Android apps with offline playback
3. **Personalization Engine**: Amazon Personalize for recommendations
4. **Enterprise Features**: Multi-tenancy, white-label deployments, SLA guarantees

### Extensibility Points
- **EventBridge**: Add new workflows without modifying existing rules
- **Step Functions**: Extend state machine with new states non-invasively
- **DynamoDB**: NoSQL schema allows adding attributes without migrations
- **Lambda**: Microservices approach enables independent deployments
- **SNS**: Pub/sub pattern allows new subscribers without publisher changes

---

## Document Maintenance

**Last Updated**: Issue #2 - Foundational Architecture Design  
**Maintained By**: qdev team  
**Next Review**: After Issue #3 (TDD: S3 Buckets + EventBridge Rule)  

**Update Triggers:**
- New AWS service integration
- Significant architectural change
- Security enhancements
- Multi-region or DR implementations

**Synchronization Requirement:**
This document MUST be updated whenever CDK infrastructure code changes. See `.github/AGENT_GUIDELINES.md` for detailed synchronization requirements. ARCHITECTURE.md is the **single source of truth** for all system design decisions.

---

**Next Steps**: Issue #3 will implement the core S3 buckets and EventBridge rule using TDD (tests first, always).
