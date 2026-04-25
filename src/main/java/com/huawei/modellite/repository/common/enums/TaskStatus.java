package com.huawei.modellite.repository.common.enums;

public enum TaskStatus {
    PENDING("Pending", "待执行"),
    RUNNING("Running", "执行中"),
    PAUSED("Paused", "已暂停"),
    COMPLETED("Completed", "已完成"),
    FAILED("Failed", "失败"),
    CANCELLED("Cancelled", "已取消");

    private final String dbValue;
    private final String displayName;

    TaskStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TaskStatus fromDbValue(String dbValue) {
        for (TaskStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus: " + dbValue);
    }
}