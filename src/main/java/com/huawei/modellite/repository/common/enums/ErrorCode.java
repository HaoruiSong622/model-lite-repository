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

    // ===== 分类相关 =====
    public static final String CATEGORY_NOT_FOUND = "0102015";             // 分类不存在
    public static final String CATEGORY_NAME_EXISTS = "0102016";           // 分类名称已存在
    public static final String CATEGORY_BUILTIN = "0102017";               // 内置分类不允许删除

    // ===== 模型类型相关 =====
    public static final String MODEL_TYPE_NAME_EXISTS = "0102018";         // 同分类下类型名称已存在
    public static final String MODEL_TYPE_NOT_FOUND = "0102019";           // 模型类型不存在
    public static final String MODEL_TYPE_BUILTIN = "0102020";             // 内置模型类型不允许删除

    // ===== 标签相关 =====
    public static final String TAG_NAME_EXISTS = "0102021";               // 标签名称已存在
    public static final String TAG_NOT_FOUND = "0102022";                 // 标签不存在
    public static final String TAG_BUILTIN = "0102023";                   // 内置标签不允许删除
    // 0102024: DEPRECATED - 原标签被引用禁止删除，现改为级联删除
    public static final String MODEL_TAG_LIMIT_EXCEEDED = "0102025";       // 模型标签数量超过上限
    public static final String MODEL_TAG_NOT_FOUND = "0102026";            // 模型与标签无关联

    // ===== 模型版本相关 =====
    public static final String MODEL_TYPE_NOT_BELONG_TO_CATEGORY = "0102027"; // 模型类型不属于该分类
    public static final String MODEL_RESOURCE_GROUP_IMMUTABLE = "0102028";   // 资源组不可修改
    public static final String MODEL_GLOBAL_CAPACITY_EXCEEDED = "0102029";   // 全局模型数量超出限制
    public static final String VERSION_INVALID_CREATE_MODE = "0102030";      // 无效的版本创建模式
    public static final String TAG_NOT_FOUND_F3 = "0102031";                 // 标签不存在
    public static final String MODEL_TAG_COUNT_EXCEEDED = "0102032";          // 模型标签数量超出限制（≤20）
    public static final String STORAGE_PATH_PVC_NAME_REQUIRED = "0102033";
    public static final String STORAGE_PATH_NFS_REQUIRED = "0102034";
    public static final String VERSION_STATUS_INVALID_FOR_REGISTER = "0102035";
    public static final String REGISTER_SOURCE_TYPE_REQUIRED = "0102036";

    // ===== 上传任务扩展 =====
    public static final String UPLOAD_TASK_STATUS_CONFLICT = "0102037";
    public static final String UPLOAD_TASK_ACTIVE_EXISTS = "0102038";
    public static final String UPLOAD_TASK_VERSION_NOT_NO_WEIGHT = "0102039";
    public static final String UPLOAD_TASK_INVALID_PROGRESS = "0102040";
    public static final String UPLOAD_SOURCE_PATH_INVALID = "0102041";
    public static final String UPLOAD_CIFS_CREDENTIALS_REQUIRED = "0102042";
    public static final String UPLOAD_TASK_JOB_SUBMIT_FAILED = "0102043";
    public static final String UPLOAD_TASK_ALREADY_TERMINATED = "0102044";
}
