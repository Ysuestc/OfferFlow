## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Provide a usable browser workspace
**Reason**: 用户明确仅面向电脑端，停止原需求中的手机平台验收。
**Migration**: 全部业务场景迁移至 Provide a usable desktop workspace，保留原操作、持久化与错误行为，仅收敛平台范围。

### Requirement: Enter an application without prebuilt catalogs
**Reason**: 用户明确仅面向电脑端，停止原需求中的手机平台验收。
**Migration**: 全部业务场景迁移至 Enter an application from the desktop workspace，保留原操作、持久化与错误行为，仅收敛平台范围。

### Requirement: Select company type using visible tags
**Reason**: 用户明确仅面向电脑端，停止原需求中的手机平台验收。
**Migration**: 全部业务场景迁移至 Select company type using desktop tags，保留原操作、持久化与错误行为，仅收敛平台范围。
