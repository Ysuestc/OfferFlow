-- V1 is immutable after first use. Store all DATETIME values in UTC sessions.
CREATE TABLE company (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'OTHER',
    website VARCHAR(2048) NULL,
    notes TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_company_name (name),
    CONSTRAINT ck_company_type CHECK (CAST(type AS BINARY) IN ('INTERNET', 'BANK', 'STATE_OWNED', 'RESEARCH_INSTITUTE', 'FOREIGN', 'OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE job_position (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    company_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NULL,
    direction VARCHAR(128) NULL,
    jd MEDIUMTEXT NULL,
    recruitment_batch VARCHAR(64) NULL,
    PRIMARY KEY (id),
    KEY idx_position_company (company_id),
    CONSTRAINT fk_position_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    job_position_id BIGINT NOT NULL,
    channel VARCHAR(64) NULL,
    applied_on DATE NULL,
    submitted BOOLEAN NOT NULL DEFAULT FALSE,
    current_stage VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'TO_APPLY',
    current_stage_on DATE NULL,
    end_reason VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    notes TEXT NULL,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application_position (job_position_id),
    KEY idx_application_stage (current_stage, current_stage_on, id),
    KEY idx_application_submission (submitted, applied_on, id),
    CONSTRAINT fk_application_position FOREIGN KEY (job_position_id) REFERENCES job_position (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_application_stage CHECK (CAST(current_stage AS BINARY) IN ('TO_APPLY', 'SUBMITTED', 'ASSESSMENT', 'WRITTEN_TEST', 'FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'THIRD_INTERVIEW', 'HR_INTERVIEW', 'OFFER', 'REJECTED', 'ENDED')),
    CONSTRAINT ck_application_submitted CHECK (submitted IN (0, 1)),
    CONSTRAINT ck_application_submission_date CHECK (submitted = 1 OR applied_on IS NULL),
    CONSTRAINT ck_application_version CHECK (version >= 0),
    CONSTRAINT ck_application_reason_code CHECK (end_reason IS NULL OR CAST(end_reason AS BINARY) IN ('ACCEPTED_OFFER', 'DECLINED_OFFER', 'WITHDRAWN', 'POSITION_CLOSED', 'OTHER')),
    CONSTRAINT ck_application_ending CHECK (
        (current_stage = 'ENDED' AND end_reason IS NOT NULL)
        OR (current_stage <> 'ENDED' AND end_reason IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE application_stage_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    application_id BIGINT NOT NULL,
    stage VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    stage_on DATE NULL,
    end_reason VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    remark TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_history_application (application_id, id),
    CONSTRAINT fk_history_application FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_history_stage CHECK (CAST(stage AS BINARY) IN ('TO_APPLY', 'SUBMITTED', 'ASSESSMENT', 'WRITTEN_TEST', 'FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'THIRD_INTERVIEW', 'HR_INTERVIEW', 'OFFER', 'REJECTED', 'ENDED')),
    CONSTRAINT ck_history_reason_code CHECK (end_reason IS NULL OR CAST(end_reason AS BINARY) IN ('ACCEPTED_OFFER', 'DECLINED_OFFER', 'WITHDRAWN', 'POSITION_CLOSED', 'OTHER')),
    CONSTRAINT ck_history_ending CHECK (
        (stage = 'ENDED' AND end_reason IS NOT NULL)
        OR (stage <> 'ENDED' AND end_reason IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interview (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    application_id BIGINT NOT NULL,
    round_name VARCHAR(64) NOT NULL,
    interview_at DATETIME(6) NULL,
    format VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL,
    questions MEDIUMTEXT NULL,
    answers MEDIUMTEXT NULL,
    review MEDIUMTEXT NULL,
    result TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_id_application (id, application_id),
    KEY idx_interview_application_time (application_id, interview_at, id),
    KEY idx_interview_time (interview_at, id),
    CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_interview_format CHECK (format IS NULL OR CAST(format AS BINARY) IN ('ONLINE', 'OFFLINE', 'AI'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE todo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    application_id BIGINT NOT NULL,
    interview_id BIGINT NULL,
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    title VARCHAR(255) NOT NULL,
    due_at DATETIME(6) NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME(6) NULL,
    notes TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_todo_application (application_id, id),
    KEY idx_todo_upcoming (completed, due_at, id),
    KEY idx_todo_interview_application (interview_id, application_id),
    CONSTRAINT fk_todo_application FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_todo_interview_application FOREIGN KEY (interview_id, application_id) REFERENCES interview (id, application_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_todo_kind CHECK (CAST(kind AS BINARY) IN ('WRITTEN_TEST', 'INTERVIEW', 'ASSESSMENT', 'MATERIAL', 'OFFER_DEADLINE', 'OTHER')),
    CONSTRAINT ck_todo_completed CHECK (completed IN (0, 1)),
    CONSTRAINT ck_todo_completion_time CHECK (completed = 1 OR completed_at IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
