-- Long interview reviews and completion state must not be overwritten by stale editors.
ALTER TABLE interview
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_interview_version CHECK (version >= 0);
ALTER TABLE todo
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_todo_version CHECK (version >= 0);
