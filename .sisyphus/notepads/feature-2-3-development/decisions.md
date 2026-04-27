## Design Decisions

### SourcePathType Enum Location
- Placed in `modelweight.domain.aggregate.model` package per design doc F3 (StoragePath is a ValueObject within model aggregate)
- Kept separate from `common.enums` because it's domain-specific to model weight storage, not a generic cross-cutting concern

### Field Ordering in model_version
- Followed design doc F1 exactly: `source_type` after `internal_path`, then `nfs_server`, then `nfs_path`
- This ordering groups storage path fields together logically

### PostgreSQL V2 Migration Script
- Used `ALTER TABLE ... ADD COLUMN` with `DEFAULT NULL` for backward compatibility
- Added `COMMENT ON COLUMN` for documentation consistency with existing PostgreSQL DDL
- Did NOT modify V1 script to avoid breaking existing deployments
