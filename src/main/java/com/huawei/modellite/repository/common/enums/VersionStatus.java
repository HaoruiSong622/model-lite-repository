package com.huawei.modellite.repository.common.enums;

public enum VersionStatus {
    NO_WEIGHT("NoWeight", "无权重"),
    UPLOADING("Uploading", "上传中"),
    AVAILABLE("Available", "可用"),
    UPLOAD_FAILED("UploadFailed", "上传失败"),
    VALIDATION_FAILED("ValidationFailed", "校验失败"),
    ERROR("Error", "异常");

    private final String dbValue;
    private final String displayName;

    VersionStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VersionStatus fromDbValue(String dbValue) {
        for (VersionStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown VersionStatus: " + dbValue);
    }
}
