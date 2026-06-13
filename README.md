# cdk-sleep-java-qdev

[![CI](https://github.com/<org>/cdk-sleep-java-qdev/actions/workflows/ci.yml/badge.svg)](https://github.com/<org>/cdk-sleep-java-qdev/actions)
[![Test Coverage](https://img.shields.io/badge/coverage-80%2B-brightgreen)](https://github.com/<org>/cdk-sleep-java-qdev)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**A production-ready, event-driven serverless audio processing pipeline built with AWS CDK (Java) using strict Test-Driven Development (TDD) and AI-assisted development.**

> 🧪 **Part of the TDD Infrastructure as Code Experiment Series**  
> This project explores TDD applied to IaC across 5 languages × 3 AI agents. See [EXPERIMENT.md](EXPERIMENT.md) for the comprehensive experiment design.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Documentation](#documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Project Status](#project-status)
- [Experiment Design](#experiment-design)
- [License](#license)

---

## Overview

**cdk-sleep-java-qdev** is an event-driven, serverless sleep audio processing pipeline that:

1. Accepts audio files or text uploaded to S3
2. Automatically triggers processing via EventBridge
3. Orchestrates workflow with AWS Step Functions
4. Processes audio or synthesizes speech with Amazon Polly
5. Stores processed output in S3
6. Tracks metadata in DynamoDB
7. Sends notifications via SNS on success/failure

**Built with**:
- ☕ **Java 17** + AWS CDK for type-safe infrastructure
- 🧪 **Strict TDD** (80%+ test coverage, 70+ tests)
- 🤖 **AI-Assisted** development with Amazon Q Developer
- 📝 **Issue-Driven** development (12 completed issues)
- 📐 **Architecture-as-Code** with Mermaid diagrams

---

## Features

### Core Capabilities

✅ **Event-Driven Architecture**
- S3 → EventBridge → Step Functions → Lambda pipeline
- Decoupled components for scalability and maintainability

✅ **Audio Processing**
- Text-to-speech synthesis with Amazon Polly (Neural voice)
- Audio file processing with output to S3
- Automatic format detection and validation

✅ **Workflow Orchestration**
- Step Functions state machine with visual workflow
- Input validation (file format checking)
- Metadata tracking (DynamoDB)
- Success/failure notifications (SNS)

✅ **Error Handling & Resilience**
- Retry policies with exponential backoff
- Specific error type handling
- Graceful failure paths with status updates

✅ **Observability**
- AWS X-Ray tracing (Lambda + Step Functions)
- Structured JSON logging
- CloudWatch Alarms for failures
- Complete execution history

✅ **Multi-Environment Support**
- Dev, Stage, Prod configurations
- Environment-specific removal policies
- CDK Pipelines skeleton for CI/CD

### Security & Compliance

- 🔒 Encryption at rest (S3, DynamoDB, SNS with KMS)
- 🔒 Least-privilege IAM policies
- 🔒 Public access blocked on all S3 buckets
- 🔒 Point-in-time recovery for DynamoDB
- 🔒 Versioning enabled on S3 buckets

---

## Architecture

```mermaid
graph TB
    User[User/Client] -->|1. Upload| InputBucket[S3 Input Bucket]
    InputBucket -->|2. Event| EventBridge[EventBridge]
    EventBridge -->|3. Trigger| StateMachine[Step Functions]
    StateMachine -->|4. Validate| Validation{File Format?}
    Validation -->|Valid| Lambda[Lambda Function]
    Lambda -->|Process| Polly[Amazon Polly]
    Lambda -->|Store| OutputBucket[S3 Output Bucket]
    Lambda -->|Update| DynamoDB[(DynamoDB)]
    StateMachine -->|Success| SNS[SNS Success Topic]
    Validation -->|Invalid| SNSFail[SNS Failure Topic]
    
    style InputBucket fill:#4CAF50
    style Lambda fill:#FF5722
    style StateMachine fill:#2196F3
    style SNS fill:#8BC34A
```

**For the complete architecture with all components, flows, and error handling**, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Quick Start

### Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Node.js 18+** (for AWS CDK)
- **AWS CDK CLI**: `npm install -g aws-cdk`
- **AWS CLI** configured with credentials

### Installation

```bash
# Clone the repository
git clone <repository-url>
cd cdk-sleep-java-qdev

# Install dependencies
mvn clean install

# Verify setup
mvn test
cdk synth
```

### Deployment

```bash
# Deploy to AWS (default: dev environment)
cdk deploy

# Deploy to production (use with caution)
cdk deploy --context environment=prod
```

---

## Documentation

📚 **Comprehensive documentation for developers, architects, and researchers:**

| Document | Description | Audience |
|----------|-------------|----------|
| **[EXPERIMENT.md](EXPERIMENT.md)** | Comprehensive experiment design: methodology, actors, prompting patterns, observations | Researchers, AI/ML Engineers |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Technical architecture: components, flows, diagrams, security | Architects, Developers |
| **[META_PATTERNS.md](META_PATTERNS.md)** | Reusable meta-prompting patterns and templates | AI Engineers, Tech Leads |
| **[SUMMARY.md](SUMMARY.md)** | Executive summary: what was built, key decisions, lessons learned | Product Managers, Stakeholders |
| **[CONTRIBUTING.md](CONTRIBUTING.md)** | Development workflow: TDD process, code standards, Git conventions | Contributors, Developers |
| **[.github/AGENT_GUIDELINES.md](.github/AGENT_GUIDELINES.md)** | AI agent persona, TDD workflow, synchronization rules | AI Agents, Automation Engineers |

---

## Development

This project follows **strict Test-Driven Development (TDD)**:

### TDD Workflow

1. **RED**: Write failing test first
2. **GREEN**: Write minimal code to pass test
3. **REFACTOR**: Improve code quality

### Development Commands

```bash
# Run tests
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# Synthesize CloudFormation
cdk synth

# Preview changes
cdk diff

# Deploy to AWS
cdk deploy
```

### Before Committing

- [ ] Tests written BEFORE implementation
- [ ] All tests pass (`mvn test`)
- [ ] CDK synthesis succeeds (`cdk synth`)
- [ ] ARCHITECTURE.md updated (if infrastructure changed)
- [ ] Commit message follows Conventional Commits

**See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed development guidelines.**

---

## Testing

### Test Suite

| Test Type | File | Count | Coverage |
|-----------|------|-------|----------|
| **Unit Tests** | `CdkBaseTest.java` | 40 | Core infrastructure |
| **Integration Tests** | `PipelineIntegrationTest.java` | 16 | Component interactions |
| **Lambda Tests** | `SleepAudioProcessorTest.java` | 8 | Function logic |
| **End-to-End Tests** | `EndToEndValidationTest.java` | 6 | Complete pipeline |
| **Total** | All files | **70+** | **80%+** |

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CdkBaseTest

# Run with coverage report
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html
```

---

## Deployment

### Environments

| Environment | Removal Policy | Auto-Delete | Use Case |
|-------------|----------------|-------------|----------|
| **Dev** | DESTROY | Yes | Rapid development, testing |
| **Stage** | RETAIN | No | Pre-production validation |
| **Prod** | RETAIN | No | Production workloads |

### Deploy Commands

```bash
# Development (default)
cdk deploy

# Production (with extra confirmation)
cdk deploy --context environment=prod --require-approval broadening

# Destroy stack (dev only recommended)
cdk destroy
```

---

## Project Status

**Status**: ✅ **Complete and Production-Ready**  
**Version**: 1.0 (Final Release)  
**Issues Completed**: 12 (+ issue #14 for experiment documentation)  
**Test Coverage**: 80%+  
**Total Tests**: 70+  
**Development Approach**: Strict TDD + Issue-Driven + AI-Assisted

### Completed Features

- ✅ Event-driven architecture (S3 → EventBridge → Step Functions → Lambda)
- ✅ Audio processing and text-to-speech conversion (Amazon Polly)
- ✅ Input validation and error handling
- ✅ Retry policies with exponential backoff
- ✅ Observability (X-Ray, CloudWatch, structured logging)
- ✅ Multi-environment support (dev, stage, prod)
- ✅ Comprehensive test suite (70+ tests)
- ✅ Complete documentation (6 documents)
- ✅ Experiment design documentation

### Future Enhancements

- 🔄 Advanced audio processing (noise reduction, normalization)
- 🔄 API Gateway integration
- 🔄 CloudWatch Dashboards
- 🔄 Multi-region deployment
- 🔄 Cost optimization analysis

---

## Experiment Design

### 🧪 This is an Experimental Research Project

**cdk-sleep-java-qdev** is part of a larger experiment exploring:

**Research Question**: Can strict TDD be effectively applied to Infrastructure as Code with AI assistance?

**Experimental Matrix**: 5 Languages × 3 AI Agents

| Language | AI Agent | Status |
|----------|----------|--------|
| **Java** | **Q Developer** | **✅ Complete (this project)** |
| TypeScript | Copilot | 🔄 Planned |
| Python | Claude | 🔄 Planned |
| Go | Q Developer | 🔄 Planned |
| C# | Copilot | 🔄 Planned |

### Key Findings (Java + Q Developer)

1. ✅ **TDD works for IaC**: CDK Assertions enable comprehensive infrastructure testing
2. ✅ **AI accelerates TDD**: Q Developer effectively suggests tests and implementations
3. ✅ **Documentation stays synchronized**: When made mandatory in workflow
4. ✅ **Issue-driven development scales**: 12 issues provided clear structure
5. ✅ **Meta-patterns are extractable**: 9 reusable patterns identified

**📖 Read the full experiment design, methodology, and findings in [EXPERIMENT.md](EXPERIMENT.md)**

---

## Technology Stack

- **Infrastructure as Code**: AWS CDK 2.x (Java)
- **Language**: Java 17 (LTS)
- **Build Tool**: Maven 3.8+
- **Testing**: JUnit 5, CDK Assertions
- **AWS Services**: S3, Lambda, Step Functions, DynamoDB, SNS, Polly, EventBridge, CloudWatch, X-Ray, KMS, IAM
- **CI/CD**: GitHub Actions
- **Development**: Amazon Q Developer (AI assistance)

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for:

- Development workflow and TDD requirements
- Code style and standards
- Commit message conventions (Conventional Commits)
- Pull request process
- Architecture documentation synchronization

**Key Requirement**: All contributions MUST include tests written before implementation (TDD).

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- **Amazon Q Developer** - AI agent that assisted with TDD implementation
- **AWS CDK Team** - For the excellent CDK framework and documentation
- **JUnit 5 Team** - For the robust testing framework

---

## Contact & Support

- **Issues**: [GitHub Issues](https://github.com/<org>/cdk-sleep-java-qdev/issues)
- **Discussions**: [GitHub Discussions](https://github.com/<org>/cdk-sleep-java-qdev/discussions)
- **Documentation**: See [Documentation](#documentation) section above

---

**Built with ☕ Java, ☁️ AWS CDK, 🧪 TDD, and 🤖 AI Assistance**

**⭐ If you find this experiment valuable, please star the repository!**
