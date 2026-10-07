# persistence-model Specification

## Purpose
为 OfferFlow 的单体后端建立版本化 MySQL 迁移、分离的公司与岗位及投递相关实体、受数据库保护的关联与状态事实、可保留未知日期的 UTC 时间存储，以及经过真实 MySQL 验证的乐观锁和事务基础，供后续业务服务在统一约束下实现。

## Requirements

### Requirement: Versioned MySQL initialization
The system SHALL create its six business tables using Flyway versioned SQL migrations, SHALL validate recorded checksums, and MUST disable automatic baseline and clean.

#### Scenario: Empty supported MySQL database
- **WHEN** mysql mode starts against an empty dedicated MySQL database supporting enforced CHECK constraints
- **THEN** company, job_position, application, application_stage_history, interview and todo exist with one successful V1 migration and no seed personal data

#### Scenario: Repeated migration
- **WHEN** Flyway migrate runs again with unchanged source migrations
- **THEN** no additional migration is applied and existing business rows are retained

#### Scenario: Checksum mismatch
- **WHEN** an applied migration checksum differs from its source
- **THEN** validation fails and the application does not accept the mismatch automatically

### Requirement: Normalized typed persistence
The system SHALL map Company, JobPosition, Application, ApplicationStageHistory, Interview and Todo to separate entities and MyBatis-Plus Mappers with database-generated identifiers and audit times.

#### Scenario: Round trip six related records
- **WHEN** each Mapper inserts fictional related data and reads it back
- **THEN** associations, Chinese and supplementary Unicode, enums, nullable fields, dates and interview text retain their values
- **AND** database identifiers and audit times are generated

### Requirement: Protected associations
The database MUST reject orphan children, deletion of referenced parent records, and a Todo linked to an Interview belonging to a different Application.

#### Scenario: Missing company or application
- **WHEN** a position references a missing company or a history, interview or todo references a missing application
- **THEN** the write fails under enforced foreign keys

#### Scenario: Delete referenced parent
- **WHEN** a company, job position, application or linked interview still has child records
- **THEN** the deletion fails and those records remain

#### Scenario: Cross application interview link
- **WHEN** a Todo belonging to one Application references an Interview from another Application
- **THEN** the database rejects the write

### Requirement: One application per concrete position
The single-user database MUST allow at most one Application per concrete JobPosition. Different recruitment batches SHALL be representable by separate JobPosition records without company/name uniqueness.

#### Scenario: Duplicate application through another channel
- **WHEN** an Application is inserted for a JobPosition already linked to another Application
- **THEN** the database rejects the duplicate even when its channel differs

#### Scenario: Same job name in different batches
- **WHEN** two JobPosition records for one Company share a name but have different recruitment batches
- **THEN** each can have its own Application

### Requirement: Rejection direction and ending reason
REJECTED SHALL mean employer rejection. The database MUST require a valid ending reason for ENDED snapshots and history records and MUST reject an ending reason for other stages. Valid reasons SHALL be ACCEPTED_OFFER, DECLINED_OFFER, WITHDRAWN, POSITION_CLOSED and OTHER.

#### Scenario: End a process
- **WHEN** an Application or history record uses ENDED with a valid reason
- **THEN** it can be stored independently of an employer-rejection record

#### Scenario: Inconsistent ending reason
- **WHEN** an ENDED snapshot or history has no reason, an undefined reason, or a non ENDED stage has a reason
- **THEN** the database rejects the write

### Requirement: Facts and unknown dates
The database SHALL persist actual submission separately from current stage, SHALL preserve unknown business dates and times as NULL, and MUST reject invalid enumerations or contradictory static facts.

#### Scenario: Unknown submission and stage dates
- **WHEN** an actual submitted application and its history have unknown business dates
- **THEN** submitted is true and those dates remain NULL while recording audit times are known

#### Scenario: Invalid codes and facts
- **WHEN** a write uses an undefined stage, noncanonical code casing, invalid boolean or negative version, a submission date without actual submission, or a completion time for an incomplete todo
- **THEN** an enforced database constraint rejects it

### Requirement: UTC instant persistence
Interview times, Todo times and audit times SHALL use UTC DATETIME(6) and SHALL retain their instant and microsecond precision independently of the JVM default time zone. Date-only submission and history fields SHALL remain calendar dates.

#### Scenario: JVM with non UTC time zone
- **WHEN** Mappers write an Instant and a LocalDate using a non UTC JVM zone
- **THEN** SQL stores the instant in UTC and reads the same Instant to microsecond precision, and the LocalDate is unchanged

### Requirement: Application concurrency and transaction support
Application SHALL have a version-controlled update path through MyBatis-Plus. When snapshot and history writes are explicitly grouped in a Spring-managed transaction, they MUST use that transaction and commit or roll back together.

#### Scenario: Stale application update
- **WHEN** two readers update the same application version sequentially
- **THEN** the first update affects one row and increments the version, and the stale second update affects zero rows without replacing the first result

#### Scenario: Snapshot and history rollback
- **WHEN** a transaction updates current stage and adds a new history record but fails before commit
- **THEN** both writes roll back and the original stage and history remain

#### Scenario: Successful transaction
- **WHEN** a transaction updates the snapshot and adds a new history record successfully
- **THEN** both writes persist and older history records remain

### Requirement: Isolated MySQL verification
Required database behavior SHALL be verified on a real, dedicated MySQL test instance or explicit dedicated test schema. The integration profile MUST fail if required test settings are absent and MUST NOT silently skip checks or target the ordinary application database.

#### Scenario: Explicit integration verification
- **WHEN** the isolated runner initializes its own local MySQL instance and runs mysql-integration verify
- **THEN** migration and relational tests execute without modifying an existing MySQL service or business database

#### Scenario: Missing test settings
- **WHEN** mysql-integration is requested without explicit dedicated OFFERFLOW_TEST settings
- **THEN** verification fails with a controlled setup error before business database access
