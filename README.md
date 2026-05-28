# cdk-sleep-java-qdev

## Project Overview

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

 * `mvn clean test`  run unit tests
 * `mvn package`     compile and package application
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [.github/AGENT_GUIDELINES.md](.github/AGENT_GUIDELINES.md) for development guidelines, coding standards, and TDD requirements.
