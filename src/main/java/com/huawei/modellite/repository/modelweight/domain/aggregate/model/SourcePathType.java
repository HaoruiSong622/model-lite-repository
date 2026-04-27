package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

public enum SourcePathType {
    PVC("PVC", "使用已有PVC"),
    NFS("NFS", "使用NFS路径");

    private final String dbValue;
    private final String displayName;

    SourcePathType(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SourcePathType fromDbValue(String dbValue) {
        for (SourcePathType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SourcePathType: " + dbValue);
    }
}
