# cdk-sleep-java-qdev

## Project Overview

**Status**: ✅ Complete and Production-Ready

**Version**: 1.0 (Final Release)

**cdk-sleep-java-qdev** is an event-driven, serverless sleep audio processing pipeline built with AWS Cloud Development Kit (CDK) using Java. This project demonstrates modern cloud-native architecture patterns for audio file processing, leveraging Amazon S3 for storage, Amazon EventBridge for event orchestration, AWS Lambda for serverless compute, Amazon DynamoDB for metadata persistence, and Amazon SNS for notifications. The pipeline automatically processes uploaded sleep audio files, extracts metadata, performs transformations, and notifies downstream systems—all with zero server management and automatic scaling.

## Test-Driven Development (TDD) - Strict Rules

**This project follows mandatory Test-Driven Development practices:**

1. **NO CODE WITHOUT TESTS FIRST**: All implementation code must be preceded by failing tests (RED phase)
2. **RED → GREEN → REFACTOR**: Follow the TDD cycle religiously—write failing test, make it pass, then refactor
3. **CDK Assertions Required**: Every AWS CDK construct must have assertions validating resource properties, configurations, and relationships using `software.amazon.awscdk.assertions`
4. **Minimum 80% Coverage**: Code coverage below 80% will fail CI/CD pipeline checks
5. **Tests Must Be Meaningful**: Write tests that validate behavior, not just achieve coverage metrics
6. **Architecture Documentation Sync**: Every infrastructure change must update `ARCHITECTURE.md` (both text and Mermaid diagram)

**Why TDD?**
- Infrastructure as Code (IaC) changes can be costly and risky—tests catch issues before deployment
- TDD ensures every component is designed for testability and maintainability
- Comprehensive test suites enable confident refactoring and rapid iteration
- Tests serve as living documentation of system behavior

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed TDD guidelines and development workflow.

## Features

✅ **Event-Driven Architecture**: Fully decoupled components using EventBridge  
✅ **Text-to-Speech**: Converts text prompts to sleep audio using Amazon Polly (Neural Voice)  
✅ **Audio Processing**: Processes and enhances uploaded audio files  
✅ **Input Validation**: Validates file formats before processing  
✅ **Error Handling**: Comprehensive error handling with retry policies  
✅ **Observability**: X-Ray tracing, structured logging, CloudWatch alarms  
✅ **Security**: End-to-end encryption (S3, DynamoDB, SNS), least-privilege IAM  
✅ **Multi-Environment**: Support for dev, stage, and prod environments  
✅ **Notifications**: SNS alerts for success and failure scenarios  
✅ **Metadata Tracking**: DynamoDB tracks processing status and results  

## Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd cdk-sleep-java-qdev

# Install dependencies
mvn clean install

# Run tests
mvn test

# Synthesize CloudFormation template
cdk synth

# Deploy to AWS (dev environment)
cdk deploy
```

## Architecture

The pipeline follows an event-driven architecture:

**S3 (upload) → EventBridge → Lambda (process) → S3/DynamoDB/SNS**

For detailed architecture documentation, diagrams, and component descriptions, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Node.js 18+** and **npm** (for AWS CDK)
- **AWS CDK CLI**: `npm install -g aws-cdk`
- **AWS CLI** configured with appropriate credentials

## Useful commands
## Installation & Setup
 * `mvn clean test`  run unit tests
### 1. Install Prerequisites

**Java 17**:
```bash
# macOS (using Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt-get install openjdk-17-jdk

# Verify installation
java -version
```

**AWS CDK**:
```bash
npm install -g aws-cdk
cdk --version
```

**AWS CLI**:
```bash
# macOS
brew install awscli

# Or use pip
pip install awscli

# Configure credentials
aws configure
```

### 2. Bootstrap CDK (First Time Only)

```bash
# Bootstrap your AWS environment for CDK
cdk bootstrap aws://<ACCOUNT_ID>/<REGION>
```

### 3. Build and Test

```bash
# Clean and build
mvn clean install

# Run all tests
mvn test

# Generate test coverage report
mvn test jacoco:report
```

## Deployment

### Development Environment (Default)

```bash
# Synthesize CloudFormation template
cdk synth

# Preview changes
cdk diff

# Deploy to AWS
cdk deploy
```

### Production Environment

```bash
# Deploy with production settings (RETAIN removal policy)
cdk deploy --context environment=prod
```

### Destroy Stack (Development Only)

```bash
# WARNING: This will delete all resources
cdk destroy
```

## Usage

### Upload Audio for Processing

After deployment, upload audio files to the Input S3 bucket:

```bash
# Get bucket name from CDK outputs
INPUT_BUCKET=$(aws cloudformation describe-stacks \
  --stack-name CdkBaseStack \
  --query 'Stacks[0].Outputs[?OutputKey==`InputBucketName`].OutputValue' \
  --output text)

# Upload audio file
aws s3 cp myaudio.mp3 s3://${INPUT_BUCKET}/

# Upload text file (will be converted to speech)
echo "Welcome to your sleep meditation" > prompt.txt
aws s3 cp prompt.txt s3://${INPUT_BUCKET}/
```

### Monitor Processing

```bash
# View State Machine executions
aws stepfunctions list-executions --state-machine-arn <STATE_MACHINE_ARN>

# Check DynamoDB for metadata
aws dynamodb scan --table-name <TABLE_NAME>

# View CloudWatch Logs
aws logs tail /aws/lambda/SleepAudioProcessor --follow
```

### Subscribe to Notifications

```bash
# Subscribe email to success topic
aws sns subscribe \
  --topic-arn <SUCCESS_TOPIC_ARN> \
  --protocol email \
  --notification-endpoint your-email@example.com

# Subscribe to failure topic
aws sns subscribe \
  --topic-arn <FAILURE_TOPIC_ARN> \
  --protocol email \
  --notification-endpoint your-email@example.com
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CdkBaseTest

# Run with coverage
mvn clean test jacoco:report
```
## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [.github/AGENT_GUIDELINES.md](.github/AGENT_GUIDELINES.md) for development guidelines, coding standards, and TDD requirements.
