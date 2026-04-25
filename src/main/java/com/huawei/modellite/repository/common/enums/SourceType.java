package com.huawei.modellite.repository.common.enums;

public enum SourceType {
    NFS("NFS", "NFS 存储"),
    CIFS("CIFS", "CIFS 存储"),
    PVC("PVC", "PVC 存储");

    private final String dbValue;
    private final String displayName;

    SourceType(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SourceType fromDbValue(String dbValue) {
        for (SourceType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SourceType: " + dbValue);
    }
}