package com.huawei.modellite.repository.common.enums;

public enum LockType {
    INFERENCE("Inference", "推理服务"),
    TRAINING("Training", "训练任务"),
    EVALUATION("Evaluation", "评测任务"),
    DEVELOPMENT("Development", "模型开发");

    private final String dbValue;
    private final String displayName;

    LockType(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LockType fromDbValue(String dbValue) {
        for (LockType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown LockType: " + dbValue);
    }
}