-- ============================================================
-- V3__add_upload_task_version_column.sql
-- ModelLite Repository - Add version column to upload_task
-- PostgreSQL migration
-- ============================================================

ALTER TABLE upload_task
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN upload_task.version IS '上传任务版本号，用于乐观锁并发控制';

CREATE INDEX idx_upload_active ON upload_task (model_id, version_id)
    WHERE status IN ('Pending', 'Running', 'Paused') AND deleted = FALSE;
