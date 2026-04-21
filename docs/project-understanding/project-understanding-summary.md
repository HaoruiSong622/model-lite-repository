# ModelLite 旧版本项目代码信息汇总

> 本文档汇总了旧版本 ModelLite 项目（尤其是 repository 模块）的结构、接口、数据模型、业务规则等核心信息，供重构开发参考。

---

## 1. 项目整体结构

```
modellite-project/
├── ai-executor/
│   ├── api_manager.py
│   ├── evaluation/
│   ├── quantization/
│   └── type_conversion/
├── common/
├── controller-manager/
│   ├── client/
│   ├── controller/
│   ├── crd/
│   └── webhook/
├── finetune/
│   └── src/
├── frontend/
│   ├── public/
│   ├── shared/
│   ├── src/
│   └── webpack/
├── gateway/
│   └── src/
├── inference/
│   ├── inference/
│   └── router/
├── modellite-utils/
│   └── src/
└── repository/
    └── src/
```

---

## 2. 模块职责与依赖关系

### 2.1 模块职责说明

| 模块 | 职责说明 |
|------|----------|
| ai-executor | 负责 AI 模型执行相关的管理功能 |
| common | 包含项目通用的共享组件和工具 |
| controller-manager | 负责 Kubernetes 控制器管理 |
| finetune | 处理模型微调相关的功能 |
| frontend | 提供前端用户界面 |
| gateway | 作为系统的 API 网关和路由入口 |
| inference | 负责模型推理服务的部署和管理 |
| modellite-utils | 包含项目通用工具类和辅助功能 |
| repository | 负责模型仓库管理和存储 |

### 2.2 模块依赖关系

| 依赖关系 | 说明 |
|----------|------|
| controller-manager → modellite-utils | 依赖通用工具功能 |
| inference → controller-manager | 进行 Kubernetes 资源管理 |
| finetune → repository | 进行模型存储管理 |
| gateway → finetune/inference/repository | 作为入口调用各模块功能 |
| frontend → gateway | 通过 gateway 与后端交互 |
| ai-executor → inference/finetune | 提供底层执行支持 |

---

## 3. Repository 模块接口契约

### 3.1 ManageController（控制器层）

负责模型仓核心业务能力，包括模型的增删改查、版本管理、元数据维护、分类管理等功能。

```java
public class ManageController {

    // ===== 模型查询接口 =====
    
    /**
     * 批量查询模型仓的模型列表信息
     * @param request 请求信息
     * @param queryModelRequest 批量查询模型仓请求体
     * @return 模型列表信息
     */
    public ResponseEntity<BaseResponse> queryModelList(HttpServletRequest request, QueryModelRequest queryModelRequest);

    /**
     * 查询单个模型详情
     * @param request 请求信息
     * @param modelName 模型名称
     * @param receivedCycled 是否搜索回收站
     * @param modelOwnerGroupId 模型的资源组id
     * @return 查询单个模型详情响应
     */
    public ResponseEntity<BaseResponse> querySingleModelInfo(HttpServletRequest request, String modelName, Integer receivedCycled, String modelOwnerGroupId);

    /**
     * 根据资源组查询组下的资源[仅admin调用]
     * @param request 请求信息
     * @param queryModelRequest 批量查询模型仓请求体
     * @return 模型列表信息
     */
    public ResponseEntity<BaseResponse> queryModelListByResourceGruop(HttpServletRequest request, QueryModelRequest queryModelRequest);

    /**
     * 查询模型去重后的字段
     * @param request 请求信息
     * @param parameterName 查询参数名称
     * @return 去重后的字段列表
     */
    public ResponseEntity<BaseResponse> queryModelList(HttpServletRequest request, String parameterName);

    // ===== 模型删除/恢复接口 =====
    
    /**
     * 软删除单个模型
     * @param request 请求信息
     * @param modelName 模型名称
     * @param modelOwnerGroupId 模型的资源组id
     * @return 软删除响应
     */
    public ResponseEntity<BaseResponse> deleteModel(HttpServletRequest request, String modelName, String modelOwnerGroupId);

    /**
     * 软删除单个模型的指定版本
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号
     * @param modelOwnerGroupId 模型的资源组id
     * @return 软删除响应
     */
    public ResponseEntity<BaseResponse> deleteModelVersion(HttpServletRequest request, String modelName, String versionNo, String modelOwnerGroupId);

    /**
     * 硬删除单个模型/单个模型的指定版本
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号（可选）
     * @param modelOwnerGroupId 模型的资源组id
     * @return 硬删除响应
     */
    public ResponseEntity<BaseResponse> hardDeleteModelVersion(HttpServletRequest request, String modelName, String versionNo, String modelOwnerGroupId);

    /**
     * 回收站模型/版本恢复接口
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号（可选）
     * @param modelOwnerGroupId 模型的资源组id
     * @return 恢复响应
     */
    public ResponseEntity<BaseResponse> restoreVersionFromTrash(HttpServletRequest request, String modelName, String versionNo, String modelOwnerGroupId);

    // ===== 模型元数据接口 =====
    
    /**
     * 新增/更新模型元数据json文件
     * @param httpServletRequest 请求
     * @param modelName 模型名称
     * @param mergeModelRequest 新增/更新模型元数据请求体
     * @return 新增/更新响应
     */
    public ResponseEntity<BaseResponse> mergeModelJsonFile(HttpServletRequest httpServletRequest, String modelName, MergeModelRequest mergeModelRequest);

    /**
     * 新增/更新版本元数据json文件
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号
     * @param mergeVersionRequest 新增/更新请求体
     * @return 新增/更新响应
     */
    public ResponseEntity<BaseResponse> mergeVersionJsonFile(HttpServletRequest request, String modelName, String versionNo, MergeVersionRequest mergeVersionRequest);

    // ===== 同步任务接口 =====
    
    /**
     * 主动同步所有模型列表信息
     * @return 下发刷新任务响应
     */
    public ResponseEntity<BaseResponse> syncModelList();

    /**
     * 查询同步任务的状态信息
     * @return 同步任务的状态信息响应
     */
    public ResponseEntity<BaseResponse> querySyncStatus();

    // ===== 分类/类型管理接口 =====
    
    /**
     * 查询所有模型分类
     * @param modelCategoryList 支持功能列表，为空则返回全部
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> querySupportModelCategory(List<String> modelCategoryList);

    /**
     * 新增模型分类
     * @param modelCategory 新增的模型分类
     * @return 操作响应
     */
    public ResponseEntity<BaseResponse> addModelCategory(String modelCategory);

    /**
     * 删除模型分类
     * @param modelCategory 要删除的模型分类
     * @return 操作响应
     */
    public ResponseEntity<BaseResponse> delModelCategory(String modelCategory);

    /**
     * 查询支持的模型类型字段
     * @param modelCategoryList 支持功能列表
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> querySupportModelType(List<String> modelCategoryList);

    /**
     * 新增模型类型
     * @param modelCategory 新增的模型类型所属的分类
     * @param modelType 新增的模型类型
     * @return 操作响应
     */
    public ResponseEntity<BaseResponse> addModelType(String modelCategory, String modelType);

    /**
     * 删除模型类型
     * @param modelCategory 要删除的模型类型所属的分类
     * @param modelType 要删除的模型类型
     * @return 操作响应
     */
    public ResponseEntity<BaseResponse> delModelType(String modelCategory, String modelType);

    // ===== 量化任务接口 =====
    
    /**
     * 根据模型类型查询支持的量化类型列表
     * @param modelType 模型类型
     * @return 支持的量化类型列表
     */
    public ResponseEntity<RestResponse<List<String>>> queryQuantTypes(String modelType);

    /**
     * 创建模型量化任务
     * @param httpServletRequest http请求
     * @param quantCreateRequest 模型量化请求体
     * @return 量化任务id
     */
    public ResponseEntity<RestResponse<Long>> createModelQuantTask(HttpServletRequest httpServletRequest, QuantCreateRequest quantCreateRequest);

    /**
     * 查询模型量化任务列表
     * @param request 模型量化请求体
     * @param httpServletRequest http请求
     * @return 量化任务列表
     */
    public ResponseEntity<RestResponse<PageResult<ModelQuantTaskVo>>> queryModelQuantTasks(QuantQueryRequest request, HttpServletRequest httpServletRequest);

    /**
     * 删除量化任务
     * @param httpServletRequest 请求信息
     * @param taskId 任务id
     * @return 操作响应
     */
    public ResponseEntity<RestResponse> deleteModelQuantTask(Long taskId, HttpServletRequest httpServletRequest);

    /**
     * 停止量化任务
     * @param httpServletRequest 请求信息
     * @param taskId 任务id
     * @return 操作响应
     */
    public ResponseEntity<RestResponse> stopModelQuantTask(Long taskId, HttpServletRequest httpServletRequest);

    /**
     * 重试量化任务
     * @param httpServletRequest 请求信息
     * @param taskId 任务id
     * @return 操作响应
     */
    public ResponseEntity<RestResponse> restartModelQuantTask(Long taskId, HttpServletRequest httpServletRequest);

    // ===== 健康检查 =====
    
    /**
     * 健康检查
     * @param request 请求头
     * @return 健康检查响应
     */
    public ResponseEntity<String> healthCheck(HttpServletRequest request);
}
```

### 3.2 ModelManageService（服务层）

负责模型管理核心业务逻辑，包括模型信息处理、版本控制、文件操作、元数据维护等。

```java
public class ModelManageService {

    // ===== 模型查询 =====
    
    /**
     * 批量查询模型仓的模型列表信息
     * @param queryModelRequest 模型列表查询请求
     * @param request 请求信息
     * @param userResourceGroupId 用户资源组id
     * @return 模型列表信息
     */
    public QueryModelListResp queryModelInfo(QueryModelRequest queryModelRequest, HttpServletRequest request, String userResourceGroupId);

    /**
     * 查询模型去重后的字段
     * @param parameterName 查询去重的参数名称
     * @param userResourceGroupId 用户资源组id
     * @return 去重后的列表
     */
    public QueryDistinctNameListResp queryDistinctNamesList(String parameterName, String userResourceGroupId);

    /**
     * 查询单个模型信息和对应的版本信息
     * @param request 请求信息
     * @param modelName 模型名称
     * @param receivedCycled 接收是否在回收站
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型实例的创建者资源组id
     * @return 单个模型信息和对应的版本信息
     */
    public BaseResponse querySingleModelInfo(HttpServletRequest request, String modelName, Integer receivedCycled, String userResourceGroupId, String modelOwnerGroupId);

    // ===== 模型删除/恢复 =====
    
    /**
     * 硬删除单个模型数据
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 硬删除响应
     */
    public BaseResponse hardDeleteModel(String modelName, String userResourceGroupId, String modelOwnerGroupId);

    /**
     * 软删除单个模型
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型实例的创建者资源组id
     * @return 软删除响应
     */
    public BaseResponse softDeleteModelByName(String modelName, String userResourceGroupId, String modelOwnerGroupId);

    /**
     * 硬删除单条版本记录
     * @param versionNo 版本号
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 硬删除响应
     */
    public BaseResponse hardDeleteModelVersion(String versionNo, String modelName, String userResourceGroupId, String modelOwnerGroupId);

    /**
     * 软删除单个模型的指定版本
     * @param versionNo 版本号
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 软删除响应
     */
    public BaseResponse softDeleteByVersionAndModel(String versionNo, String modelName, String userResourceGroupId, String modelOwnerGroupId);

    /**
     * 回收站模型恢复
     * @param modelName 模型名
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 恢复响应
     */
    public BaseResponse restoreModelFromTrash(String modelName, String userResourceGroupId, String modelOwnerGroupId);

    /**
     * 回收站版本恢复
     * @param versionNo 模型版本
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 恢复响应
     */
    public BaseResponse restoreVersionFromTrash(String versionNo, String modelName, String userResourceGroupId, String modelOwnerGroupId);

    // ===== 元数据管理 =====
    
    /**
     * 新增/更新模型元数据json文件
     * @param modelName 模型名称
     * @param mergeModelRequest 请求体
     * @param userResourceGroupId 用户的资源组id
     * @return 增/更新响应
     */
    public CreateModelResp mergeModelJsonFile(String modelName, MergeModelRequest mergeModelRequest, String userResourceGroupId);

    /**
     * 新增/更新版本元数据json文件
     * @param versionNo 版本号
     * @param modelName 模型名称
     * @param mergeVersionRequest 请求体
     * @param userResourceGroupId 用户资源组id
     * @return 增/更新响应
     */
    public BaseResponse mergeVersionJsonFile(String versionNo, String modelName, MergeVersionRequest mergeVersionRequest, String userResourceGroupId);

    // ===== 分类/类型管理 =====
    
    /**
     * 查询支持的模型类型名称
     * @param modelCategoryList 支持功能列表,不填默认查询全部
     * @return 符合条件的模型类型列表
     */
    public QuerySupportModelTypeResp querySupportModelType(List<String> modelCategoryList);

    /**
     * 查询模型所支持的量化类型
     * @param modelType 模型类型
     * @return 支持的量化类型列表
     */
    public List<String> querySupportQuantTypes(String modelType);

    /**
     * 查询支持的模型分类列表
     * @param supportCategoryList 支持功能列表,不填默认查询全部
     * @return 符合条件的模型分类列表
     */
    public QuerySupportModelCategoryResp querySupportModelCategory(List<String> supportCategoryList);

    /**
     * 新增模型分类
     * @param modelCategory 新增的模型分类名称
     * @return 是否新增成功响应体
     */
    public BaseResponse addModelCategory(String modelCategory);

    /**
     * 删除模型分类
     * @param modelCategory 要删除的模型分类
     * @return 是否删除成功响应体
     */
    public BaseResponse delModelCategory(String modelCategory);

    /**
     * 在模型分类下增加一个新的模型类型
     * @param modelCategory 要删除的模型类型所属分类
     * @param modelType 要删除的模型类型
     * @return 是否新增成功响应体
     */
    public BaseResponse addModelType(String modelCategory, String modelType);

    /**
     * 在模型分类下删除一个模型类型
     * @param modelCategory 要删除的模型类型所属分类
     * @param modelType 要删除的模型类型
     * @return 是否删除成功响应体
     */
    public BaseResponse delModelType(String modelCategory, String modelType);
}
```

---

## 4. 数据模型与领域对象

### 4.1 模型信息相关

```java
class ModelInfo {
    modelId: String              // 模型ID
    modelName: String            // 模型具体名称
    modelType: String            // 模型类型
    author: String               // 模型作者
    seriesName: String           // 模型系列名称
    modelDescription: String     // 模型描述
    versionNum: Integer          // 版本数量
    modelSize: BigDecimal        // 模型大小，单位为B
    maxSeqLength: Integer        // 最大序列长度，单位为tokens
    deprecated: Boolean          // 是否已废弃；true为在回收站，false为正常使用
    weightStatus: int            // 权重状态: 0无权重 1上传中 2已暂停 3上传中断 4空间不足导致上传中断 5取消中 6有权重
    createTime: String           // 创建时间
    updateTime: String           // 更新时间
    supportFunctionList: List<String>  // 模型类型支持的用途
    userResourceGroupId: String  // 用户资源组id
    userResourceGroupName: String // 用户资源组名称
}

class ModelInfoJson {
    modelName: String            // 模型具体名称
    modelType: String            // 模型类型
    author: String               // 模型作者
    seriesName: String           // 模型系列名称
    modelDescription: String     // 模型描述
    modelSize: BigDecimal        // 模型大小，单位为B
    maxSeqLength: Integer        // 最大序列长度，单位为tokens
    deprecated: Boolean          // 是否已废弃
    createTime: String           // 创建时间
    updateTime: String           // 更新时间
    userResourceGroupId: String  // 用户资源组id
    userResourceGroupName: String // 用户资源组Name
}

class ModelInfoEntry {
    modelId: String              // 模型ID
    modelName: String            // 模型具体名称
    modelType: String            // 模型类型
    author: String               // 模型作者
    seriesName: String           // 模型系列名称
    modelDescription: String     // 模型描述
    versionNum: Integer          // 版本数量
    modelSize: BigDecimal        // 模型大小，单位为B
    maxSeqLength: Integer        // 最大序列长度，单位为tokens
    deprecated: Boolean          // 是否已废弃
    createTime: String           // 创建时间
    updateTime: String           // 更新时间
    userResourceGroupId: String  // 用户资源组id
    userResourceGroupName: String // 用户资源组Name
}

class ModelConfig {
    modelInfo: ModelInfo         // 模型信息
    configJson: String           // 配置JSON字符串
    versionInfoEntryList: List<VersionInfo>  // 版本信息列表
    versionListFilesMap: Map<String, List<String>>  // 版本文件映射
    versionSetWeightMissingIndexMap: Map<String, Set<Integer>>  // 版本权重缺失索引映射
}
```

### 4.2 版本信息相关

```java
class VersionInfo {
    versionId: String            // 版本id
    modelName: String            // 版本对应模型的名称
    versionNo: String            // 版本号
    versionPath: String          // 版本存放路径
    versionDescription: String   // 版本描述
    createTime: String           // 更新时间（版本创建时间）
    trainFrame: String           // 训练框架
    trainType: String            // 训练类型
    trainStrategy: String        // 训练策略
    trainTime: Long              // 训练时长,单位为毫秒
    finalLoss: String            // 最终loss
    deprecated: Boolean          // 是否已废弃
    weightStatue: int            // 状态 0无权重 1上传中 2已暂停 3上传中断 4空间不足导致上传中断 5取消中 6有权重
    weightType: String           // 模型权重类型
    sourceVersion: String        // 模型权重来源版本
    userResourceGroupId: String  // 权重模型的用户资源组id
}

class VersionInfoJson {
    modelName: String            // 版本对应模型的名称
    versionNo: String            // 版本号
    versionDescription: String   // 版本描述
    updateTime: String           // 更新时间
    trainFrame: String           // 训练框架
    trainType: String            // 训练类型
    trainStrategy: String        // 训练策略
    trainTime: Long              // 训练时长,单位为毫秒
    finalLoss: String            // 最终loss
    deprecated: Boolean          // 是否已废弃
    weightType: String           // 模型权重类型
    sourceVersion: String        // 模型权重来源版本
    userResourceGroupId: String  // 模型的资源组Id
}

class VersionInfoEntry {
    versionId: String            // 版本id
    modelName: String            // 版本对应模型的名称
    versionNo: String            // 版本号
    versionPath: String          // 版本所在路径
    versionDescription: String   // 版本描述
    updateTime: String           // 创建时间
    trainFrame: String           // 训练框架
    trainType: String            // 训练类型
    trainStrategy: String        // 训练策略
    trainTime: Long              // 训练时长,单位为毫秒
    finalLoss: String            // 最终loss
    deprecated: Boolean          // 是否已废弃
    weightType: String           // 模型权重类型
    sourceVersion: String        // 权重来源版本
    userResourceGroupId: String  // 权重模型的用户资源组id
}
```

### 4.3 分类与类型

```java
class ModelCategoryTypeInfo {
    singleCategoryTypeList: List<ModelTypeEntry>  // 该分类下模型类型列表
    modelCategoryName: String    // 模型分类名称
    internalBuilt: boolean       // 是否内置
}

class ModelCategoryEntry {
    modelCategory: String        // 模型分类
    internalBuilt: boolean       // 是否为内置
}

class ModelTypeEntry {
    modelType: String            // 模型类型
    supportFinetune: boolean     // 支持精调
    supportQuantify: boolean     // 支持量化
    modelCategory: String        // 模型分类
    internalBuilt: boolean       // 是否内置
}
```

### 4.4 任务与同步

```java
class SynTaskRecordEntity {
    id: String                   // 同步任务id
    state: Integer               // 同步任务状态（0：执行成功；1：执行中；2：失败）
    startTime: String            // 开始时间
    endTime: String              // 结束时间
    createType: String           // 创建类型（0：定时任务；1：接口触发）
    failReason: String           // 失败原因
}

class SynTaskRecordEntry {
    id: String                   // 同步任务id
    state: Integer               // 同步任务状态（0：执行成功；1：执行中；2：失败）
    startTime: String            // 开始时间
    endTime: String              // 结束时间
    createType: String           // 创建类型（0：定时任务；1：接口触发）
    failReason: String           // 失败原因
}

class UploadWeightTaskEntry {
    modelName: String            // 模型名称，用于确定任务
    versionNo: String            // 版本名称，用于确定任务
    sharedPath: String           // 远程共享目录
    status: int                  // 传输状态 0无权重 1上传中 2已暂停 3上传中断 4空间不足 5取消中 6有权重
    progress: float              // 百分比进度,默认0.00
    beginDate: LocalDate         // 任务创建时间
    taskId: String               // 任务ID
    userResourceGroupId: String  // 任务的用户资源组id
}

class UploadWeightMountRemoteEntry {
    sharedPath: String           // 远程共享目录
    agreement: String            // 使用的协议
    userName: String             // CIFS用户名
    password: String             // CIFS密码
    mountPath: String            // 本地挂载点
    mountDate: LocalDate         // 第一次挂载时间
    recentUploadDate: LocalDate  // 最近的使用时间
}
```

### 4.5 通用响应结构

```java
class PageResult<T> {
    count: long                  // 总数
    result: List<T>              // 查询结果列表
}
```

---

## 5. 业务规则与状态机

### 5.1 业务规则

| 规则名称 | 触发条件 | 处理方式 |
|----------|----------|----------|
| 模型数量上限 | 创建新模型时 | 数量超过100个拒绝创建 |
| 模型名称长度限制 | 创建或修改模型时 | 超过32字符拒绝操作 |
| 部署中模型禁止删除 | 尝试删除模型时 | 正在部署则抛出异常阻止 |
| 传输任务中版本禁止删除 | 尝试删除模型版本时 | 存在传输任务则抛出异常阻止 |
| 传输任务并发上限 | 创建新传输任务时 | 超过5个并发拒绝创建 |
| 传输任务状态限制 | 创建传输任务时 | 仅无权重(0)或有权重(6)可创建 |
| 上传中禁止重复任务 | 创建传输任务时 | 上传中(1)或取消中(5)拒绝创建 |
| 共享路径格式校验 | 提交共享路径时 | 不符合正则格式拒绝操作 |

### 5.2 权重状态机

#### 状态定义

| 状态码 | 状态名 | 描述 |
|--------|--------|------|
| 0 | EMPTY_WEIGHT | 无权重 |
| 1 | UPLOAD | 上传中 |
| 2 | PAUSE | 已暂停 |
| 3 | INTERRUPT | 上传中断 |
| 4 | SPACE_LIMITED | 空间不足导致上传中断 |
| 5 | CANCEL | 取消中 |
| 6 | HAS_WEIGHT | 有权重 |
| 7 | WEIGHT_MISSING | 权重缺失 |

#### 状态流转图

```
                    ┌─────────────────────────────────────────┐
                    │                                         │
                    ▼                                         │
EMPTY_WEIGHT(0) ───► UPLOAD(1) ───► HAS_WEIGHT(6)            │
      │                  │                                  │
      │                  ├──► PAUSE(2) ───► UPLOAD(1)       │
      │                  │         │                       │
      │                  │         └──► CANCEL(5)          │
      │                  │                                │
      │                  ├──► INTERRUPT(3) ───► UPLOAD(1)  │
      │                  │                                │
      │                  ├──► SPACE_LIMITED(4) ───► UPLOAD(1)
      │                  │                                │
      │                  └──► CANCEL(5) ───► EMPTY_WEIGHT(0)
      │                                                   │
      └───────────────────────────────────────────────────┘
      
HAS_WEIGHT(6) ───► WEIGHT_MISSING(7) ───► UPLOAD(1)
```

#### 状态流转规则

| 当前状态 | 触发事件 | 目标状态 |
|----------|----------|----------|
| EMPTY_WEIGHT | 开始上传 | UPLOAD |
| UPLOAD | 暂停 | PAUSE |
| UPLOAD | 上传完成 | HAS_WEIGHT |
| UPLOAD | 上传中断 | INTERRUPT |
| UPLOAD | 空间不足 | SPACE_LIMITED |
| UPLOAD | 取消上传 | CANCEL |
| PAUSE | 继续上传 | UPLOAD |
| PAUSE | 取消上传 | CANCEL |
| INTERRUPT | 重新上传 | UPLOAD |
| SPACE_LIMITED | 释放空间后重新上传 | UPLOAD |
| CANCEL | 取消完成 | EMPTY_WEIGHT |
| HAS_WEIGHT | 权重文件丢失 | WEIGHT_MISSING |
| WEIGHT_MISSING | 重新上传 | UPLOAD |

---

## 6. 外部系统依赖

| 依赖组件 | 用途 | 输入 | 输出 |
|----------|------|------|------|
| model_io_manager | 获取正在部署的模型列表信息 | 无 | 模型名称列表、模型状态 |
| MODEL_MANAGER_URL 环境变量 | 提供 model_io_manager 服务的访问地址 | 环境变量配置的URL | 服务访问端点 |

---

## 7. 旧版本与新版本对照要点

### 7.1 架构差异

| 方面 | 旧版本 | 新版本(v1.0架构) |
|------|--------|------------------|
| 技术栈 | Python (部分模块) + Java | Java 21 + Spring Boot 3.4.5 |
| 数据库 | 未明确 | PostgreSQL |
| ORM | 未明确 | MyBatis |
| 异步任务 | 未明确 | K8s Job + fabric8 Informer |
| 部署 | Kubernetes | Kubernetes + Leader Election |
| API版本 | 无明确版本号 | /v2 |

### 7.2 功能对照

| 旧版本功能 | 新版本需求 | 状态 |
|------------|------------|------|
| 模型列表查询 | REQ-QUERY-001 | ✓ 保留 |
| 模型详情查询 | REQ-MODEL-002 | ✓ 保留 |
| 软删除模型/版本 | REQ-DELETE-001 | ✓ 保留 |
| 硬删除模型/版本 | REQ-DELETE-002 | ✓ 保留 |
| 回收站恢复 | REQ-DELETE-003 | ✓ 保留 |
| 模型元数据更新 | REQ-MODEL-003 | ✓ 保留 |
| 分类/类型管理 | REQ-CATEGORY-001 | ✓ 保留 |
| 量化任务管理 | 范围外 | ✗ 不包含 |
| 同步任务 | 未明确 | - 待确认 |
| 权重上传任务 | REQ-UPLOAD-001/002 | ✓ 保留并增强 |
| 版本锁机制 | REQ-M2M-003 | ✓ 新增 |
| 权重格式转换 | REQ-CONVERT-001 | ✓ 新增 |
| 纳管方式 | REQ-REGISTER-001 | ✓ 新增概念 |

### 7.3 数据模型对照

| 旧版本字段 | 新版本字段 | 变化说明 |
|------------|------------|----------|
| modelId (String) | id (UUID) | 改用UUID |
| deprecated (Boolean) | deleted (Boolean) | 字段名变化 |
| weightStatus (int) | status (VARCHAR) | 改用枚举字符串 |
| 无版本锁 | version_lock 表 | 新增 |
| 无权重类型识别 | weight_type | 新增自动识别 |
| userResourceGroupId | resource_group | 字段名变化 |

---

## 8. 参考资料

- [需求规格说明书 v1.1](../ModelLite-模型仓库-需求规格说明书-v1.1.md)
- [架构设计文档 v1.0](../architecture/ModelLite-模型仓库-架构设计-v1.0.md)

---

**文档生成日期**: 2026-04-20