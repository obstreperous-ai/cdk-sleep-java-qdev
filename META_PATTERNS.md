# Meta-Patterns for Agentic TDD Infrastructure as Code

## Overview

This document extracts reusable meta-prompting patterns and guidelines from the **cdk-sleep-java-qdev** experiment. These patterns can be used as templates for future agentic Test-Driven Development (TDD) Infrastructure as Code (IaC) projects.

**Purpose**: Provide a blueprint for AI-assisted development of AWS CDK projects following strict TDD discipline and architectural best practices.

**Source Project**: cdk-sleep-java-qdev - A production-ready, event-driven serverless audio processing pipeline built entirely through issue-driven development with Q Developer.

---

## Core Meta-Patterns

### 1. Agent Persona Definition

**Pattern**: Establish a clear, consistent agent persona at the project start.

**Template**:
```
You are a **Senior [TECHNOLOGY] [FRAMEWORK] TDD Specialist**.

Core Responsibilities:
- Be explicit and verbose in all communication
- Write tests before all implementation code
- Maintain perfect synchronization between code and architecture documentation
- Follow [FRAMEWORK] best practices and security standards
```

**Application in cdk-sleep-java-qdev**:
```
You are a **Senior AWS CDK Java TDD Specialist**.

- Be explicit and verbose
- Tests before implementation
- Maintain perfect sync of ARCHITECTURE.md Mermaid diagram
```

**Benefits**:
- Consistent behavior across all agent interactions
- Clear expectations for code quality and process
- Reduces ambiguity in agent responses

---

### 2. Test-First Development Mandate

**Pattern**: Make TDD non-negotiable with explicit workflow steps.

**Template**:
```
MANDATORY TDD WORKFLOW:

1. RED Phase:
   - Write a failing test that describes desired behavior
   - Ensure test fails for the right reason (not compilation errors)
   - Use descriptive test names

2. GREEN Phase:
   - Write minimal code to make the test pass
   - Do not over-engineer
   - Focus solely on satisfying test requirements

3. REFACTOR Phase:
   - Improve code quality, readability, structure
   - Keep tests green throughout refactoring
   - Apply design patterns where appropriate
```

**Enforcement Mechanisms**:
- CI/CD pipeline checks for minimum coverage (80%+)
- Code review checklist includes TDD verification
- Documentation updates require test evidence

---

### 3. Architecture as Source of Truth

**Pattern**: Designate a single architectural document as the authoritative reference.

**Template**:
```
ARCHITECTURE.md Requirements:

1. Single source of truth for all design decisions
2. Must include:
   - Visual diagram (Mermaid/PlantUML)
   - Component descriptions
   - Data flow explanations
   - Security considerations
   - Integration points

3. Mandatory synchronization:
   - Every infrastructure change updates ARCHITECTURE.md
   - Update both textual description AND diagram
   - Validate diagram rendering before commit

4. Reference before implementation:
   - Read ARCHITECTURE.md before starting new features
   - Ensure new components fit existing design
   - Update immediately after implementation
```

**Workflow Integration**:
```
1. Write tests (RED)
2. Implement code (GREEN)
3. Refactor (REFACTOR)
4. Update ARCHITECTURE.md text
5. Update ARCHITECTURE.md diagram
6. Verify diagram renders
7. Commit with conventional message
```

---

### 4. Explicit Verbosity Requirement

**Pattern**: Demand detailed explanations for all changes.

**Template**:
```
Communication Standards:

BAD: "Added S3 bucket"

GOOD:
"Added S3 source bucket for [PURPOSE] with the following configuration:

- Purpose: [Specific role in architecture]
- Configuration:
  * [Setting 1]: [Value and rationale]
  * [Setting 2]: [Value and rationale]
- Security: [Encryption, access controls]
- Integration: [How it connects to other components]
- Rationale: [Why this approach vs alternatives]

Test Coverage:
- [Test 1 description]
- [Test 2 description]"
```

**Application Areas**:
- Commit messages
- Pull request descriptions
- Code comments and JavaDoc
- Architecture documentation
- Issue responses

---

### 5. Issue-Driven Development Framework

**Pattern**: Break complex projects into discrete, testable issues.

**Template**:
```
Issue Structure:

Title: [#N] [Component] - [Brief Description]

Body:
1. Goal: [What needs to be achieved]
2. Acceptance Criteria:
   - [ ] Criterion 1 (testable)
   - [ ] Criterion 2 (testable)
3. TDD Requirements:
   - Tests must be written first
   - Coverage: [minimum %]
4. Architecture Impact:
   - Components affected: [list]
   - Diagram updates: [yes/no]
5. Success Criteria:
   - [ ] Tests pass
   - [ ] cdk synth succeeds
   - [ ] Architecture docs updated
```

**Progressive Complexity**:
1. Issues #1-#2: Foundation (setup, basic structure)
2. Issues #3-#5: Core components (storage, events, compute)
3. Issues #6-#8: Integration (workflows, orchestration)
4. Issues #9-#11: Enhancement (validation, error handling, observability)
5. Issue #12: Validation (end-to-end testing)

---

### 6. Conventional Commits Mandate

**Pattern**: Enforce semantic versioning through commit message format.

**Template**:
```
Format:
<type>(<scope>): <subject>

<body>

<footer>

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation only
- test: Test additions/modifications
- refactor: Code change without functionality change
- chore: Maintenance, dependencies
- ci: CI/CD changes

Examples:
feat(s3): add source bucket with EventBridge notifications
test(lambda): add unit tests for metadata extractor
docs(architecture): update Mermaid diagram with Lambda function
```

**Enforcement**:
- Git hooks to validate commit format
- CI checks for conventional commit compliance
- Automated changelog generation

---

### 7. Infrastructure Testing Patterns

**Pattern**: Define specific testing requirements for IaC.

**Template for CDK Projects**:
```java
// Pattern 1: Resource Existence
@Test
void testResourceExists() {
    template.resourceCountIs("AWS::S3::Bucket", 1);
}

// Pattern 2: Resource Properties
@Test
void testResourceConfiguration() {
    template.hasResourceProperties("AWS::S3::Bucket",
        Map.of("BucketEncryption", 
            Map.of("ServerSideEncryptionConfiguration",
                List.of(Map.of("ServerSideEncryptionByDefault",
                    Map.of("SSEAlgorithm", "AES256"))))));
}

// Pattern 3: IAM Permissions
@Test
void testIAMPermissions() {
    template.hasResourceProperties("AWS::IAM::Policy",
        Map.of("PolicyDocument",
            Map.of("Statement", Match.arrayWith(...))));
}

// Pattern 4: Integration Points
@Test
void testEventBridgeRule() {
    template.hasResourceProperties("AWS::Events::Rule",
        Map.of("EventPattern",
            Map.of("source", List.of("aws.s3"))));
}
```

**Test Categories**:
1. **Unit Tests**: Individual construct validation
2. **Integration Tests**: Component interactions
3. **Stack Tests**: Full stack synthesis
4. **Assertion Tests**: CDK template validation

---

### 8. Development Checklist Pattern

**Pattern**: Provide a mandatory pre-commit checklist.

**Template**:
```
Before Committing:

□ Tests written BEFORE implementation (RED phase)
□ All tests pass (GREEN phase)
□ Code refactored for quality (REFACTOR phase)
□ [FRAMEWORK] assertions validate infrastructure
□ `[BUILD_COMMAND]` passes locally
□ `[SYNTH_COMMAND]` succeeds without errors
□ ARCHITECTURE.md textual description updated
□ ARCHITECTURE.md diagram updated and renders
□ Code follows [LANGUAGE] and [FRAMEWORK] best practices
□ Commit message follows Conventional Commits
□ Public APIs documented
□ No secrets or hardcoded credentials
```

---

### 9. Progressive Disclosure Documentation

**Pattern**: Layer documentation from quick-start to deep-dive.

**Structure**:
```
README.md (Entry Point):
- Project overview
- Quick start (< 5 minutes)
- Key features
- Links to detailed docs

ARCHITECTURE.md (Technical Deep-Dive):
- Visual diagrams
- Component details
- Data flows
- Security model

CONTRIBUTING.md (Development Guide):
- Setup instructions
- Development workflow
- Testing requirements
- Commit conventions

AGENT_GUIDELINES.md (AI Agent Specific):
- Agent persona
- TDD workflow
- Architecture sync rules

META_PATTERNS.md (Reusable Templates):
- Extracted patterns
- Templates for new projects
- Lessons learned
```

---

## Experiment Methodology

### Pure Issue-Driven Development

**Approach**: All development happens through GitHub issues, no direct commits.

**Workflow**:
1. Create issue with clear acceptance criteria
2. Agent implements feature following TDD
3. Agent submits pull request
4. Review and merge
5. Close issue and create next issue

**Benefits**:
- Complete audit trail
- Testable increments
- Clear progress tracking
- Agent accountability

### AI Agent Collaboration Model

**Key Principles**:
1. **Explicit Instructions**: Agent guidelines are comprehensive and unambiguous
2. **Verification Steps**: Every action includes validation commands
3. **Iterative Refinement**: Each issue builds on previous learnings
4. **Documentation First**: Architecture drives implementation

---

## Reusable Prompt Templates

### New Feature Implementation Prompt
```
Implement [FEATURE_NAME] following strict TDD:

1. Review ARCHITECTURE.md to understand existing structure
2. Write failing tests first (RED)
3. Implement minimal code to pass tests (GREEN)
4. Refactor for quality (REFACTOR)
5. Update ARCHITECTURE.md (text + diagram)
6. Verify: `mvn test` and `cdk synth` pass
7. Use conventional commit format
```

### Architecture Review Prompt
```
Review current ARCHITECTURE.md and code:
1. Identify any drift between documentation and implementation
2. Suggest improvements to architecture clarity
3. Validate Mermaid diagram accuracy
4. Ensure all components are documented
```

---

## Lessons Learned from cdk-sleep-java-qdev

1. **TDD Discipline Works**: 70+ tests, 80%+ coverage, production-ready
2. **Architecture-First**: ARCHITECTURE.md prevented technical debt
3. **Small Issues**: 12 focused issues > 1 large implementation
4. **Agent Consistency**: Clear guidelines = consistent output
5. **Documentation Value**: Docs written alongside code stay accurate

---

**This document is a living template. Adapt patterns to your technology stack, team size, and project requirements.**

**Source**: https://github.com/[org]/cdk-sleep-java-qdev
**License**: MIT
