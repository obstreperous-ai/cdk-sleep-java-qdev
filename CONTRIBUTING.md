# Contributing to cdk-sleep-java-qdev

Thank you for your interest in contributing to the **cdk-sleep-java-qdev** project! This document provides guidelines and best practices for contributing to this AWS CDK Java project.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Development Philosophy](#development-philosophy)
3. [Getting Started](#getting-started)
4. [Development Workflow](#development-workflow)
5. [Test-Driven Development (TDD) Requirements](#test-driven-development-tdd-requirements)
6. [Code Style and Standards](#code-style-and-standards)
7. [Commit Message Conventions](#commit-message-conventions)
8. [Pull Request Process](#pull-request-process)
9. [Architecture Documentation](#architecture-documentation)

## Code of Conduct

This project adheres to professional standards of collaboration. Be respectful, inclusive, and constructive in all interactions.

## Development Philosophy

This project follows **strict Test-Driven Development (TDD)** principles:

- **Tests MUST be written before implementation code**
- All features must have comprehensive test coverage
- Infrastructure as Code (IaC) must be validated with CDK assertions
- Every commit must maintain or improve test coverage
- Architecture documentation must stay synchronized with implementation

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+** for dependency management
- **Node.js 18+** and **npm** (required for AWS CDK)
- **AWS CDK CLI** installed globally: `npm install -g aws-cdk`
- **AWS CLI** configured with appropriate credentials
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Initial Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd cdk-sleep-java-qdev
   ```

2. **Install dependencies**:
   ```bash
   mvn clean install
   ```

3. **Verify setup**:
   ```bash
   mvn test
   cdk synth
   ```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feat/your-feature-name
```

Use conventional branch naming:
- `feat/` for new features
- `fix/` for bug fixes
- `docs/` for documentation changes
- `test/` for test additions or modifications
- `refactor/` for code refactoring

### 2. Follow TDD Cycle

**RED → GREEN → REFACTOR**

1. **RED**: Write a failing test first
2. **GREEN**: Write minimal code to make the test pass
3. **REFACTOR**: Improve code quality while keeping tests green

### 3. Run Tests Frequently

```bash
mvn test
```

### 4. Validate CDK Synthesis

```bash
cdk synth
```

## Test-Driven Development (TDD) Requirements

### MANDATORY TDD Rules

1. **NO IMPLEMENTATION WITHOUT TESTS**: Every new class, method, or infrastructure component must have tests written FIRST
2. **CDK Assertions Required**: All CDK constructs must be validated using `software.amazon.awscdk.assertions` library
3. **Test Coverage**: Minimum 80% code coverage (enforced in CI)
4. **Unit Tests**: Test individual components in isolation
5. **Integration Tests**: Test component interactions and AWS resource configurations

### Test Structure

```java
@Test
void testDescriptiveName() {
    // ARRANGE: Set up test data and dependencies
    
    // ACT: Execute the code under test
    
    // ASSERT: Verify expected outcomes
}
```

## Code Style and Standards

- Follow standard Java naming conventions (camelCase for methods/variables, PascalCase for classes)
- Use meaningful, descriptive names for variables, methods, and classes
- Keep methods small and focused (single responsibility principle)
- Add JavaDoc comments for public APIs
- Maximum line length: 120 characters
- Use 4 spaces for indentation (no tabs)

## Commit Message Conventions

This project uses **Conventional Commits** specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring without functionality change
- `chore`: Maintenance tasks, dependency updates
- `ci`: CI/CD pipeline changes

### Examples

```
feat(lambda): add audio metadata extractor function

Implement Lambda function to extract metadata from uploaded audio files.
Uses AWS SDK for S3 integration and custom audio parsing library.

Refs: #123
```

```
test(stack): add CDK assertions for S3 bucket configuration

Validate S3 bucket encryption, versioning, and event notifications.
```

## Pull Request Process

1. **Ensure all tests pass**: `mvn clean test`
2. **Verify CDK synthesis**: `cdk synth`
3. **Update documentation**: If your change affects architecture, update `ARCHITECTURE.md`
4. **Write descriptive PR title**: Use conventional commit format
5. **Provide detailed PR description**:
   - What does this PR do?
   - Why is this change needed?
   - How was it tested?
   - Any breaking changes?
6. **Request review**: Tag at least one reviewer
7. **Address feedback**: Respond to all review comments
8. **Squash commits**: Keep git history clean before merging

## Architecture Documentation

### Critical Requirement: ARCHITECTURE.md Synchronization

**Every infrastructure change MUST update ARCHITECTURE.md**:

1. **Update textual description**: Explain what changed and why
2. **Update Mermaid diagram**: Keep the visual representation accurate
3. **Document new AWS resources**: Include purpose, configuration, and integration points
4. **Explain event flows**: Update event-driven patterns if modified
5. **Version changes**: Note significant architectural decisions

### Mermaid Diagram Guidelines

- Use `flowchart TD` syntax for clarity
- Include AWS service names explicitly
- Show data flow direction with arrows
- Add labels to edges for event/data types
- Use consistent color coding for service categories
- Validate Mermaid syntax before committing

## Questions and Support

- **Issues**: Open GitHub issues for bugs or feature requests
- **Discussions**: Use GitHub Discussions for questions and ideas
- **Agent Guidelines**: Review `.github/AGENT_GUIDELINES.md` for AI agent collaboration standards

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see LICENSE file).

---

**Remember**: Test-Driven Development is not optional. Tests come first, always. This discipline ensures code quality, maintainability, and confidence in our infrastructure deployments.

## Project Status

**Current Status**: ✅ Complete and Production-Ready  
**Version**: 1.0 (Final Release)  
**Test Coverage**: 80%+  
**Total Tests**: 70+ comprehensive tests  

### Completed Features

- ✅ Event-driven architecture (S3 → EventBridge → Step Functions → Lambda)
- ✅ Audio processing and text-to-speech conversion
- ✅ Input validation and error handling
- ✅ Retry policies with exponential backoff
- ✅ Observability (X-Ray, CloudWatch, structured logging)
- ✅ Multi-environment support (dev, stage, prod)
- ✅ Comprehensive test suite
- ✅ Complete documentation

For detailed information, see [SUMMARY.md](SUMMARY.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

