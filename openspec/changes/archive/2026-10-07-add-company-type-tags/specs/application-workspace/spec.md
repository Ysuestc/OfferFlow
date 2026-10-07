## ADDED Requirements
### Requirement: Select company type using visible tags
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
#### Scenario: Use tags on keyboard and narrow screens
- **WHEN** tags are used with keyboard navigation or at mobile width
- **THEN** selection has an accessible pressed state and visible focus, and tags wrap without horizontal page overflow.
