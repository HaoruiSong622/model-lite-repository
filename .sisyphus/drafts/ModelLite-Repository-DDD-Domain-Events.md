# ModelLite 模型仓库 - 领域事件风暴（Domain Event Storming）

> **文档类型**: DDD 领域事件清单  
> **文档版本**: v1.2  
> **编写日期**: 2026-04-21  
> **适用范围**: ModelLite 平台模型仓库模块 DDD 架构设计  
> **目标读者**: 架构师、领域专家、后端开发工程师

---

## 1. 事件风暴概述

### 1.1 什么是领域事件

领域事件（Domain Event）是领域中已经发生且领域专家关心的事实。事件命名使用**过去时态**，表示不可变的、已发生的事实。

**核心特征**:
- **不可变性**: 事件一旦发布，不能修改或撤销
- **时态性**: 必须使用过去时命名（Created, Completed, Failed）
- **业务意义**: 事件必须对业务有价值，领域专家关心该事件
- **解耦作用**: 用于跨聚合通信，实现最终一致性

### 1.2 事件命名规范

```
[业务对象] + [业务动作] + Event

示例:
- ModelCreatedEvent (模型已创建)
- VersionUploadedEvent (版本已上传)
- WeightValidationCompletedEvent (权重校验已完成)
```

### 1.3 事件分类

| 分类 | 说明 | 示例 |
|------|------|------|
| **生命周期事件** | 实体创建、修改、删除 | ModelCreated, VersionDeleted |
| **状态变更事件** | 状态转换 | VersionStatusChanged |
| **业务流程事件** | 业务流程里程碑 | WeightUploaded, ValidationCompleted |
| **异常事件** | 错误和失败 | UploadFailed, ValidationFailed |
| **时间事件** | 基于时间触发 | LocksCleaned |

---

## 2. 模型聚合领域事件

### 2.1 模型生命周期事件

#### ModelCreatedEvent - 模型已创建
```yaml
触发时机: 创建模型及第一个版本成功后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  name: String           # 模型名称
  categoryId: UUID       # 分类ID
  typeId: UUID           # 类型ID
  resourceGroup: String  # 资源组
  createUser: String     # 创建者
  createTime: DateTime   # 创建时间
  firstVersionId: UUID   # 首个版本ID
业务价值: 
  - 记录操作日志
  - 触发首个版本的初始化流程
消费方:
  - AuditLogHandler (审计日志)
  - NotificationHandler (通知)
```

#### ModelModifiedEvent - 模型已修改
```yaml
触发时机: 修改模型元数据（描述、分类、类型、标签）后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  changedFields: Map     # 变更字段及新值
  oldValues: Map         # 变更字段旧值
  operator: String       # 操作者
  updateTime: DateTime   # 更新时间
业务价值:
  - 记录变更历史
  - 触发关联更新（如分类变更后的索引更新）
消费方:
  - AuditLogHandler
  - SearchIndexHandler (搜索索引更新)
```

#### ModelSoftDeletedEvent - 模型已软删除
```yaml
触发时机: 软删除模型后（级联删除所有版本）
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  name: String           # 模型名称
  versionIds: List<UUID> # 被级联删除的版本ID列表
  operator: String       # 操作者
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录删除操作
  - 触发回收站管理
  - 清理相关资源
消费方:
  - AuditLogHandler
  - RecycleBinHandler
  - ResourceCleanupHandler
```

#### ModelHardDeletedEvent - 模型已硬删除
```yaml
触发时机: 硬删除模型后（从回收站彻底删除）
发布方: Model Aggregate
事件数据:
  modelId: UUID              # 模型ID
  name: String               # 模型名称
  deletedVersions: List      # 被删除的版本信息（含路径）
  operator: String           # 操作者
  deleteTime: DateTime       # 删除时间
业务价值:
  - 彻底清理存储资源
  - 记录审计日志
消费方:
  - AuditLogHandler
  - FileCleanupHandler (文件清理)
  - SearchIndexHandler (索引清理)
```

#### ModelRestoredEvent - 模型已恢复
```yaml
触发时机: 从回收站恢复模型后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  name: String           # 模型名称
  restoredVersions: List # 恢复的版本列表
  operator: String       # 操作者
  restoreTime: DateTime  # 恢复时间
业务价值:
  - 恢复业务数据
  - 记录恢复操作
消费方:
  - AuditLogHandler
  - RecycleBinHandler
  - SearchIndexHandler
```

### 2.2 版本生命周期事件

#### VersionCreatedEvent - 版本已创建
```yaml
触发时机: 创建新版本后
发布方: Model Aggregate (通过 ModelVersion 实体)
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  versionNumber: Integer # 版本号
  uploadType: Enum       # 上传类型 (REGISTER/UPLOAD/ARCHIVE/CONVERT)
  storagePath: String    # 存储路径
  createUser: String     # 创建者
  createTime: DateTime   # 创建时间
业务价值:
  - 触发权重上传流程
  - 记录版本创建
消费方:
  - AuditLogHandler
  - WeightUploadHandler
```

#### VersionStatusChangedEvent - 版本状态已变更
```yaml
触发时机: 版本状态发生任何变更后
发布方: ModelVersion Entity
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  versionNumber: Integer # 版本号
  oldStatus: Enum        # 旧状态
  newStatus: Enum        # 新状态
  changeTime: DateTime   # 变更时间
  reason: String         # 变更原因（可选）
业务价值:
  - 状态机审计
  - 触发状态相关的后续操作
消费方:
  - AuditLogHandler
  - StatusNotificationHandler
```

#### VersionSoftDeletedEvent - 版本已软删除
```yaml
触发时机: 软删除特定版本后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  versionNumber: Integer # 版本号
  operator: String       # 操作者
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录版本删除
  - 触发版本级回收站管理
消费方:
  - AuditLogHandler
  - RecycleBinHandler
```

#### VersionHardDeletedEvent - 版本已硬删除
```yaml
触发时机: 硬删除特定版本后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  versionNumber: Integer # 版本号
  storagePath: String    # 存储路径（用于清理）
  isRegistered: Boolean  # 是否为纳管版本
  operator: String       # 操作者
  deleteTime: DateTime   # 删除时间
业务价值:
  - 清理存储资源
  - 记录彻底删除操作
消费方:
  - AuditLogHandler
  - FileCleanupHandler
```

#### VersionRestoredEvent - 版本已恢复
```yaml
触发时机: 从回收站恢复特定版本后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  versionNumber: Integer # 版本号
  operator: String       # 操作者
  restoreTime: DateTime  # 恢复时间
业务价值:
  - 恢复版本数据
  - 记录恢复操作
消费方:
  - AuditLogHandler
  - RecycleBinHandler
```

### 2.3 版本锁定事件

#### VersionLockedEvent - 版本已锁定
```yaml
触发时机: 训推任务成功锁定版本后
发布方: Model Aggregate (通过 ModelVersion 实体)
事件数据:
  versionId: UUID        # 版本ID
  lockId: UUID           # 锁ID
  lockerId: String       # 锁定者ID（任务ID）
  lockType: Enum         # 锁定类型 (TRAINING/INFERENCE/EVALUATION/DEVELOPMENT)
  expireTime: DateTime   # 过期时间
  lockTime: DateTime     # 锁定时间
业务价值:
  - 记录锁定操作
  - 防止版本被删除
消费方:
  - AuditLogHandler
  - LockMonitoringHandler
```

#### VersionUnlockedEvent - 版本已解锁
```yaml
触发时机: 训推任务解锁版本后
发布方: Model Aggregate
事件数据:
  versionId: UUID        # 版本ID
  lockId: UUID           # 锁ID
  lockerId: String       # 锁定者ID
  remainingLocks: Integer# 剩余锁数量
  unlockTime: DateTime   # 解锁时间
业务价值:
  - 记录解锁操作
  - 当剩余锁为0时，版本可删除
消费方:
  - AuditLogHandler
  - LockMonitoringHandler
```

#### VersionLockRenewedEvent - 版本锁已续约
```yaml
触发时机: 锁持有者成功续约后
发布方: VersionLockService (领域服务)
事件数据:
  versionId: UUID        # 版本ID
  lockId: UUID           # 锁ID
  lockerId: String       # 锁定者ID
  oldExpireTime: DateTime # 原过期时间
  newExpireTime: DateTime # 新过期时间（当前时间 + TTL）
  renewalTime: DateTime  # 续约时间
  renewalCount: Integer  # 累计续约次数
业务价值:
  - 记录锁续约操作
  - 追踪长时间运行的任务
  - 监控续约频率（异常频繁的续约可能表示问题）
消费方:
  - AuditLogHandler
  - LockMonitoringHandler
```

#### VersionLockRenewalFailedEvent - 版本锁续约已失败
```yaml
触发时机: 锁续约失败时（锁已过期、身份验证失败等）
发布方: VersionLockService (领域服务)
事件数据:
  versionId: UUID        # 版本ID
  lockId: UUID           # 锁ID
  lockerId: String       # 锁定者ID
  reason: String         # 失败原因
    - LOCK_EXPIRED       # 锁已过期
    - INVALID_LOCKER     # 锁定者身份验证失败
    - LOCK_NOT_FOUND     # 锁不存在
  failureTime: DateTime  # 失败时间
业务价值:
  - 记录续约失败
  - 触发告警（任务可能因锁过期而被中断）
消费方:
  - AuditLogHandler
  - AlertHandler (告警)
```

#### VersionLockExpiredEvent - 版本锁已过期
```yaml
触发时机: Leader节点巡检发现锁已过期并清理后
发布方: VersionLockService (领域服务)
事件数据:
  versionId: UUID        # 版本ID
  expiredLockIds: List   # 过期锁ID列表
  lockerId: String       # 原锁定者ID（用于追踪）
  originalLockTime: DateTime # 原锁定时间
  originalExpireTime: DateTime # 原过期时间
  cleanupTime: DateTime  # 清理时间
  missedRenewals: Integer # 错过续约次数（预估）
业务价值:
  - 防止僵尸锁
  - 记录异常解锁（调用方未正常解锁或续约）
  - 分析锁过期原因（任务异常退出 vs 正常完成未解锁）
消费方:
  - AuditLogHandler
  - AlertHandler (告警)
```

#### VersionLockExpiringSoonEvent - 版本锁即将过期
```yaml
触发时机: Leader节点巡检发现锁即将过期（如剩余时间 < 1小时）
发布方: VersionLockService (领域服务)
事件数据:
  versionId: UUID        # 版本ID
  lockId: UUID           # 锁ID
  lockerId: String       # 锁定者ID
  currentExpireTime: DateTime # 当前过期时间
  remainingMinutes: Integer # 剩余分钟数
  warningTime: DateTime  # 告警时间
业务价值:
  - 提前预警，给任务方预留续约时间
  - 防止因疏忽导致锁过期
消费方:
  - AlertHandler (告警通知)
  - NotificationHandler (通知任务方)
```

---

## 3. 权重管理领域事件

### 3.1 权重上传事件

#### WeightRegisteredEvent - 权重已纳管
```yaml
触发时机: 纳管操作完成后（仅记录路径，不复制文件）
发布方: WeightUploadService (领域服务)
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  sourcePath: String     # 源存储路径
  isReadOnly: Boolean    # 是否只读（纳管版本为true）
  registerTime: DateTime # 纳管时间
业务价值:
  - 触发权重校验
消费方:
  - AuditLogHandler
  - WeightValidationHandler
  - WeightTypeRecognitionHandler
```

#### WeightUploadStartedEvent - 权重上传已开始
```yaml
触发时机: 上传任务创建并启动后
发布方: UploadTask Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  taskId: UUID           # 任务ID
  sourceType: Enum       # 源类型 (NFS/CIFS/PVC)
  sourcePath: String     # 源路径
  targetPath: String     # 目标路径
  createUser: String     # 创建者
  startTime: DateTime    # 开始时间
业务价值:
  - 记录上传开始
  - 版本状态变更为上传中
消费方:
  - AuditLogHandler
  - TaskMonitoringHandler
```

#### WeightUploadCompletedEvent - 权重上传已完成
```yaml
触发时机: 上传任务成功完成后
发布方: UploadTask Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  taskId: UUID           # 任务ID
  targetPath: String     # 目标路径
  completionTime: DateTime # 完成时间
业务价值:
  - 触发权重校验和类型识别
  - 更新版本状态为可用
消费方:
  - AuditLogHandler
  - WeightValidationHandler
  - WeightTypeRecognitionHandler
  - VersionStatusHandler
```

#### WeightUploadFailedEvent - 权重上传已失败
```yaml
触发时机: 上传任务失败后
发布方: UploadTask Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  taskId: UUID           # 任务ID
  errorCode: String      # 错误码
  errorMessage: String   # 错误信息
  failureTime: DateTime  # 失败时间
业务价值:
  - 记录失败原因
  - 更新版本状态为上传失败
  - 触发告警
消费方:
  - AuditLogHandler
  - VersionStatusHandler
  - AlertHandler
```

### 3.2 权重校验事件

#### WeightValidationStartedEvent - 权重校验已开始
```yaml
触发时机: 触发权重完整性校验后
发布方: WeightValidationService
事件数据:
  versionId: UUID        # 版本ID
  validationType: Enum   # 校验类型 (FILE_COUNT/SHA256/...)
  startTime: DateTime    # 开始时间
业务价值:
  - 记录校验开始
消费方:
  - AuditLogHandler
```

#### WeightValidationCompletedEvent - 权重校验已完成
```yaml
触发时机: 权重完整性校验完成后
发布方: WeightValidationService
事件数据:
  versionId: UUID        # 版本ID
  validationResult: Enum # 校验结果 (PASSED/FAILED)
  details: Map           # 校验详情
  completionTime: DateTime # 完成时间
业务价值:
  - 更新版本校验状态
  - 如失败，标记版本为校验失败
消费方:
  - AuditLogHandler
  - VersionStatusHandler
```

#### WeightTypeRecognizedEvent - 权重类型已识别
```yaml
触发时机: 权重类型识别完成后
发布方: WeightValidationService
事件数据:
  versionId: UUID        # 版本ID
  weightType: String     # 识别的权重类型 (FP16/w8a8/...)
  recognizedTime: DateTime # 识别时间
业务价值:
  - 更新版本权重类型
  - 展示给用户
消费方:
  - AuditLogHandler
  - VersionMetadataHandler
```

#### WeightTypeRecognitionFailedEvent - 权重类型识别已失败
```yaml
触发时机: 权重类型识别失败时（config.json不存在或格式异常）
发布方: WeightValidationService
事件数据:
  versionId: UUID        # 版本ID
  reason: String         # 失败原因
  failureTime: DateTime  # 失败时间
业务价值:
  - 记录识别失败
  - 标记版本为异常状态
消费方:
  - AuditLogHandler
  - VersionStatusHandler
  - AlertHandler
```

### 3.3 权重归档事件

#### TrainingWeightArchivedEvent - 训练权重已归档
```yaml
触发时机: 训练模块归档权重完成
发布方: TrainingArchiveService
事件数据:
  modelId: UUID             # 模型ID
  versionId: UUID           # 新版本ID
  versionNumber: Integer    # 版本号
  trainingMetadata: Object  # 训练元数据
    - trainFrame: String    # 训练框架
    - trainType: String     # 训练类型
    - trainStrategy: String # 训练策略
    - trainTime: Long       # 训练时长
    - finalLoss: String     # 最终loss
    - sourceVersion: String # 来源版本
  archiveTime: DateTime     # 归档时间
业务价值:
  - 记录训练产出
  - 触发权重校验
消费方:
  - AuditLogHandler
  - WeightValidationHandler
```

### 3.4 权重转换事件

#### WeightConversionStartedEvent - 权重转换已开始
```yaml
触发时机: 转换任务创建并启动后
发布方: ConvertTask Aggregate
事件数据:
  modelId: UUID          # 模型ID
  sourceVersionId: UUID  # 源版本ID
  targetVersionId: UUID  # 目标版本ID
  taskId: UUID           # 任务ID
  sourceFormat: Enum     # 源格式
  targetFormat: Enum     # 目标格式
  createUser: String     # 创建者
  startTime: DateTime    # 开始时间
业务价值:
  - 记录转换开始
  - 源版本已自动锁定
消费方:
  - AuditLogHandler
  - TaskMonitoringHandler
```

#### WeightConversionCompletedEvent - 权重转换已完成
```yaml
触发时机: 转换任务成功完成后
发布方: ConvertTask Aggregate
事件数据:
  modelId: UUID          # 模型ID
  sourceVersionId: UUID  # 源版本ID
  targetVersionId: UUID  # 目标版本ID
  taskId: UUID           # 任务ID
  completionTime: DateTime # 完成时间
业务价值:
  - 解锁源版本
  - 更新目标版本状态为可用
  - 触发目标版本校验
消费方:
  - AuditLogHandler
  - VersionUnlockHandler
  - VersionStatusHandler
  - WeightValidationHandler
```

#### WeightConversionFailedEvent - 权重转换已失败
```yaml
触发时机: 转换任务失败后
发布方: ConvertTask Aggregate
事件数据:
  modelId: UUID          # 模型ID
  sourceVersionId: UUID  # 源版本ID
  taskId: UUID           # 任务ID
  errorCode: String      # 错误码
  errorMessage: String   # 错误信息
  failureTime: DateTime  # 失败时间
业务价值:
  - 解锁源版本
  - 记录失败原因
消费方:
  - AuditLogHandler
  - VersionUnlockHandler
  - AlertHandler
```

---

## 4. 任务管理领域事件

### 4.1 上传任务事件

#### UploadTaskCreatedEvent - 上传任务已创建
```yaml
触发时机: 上传任务创建后
发布方: UploadTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  sourcePath: String     # 源路径
  sourceType: Enum       # 源类型
  targetPath: String     # 目标路径
  createUser: String     # 创建者
  createTime: DateTime   # 创建时间
业务价值:
  - 记录任务创建
消费方:
  - AuditLogHandler
```

#### UploadTaskPausedEvent - 上传任务已暂停
```yaml
触发时机: 用户暂停上传任务后
发布方: UploadTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  pauseTime: DateTime    # 暂停时间
  currentProgress: Integer # 当前进度
业务价值:
  - 记录暂停操作
消费方:
  - AuditLogHandler
```

#### UploadTaskResumedEvent - 上传任务已恢复
```yaml
触发时机: 用户恢复上传任务后
发布方: UploadTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  resumeTime: DateTime   # 恢复时间
业务价值:
  - 记录恢复操作
消费方:
  - AuditLogHandler
```

#### UploadTaskCancelledEvent - 上传任务已取消
```yaml
触发时机: 用户取消上传任务后
发布方: UploadTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  versionId: UUID        # 版本ID
  cancelTime: DateTime   # 取消时间
业务价值:
  - 记录取消操作
  - 触发版本状态更新
消费方:
  - AuditLogHandler
  - VersionStatusHandler
```

#### UploadTaskDeletedEvent - 上传任务已删除
```yaml
触发时机: 删除上传任务记录后
发布方: UploadTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录任务删除
  - 清理任务相关资源
消费方:
  - AuditLogHandler
```

### 4.2 转换任务事件

#### ConvertTaskCreatedEvent - 转换任务已创建
```yaml
触发时机: 转换任务创建后
发布方: ConvertTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  modelId: UUID          # 模型ID
  sourceVersionId: UUID  # 源版本ID
  targetVersionId: UUID  # 目标版本ID
  sourceFormat: Enum     # 源格式
  targetFormat: Enum     # 目标格式
  createUser: String     # 创建者
  createTime: DateTime   # 创建时间
业务价值:
  - 记录任务创建
消费方:
  - AuditLogHandler
```

#### ConvertTaskDeletedEvent - 转换任务已删除
```yaml
触发时机: 删除转换任务记录后
发布方: ConvertTask Aggregate
事件数据:
  taskId: UUID           # 任务ID
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录任务删除
消费方:
  - AuditLogHandler
```

---

## 5. 分类体系领域事件

### 5.1 分类管理事件

#### CategoryCreatedEvent - 分类已创建
```yaml
触发时机: 创建新分类后
发布方: Category Aggregate
事件数据:
  categoryId: UUID       # 分类ID
  name: String           # 分类名称
  description: String    # 分类描述
  isBuiltIn: Boolean     # 是否内置
  createUser: String     # 创建者
  createTime: DateTime   # 创建时间
业务价值:
  - 记录分类创建
消费方:
  - AuditLogHandler
  - SearchIndexHandler
```

#### CategoryDeletedEvent - 分类已删除
```yaml
触发时机: 删除分类后（确保无模型引用）
发布方: Category Aggregate
事件数据:
  categoryId: UUID       # 分类ID
  name: String           # 分类名称
  operator: String       # 操作者
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录分类删除
消费方:
  - AuditLogHandler
  - SearchIndexHandler
```

### 5.2 类型管理事件

#### ModelTypeCreatedEvent - 模型类型已创建
```yaml
触发时机: 在分类下创建新类型后
发布方: Category Aggregate
事件数据:
  typeId: UUID           # 类型ID
  categoryId: UUID       # 所属分类ID
  name: String           # 类型名称
  description: String    # 类型描述
  supportFinetune: Boolean # 是否支持微调
  isBuiltIn: Boolean     # 是否内置
  createUser: String     # 创建者
  createTime: DateTime   # 创建时间
业务价值:
  - 记录类型创建
消费方:
  - AuditLogHandler
  - SearchIndexHandler
```

#### ModelTypeDeletedEvent - 模型类型已删除
```yaml
触发时机: 删除类型后（确保无模型引用）
发布方: Category Aggregate
事件数据:
  typeId: UUID           # 类型ID
  categoryId: UUID       # 所属分类ID
  name: String           # 类型名称
  operator: String       # 操作者
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录类型删除
消费方:
  - AuditLogHandler
  - SearchIndexHandler
```

---

## 6. 标签管理领域事件

#### TagCreatedEvent - 标签已创建
```yaml
触发时机: 创建新标签后
发布方: Tag Entity
事件数据:
  tagId: UUID            # 标签ID
  name: String           # 标签名称
  createTime: DateTime   # 创建时间
业务价值:
  - 记录标签创建
消费方:
  - AuditLogHandler
```

#### TagAddedToModelEvent - 标签已添加到模型
```yaml
触发时机: 为模型添加标签后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  tagId: UUID            # 标签ID
  tagName: String        # 标签名称
  operator: String       # 操作者
  addTime: DateTime      # 添加时间
业务价值:
  - 记录标签关联
消费方:
  - AuditLogHandler
  - SearchIndexHandler
```

#### TagRemovedFromModelEvent - 标签已从模型移除
```yaml
触发时机: 从模型移除标签后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  tagId: UUID            # 标签ID
  tagName: String        # 标签名称
  operator: String       # 操作者
  removeTime: DateTime   # 移除时间
业务价值:
  - 记录标签解除关联
消费方:
  - AuditLogHandler
  - SearchIndexHandler
```

#### TagDeletedEvent - 标签已删除
```yaml
触发时机: 删除标签后（如有模型使用，仅删除标签定义，保留关联）
发布方: Tag Entity
事件数据:
  tagId: UUID            # 标签ID
  name: String           # 标签名称
  operator: String       # 操作者
  deleteTime: DateTime   # 删除时间
业务价值:
  - 记录标签删除
消费方:
  - AuditLogHandler
```

---

## 7. 回收站管理领域事件

#### ModelMovedToRecycleBinEvent - 模型已移至回收站
```yaml
触发时机: 模型软删除后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  name: String           # 模型名称
  versionCount: Integer  # 包含版本数
  operator: String       # 操作者
  moveTime: DateTime     # 移入时间
业务价值:
  - 记录回收站操作
消费方:
  - AuditLogHandler
  - RecycleBinHandler
```

#### VersionMovedToRecycleBinEvent - 版本已移至回收站
```yaml
触发时机: 版本软删除后
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  versionId: UUID        # 版本ID
  versionNumber: Integer # 版本号
  operator: String       # 操作者
  moveTime: DateTime     # 移入时间
业务价值:
  - 记录回收站操作
消费方:
  - AuditLogHandler
  - RecycleBinHandler
```

---

## 8. 系统级领域事件

#### ExpiredLocksCleanedEvent - 过期锁已清理
```yaml
触发时机: Leader节点巡检清理过期锁后
发布方: VersionLockService
事件数据:
  cleanedLockCount: Integer # 清理的锁数量
  cleanedLockIds: List      # 清理的锁ID列表
  cleanupTime: DateTime     # 清理时间
业务价值:
  - 记录僵尸锁清理
  - 发现异常（调用方未正常解锁或续约）
消费方:
  - AuditLogHandler
  - AlertHandler
```

#### LockRenewalPatternAnomalyDetectedEvent - 锁续约模式异常已检测到
```yaml
触发时机: 系统检测到异常的锁续约模式时（如过于频繁的续约、长时间未续约等）
发布方: LockMonitoringHandler
事件数据:
  versionId: UUID          # 版本ID
  lockId: UUID             # 锁ID
  lockerId: String         # 锁定者ID
  anomalyType: Enum        # 异常类型
    - EXCESSIVE_RENEWAL    # 续约过于频繁（可能表示任务异常循环）
    - LONG_NO_RENEWAL      # 长时间未续约（可能表示任务已挂起）
    - IRREGULAR_PATTERN    # 不规则的续约模式
  details: String          # 异常详情
  detectionTime: DateTime  # 检测时间
业务价值:
  - 发现潜在的任务异常
  - 提前预警可能的锁过期风险
消费方:
  - AlertHandler (告警)
  - NotificationHandler (通知任务方)
```

#### ResourceGroupVisibilityChangedEvent - 资源组可见性已变更
```yaml
触发时机: 模型资源组变更后（如未来支持）
发布方: Model Aggregate
事件数据:
  modelId: UUID          # 模型ID
  oldResourceGroup: String # 旧资源组
  newResourceGroup: String # 新资源组
  operator: String       # 操作者
  changeTime: DateTime   # 变更时间
业务价值:
  - 记录权限变更
消费方:
  - AuditLogHandler
  - PermissionHandler
```

---

## 9. 事件总览矩阵

### 9.1 按聚合分类

| 聚合 | 事件数量 | 核心事件 |
|------|----------|----------|
| **Model Aggregate** | 15 | ModelCreated, ModelModified, ModelDeleted, VersionCreated, VersionStatusChanged, VersionLocked, VersionUnlocked |
| **UploadTask Aggregate** | 6 | UploadTaskCreated, WeightUploadStarted, WeightUploadCompleted, WeightUploadFailed, UploadTaskPaused, UploadTaskCancelled |
| **ConvertTask Aggregate** | 4 | ConvertTaskCreated, WeightConversionStarted, WeightConversionCompleted, WeightConversionFailed |
| **Category Aggregate** | 4 | CategoryCreated, CategoryDeleted, ModelTypeCreated, ModelTypeDeleted |
| **Tag Entity** | 2 | TagCreated, TagDeleted |
 | **领域服务** | 10 | WeightRegistered, TrainingWeightArchived, WeightValidationCompleted, WeightTypeRecognized, VersionLockRenewed, VersionLockRenewalFailed, VersionLockExpired, VersionLockExpiringSoon, ExpiredLocksCleaned, LockRenewalPatternAnomalyDetected |

### 9.2 按业务领域分类

| 业务领域 | 事件数量 | 事件列表 |
|----------|----------|----------|
| **模型生命周期** | 7 | ModelCreated, ModelModified, ModelSoftDeleted, ModelHardDeleted, ModelRestored, ModelMovedToRecycleBin, ResourceGroupVisibilityChanged |
| **版本生命周期** | 7 | VersionCreated, VersionStatusChanged, VersionSoftDeleted, VersionHardDeleted, VersionRestored, VersionMovedToRecycleBin |
| **版本锁定** | 7 | VersionLocked, VersionUnlocked, VersionLockRenewed, VersionLockRenewalFailed, VersionLockExpired, VersionLockExpiringSoon, LockRenewalPatternAnomalyDetected |
| **权重管理** | 10 | WeightRegistered, WeightUploadStarted, WeightUploadCompleted, WeightUploadFailed, WeightValidationStarted, WeightValidationCompleted, WeightTypeRecognized, WeightTypeRecognitionFailed, TrainingWeightArchived |
| **权重转换** | 3 | WeightConversionStarted, WeightConversionCompleted, WeightConversionFailed |
| **任务管理** | 7 | UploadTaskCreated, UploadTaskPaused, UploadTaskResumed, UploadTaskCancelled, UploadTaskDeleted, ConvertTaskCreated, ConvertTaskDeleted |
| **分类管理** | 4 | CategoryCreated, CategoryDeleted, ModelTypeCreated, ModelTypeDeleted |
| **标签管理** | 4 | TagCreated, TagAddedToModel, TagRemovedFromModel, TagDeleted |
| **系统运维** | 2 | ExpiredLocksCleaned, LockRenewalPatternAnomalyDetected |

### 9.3 按事件类型分类

| 事件类型 | 数量 | 示例 |
|----------|------|------|
| **生命周期事件** | 20 | ModelCreated, VersionCreated, CategoryCreated |
| **状态变更事件** | 3 | VersionStatusChanged, ResourceGroupVisibilityChanged |
| **业务流程事件** | 18 | WeightUploadCompleted, WeightValidationCompleted, TrainingWeightArchived |
| **异常事件** | 4 | WeightUploadFailed, WeightTypeRecognitionFailed, WeightConversionFailed |
| **时间触发事件** | 4 | VersionLockExpired, VersionLockExpiringSoon, ExpiredLocksCleaned, LockRenewalPatternAnomalyDetected |

---

## 10. 关键业务流程的事件序列

### 10.1 权重上传流程事件序列

```
1. UploadTaskCreatedEvent (上传任务已创建)
   ↓
2. WeightUploadStartedEvent (权重上传已开始)
   ↓
3. [分支] WeightUploadCompletedEvent (权重上传已完成)
      ↓
      4a. WeightValidationStartedEvent (权重校验已开始)
          ↓
      5a. [并行] WeightValidationCompletedEvent (权重校验已完成)
               ↓
               6a. VersionStatusChangedEvent (版本状态: 上传中→可用)
          [并行] WeightTypeRecognizedEvent (权重类型已识别)
   [分支] WeightUploadFailedEvent (权重上传已失败)
          ↓
          4b. VersionStatusChangedEvent (版本状态: 上传中→上传失败)
```

### 10.2 权重纳管流程事件序列

```
1. VersionCreatedEvent (版本已创建)
   ↓
2. WeightRegisteredEvent (权重已纳管)
   ↓
3. WeightValidationStartedEvent (权重校验已开始)
   ↓
4. [并行] WeightValidationCompletedEvent (权重校验已完成)
            ↓
            5a. VersionStatusChangedEvent (版本状态: 无权重→可用)
   [并行] WeightTypeRecognizedEvent (权重类型已识别)
```

### 10.3 训练权重归档流程事件序列

```
1. VersionCreatedEvent (版本已创建 - 归档版本)
   ↓
2. TrainingWeightArchivedEvent (训练权重已归档)
   ↓
3. WeightValidationStartedEvent (权重校验已开始)
   ↓
4. [并行] WeightValidationCompletedEvent (权重校验已完成)
            ↓
            5a. VersionStatusChangedEvent (版本状态: 无权重→可用)
   [并行] WeightTypeRecognizedEvent (权重类型已识别)
```

### 10.4 权重格式转换流程事件序列

```
1. ConvertTaskCreatedEvent (转换任务已创建)
   ↓
2. VersionLockedEvent (源版本已锁定)
   ↓
3. VersionCreatedEvent (目标版本已创建)
   ↓
4. WeightConversionStartedEvent (权重转换已开始)
   ↓
5. [分支] WeightConversionCompletedEvent (权重转换已完成)
      ↓
      6a. VersionUnlockedEvent (源版本已解锁)
          ↓
      7a. WeightValidationStartedEvent (目标版本校验开始)
          ↓
      8a. VersionStatusChangedEvent (目标版本状态: 无权重→可用)
   [分支] WeightConversionFailedEvent (权重转换已失败)
          ↓
          6b. VersionUnlockedEvent (源版本已解锁)
              ↓
          7b. VersionStatusChangedEvent (目标版本状态: 无权重→异常)
```

### 10.5 模型删除流程事件序列

```
1. [检查] VersionUnlockedEvent (确保无锁定)
   ↓
2. [循环] VersionSoftDeletedEvent (每个版本已软删除)
   ↓
3. ModelSoftDeletedEvent (模型已软删除)
   ↓
4. ModelMovedToRecycleBinEvent (模型已移至回收站)
```

### 10.6 版本锁生命周期事件序列

```
1. VersionLockedEvent (版本已锁定)
   ↓
2. [循环] VersionLockRenewedEvent (版本锁已续约) -- 任务方定期续约
   ↓
3. [分支] VersionUnlockedEvent (版本已解锁) -- 任务正常完成，主动解锁
      ↓
      4a. 锁正常释放，版本可删除
   [分支] VersionLockExpiredEvent (版本锁已过期) -- 任务异常，未续约也未解锁
      ↓
      4b. ExpiredLocksCleanedEvent (过期锁已清理) -- Leader巡检清理
      ↓
      5b. 锁强制释放，版本可删除
```

### 10.7 锁续约预警流程事件序列

```
1. VersionLockedEvent (版本已锁定)
   ↓
2. [任务运行中...]
   ↓
3. VersionLockExpiringSoonEvent (版本锁即将过期) -- 剩余时间 < 1小时
   ↓
4. [分支] VersionLockRenewedEvent (版本锁已续约) -- 任务方收到预警后续约
      ↓
      5a. 锁有效期延长，继续监控
   [分支] VersionLockExpiredEvent (版本锁已过期) -- 任务方未续约
      ↓
      5b. ExpiredLocksCleanedEvent (过期锁已清理)
      ↓
      6b. AlertHandler发送告警通知
```

---

## 11. 事件处理器映射

### 11.1 审计日志处理器 (AuditLogHandler)

**订阅事件**: 所有事件

**职责**:
- 将所有领域事件翻译为操作日志格式
- 上报到平台统一日志服务
- 记录操作者、操作时间、操作结果

### 11.2 版本状态处理器 (VersionStatusHandler)

**订阅事件**:
- WeightUploadCompletedEvent
- WeightUploadFailedEvent
- WeightValidationCompletedEvent
- WeightTypeRecognitionFailedEvent
- WeightConversionCompletedEvent
- WeightConversionFailedEvent
- UploadTaskCancelledEvent

**职责**:
- 根据业务事件更新版本状态
- 状态流转控制

### 11.3 权重校验处理器 (WeightValidationHandler)

**订阅事件**:
- WeightRegisteredEvent
- WeightUploadCompletedEvent
- TrainingWeightArchivedEvent
- WeightConversionCompletedEvent

**职责**:
- 触发权重完整性校验
- 触发权重类型识别

### 11.4 权重类型识别处理器 (WeightTypeRecognitionHandler)

**订阅事件**:
- WeightRegisteredEvent
- WeightUploadCompletedEvent
- TrainingWeightArchivedEvent

**职责**:
- 解析 config.json
- 识别权重数据类型

### 11.5 版本解锁处理器 (VersionUnlockHandler)

**订阅事件**:
- WeightConversionCompletedEvent
- WeightConversionFailedEvent

**职责**:
- 解锁源版本
- 更新版本锁定状态

### 11.6 搜索索引处理器 (SearchIndexHandler)

**订阅事件**:
- ModelCreatedEvent
- ModelModifiedEvent
- ModelRestoredEvent
- ModelHardDeletedEvent
- CategoryCreatedEvent
- CategoryDeletedEvent
- ModelTypeCreatedEvent
- ModelTypeDeletedEvent
- TagAddedToModelEvent
- TagRemovedFromModelEvent

**职责**:
- 更新搜索引擎索引
- 维护搜索数据一致性

### 11.7 告警处理器 (AlertHandler)

**订阅事件**:
- WeightUploadFailedEvent
- WeightTypeRecognitionFailedEvent
- WeightConversionFailedEvent
- VersionLockExpiredEvent
- VersionLockRenewalFailedEvent
- VersionLockExpiringSoonEvent

**职责**:
- 发送告警通知
- 记录异常事件
- 通知任务方锁即将过期或已过期

### 11.8 文件清理处理器 (FileCleanupHandler)

**订阅事件**:
- ModelHardDeletedEvent
- VersionHardDeletedEvent

**职责**:
- 删除存储上的权重文件
- 清理残留数据

### 11.9 回收站处理器 (RecycleBinHandler)

**订阅事件**:
- ModelSoftDeletedEvent
- ModelRestoredEvent
- VersionSoftDeletedEvent
- VersionRestoredEvent
- ModelMovedToRecycleBinEvent
- VersionMovedToRecycleBinEvent

**职责**:
- 维护回收站数据
- 支持回收站查询

### 11.10 任务监控处理器 (TaskMonitoringHandler)

**订阅事件**:
- WeightUploadStartedEvent
- UploadTaskPausedEvent
- UploadTaskResumedEvent

**职责**:
- 监控任务执行状态
- 记录任务进度

### 11.11 锁监控处理器 (LockMonitoringHandler)

**订阅事件**:
- VersionLockedEvent
- VersionUnlockedEvent
- VersionLockRenewedEvent
- VersionLockRenewalFailedEvent
- VersionLockExpiredEvent
- VersionLockExpiringSoonEvent

**职责**:
- 监控锁的生命周期
- 统计锁的使用情况
- 分析锁过期原因
- 生成锁使用报告

---

## 12. 事件存储与追溯

### 12.1 事件存储策略

| 存储方式 | 适用场景 | 保留策略 |
|----------|----------|----------|
| **数据库** | 需要长期审计的事件 | 永久保留 |
| **日志文件** | 所有事件 | 轮转保留 90 天 |
| **消息队列** | 实时分发（如需要） | 不存储 |

### 12.2 事件追溯能力

通过领域事件可以实现：
- **操作审计**: 完整的操作历史记录
- **状态重建**: 通过重放事件重建对象状态（事件溯源）
- **故障排查**: 追踪问题发生的完整链路
- **业务分析**: 统计操作频率、失败率等

---

## 13. 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-04-21 | 初始版本，基于需求规格说明书 v1.2 进行事件风暴，识别 48 个领域事件 | Prometheus |
| v1.1 | 2026-04-21 | 补充锁续约相关事件：VersionLockRenewedEvent、VersionLockRenewalFailedEvent、VersionLockExpiringSoonEvent、LockRenewalPatternAnomalyDetectedEvent；新增锁监控处理器；更新事件总数为52个；补充锁生命周期和续约预警流程的事件序列 | Prometheus |
| v1.2 | 2026-04-21 | 统一术语：将所有"上传/Upload"相关字眼统一替换为"上传/Upload"，包括WeightUploadService→WeightUploadService、WeightUploadHandler→WeightUploadHandler、uploadType→uploadType、触发权重上传流程→触发权重上传流程 | Prometheus |

---

## 14. 参考文档

- [ModelLite-模型仓库-需求规格说明书-v1.2.md](/docs/ModelLite-模型仓库-需求规格说明书-v1.2.md)
- [ModelLite-模型仓库-DDD架构设计-v1.0.md](/docs/architecture/ModelLite-模型仓库-DDD架构设计-v1.0.md)
- [ModelLite-Repository-DDD-Ubiquitous-Language.md](./ModelLite-Repository-DDD-Ubiquitous-Language.md)

---

**文档结束**
