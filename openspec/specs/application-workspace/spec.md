# application-workspace Specification

## Purpose
为个人秋招提供可以本机运行的公司、岗位和投递管理闭环，保证资料分表、阶段历史可追溯、错误安全可恢复，并用真实持久化接口支撑浏览器操作。

## Requirements

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

### Requirement: Provide a usable desktop workspace
The system SHALL provide desktop company, position and application views backed by real JSON APIs.
#### Scenario: Operate the full flow
- **WHEN** a user creates catalogs and an application, changes stage, edits details and refreshes
- **THEN** the browser shows the persisted values and timeline using Chinese stage labels.
#### Scenario: Handle empty and failed operations
- **WHEN** there are no records, a request fails or another edit wins concurrently
- **THEN** the interface provides an actionable empty or error state, preserves unsaved form input, and enables reload after a conflict.
#### Scenario: Use a desktop browser
- **WHEN** the workspace is viewed in a desktop browser
- **THEN** navigation, forms and records are usable without page-wide horizontal overflow.

### Requirement: Enter an application from the desktop workspace
The system SHALL default new application entry to explicit company and position text, save associated records atomically, and retain an existing-position mode.
#### Scenario: Create a complete application directly
- **WHEN** the user enters a new company name, position name and valid application details without searching
- **THEN** company, position, application and initial history commit together and remain visible after refresh.
#### Scenario: Reuse unambiguous records
- **WHEN** one company name and one position identity match existing records
- **THEN** those records are reused without overwriting their metadata, and a position already applied to returns 409 without another application or history.
#### Scenario: Resolve catalog ambiguity
- **WHEN** multiple companies share the name or multiple positions share the company, name, location, direction and batch
- **THEN** the request returns 409 and directs selection of an explicit existing company or position; no record is silently chosen.
#### Scenario: Distinguish position identities
- **WHEN** location, direction or recruitment batch differs from an existing position
- **THEN** a distinct position can be created while keeping the one-application-per-position contract.
#### Scenario: Reject invalid or failed entry atomically
- **WHEN** required input or stage meaning is invalid, a selected company is missing, or history persistence fails
- **THEN** a safe error is returned and no newly created company, position, application or history remains.
#### Scenario: Recover from search or submission failure
- **WHEN** existing-position search finds no result or saving fails
- **THEN** the browser offers direct entry, preserves the entered application details, and explains that searching requires selecting an actual result.
#### Scenario: Preserve existing entry on desktop
- **WHEN** entry starts from a preset position in a desktop browser
- **THEN** preset selection remains valid and both modes can be saved without horizontal page overflow.

### Requirement: Select company type using desktop tags
The system SHALL show single-choice company type tags during direct application entry and company editing, using the existing company classification.
#### Scenario: Select and persist a new company type
- **WHEN** the user chooses INTERNET, BANK, STATE_OWNED, RESEARCH_INSTITUTE, FOREIGN or OTHER without expanding optional details and saves a new company application
- **THEN** exactly one tag is selected and the company retains that type after refresh.
#### Scenario: Retain selection and protect existing catalogs
- **WHEN** saving fails or entry mode changes, or an existing company is selected
- **THEN** unsaved selection is retained, while an existing company displays its stored type and application entry does not overwrite it.
#### Scenario: Edit company classification
- **WHEN** an existing company is edited and another tag is selected
- **THEN** the existing selection is visible and the chosen type persists through the existing company update API.
#### Scenario: Use tags with keyboard on desktop
- **WHEN** tags are used with keyboard navigation in a desktop browser
- **THEN** selection has an accessible pressed state and visible focus, and tags remain usable without horizontal page overflow.
