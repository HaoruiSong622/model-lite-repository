package com.huawei.modellite.repository.common.enums;

public enum TagType {
    USER("USER", "用户自定义标签"),
    CAPABILITY("CAPABILITY", "能力标签");

    private final String dbValue;
    private final String displayName;

    TagType(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TagType fromDbValue(String dbValue) {
        for (TagType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TagType: " + dbValue);
    }
}