## MODIFIED Requirements

### Requirement: Independent HTTP liveness
The backend SHALL support an explicit standalone mode that starts without MySQL or Redis and a mysql mode that requires successful database connection and migrations. GET /api/v1/health SHALL remain an HTTP process liveness endpoint.

#### Scenario: Check a started application
- **WHEN** a developer starts the default standalone profile without database settings
- **THEN** the application starts and GET /api/v1/health returns HTTP 200 with code SUCCESS and data.status UP
- **AND** no database readiness claim is made

#### Scenario: Start mysql with a valid database
- **WHEN** the mysql profile is selected with valid configuration for an empty supported MySQL database
- **THEN** Flyway migrations complete before startup succeeds and the health response keeps its existing contract

#### Scenario: Start mysql with an unavailable database
- **WHEN** the mysql profile cannot connect or validate its database
- **THEN** startup fails rather than silently disabling persistence or migrations
