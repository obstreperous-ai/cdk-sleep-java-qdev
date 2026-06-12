# Architecture Documentation

## System Overview

**cdk-sleep-java-qdev** is an event-driven, serverless sleep audio processing pipeline built with AWS CDK (Java). The system automatically processes audio files uploaded to S3, enriches metadata, synthesizes speech, and notifies downstream systems—all with zero server management and automatic scaling.

## Architecture Diagram (Updated for Issue #11)

```mermaid
graph TB
    User[User/Client] -->|1. Upload Audio| InputBucket[S3 Input Bucket<br/>Versioned, Encrypted]
    EventBridge -->|3. Trigger| StateMachine[Step Functions<br/>State Machine<br/>X-Ray Tracing Enabled]
    InputBucket -->|2. Object Created Event| EventBridge[EventBridge Rule]
    EventBridge -->|3. Trigger| StateMachine[Step Functions<br/>State Machine]
    
    StateMachine -->|4. Validate Input| Validation{ValidateInput<br/>CheckFileExtension}
    Validation -->|Invalid Format| ValidationError[ValidationFailed<br/>Pass State]
    ValidationError -->|Update Status| DynamoFailed[UpdateStatusFailed]
    DynamoFailed -->|Notify| FailureSNS[SNS Topic<br/>Pipeline Failed<br/>KMS Encrypted]
    
    Validation -->|Valid Format| PutMetadata[WriteInitialMetadata<br/>DynamoDB]
    PutMetadata -->|5a. Record Created| DynamoDB[(DynamoDB Table<br/>audioId PK)]
    PutMetadata -->|5b. Invoke| Lambda[Lambda Function<br/>SleepAudioProcessor<br/>FULL PROCESSING<br/>Java 17<br/>X-Ray Tracing]
    Lambda -->|Read| InputBucket
    Lambda -->|Write| OutputBucket[S3 Output Bucket<br/>Versioned, Encrypted]
    Lambda -->|Generate Audio| Polly
    Lambda -->|Update Metadata| DynamoDB
    
    Lambda -->|6. Success Response| PollyTask[PollyTextToSpeech<br/>State Machine Task]
    PollyTask -->|7. Continue| UpdateComplete[UpdateStatusCompleted<br/>DynamoDB]
    
    UpdateComplete -->|8. Update Status| DynamoDB
    UpdateComplete -->|9. Success| SuccessSNS[SNS Topic<br/>Pipeline Completed<br/>KMS Encrypted]
    
    Lambda -.->|Lambda.ServiceException<br/>Retry 3x + Exp Backoff| Lambda
    Lambda -.->|Error + Catch| ErrorPath[Error Handler]
    PollyTask -.->|Polly.ServiceFailureException<br/>Retry 2x + Exp Backoff| PollyTask
    PollyTask -.->|Error + Catch| ErrorPath
    PutMetadata -.->|DynamoDB Throttle<br/>Retry 3x| PutMetadata
    ErrorPath -.->|Route by Error Type| DynamoFailed
    
    StateMachine -.->|Failures| Alarm1[CloudWatch Alarm<br/>Execution Failures]
    Lambda -.->|Errors > 5| Alarm2[CloudWatch Alarm<br/>Lambda Errors]
    
    StateMachine -.->|Logs| CloudWatch[CloudWatch Logs<br/>All Execution Data]
    Lambda -->|Logs| CloudWatch
    
    SuccessSNS -->|Notify| Subscribers[Email/SMS/Lambda<br/>Subscribers]
    FailureSNS -->|Alert| Subscribers
    
    style InputBucket fill:#4CAF50,stroke:#2E7D32,color:#fff
    style OutputBucket fill:#4CAF50,stroke:#2E7D32,color:#fff
    style DynamoDB fill:#FF9800,stroke:#E65100,color:#fff
    style StateMachine fill:#2196F3,stroke:#0D47A1,color:#fff
    style Validation fill:#FFC107,stroke:#F57F17,color:#000
    style Lambda fill:#FF5722,stroke:#BF360C,color:#fff
    style PollyTask fill:#9C27B0,stroke:#4A148C,color:#fff
    style SuccessSNS fill:#8BC34A,stroke:#33691E,color:#fff
    style FailureSNS fill:#F44336,stroke:#B71C1C,color:#fff
    style Alarm1 fill:#FF5722,stroke:#BF360C,color:#fff
    style Alarm2 fill:#FF5722,stroke:#BF360C,color:#fff
    style EventBridge fill:#FFC107,stroke:#F57F17,color:#000
```

## Architecture Flow

## Deployment Architecture (NEW in Issue #9)

```mermaid
graph LR
    Developer[Developer] -->|Push Code| CodeRepo[Code Repository]
    CodeRepo -->|Trigger| Pipeline[CDK Pipeline]
    Pipeline -->|Synth & Test| Build[Build & Test]
    Build -->|Deploy| DevEnv[Dev Environment<br/>Auto-deploy]
    DevEnv -->|Tests Pass| StageEnv[Stage Environment<br/>Auto-deploy]
    StageEnv -->|Manual Approval| ProdEnv[Prod Environment<br/>Retain Policy]
    
    DevEnv -.->|DESTROY Policy| DevResources[Dev Resources<br/>Auto-cleanup]
    StageEnv -.->|RETAIN Policy| StageResources[Stage Resources<br/>Protected]
    ProdEnv -.->|RETAIN Policy| ProdResources[Prod Resources<br/>Protected]
    
    style Pipeline fill:#2196F3,stroke:#0D47A1,color:#fff
    style DevEnv fill:#4CAF50,stroke:#2E7D32,color:#fff
    style StageEnv fill:#FFC107,stroke:#F57F17,color:#000
    style ProdEnv fill:#F44336,stroke:#B71C1C,color:#fff
```

### Multi-Environment Strategy
- **Dev**: Rapid development, auto-cleanup (DESTROY policy)
- **Stage**: Pre-production testing, data retention (RETAIN policy)
- **Prod**: Production workloads, full data protection (RETAIN policy)

CDK Pipelines skeleton created for future automated deployment across environments.

### Complete End-to-End Pipeline with Input Validation
### 1. File Upload (Entry Point)
- User uploads audio file to **S3 Input Bucket**
- Bucket has versioning and S3-managed encryption enabled
- EventBridge notifications are enabled on the bucket

### 2. Event Triggering
- S3 emits **Object Created** event to EventBridge
- **EventBridge Rule** filters for events from the input bucket
- Rule transforms the event and triggers Step Functions state machine

### 3. Workflow Orchestration (Step Functions)
The state machine orchestrates the following steps:
The state machine orchestrates a complete pipeline with input validation and error handling:

#### Step 3.0: Input Validation (NEW in Issue #8)
- **ValidateInput** (Pass state) extracts and validates input fields
- Validates bucket name, object key, and event time are present
- Extracts file extension for format validation
- **CheckFileExtension** (Choice state) validates supported formats
  - Supported formats: `.mp3`, `.wav`, `.m4a`, `.flac`
  - Valid files proceed to processing pipeline
  - Invalid files route to error path
- **ValidationFailed** (Pass state) captures validation errors
  - Sets clear error message for unsupported formats
  - Routes to UpdateStatusFailed → SNS failure notification → End
#### Step 3a: Write Initial Metadata
#### Step 3.1: Write Initial Metadata
- Stores: `audioId`, `status: PROCESSING`, `inputBucket`, `inputKey`, `createdAt`
- Provides audit trail and enables status tracking

#### Step 3b: Lambda Processing
#### Step 3.2: Lambda Processing with FULL AUDIO PROCESSING (NEW in Issue #11)
- **Lambda receives**: S3 event details (bucket, key, audioId, eventTime)
- **Input Validation**:
  - Validates bucket and key are not empty
  - Checks file extension matches supported formats
  - Throws exceptions for invalid inputs

- **Core Audio Processing** (NEW in Issue #11):
  1. **Download Input**: Retrieves file from Input S3 bucket using AWS SDK v2
  2. **Determine Processing Type**:
     - Small files (<1KB) or .txt files → Text-to-Speech mode
     - Larger audio files → Audio enhancement mode
  3. **Text-to-Speech Processing** (if text input):
     - Reads text content from input
     - Enhances short text with sleep-friendly introductions
     - Uses Amazon Polly (Joanna neural voice) to synthesize speech
     - Generates MP3 audio output
  4. **Audio Processing** (if audio input):
     - Basic pass-through processing (placeholder for future enhancements)
     - Future: volume normalization, frequency filtering, ambient mixing
  5. **Upload Output**:
     - Generates output key: `processed/{originalname}-{timestamp}.ext`
     - Uploads processed audio to Output S3 bucket
     - Sets content-type to audio/mpeg
  6. **Update DynamoDB Metadata**:
     - Stores output S3 URI
     - Records output file size
     - Logs processing type (TEXT_TO_SPEECH or AUDIO_PROCESSING)
     - Updates timestamp
  7. **Return Response**:
     - Status: COMPLETED
     - Output S3 location and metadata
     - Processing type and file size

  - Update DynamoDB with enriched data
  - Perform audio transformations

- **Note**: Polly is now called WITHIN the Lambda function (Issue #11)
- State machine Polly task remains for compatibility and future enhancements
- Uses neural voice (Joanna) for high-quality output
- Currently uses placeholder text (to be replaced with dynamic content)

- Returns audio stream metadata
#### Step 3.4: Status Update (Success Path)
#### Step 3d: Status Update (Success Path)
- **DynamoDB UpdateItem** updates status to `COMPLETED`

- Adds `updatedAt` timestamp
#### Step 3.5: Success Notification
#### Step 3e: Success Notification
- **SNS Publish** sends completion notification
- Includes full execution context for downstream processing

- Triggers PipelineSucceeded state
#### Step 3.6: Status Update (Failure Path)
#### Step 3f: Status Update (Failure Path)
- **DynamoDB UpdateItem** updates status to `FAILED`
- Stores error information in `errorInfo` attribute

- Handles both validation failures and processing errors
#### Step 3.7: Failure Notification
#### Step 3g: Failure Notification
- **SNS Publish** sends failure alert
- Includes error details for troubleshooting

## Error Handling Strategy (NEW in Issue #10)

### Retry Policies with Exponential Backoff

The pipeline implements intelligent retry policies for transient failures:

**Lambda Function (ProcessAudio)**:
- Errors: `Lambda.ServiceException`, `Lambda.TooManyRequestsException`
- Initial interval: 2 seconds
- Max attempts: 3
- Backoff rate: 2.0 (exponential)
- Total retry time: ~14 seconds maximum

**Amazon Polly (Text-to-Speech)**:
- Errors: `Polly.ServiceFailureException`, `Polly.ThrottlingException`
- Initial interval: 3 seconds
- Max attempts: 2
- Backoff rate: 2.0 (exponential)
- Total retry time: ~9 seconds maximum

**DynamoDB Operations** (PutItem, UpdateItem):
- Errors: `DynamoDB.ProvisionedThroughputExceededException`
- Initial interval: 2 seconds
- Max attempts: 3
- Backoff rate: 2.0 (exponential)
- Total retry time: ~14 seconds maximum

### Error Type Routing

The state machine catches and handles specific error types:

1. **Lambda-specific errors**:
   - `Lambda.ServiceException` - AWS Lambda service errors
   - `Lambda.AWSLambdaException` - Lambda runtime errors
   - `Lambda.SdkClientException` - SDK client errors
   - Fallback: All other errors caught by `States.ALL`

2. **Polly-specific errors**:
   - `Polly.ServiceFailureException` - Polly service failures
   - `Polly.InvalidSsmlException` - Invalid SSML syntax
   - `Polly.ThrottlingException` - Rate limit exceeded
   - Fallback: All other errors caught by `States.ALL`

3. **Error Context Preservation**:
   - All errors capture context in `$.errorMessage`
   - Error information stored in DynamoDB `errorInfo` attribute
   - Full execution context published to SNS failure topic

### Error Flow
1. Task fails after exhausting retries
2. Catch block routes to `UpdateStatusFailed`
3. DynamoDB updated with error details
4. SNS failure notification sent with full context
5. State machine ends in `PipelineFailed` state
- Triggers PipelineFailed state
- Encrypted with KMS
### 4. Monitoring and Observability (Enhanced in Issue #10)

### 4. Monitoring and Observability
- **CloudWatch Logs** capture all execution data
- **X-Ray Tracing** enabled on Lambda and State Machine for distributed tracing
- **Structured JSON Logging** in Lambda for better log analysis
- **CloudWatch Alarms** alert on critical failures
- Step Functions logs all state transitions and data
- Lambda logs processing details
- Enables debugging, monitoring, and compliance

## Component Details

### S3 Input Bucket
**Purpose**: Receives raw audio files for processing

**Configuration**:
- S3-managed encryption (SSE-S3)
- Versioning enabled
- EventBridge notifications enabled
- Public access blocked (all 4 settings)
- Removal policy: DESTROY (dev environment)
- Auto-delete objects on stack deletion (dev)

**Security**:
- Block all public access
- Encryption at rest
- Version history for audit trail

### S3 Output Bucket
**Purpose**: Stores processed audio files

**Configuration**:
- S3-managed encryption (SSE-S3)
- Versioning enabled
- Public access blocked
- Removal policy: DESTROY (dev environment)
- Auto-delete objects on stack deletion (dev)

**Security**:
- Block all public access
- Encryption at rest
- Version history

### DynamoDB Table
**Purpose**: Stores audio pipeline metadata and status

**Schema**:
- **Partition Key**: `audioId` (String) - typically the S3 object key
- **Attributes** (sample):
  - `status`: PROCESSING | COMPLETED | FAILED
  - `inputBucket`: Source S3 bucket name
  - `inputKey`: Source S3 object key
  - `createdAt`: Pipeline start timestamp
  - `updatedAt`: Last update timestamp
  - `errorInfo`: Error details (if failed)

**Configuration**:
- Billing mode: PAY_PER_REQUEST (on-demand)
- Encryption: AWS-managed (SSE)
- Point-in-time recovery enabled
- Removal policy: DESTROY (dev environment)

**Security**:
- Encryption at rest with AWS-managed keys
- Least-privilege IAM policies

### Input Validation (NEW in Issue #8)
**Purpose**: Ensure only valid audio files enter the pipeline

**Validation Layers**:
1. **State Machine Level** (CheckFileExtension Choice state):
   - Pattern matching for file extensions
   - Supported formats: `.mp3`, `.wav`, `.m4a`, `.flac`
   - Routes invalid files to error path before processing

2. **Lambda Level** (SleepAudioProcessor):
   - Required field validation (bucket, key)
   - File extension validation (`.mp3`, `.wav`, `.m4a`, `.flac`, `.ogg`)
   - Detailed error logging
   - Throws exceptions for Step Functions error handling
- Point-in-time recovery for data protection

## Observability and Monitoring (NEW in Issue #10)

### X-Ray Tracing

**Lambda Function**:
- Active tracing enabled
- Captures request/response timing
- Traces AWS SDK calls (DynamoDB, S3)
- Provides end-to-end request tracking

**Step Functions State Machine**:
- Tracing enabled for all executions
- Visualizes workflow execution paths
- Tracks timing for each state
- Identifies bottlenecks and latency issues

### Structured Logging

**Lambda Function Logging Format** (JSON):
```json
{
  "level": "INFO|ERROR",
  "message": "Processing started|completed|failed",
  "requestId": "aws-request-id",
  "bucket": "bucket-name",
  "key": "object-key",
  "audioId": "audio-id",
  "status": "SUCCESS|FAILED",
  "durationMs": 1234,
  "timestamp": 1234567890,
  "error": "error message (if failed)",
  "errorType": "exception type (if failed)"
}
```

**Benefits**:
- Easy parsing and analysis with CloudWatch Insights
- Consistent log format across all requests
- Request correlation via requestId
- Performance metrics (durationMs)
- Error categorization

### CloudWatch Alarms

**1. State Machine Execution Failures Alarm**:
- Metric: `AWS/States` → `ExecutionsFailed`
- Threshold: > 0 failures
- Period: 5 minutes
- Action: Alert when any execution fails
- Naming: `SleepAudio-StateMachine-Failures-{environment}`

**2. Lambda Function Errors Alarm**:
- Metric: `AWS/Lambda` → `Errors`
- Threshold: > 5 errors
- Period: 5 minutes
- Action: Alert when error rate is high
- Naming: `SleepAudio-Lambda-Errors-{environment}`

**Alarm Configuration**:
- Evaluation periods: 1
- Missing data treatment: NOT_BREACHING
- Environment-specific naming for multi-environment support

### CloudWatch Logs Insights Queries

**Example queries for structured logs**:

```
# Find all errors in last hour
fields @timestamp, message, error, errorType, requestId
| filter level = "ERROR"
| sort @timestamp desc

# Calculate average processing duration
fields durationMs
| filter level = "INFO" and message = "Processing completed successfully"
| stats avg(durationMs) as avgDuration, max(durationMs) as maxDuration, min(durationMs) as minDuration

# Track processing by audio file
fields @timestamp, audioId, status, durationMs
| filter message = "Processing completed successfully"
| sort @timestamp desc
```


**Purpose**: Core audio processing - downloads, processes/generates, and uploads audio
**Purpose**: Process and enrich audio file metadata

**Configuration**:
- Runtime: Java 17
- Handler: `com.myorg.SleepAudioProcessor::handleRequest`
- Environment Variables:
  - `OUTPUT_BUCKET`: S3 output bucket name

  - `ENVIRONMENT`: Current environment (dev/stage/prod)
- **X-Ray Tracing**: Active (Issue #10)
- **Structured Logging**: JSON format (Issue #10)

**Current Functionality** (FULLY IMPLEMENTED in Issue #11):
- **Input Validation**:
  - Validates required fields (bucket, key)
  - Validates file extension against supported formats
  - Logs validation steps with structured logging
  - Throws exceptions for invalid inputs

- **Audio Processing Pipeline**:
  1. Downloads input file from S3 Input bucket
  2. Determines processing type based on file size and extension
  3. For text inputs: Uses Amazon Polly to synthesize sleep audio
  4. For audio inputs: Performs basic processing (pass-through for now)
  5. Uploads processed audio to S3 Output bucket
  6. Updates DynamoDB with output metadata (S3 URI, file size, processing type)
  7. Returns structured response to Step Functions

- **Output Naming Convention**:
  - Pattern: `processed/{originalname}-{timestamp}.{extension}`
  - Example: `processed/myaudio-1704067200000.mp3`
  - Ensures unique output files and easy traceability

- Logs input for debugging and monitoring
- **Input Validation**:
  - Validates required fields (bucket, key)
  - Validates file extension against supported formats
  - Logs validation steps
  - Throws exceptions for invalid inputs

**Future Enhancements**:
- Advanced audio processing: volume normalization, frequency filtering
- Mix with ambient sleep sounds
- Multiple voice options for Polly
- Audio format conversion and optimization
- Extract detailed metadata (duration, bitrate, sample rate)
- Update DynamoDB with enriched data
- Generate thumbnails or waveform visualizations

**Permissions**:
- Read/Write access to DynamoDB table
- Polly SynthesizeSpeech permission (via execution role)
- Read access to S3 input bucket
- Write access to S3 output bucket
- CloudWatch Logs write access (automatic)

**Error Handling**:
- Catches all exceptions and logs error details
- Throws RuntimeException to trigger Step Functions error handling
- Step Functions routes to failure path on error

### EventBridge Rule
**Purpose**: Triggers pipeline when audio files are uploaded

**Event Pattern**:
```json
{
  "source": ["aws.s3"],
  "detail-type": ["Object Created"],
  "detail": {
    "bucket": {
      "name": ["<input-bucket-name>"]
    }
  }
}
```

**Target**: Step Functions state machine with transformed input

**Input Transformation**:
- Maps S3 event fields to state machine input
- Extracts: bucket name, object key, event time, event details

### Step Functions State Machine
**Purpose**: Orchestrates the audio processing workflow

**Type**: STANDARD (for long-running workflows and auditing)

0. **ValidateInput** (Pass) - NEW in Issue #8
0.5. **CheckFileExtension** (Choice) - NEW in Issue #8
0.6. **ValidationFailed** (Pass) - NEW in Issue #8
1. **WriteInitialMetadata** (DynamoDB PutItem) - Issue #5
2. **ProcessAudio** (Lambda Invoke) - Issue #7
3. **PollyTextToSpeech** (CallAwsService) - Issue #4
3. **PollyTextToSpeech** (CallAwsService)

**Retry Policies** (NEW in Issue #10):
- Lambda, Polly, and DynamoDB tasks have retry policies
- See "Error Handling Strategy" section for details

4. **UpdateStatusCompleted** (DynamoDB UpdateItem)
5. **PublishSuccessNotification** (SNS Publish)
6. **UpdateStatusFailed** (DynamoDB UpdateItem)
7. **PublishFailureNotification** (SNS Publish)
8. **PipelineSucceeded** (Succeed)
9. **PipelineFailed** (Fail)
**Error Handling** (Enhanced in Issue #8):
- **Validation Errors**:
  - CheckFileExtension routes unsupported formats to error path
- **Specific error type handling** (Issue #10):
  - Lambda: Catches `Lambda.ServiceException`, `Lambda.AWSLambdaException`, etc.
  - Polly: Catches `Polly.ServiceFailureException`, `Polly.InvalidSsmlException`, etc.
  - Fallback catch-all for unexpected errors
  - See "Error Handling Strategy" section for full details
  - ValidationFailed state captures error details
  - No processing occurs for invalid files
- **Processing Errors**:
**Error Handling**:
- Lambda task has Catch block for all errors (`States.ALL`)
- Polly task has Catch block for all errors (`States.ALL`)
- Error path: Update status to FAILED → Publish failure notification → End in failure state
- Error details stored in `$.errorMessage` for debugging

**Logging**:
- CloudWatch Logs integration
- Log level: ALL (includes execution data)
- Enables debugging and compliance

**Permissions**:
- DynamoDB: PutItem, UpdateItem (via grant methods)
- Polly: SynthesizeSpeech (via policy statement)
- SNS: Publish to both topics (via policy statement)
- Lambda: InvokeFunction (via LambdaInvoke construct)
- S3: Read from input bucket, write to output bucket (via grant methods)
- CloudWatch Logs: PutLogEvents (automatic)

### Amazon Polly
**Purpose**: Text-to-speech synthesis for sleep audio

**Configuration**:
- Voice: Joanna (neural engine)
- Output format: MP3
- Text: Currently placeholder (to be replaced with dynamic content)

**Future Enhancements**:
- Dynamic text from S3 objects or DynamoDB
- Multiple voice options
- SSML support for advanced speech control
- Audio processing (pitch, speed adjustments)

### SNS Topics
**Purpose**: Notify subscribers of pipeline events

**Topics**:
1. **SleepAudioPipelineCompleted**: Success notifications
2. **SleepAudioPipelineFailed**: Failure alerts

**Configuration**:
- KMS encryption enabled (customer-managed key)
- Key rotation enabled for security
- Display names for easy identification

**Security**:
- Encrypted at rest and in transit
- Automatic key rotation
- Least-privilege publish permissions

### CloudWatch Logs
**Purpose**: Centralized logging for observability

**Log Groups**:
- Step Functions execution logs
- Lambda function logs

**Configuration**:
- Removal policy: DESTROY (dev environment)
- Log level: ALL for Step Functions (includes execution data)

## Security Considerations

### Encryption
- **S3**: Server-side encryption (SSE-S3) on all buckets
- **DynamoDB**: AWS-managed encryption (SSE)
- **SNS**: KMS encryption with customer-managed key and automatic rotation
- **Data in transit**: HTTPS/TLS for all AWS service communication

### IAM Permissions (Least Privilege)
- **State Machine Role**:
  - DynamoDB: Read/write to metadata table only
  - Polly: SynthesizeSpeech only
  - SNS: Publish to specific topics only
  - Lambda: Invoke specific function only
  - S3: Read from input, write to output buckets only
  - CloudWatch Logs: Write logs

- **Lambda Execution Role**:
  - DynamoDB: Read/write to metadata table only
  - S3: Read from input bucket, write to output bucket only
  - CloudWatch Logs: Write logs

- **EventBridge Rule**:
  - Step Functions: StartExecution on specific state machine only

### Network Security
- All S3 buckets have public access blocked (4 settings)
- All services communicate over AWS internal network (no internet exposure)

### Data Protection
- S3 versioning enabled for data recovery
- DynamoDB point-in-time recovery enabled
- CloudWatch Logs for audit trail

## Future Enhancements

1. **Core Audio Processing** (Issue #11)
   - ✅ Download input from S3
   - ✅ Text-to-Speech with Polly integration
   - ✅ Upload output to S3
   - ✅ Update DynamoDB with output metadata
   - Advanced audio enhancement (future)
   - Multiple Polly voices (future)
   - Deep content validation (planned)

2. **Input Validation** (Issue #8)
   - ✅ File format validation (extension-based)
   - Size limits
   - Content type verification
3. **Deployment & Testing** (Issue #9)
   - ✅ Multi-environment support (dev, stage, prod)

3. **Advanced Error Handling & Observability** (Issue #10)
   - ✅ Retry policies with exponential backoff
   - ✅ Specific error type handling
   - ✅ X-Ray tracing (Lambda + State Machine)
   - ✅ Structured JSON logging
   - ✅ CloudWatch Alarms for failures
   - ✅ CDK Pipelines skeleton
   - ✅ Environment-based resource policies
   - ✅ Comprehensive pipeline testing
   - CI/CD integration (in progress)
   - Performance benchmarks

4. **Advanced Audio Processing**
   - Noise reduction
   - Volume normalization
   - Format conversion
   - Compression optimization

5. **Enhanced Monitoring**
   - CloudWatch Dashboards
   - Custom metrics
   - ✅ CloudWatch Alarms (Issue #10)
   - ✅ X-Ray tracing (Issue #10)
   - CloudWatch Dashboards (optional enhancement)
   - Custom metrics (future)
   - ✅ Environment-specific configurations
   - ✅ Resource tagging
   - Cost optimization
   - Backup strategies
   - Manual approval gates for prod deployments

7. **API Layer**
   - API Gateway for programmatic access
   - Authentication and authorization
   - Rate limiting
   - API documentation

## Technology Stack

- **Infrastructure as Code**: AWS CDK (Java)
- **Compute**: AWS Lambda (Java 17)
- **Orchestration**: AWS Step Functions
- **Storage**: Amazon S3
- **Database**: Amazon DynamoDB
- **AI/ML**: Amazon Polly
- **Messaging**: Amazon SNS
- **Event Management**: Amazon EventBridge
- **Monitoring**: Amazon CloudWatch
- **Security**: AWS KMS, AWS IAM
- **CI/CD**: CDK Pipelines (skeleton)
- **Build Tool**: Maven
- **Testing**: JUnit 5, CDK Assertions

## Development Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Test-Driven Development (TDD) guidelines
- Coding standards
- Git workflow
- Pull request process

## Deployment
### Local Development

```bash
# Run tests
mvn clean test
# Synthesize CloudFormation (default: dev environment)
# Synthesize CloudFormation
cdk synth
# Synthesize for specific environment
cdk synth --context environment=prod


# Preview changes
cdk diff

# Deploy to AWS
cdk deploy
```
### Multi-Environment Deployment

**Development Environment** (default):
- Removal policy: DESTROY (easy cleanup)
- Auto-delete objects enabled
- Rapid iteration and testing

**Production Environment**:
- Removal policy: RETAIN (data protection)
- Auto-delete objects disabled
- Manual deployment recommended

```bash
# Deploy to production (use with caution)
cdk deploy --context environment=prod
```

### CDK Pipelines (Future)
The `SleepAudioPipelineStack` provides a skeleton for automated CI/CD deployment across multiple environments. Future enhancements will include:
- Source integration (GitHub/CodeCommit)
- Automated testing stages
- Manual approval gates
- Progressive deployment (dev → stage → prod)

**Last Updated**: Issue #11 - Core Audio Processing Logic and Output Handling
