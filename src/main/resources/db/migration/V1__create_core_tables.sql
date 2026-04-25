-- ============================================================
-- V1__create_core_tables.sql
-- ModelLite Repository - Core schema (10 tables)
-- PostgreSQL DDL for Flyway migration
-- ============================================================

-- -----------------------------------------------------------
-- Table 1: category
-- -----------------------------------------------------------
CREATE TABLE category (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL UNIQUE,
    description         VARCHAR(500) DEFAULT '',
    is_builtin          BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE category IS '模型分类表（一级分类），如 TextGeneration、ImageTextToText';
COMMENT ON COLUMN category.id IS '分类ID（UUID，自动生成）';
COMMENT ON COLUMN category.name IS '分类名称，全局唯一';
COMMENT ON COLUMN category.is_builtin IS '是否内置分类（内置分类不可删除）';

-- -----------------------------------------------------------
-- Table 2: model_type
-- -----------------------------------------------------------
CREATE TABLE model_type (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id         UUID NOT NULL REFERENCES category(id),
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500) DEFAULT '',
    is_builtin          BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_model_type_name UNIQUE (category_id, name)
);
COMMENT ON TABLE model_type IS '模型类型表（二级分类），如 glm-5、Qwen2.5-VL-7B';
COMMENT ON COLUMN model_type.category_id IS '所属分类ID（外键引用 category.id）';
COMMENT ON COLUMN model_type.is_builtin IS '是否内置类型（内置类型不可删除）';

-- -----------------------------------------------------------
-- Table 3: tag
-- -----------------------------------------------------------
CREATE TABLE tag (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(50) NOT NULL UNIQUE,
    tag_type            VARCHAR(20) NOT NULL,
    is_builtin          BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE tag IS '标签表，同时服务两种关联场景：USER=用户自定义标签，CAPABILITY=能力标签';
COMMENT ON COLUMN tag.tag_type IS '标签类型：USER=用户自定义标签，CAPABILITY=能力标签';
COMMENT ON COLUMN tag.is_builtin IS '是否内置标签（内置标签不允许删除）';

-- -----------------------------------------------------------
-- Table 4: model
-- -----------------------------------------------------------
CREATE TABLE model (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(2000) DEFAULT '',
    category_id         UUID NOT NULL REFERENCES category(id),
    type_id             UUID NOT NULL REFERENCES model_type(id),
    resource_group      VARCHAR(100) NOT NULL,
    create_user         VARCHAR(100) NOT NULL,
    author              VARCHAR(100) DEFAULT NULL,
    series_name         VARCHAR(255) DEFAULT NULL,
    model_size          BIGINT DEFAULT NULL,
    max_seq_length      INTEGER DEFAULT NULL,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_model_name UNIQUE (name, category_id, type_id)
        WHERE (deleted = FALSE)
);
COMMENT ON TABLE model IS '模型表，模型仓库的顶层实体';
COMMENT ON COLUMN model.name IS '模型名称，同一分类+类型组合下唯一，创建后不可修改';
COMMENT ON COLUMN model.category_id IS '所属分类ID（外键引用 category.id）';
COMMENT ON COLUMN model.type_id IS '所属类型ID（外键引用 model_type.id）';
COMMENT ON COLUMN model.resource_group IS '资源组标识，创建后不可修改（设计保留扩展性）';
COMMENT ON COLUMN model.deleted IS '软删除标记，TRUE=已移入回收站';
COMMENT ON COLUMN model.series_name IS '模型系列名称（如 GLM-5 系列）';
COMMENT ON COLUMN model.model_size IS '模型大小（单位：字节）';
COMMENT ON COLUMN model.max_seq_length IS '最大序列长度（单位：tokens）';

-- -----------------------------------------------------------
-- Table 5: model_version
-- -----------------------------------------------------------
CREATE TABLE model_version (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id            UUID NOT NULL REFERENCES model(id),
    version_number      INTEGER NOT NULL,
    pvc_name            VARCHAR(255) DEFAULT NULL,
    internal_path       VARCHAR(1024) DEFAULT NULL,
    weight_type         VARCHAR(50) DEFAULT NULL,
    is_registered       BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(30) NOT NULL DEFAULT 'NoWeight',
    is_locked           BOOLEAN NOT NULL DEFAULT FALSE,
    train_frame         VARCHAR(100) DEFAULT NULL,
    train_type          VARCHAR(100) DEFAULT NULL,
    train_strategy      VARCHAR(100) DEFAULT NULL,
    train_time          BIGINT DEFAULT NULL,
    final_loss          VARCHAR(100) DEFAULT NULL,
    source_version      VARCHAR(50) DEFAULT NULL,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_model_version UNIQUE (model_id, version_number)
);
COMMENT ON TABLE model_version IS '模型版本表，模型的具体可部署实例';
COMMENT ON COLUMN model_version.version_number IS '版本号，自动递增整数（V1, V2, V3...），不允许跳号';
COMMENT ON COLUMN model_version.pvc_name IS '权重存储 PVC 名称';
COMMENT ON COLUMN model_version.internal_path IS 'PVC 内部路径';
COMMENT ON COLUMN model_version.weight_type IS '权重数据精度类型（FP16、w8a8等），自动识别';
COMMENT ON COLUMN model_version.is_registered IS '是否为纳管版本（纳管版本只读挂载）';
COMMENT ON COLUMN model_version.status IS '版本状态：NoWeight/Uploading/Available/UploadFailed/ValidationFailed/Error';
COMMENT ON COLUMN model_version.is_locked IS '是否被锁定（反规范化字段，由 version_lock 表驱动更新，绝不独立修改）';
COMMENT ON COLUMN model_version.train_frame IS '训练框架（归档版本才有）';
COMMENT ON COLUMN model_version.train_time IS '训练时长（毫秒，归档版本才有）';

-- -----------------------------------------------------------
-- Table 6: model_tag
-- -----------------------------------------------------------
CREATE TABLE model_tag (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id            UUID NOT NULL REFERENCES model(id),
    tag_id              UUID NOT NULL REFERENCES tag(id),
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_model_tag UNIQUE (model_id, tag_id)
);
COMMENT ON TABLE model_tag IS '模型-标签关联表（用户自定义标签关联）';

-- -----------------------------------------------------------
-- Table 7: model_type_tag
-- -----------------------------------------------------------
CREATE TABLE model_type_tag (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_id             UUID NOT NULL REFERENCES model_type(id),
    tag_id              UUID NOT NULL REFERENCES tag(id),
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_model_type_tag UNIQUE (type_id, tag_id)
);
COMMENT ON TABLE model_type_tag IS '模型类型-标签关联表（能力标签关联，替代 supportFinetune 字段）';

-- -----------------------------------------------------------
-- Table 8: version_lock
-- -----------------------------------------------------------
CREATE TABLE version_lock (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id          UUID NOT NULL REFERENCES model_version(id),
    locker_id           VARCHAR(200) NOT NULL,
    lock_type           VARCHAR(30) NOT NULL,
    expire_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE version_lock IS '版本锁表，保护正在被平台任务使用的权重版本不被误删';
COMMENT ON COLUMN version_lock.locker_id IS '锁持有者标识（任务ID）';
COMMENT ON COLUMN version_lock.lock_type IS '锁类型：INFERENCE/TRAINING/EVALUATION/DEVELOPMENT';
COMMENT ON COLUMN version_lock.expire_time IS '锁过期时间（默认创建时间 + 24小时）';

-- -----------------------------------------------------------
-- Table 9: upload_task
-- -----------------------------------------------------------
CREATE TABLE upload_task (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id            UUID NOT NULL REFERENCES model(id),
    version_id          UUID NOT NULL REFERENCES model_version(id),
    source_type         VARCHAR(20) NOT NULL,
    source_path         VARCHAR(1024) NOT NULL,
    cifs_username       VARCHAR(200) DEFAULT NULL,
    cifs_password       VARCHAR(200) DEFAULT NULL,
    target_path         VARCHAR(1024) NOT NULL,
    progress            INTEGER DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'Pending',
    error_message       VARCHAR(2000) DEFAULT NULL,
    create_user         VARCHAR(100) NOT NULL,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE upload_task IS '上传任务表，跟踪权重文件从外部存储拷贝到平台 PVC 的异步过程';
COMMENT ON COLUMN upload_task.source_type IS '源路径类型：NFS/CIFS/PVC';
COMMENT ON COLUMN upload_task.cifs_username IS 'CIFS 认证用户名（仅 source_type=CIFS 时必填）';
COMMENT ON COLUMN upload_task.cifs_password IS 'CIFS 认证密码（仅 source_type=CIFS 时必填）';
COMMENT ON COLUMN upload_task.status IS '任务状态：Pending/Running/Paused/Completed/Failed/Cancelled';

-- -----------------------------------------------------------
-- Table 10: convert_task
-- -----------------------------------------------------------
CREATE TABLE convert_task (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id            UUID NOT NULL REFERENCES model(id),
    source_version_id   UUID NOT NULL REFERENCES model_version(id),
    target_version_id   UUID DEFAULT NULL REFERENCES model_version(id),
    source_format       VARCHAR(50) NOT NULL,
    target_format       VARCHAR(50) NOT NULL,
    progress            INTEGER DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'Pending',
    error_message       VARCHAR(2000) DEFAULT NULL,
    create_user         VARCHAR(100) NOT NULL,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE convert_task IS '转换任务表，跟踪权重格式转换的异步过程（如 Megatron → Safetensors）';
COMMENT ON COLUMN convert_task.target_version_id IS '目标版本ID，任务创建时先为 NULL，版本创建后回填';

-- -----------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------

-- Model indexes
CREATE INDEX idx_model_category ON model (category_id) WHERE deleted = FALSE;
CREATE INDEX idx_model_type ON model (type_id) WHERE deleted = FALSE;
CREATE INDEX idx_model_resource_group ON model (resource_group) WHERE deleted = FALSE;
CREATE INDEX idx_model_create_time ON model (create_time) WHERE deleted = FALSE;

-- Model version indexes
CREATE INDEX idx_version_model ON model_version (model_id);
CREATE INDEX idx_version_status ON model_version (status) WHERE deleted = FALSE;

-- Version lock indexes
CREATE INDEX idx_lock_version ON version_lock (version_id);
CREATE INDEX idx_lock_expire ON version_lock (expire_time);
CREATE INDEX idx_lock_locker ON version_lock (locker_id, lock_type);

-- Upload task indexes
CREATE INDEX idx_upload_model ON upload_task (model_id) WHERE deleted = FALSE;
CREATE INDEX idx_upload_status ON upload_task (status) WHERE deleted = FALSE;

-- Convert task indexes
CREATE INDEX idx_convert_model ON convert_task (model_id) WHERE deleted = FALSE;
CREATE INDEX idx_convert_status ON convert_task (status) WHERE deleted = FALSE;

-- Tag indexes
CREATE INDEX idx_tag_type ON tag (tag_type);

-- Tag association indexes (for deletion reference checks)
CREATE INDEX idx_model_tag_tag ON model_tag (tag_id);
CREATE INDEX idx_model_type_tag_tag ON model_type_tag (tag_id);
