## Purpose
为个人秋招提供可以本机运行的公司、岗位和投递管理闭环，保证资料分表、阶段历史可追溯、错误安全可恢复，并用真实持久化接口支撑浏览器操作。

## ADDED Requirements

### Requirement: Manage company and position catalogs
The system SHALL expose validated CRUD and paged literal-search APIs for companies and positions, with protected references.
#### Scenario: Create and find catalogs
- **WHEN** a user saves a company and associated position with Chinese details and searches their names
- **THEN** saved records appear with deterministic pagination and can be edited after refresh.
#### Scenario: Protect a referenced catalog
- **WHEN** deletion targets a company with positions or a position with an application
- **THEN** HTTP 409 is returned and all related records remain.
#### Scenario: Reject invalid input
- **WHEN** required names are blank, a website uses an unsafe scheme, pagination exceeds limits, or an association is missing
- **THEN** the request returns a safe 400 or 404 without changing data.

### Requirement: Maintain one application per position
The system SHALL create at most one application per position and preserve explicit submission facts and unknown dates.
#### Scenario: Start at a known stage
- **WHEN** an application is created at a selected stage with an unknown business date
- **THEN** the snapshot and initial history commit together, the date stays null, and submission facts follow the selected initial stage.
#### Scenario: Duplicate application
- **WHEN** another application is created for the same position
- **THEN** HTTP 409 is returned without a second application or history.
#### Scenario: Correct application details
- **WHEN** a current-version update changes channel, notes or submission date
- **THEN** the values are saved, optional values can be cleared, and no stage history is fabricated.

### Requirement: Append transactional stage history
The system SHALL append effective manual stage changes atomically, allow jumps and corrections, and retain previous records.
#### Scenario: Jump and end
- **WHEN** an application changes directly to second interview and later ENDED with a valid reason
- **THEN** the current snapshot follows the latest operation and initial plus subsequent history remains in recording order.
#### Scenario: Preserve rejection and end meaning
- **WHEN** ENDED lacks a reason or another stage supplies a reason
- **THEN** HTTP 400 is returned without a snapshot or history update.
#### Scenario: Preserve dates and submission facts
- **WHEN** an application moves from waiting to submitted and later back to waiting with an unknown stage date
- **THEN** submitted remains true, no submission date is inferred, and all effective operations remain in history.
#### Scenario: Retry or stale edit
- **WHEN** a same-version identical stage request or a stale-version write is received
- **THEN** the former adds no history, while the latter returns 409 and cannot overwrite the current state.
#### Scenario: Roll back failed history
- **WHEN** history persistence fails after snapshot modification
- **THEN** neither the snapshot nor the new history is committed.

### Requirement: Provide a usable browser workspace
The system SHALL provide responsive company, position and application views backed by real JSON APIs.
#### Scenario: Operate the full flow
- **WHEN** a user creates catalogs and an application, changes stage, edits details and refreshes
- **THEN** the browser shows the persisted values and timeline using Chinese stage labels.
#### Scenario: Handle empty and failed operations
- **WHEN** there are no records, a request fails or another edit wins concurrently
- **THEN** the interface provides an actionable empty or error state, preserves unsaved form input, and enables reload after a conflict.
#### Scenario: Use a narrow screen
- **WHEN** the workspace is viewed at mobile width
- **THEN** navigation, forms and records are usable without page-wide horizontal overflow.

### Requirement: Run a bundled persistent local system
The system SHALL package built frontend resources with the mysql backend and document persistent local startup without affecting existing databases.
#### Scenario: Open the packaged application
- **WHEN** the frontend-enabled Jar runs with valid mysql settings
- **THEN** the same loopback port serves the workspace and working business APIs.
#### Scenario: Retain local records
- **WHEN** the optional owned-instance helper is stopped and started again
- **THEN** its records remain in its dedicated ignored datadir and existing MySQL services are untouched.
#### Scenario: Use standalone mode
- **WHEN** standalone mode is selected
- **THEN** health remains available and the workspace reports business mode unavailable without attempting database access.
