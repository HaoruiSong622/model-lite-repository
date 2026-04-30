# ModelLite 模型仓库 - 领域事件风暴（Domain Event Storming）

> **文档类型**: DDD 领域事件清单
> **文档版本**: v1.5
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
- **触发时机**: 创建模型及第一个版本成功后
- **发布方**: Model Aggregate
- **业务价值**: 记录操作日志；触发首个版本的初始化流程

#### ModelModifiedEvent - 模型已修改
- **触发时机**: 修改模型元数据（描述、分类、类型、标签）后
- **发布方**: Model Aggregate
- **业务价值**: 记录变更历史；触发关联更新（如分类变更后的索引更新）

#### ModelSoftDeletedEvent - 模型已软删除
- **触发时机**: 软删除模型后（级联删除所有版本）
- **发布方**: Model Aggregate
- **业务价值**: 记录删除操作；触发回收站管理；清理相关资源

#### ModelHardDeletedEvent - 模型已硬删除
- **触发时机**: 硬删除模型后（从回收站彻底删除）
- **发布方**: Model Aggregate
- **业务价值**: 彻底清理存储资源；记录审计日志

#### ModelRestoredEvent - 模型已恢复
- **触发时机**: 从回收站恢复模型后
- **发布方**: Model Aggregate
- **业务价值**: 恢复业务数据；记录恢复操作

### 2.2 版本生命周期事件

#### VersionCreatedEvent - 版本已创建
- **触发时机**: 创建新版本后
- **发布方**: Model Aggregate (通过 ModelVersion 实体)
- **业务价值**: 触发权重上传流程；记录版本创建

#### VersionStatusChangedEvent - 版本状态已变更
- **触发时机**: 版本状态发生任何变更后
- **发布方**: ModelVersion Entity
- **业务价值**: 状态机审计；触发状态相关的后续操作

#### VersionSoftDeletedEvent - 版本已软删除
- **触发时机**: 软删除特定版本后
- **发布方**: Model Aggregate
- **业务价值**: 记录版本删除；触发版本级回收站管理

#### VersionHardDeletedEvent - 版本已硬删除
- **触发时机**: 硬删除特定版本后
- **发布方**: Model Aggregate
- **业务价值**: 清理存储资源；记录彻底删除操作

#### VersionRestoredEvent - 版本已恢复
- **触发时机**: 从回收站恢复特定版本后
- **发布方**: Model Aggregate
- **业务价值**: 恢复版本数据；记录恢复操作

### 2.3 版本锁定事件

#### VersionLockedEvent - 版本已锁定
- **触发时机**: 训推任务成功锁定版本后
- **发布方**: Model Aggregate (通过 ModelVersion 实体)
- **业务价值**: 记录锁定操作；防止版本被删除

#### VersionUnlockedEvent - 版本已解锁
- **触发时机**: 训推任务解锁版本后
- **发布方**: Model Aggregate
- **业务价值**: 记录解锁操作；当剩余锁为 0 时版本可删除

#### VersionLockRenewedEvent - 版本锁已续约
- **触发时机**: 锁持有者成功续约后
- **发布方**: VersionLockService (领域服务)
- **业务价值**: 记录锁续约操作；追踪长时间运行的任务

#### VersionLockRenewalFailedEvent - 版本锁续约已失败
- **触发时机**: 锁续约失败时（锁已过期、身份验证失败等）
- **发布方**: VersionLockService (领域服务)
- **业务价值**: 记录续约失败；触发告警（任务可能因锁过期而被中断）

#### VersionLockExpiredEvent - 版本锁已过期
- **触发时机**: Leader 节点巡检发现锁已过期并清理后
- **发布方**: VersionLockService (领域服务)
- **业务价值**: 防止僵尸锁；记录异常解锁

#### VersionLockExpiringSoonEvent - 版本锁即将过期
- **触发时机**: Leader 节点巡检发现锁即将过期（如剩余时间 < 1 小时）
- **发布方**: VersionLockService (领域服务)
- **业务价值**: 提前预警，给任务方预留续约时间

---

## 3. 权重管理领域事件

### 3.1 权重上传事件

#### WeightRegisteredEvent - 权重已纳管
- **触发时机**: 纳管操作完成后（仅记录路径，不复制文件）
- **发布方**: WeightUploadService (领域服务)
- **业务价值**: 触发权重校验

#### WeightUploadStartedEvent - 权重上传已开始
- **触发时机**: 上传任务创建并启动后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录上传开始；版本状态变更为上传中

#### WeightUploadCompletedEvent - 权重上传已完成
- **触发时机**: 上传任务成功完成后
- **发布方**: UploadTask Aggregate
- **业务价值**: 触发权重校验和类型识别；更新版本状态为可用

#### WeightUploadFailedEvent - 权重上传已失败
- **触发时机**: 上传任务失败后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录失败原因；更新版本状态为上传失败；触发告警

### 3.2 权重校验事件

#### WeightValidationStartedEvent - 权重校验已开始
- **触发时机**: 触发权重完整性校验后
- **发布方**: WeightValidationService
- **业务价值**: 记录校验开始

#### WeightValidationCompletedEvent - 权重校验已完成
- **触发时机**: 权重完整性校验完成后
- **发布方**: WeightValidationService
- **业务价值**: 更新版本校验状态；如失败则标记版本为校验失败

#### WeightTypeRecognizedEvent - 权重类型已识别
- **触发时机**: 权重类型识别完成后
- **发布方**: WeightTypeRecognitionService
- **业务价值**: 更新版本权重类型；展示给用户

#### WeightTypeRecognitionFailedEvent - 权重类型识别已失败
- **触发时机**: 权重类型识别失败时（config.json 不存在或格式异常）
- **发布方**: WeightTypeRecognitionService
- **业务价值**: 记录识别失败；标记版本为异常状态

### 3.3 权重归档事件

#### TrainingWeightArchivedEvent - 训练权重已归档
- **触发时机**: 训练模块归档权重完成
- **发布方**: TrainingArchiveService
- **业务价值**: 记录训练产出；触发权重校验

### 3.4 权重转换事件

#### WeightConversionStartedEvent - 权重转换已开始
- **触发时机**: 转换任务创建并启动后
- **发布方**: ConvertTask Aggregate
- **业务价值**: 记录转换开始；源版本已自动锁定

#### WeightConversionCompletedEvent - 权重转换已完成
- **触发时机**: 转换任务成功完成后
- **发布方**: ConvertTask Aggregate
- **业务价值**: 解锁源版本；更新目标版本状态为可用；触发目标版本校验

#### WeightConversionFailedEvent - 权重转换已失败
- **触发时机**: 转换任务失败后
- **发布方**: ConvertTask Aggregate
- **业务价值**: 解锁源版本；记录失败原因

---

## 4. 任务管理领域事件

### 4.1 上传任务事件

#### UploadTaskCreatedEvent - 上传任务已创建
- **触发时机**: 上传任务创建后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录任务创建

#### UploadTaskPausedEvent - 上传任务已暂停
- **触发时机**: 用户暂停上传任务后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录暂停操作

#### UploadTaskResumedEvent - 上传任务已恢复
- **触发时机**: 用户恢复上传任务后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录恢复操作

#### UploadTaskCancelledEvent - 上传任务已取消
- **触发时机**: 用户取消上传任务后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录取消操作；触发版本状态更新

#### UploadTaskDeletedEvent - 上传任务已删除
- **触发时机**: 删除上传任务记录后
- **发布方**: UploadTask Aggregate
- **业务价值**: 记录任务删除；清理任务相关资源

### 4.2 转换任务事件

#### ConvertTaskCreatedEvent - 转换任务已创建
- **触发时机**: 转换任务创建后
- **发布方**: ConvertTask Aggregate
- **业务价值**: 记录任务创建

#### ConvertTaskDeletedEvent - 转换任务已删除
- **触发时机**: 删除转换任务记录后
- **发布方**: ConvertTask Aggregate
- **业务价值**: 记录任务删除

---

## 5. 分类体系领域事件

### 5.1 分类管理事件

#### CategoryCreatedEvent - 分类已创建
- **触发时机**: 创建新分类后
- **发布方**: Category Aggregate
- **业务价值**: 记录分类创建

#### CategoryDeletedEvent - 分类已删除
- **触发时机**: 删除分类后（确保无模型引用）
- **发布方**: Category Aggregate
- **业务价值**: 记录分类删除

### 5.2 类型管理事件

#### ModelTypeCreatedEvent - 模型类型已创建
- **触发时机**: 在分类下创建新类型后
- **发布方**: Category Aggregate
- **业务价值**: 记录类型创建
- **备注**: 模型类型的能力（如 supportFinetune）通过 Tag 聚合关联表达

#### ModelTypeDeletedEvent - 模型类型已删除
- **触发时机**: 删除类型后（确保无模型引用）
- **发布方**: Category Aggregate
- **业务价值**: 记录类型删除

---

## 6. 标签管理领域事件

> Tag 已提升为独立聚合根，同时服务两种关联场景：
> - 与 Model 关联：用户自定义标签，用于模型组织（REQ-TAG-001）
> - 与 ModelType 关联：能力标签（如 supportFinetune），描述模型类型的能力

#### TagCreatedEvent - 标签已创建
- **触发时机**: 创建新标签后
- **发布方**: Tag Aggregate
- **业务价值**: 记录标签创建；区分用户标签和能力标签

#### TagDeletedEvent - 标签已删除
- **触发时机**: 删除用户自定义标签后（内置标签不允许删除）
- **发布方**: Tag Aggregate
- **业务价值**: 记录标签删除
- **约束**: 内置标签（isBuiltIn=true）不允许删除

#### TagAddedToModelEvent - 标签已添加到模型
- **触发时机**: 为模型添加标签后
- **发布方**: Model Aggregate
- **业务价值**: 记录标签关联

#### TagRemovedFromModelEvent - 标签已从模型移除
- **触发时机**: 从模型移除标签后
- **发布方**: Model Aggregate
- **业务价值**: 记录标签解除关联

#### TagAddedToModelTypeEvent - 标签已添加到模型类型
- **触发时机**: 为模型类型添加能力标签后
- **发布方**: Tag Aggregate
- **业务价值**: 记录模型类型的能力标签关联；替代原 ModelType.supportFinetune 字段，支持更灵活的能力扩展

#### TagRemovedFromModelTypeEvent - 标签已从模型类型移除
- **触发时机**: 从模型类型移除能力标签后
- **发布方**: Tag Aggregate
- **业务价值**: 记录模型类型能力标签的解除关联

---

## 7. 回收站管理领域事件

#### ModelMovedToRecycleBinEvent - 模型已移至回收站
- **触发时机**: 模型软删除后
- **发布方**: Model Aggregate
- **业务价值**: 记录回收站操作

#### VersionMovedToRecycleBinEvent - 版本已移至回收站
- **触发时机**: 版本软删除后
- **发布方**: Model Aggregate
- **业务价值**: 记录回收站操作

---

## 8. 系统级领域事件

#### ExpiredLocksCleanedEvent - 过期锁已清理
- **触发时机**: Leader 节点巡检清理过期锁后
- **发布方**: VersionLockService
- **业务价值**: 记录僵尸锁清理；发现异常（调用方未正常解锁或续约）

#### LockRenewalPatternAnomalyDetectedEvent - 锁续约模式异常已检测到
- **触发时机**: 系统检测到异常的锁续约模式时
- **发布方**: LockMonitoringHandler
- **业务价值**: 发现潜在的任务异常；提前预警可能的锁过期风险

#### ResourceGroupVisibilityChangedEvent - 资源组可见性已变更
- **触发时机**: 模型资源组变更后（如未来支持）
- **发布方**: Model Aggregate
- **业务价值**: 记录权限变更

---

## 9. 事件总览矩阵

### 9.1 按业务领域分类

| 业务领域 | 事件数量 | 事件列表 |
|----------|----------|----------|
| **模型生命周期** | 7 | ModelCreated, ModelModified, ModelSoftDeleted, ModelHardDeleted, ModelRestored, ModelMovedToRecycleBin, ResourceGroupVisibilityChanged |
| **版本生命周期** | 7 | VersionCreated, VersionStatusChanged, VersionSoftDeleted, VersionHardDeleted, VersionRestored, VersionMovedToRecycleBin |
| **版本锁定** | 6 | VersionLocked, VersionUnlocked, VersionLockRenewed, VersionLockRenewalFailed, VersionLockExpired, VersionLockExpiringSoon |
| **权重管理** | 9 | WeightRegistered, WeightUploadStarted, WeightUploadCompleted, WeightUploadFailed, WeightValidationStarted, WeightValidationCompleted, WeightTypeRecognized, WeightTypeRecognitionFailed, TrainingWeightArchived |
| **权重转换** | 3 | WeightConversionStarted, WeightConversionCompleted, WeightConversionFailed |
| **任务管理** | 7 | UploadTaskCreated, UploadTaskPaused, UploadTaskResumed, UploadTaskCancelled, UploadTaskDeleted, ConvertTaskCreated, ConvertTaskDeleted |
| **分类管理** | 4 | CategoryCreated, CategoryDeleted, ModelTypeCreated, ModelTypeDeleted |
| **标签管理** | 6 | TagCreated, TagDeleted, TagAddedToModel, TagRemovedFromModel, TagAddedToModelType, TagRemovedFromModelType |
| **系统运维** | 2 | ExpiredLocksCleaned, LockRenewalPatternAnomalyDetected |

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

## 11. 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-04-21 | 初始版本，基于需求规格说明书 v1.2 进行事件风暴，识别 48 个领域事件 | Prometheus |
| v1.1 | 2026-04-21 | 补充锁续约相关事件；新增锁监控处理器；更新事件总数为52个；补充锁生命周期和续约预警流程的事件序列 | Prometheus |
| v1.2 | 2026-04-21 | 统一术语：将所有"上传/Upload"相关字眼统一替换 | Prometheus |
| v1.3 | 2026-04-22 | Tag 提升为独立聚合，新增 TagAddedToModelTypeEvent 等事件；总事件数增至 54 | Prometheus |
| v1.4 | 2026-04-22 | 删除"消费方"字段和事件处理器映射章节 | Prometheus |
| v1.5 | 2026-04-24 | 文档精简：1. 事件定义从完整 YAML 压缩为一行描述（移除逐字段列表）<br>2. 事件总览矩阵只保留按业务领域分类维度<br>3. 删除事件存储与追溯章节 | Prometheus |

---

## 12. 参考文档

- [ModelLite-模型仓库-需求规格说明书-v1.2.md](/docs/ModelLite-模型仓库-需求规格说明书-v1.2.md)
- [ModelLite-Repository-DDD-Ubiquitous-Language.md](ModelLite-Repository-DDD-Ubiquitous-Language.md)

---

**文档结束**
