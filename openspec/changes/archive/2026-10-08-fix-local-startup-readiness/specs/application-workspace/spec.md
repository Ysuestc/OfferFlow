## MODIFIED Requirements

### Requirement: Run a bundled persistent local system
The system SHALL package built frontend resources with the mysql backend and document persistent local startup without affecting existing databases. The owned-instance helper SHALL probe its loopback application directly, tolerate temporary readiness failures within a bounded startup deadline, and report readiness only when business mode is available.
#### Scenario: Open the packaged application
- **WHEN** the frontend-enabled Jar runs with valid mysql settings
- **THEN** the same loopback port serves the workspace and working business APIs.
#### Scenario: Retain local records
- **WHEN** the optional owned-instance helper is stopped and started again
- **THEN** its records remain in its dedicated ignored datadir and existing MySQL services are untouched.
#### Scenario: Use standalone mode
- **WHEN** standalone mode is selected
- **THEN** health remains available and the workspace reports business mode unavailable without attempting database access.
#### Scenario: Start with an unavailable HTTP proxy
- **WHEN** the computer has an unavailable system or environment HTTP proxy and the owned application is ready
- **THEN** the helper connects directly to its loopback port and reports readiness without changing global proxy settings.
#### Scenario: Retry temporary readiness failures
- **WHEN** connection refusal, timeout or a non-success HTTP response occurs while the owned processes remain alive
- **THEN** the helper retries until business mode is available or the startup deadline expires.
#### Scenario: Fail safely without reporting readiness
- **WHEN** readiness never succeeds, an owned process exits or the workspace reports business mode unavailable
- **THEN** the helper returns a concise failure, stops only its owned processes and retains saved data and credentials.
