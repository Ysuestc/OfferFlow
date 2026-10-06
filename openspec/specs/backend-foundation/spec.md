# backend-foundation Specification

## Purpose
为 OfferFlow 的后续业务模块提供可构建运行的单体 Java 工程、稳定的 JSON 与 HTTP 错误合同、可观察的进程存活入口和真实校验测试，使首次工程交付能独立验证而不依赖尚未建立的数据库或业务功能。

## Requirements

### Requirement: Reproducible backend build
The project SHALL build with Java 21 using a project-local Maven Wrapper and SHALL produce a runnable Spring Boot 3 Jar without relying on the system Maven installation.

#### Scenario: Build with the project wrapper
- **WHEN** a developer with JDK 21 runs the wrapper verify command
- **THEN** the configured Maven version compiles the application, runs applicable tests and packages an executable Jar

### Requirement: Independent HTTP liveness
The backend SHALL start without MySQL or Redis in this stage and SHALL expose GET /api/v1/health as an HTTP process liveness endpoint.

#### Scenario: Check a started application
- **WHEN** a JSON client requests GET /api/v1/health
- **THEN** HTTP 200 contains code SUCCESS and data.status UP
- **AND** no database readiness claim is made

### Requirement: Common response envelope
Ordinary JSON API success and error responses MUST include code, message and data. DTO field validation details SHALL use optional errors containing only field names and safe constraint messages, and MUST NOT include rejected input values.

#### Scenario: Successful JSON response
- **WHEN** a supported API request succeeds
- **THEN** code is SUCCESS and data contains the endpoint result

#### Scenario: Invalid DTO fields
- **WHEN** a request body violates declared Jakarta constraints
- **THEN** HTTP 400 contains code VALIDATION_ERROR, null data and field constraint details
- **AND** rejected input values are not included

### Requirement: Protocol and business errors
API error handling SHALL preserve actual HTTP error statuses and relevant protocol headers while returning the common envelope to JSON-compatible clients.

#### Scenario: Unknown API route
- **WHEN** a JSON client requests an unknown API route
- **THEN** HTTP 404 contains code NOT_FOUND

#### Scenario: Wrong HTTP method
- **WHEN** a client uses an unsupported method for the health route
- **THEN** HTTP 405 contains code METHOD_NOT_ALLOWED and preserves the Allow header

#### Scenario: Malformed request body or media type
- **WHEN** a JSON-compatible request contains malformed JSON or uses an unsupported input media type
- **THEN** it returns HTTP 400 BAD_REQUEST or HTTP 415 UNSUPPORTED_MEDIA_TYPE respectively
- **AND** parser implementation details are not returned

#### Scenario: Declared business conflict
- **WHEN** a controller's business operation raises a declared conflict
- **THEN** HTTP 409 contains code CONFLICT and a controlled message

#### Scenario: Client excludes JSON
- **WHEN** an API client's Accept header excludes application/json
- **THEN** the backend may return HTTP 406 without a JSON body

### Requirement: Validation and internal failure distinction
The backend MUST return HTTP 400 for invalid method inputs, and MUST treat invalid controller return values and unexpected exceptions as HTTP 500 with a generic INTERNAL_ERROR message.

#### Scenario: Invalid method parameter
- **WHEN** a bound method parameter violates a declared constraint
- **THEN** HTTP 400 contains code VALIDATION_ERROR

#### Scenario: Invalid return value
- **WHEN** a controller return value violates a declared constraint
- **THEN** HTTP 500 contains code INTERNAL_ERROR without disclosing the constraint or invalid value

#### Scenario: Unexpected server failure
- **WHEN** an unhandled server exception occurs
- **THEN** HTTP 500 contains generic INTERNAL_ERROR and null data
- **AND** exception text, stack traces and private values are absent from the response

### Requirement: Evidence before delivery
The change MUST provide successful build/test evidence and actual executable-Jar HTTP checks before acceptance.

#### Scenario: Prepare change delivery
- **WHEN** the change is ready for review and acceptance
- **THEN** wrapper build, behavioral HTTP tests, Jar startup and applicable OpenSpec checks have recorded results
- **AND** test-only helper endpoints are absent from the production artifact
