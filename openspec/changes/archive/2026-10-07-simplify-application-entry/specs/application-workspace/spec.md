## ADDED Requirements

### Requirement: Enter an application without prebuilt catalogs
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
#### Scenario: Preserve existing entry and mobile operation
- **WHEN** entry starts from a preset position or is used on a narrow screen
- **THEN** preset selection remains valid and both modes can be saved without horizontal page overflow.
