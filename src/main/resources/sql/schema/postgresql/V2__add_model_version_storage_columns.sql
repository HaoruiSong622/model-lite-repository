ALTER TABLE model_version
    ADD COLUMN source_type VARCHAR(20) DEFAULT NULL,
    ADD COLUMN nfs_server  VARCHAR(255) DEFAULT NULL,
    ADD COLUMN nfs_path    VARCHAR(1024) DEFAULT NULL;

COMMENT ON COLUMN model_version.source_type IS '源路径类型：PVC=使用已有PVC，NFS=使用NFS路径';
COMMENT ON COLUMN model_version.nfs_server IS 'NFS服务器地址（source_type=NFS时必填）';
COMMENT ON COLUMN model_version.nfs_path IS 'NFS路径（source_type=NFS时必填）';
