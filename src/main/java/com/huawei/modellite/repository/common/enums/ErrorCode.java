package com.huawei.modellite.repository.common.enums;

public final class ErrorCode {

    private ErrorCode() {}

    // ===== 模型相关 =====
    public static final String MODEL_NOT_FOUND = "0102001";
    public static final String MODEL_NAME_EXISTS = "0102002";
    public static final String MODEL_NAME_IMMUTABLE = "0102003";
    public static final String CATEGORY_HAS_MODELS = "0102004";
    public static final String MODEL_CAPACITY_EXCEEDED = "0102005";

    // ===== 版本相关 =====
    public static final String VERSION_NOT_FOUND = "0102006";
    public static final String VERSION_NUMBER_GAP = "0102007";
    public static final String VERSION_LOCKED = "0102008";
    public static final String VERSION_CAPACITY_EXCEEDED = "0102009";

    // ===== 上传任务相关 =====
    public static final String UPLOAD_TASK_NOT_FOUND = "0102010";
    public static final String FILE_SUFFIX_NOT_ALLOWED = "0102011";
    public static final String UPLOAD_TASK_CONCURRENT_LIMIT = "0102012";

    // ===== 转换任务相关 =====
    public static final String CONVERT_TASK_NOT_FOUND = "0102013";
    public static final String UNSUPPORTED_CONVERT_FORMAT = "0102014";
}
