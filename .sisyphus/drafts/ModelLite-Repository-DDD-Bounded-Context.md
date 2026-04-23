# ModelLite 模型仓库 - 限界上下文设计（Bounded Context Design）

> **文档类型**: DDD 限界上下文设计
> **文档版本**: v1.0
> **编写日期**: 2026-04-22
> **适用范围**: ModelLite 平台模型仓库模块 DDD 架构设计
> **目标读者**: 架构师、领域专家、后端开发工程师

---

## 1. 概述

### 1.1 设计目标

本文档定义模型仓库模块的限界上下文（Bounded Context），明确每个上下文内的统一语言边界、聚合设计、领域服务、仓储接口，以及上下文之间的映射关系和协作方式。

### 1.2 子域与限界上下文的映射

子域是业务视角的划分，限界上下文是代码组织视角的划分。模型仓库采用以下映射：

| 子域 | 限界上下文 | 映射关系 |
|------|-----------|----------|
| 模型权重子域 ★ Core | 模型权重上下文 (ModelWeight) | 1:1 |
| 版本锁子域 ◐ Supporting | 模型权重上下文 (ModelWeight) | 合并入模型权重上下文 |
| 权重任务子域 ★ Core | 权重任务上下文 (WeightTask) | 1:1 |
| 基础设施层 | 非上下文（技术层） | — |

> **设计决策**: 版本锁子域虽然是独立子域，但在限界上下文层面归入模型权重上下文。理由：VersionLock 与 ModelVersion 关系紧密（锁保护的是版本），且在同一代码包中更便于维护 isLocked 反规范化字段的一致性。

### 1.3 上下文总览

```mermaid
graph TB
    subgraph "模型仓库服务（单一部署单元）"
        subgraph MWC["模型权重上下文<br/>ModelWeight Context"]
            MA["Model 聚合"]
            CA["Category 聚合"]
            TA["Tag 聚合"]
            VLA["VersionLock 聚合"]
        end

        subgraph WTC["权重任务上下文<br/>WeightTask Context"]
            UTA["UploadTask 聚合"]
            CTA["ConvertTask 聚合"]
            WVS["WeightValidationService"]
            WRS["WeightTypeRecognitionService"]
        end

        subgraph INF["基础设施层"]
            TS["TaskScheduler"]
            IM["InformerManager"]
            LE["LeaderElection"]
        end
    end

    subgraph EXT["外部模块（独立服务）"]
        IS["推理服务"]
        TS_EXT["训练服务"]
        ES["评测服务"]
        DS["开发服务"]
    end

    WTC -->|"创建版本/更新状态"| MWC
    WTC -->|"转换时锁定/解锁源版本"| VLA
    WTC -->|"异步任务执行"| INF

    EXT -->|"M2M: 查询路径/锁定/解锁"| MWC
    EXT -->|"M2M: 归档"| WTC
```

---

## 2. 模型权重上下文（ModelWeight Context）

### 2.1 统一语言范围

此上下文中，语言围绕**模型实体、版本、分类体系、标签、版本锁**展开：

| 术语 | 含义 |
|------|------|
| Model | 顶层实体，模型的元数据集合 |
| ModelVersion | 模型的子实体，某一版本的权重信息 |
| Category | 一级分类，如 TextGeneration |
| ModelType | 二级分类，如 glm-5 |
| Tag | 标签（用户标签 + 能力标签），独立聚合 |
| VersionLock | 版本锁，保护版本不被删除 |
| LockOwner | 锁持有者（任务 ID + 任务类型） |
| VersionStatus | 版本状态（NoWeight/Uploading/Available/UploadFailed/ValidationFailed/Error） |
| RecycleBin | 回收站，软删除后的暂存区 |
| isLocked | 反规范化字段，由 version_lock 表驱动更新 |

### 2.2 聚合设计

#### 2.2.1 Model 聚合（核心聚合）

```mermaid
classDiagram
    class Model {
        +UUID modelId
        +ModelName name
        +String description
        +ResourceGroup resourceGroup
        +UUID categoryId
        +UUID typeId
        +String createUser
        +String author
        +Boolean deleted
        +DateTime createTime
        +DateTime updateTime
        +createModel(firstVersion) Model
        +modifyMetadata(fields) void
        +createVersion(weightSource) ModelVersion
        +updateVersionStatus(versionId, status) void
        +softDelete() void
        +hardDelete() void
        +restore() void
    }

    class ModelVersion {
        +UUID versionId
        +VersionNumber versionNumber
        +StoragePath storagePath
        +WeightType weightType
        +Boolean isRegistered
        +VersionStatus status
        +Boolean isLocked
        +TrainingMetadata trainingMetadata
        +Boolean deleted
        +DateTime createTime
        +DateTime updateTime
    }

    class ModelName {
        <<Value Object>>
        +String value
    }

    class VersionNumber {
        <<Value Object>>
        +Integer value
    }

    class StoragePath {
        <<Value Object>>
        +String pvcName
        +String internalPath
    }

    class VersionStatus {
        <<Value Object>>
        +NO_WEIGHT
        +UPLOADING
        +AVAILABLE
        +UPLOAD_FAILED
        +VALIDATION_FAILED
        +ERROR
    }

    class ResourceGroup {
        <<Value Object>>
        +String value
    }

    class TrainingMetadata {
        <<Value Object>>
        +String trainFrame
        +String trainType
        +String trainStrategy
        +Long trainTime
        +String finalLoss
        +String sourceVersion
    }

    Model "1" *-- "many" ModelVersion : contains
    Model --> ModelName
    Model --> ResourceGroup
    ModelVersion --> VersionNumber
    ModelVersion --> StoragePath
    ModelVersion --> VersionStatus
    ModelVersion --> TrainingMetadata
```

**Model 聚合根字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| modelId | UUID | 模型唯一标识 |
| name | ModelName（值对象） | 模型名称，长度 1-255，创建后不可修改 |
| description | String | 模型描述，长度 0-2000 |
| resourceGroup | ResourceGroup（值对象） | 资源组标识，创建后不可修改（预留扩展） |
| categoryId | UUID | 引用 Category 聚合，模型所属一级分类 |
| typeId | UUID | 引用 ModelType 实体，模型所属二级分类 |
| createUser | String | 创建者 |
| author | String | 模型作者 |
| deleted | Boolean | 软删除标记 |
| createTime / updateTime | DateTime | 时间戳 |

**ModelVersion 内部实体字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | UUID | 版本唯一标识 |
| versionNumber | VersionNumber（值对象） | 版本号，自动递增整数（V1, V2...） |
| storagePath | StoragePath（值对象） | 权重存储路径（PVC + 内部路径） |
| weightType | WeightType | 权重数据精度类型（FP16、w8a8 等，自动识别） |
| isRegistered | Boolean | 是否为纳管版本 |
| status | VersionStatus（值对象） | 版本状态 |
| isLocked | Boolean | 是否被锁定（反规范化，由 version_lock 表驱动） |
| trainingMetadata | TrainingMetadata（值对象） | 训练元数据（归档版本才有） |
| deleted | Boolean | 软删除标记 |

**isLocked 反规范化策略**：

ModelVersion.isLocked 是 version_lock 表的查询缓存，遵循以下规则：

| 操作 | isLocked 更新逻辑 | 事务保证 |
|------|-------------------|----------|
| 锁定 | INSERT version_lock → SET isLocked = TRUE | 同一事务 |
| 解锁 | DELETE version_lock → COUNT(剩余锁) → IF 0 THEN SET isLocked = FALSE | 同一事务 |
| 过期清理 | DELETE expired locks → 对每个受影响版本 COUNT → IF 0 THEN SET isLocked = FALSE | 同一事务 |

> **不变量**: isLocked 永远由 version_lock 表驱动更新，绝不独立修改。所有锁操作封装在 VersionLockService 中。

#### 2.2.2 Category 聚合

```mermaid
classDiagram
    class Category {
        +UUID categoryId
        +String name
        +String description
        +Boolean isBuiltIn
        +DateTime createTime
        +DateTime updateTime
        +addModelType(name, description) ModelType
        +removeModelType(typeId) void
    }

    class ModelType {
        +UUID typeId
        +String name
        +String description
        +Boolean isBuiltIn
        +DateTime createTime
        +DateTime updateTime
    }

    Category "1" *-- "many" ModelType : contains
```

**说明**：
- Category 和 ModelType 采用**真删除**（确保下无模型时才允许删除），无需 deleted 字段
- ModelType 的能力（如 supportFinetune）通过 Tag 聚合关联表达，不再作为 ModelType 的字段

#### 2.2.3 Tag 聚合

```mermaid
classDiagram
    class Tag {
        +UUID tagId
        +String name
        +TagType tagType
        +Boolean isBuiltIn
        +DateTime createTime
        +addToModel(modelId) void
        +removeFromModel(modelId) void
        +addToModelType(typeId) void
        +removeFromModelType(typeId) void
    }

    class TagType {
        <<enumeration>>
        USER
        CAPABILITY
    }

    class ModelTag {
        <<Value Object>>
        +UUID modelId
        +UUID tagId
        +DateTime createTime
    }

    class ModelTypeTag {
        <<Value Object>>
        +UUID typeId
        +UUID tagId
        +DateTime createTime
    }

    Tag --> TagType
    Tag ..> ModelTag : creates
    Tag ..> ModelTypeTag : creates
```

**Tag 聚合的双重关联场景**：

| 关联对象 | 值对象 | TagType | 说明 |
|----------|--------|---------|------|
| Model | ModelTag | USER | 用户自定义标签，用于模型组织（REQ-TAG-001） |
| ModelType | ModelTypeTag | CAPABILITY | 能力标签，描述模型类型的能力（替代 supportFinetune 字段） |

**内置 Tag 预设**：

| 内置 Tag 名称 | TagType | 说明 |
|---------------|---------|------|
| supportFinetune | CAPABILITY | 表示该模型类型支持微调训练 |
| （未来可扩展） | CAPABILITY | 如 supportQuantization、supportInference 等 |

**业务不变量**：
- isBuiltIn=true 的标签不允许删除
- 标签名称唯一

#### 2.2.4 VersionLock 聚合

```mermaid
classDiagram
    class VersionLock {
        +UUID lockId
        +UUID versionId
        +String lockerId
        +LockType lockType
        +DateTime expireTime
        +DateTime createTime
        +isExpired() Boolean
        +renew() void
    }

    class LockType {
        <<enumeration>>
        INFERENCE
        TRAINING
        EVALUATION
        DEVELOPMENT
    }

    class LockOwner {
        <<Value Object>>
        +String lockerId
        +LockType lockType
    }

    VersionLock --> LockType
    VersionLock --> LockOwner
```

**说明**：
- VersionLock 有独立的生命周期（锁定→续约→过期/解锁），但与 ModelVersion 关系紧密
- 同一版本支持多任务并发锁定（引用计数），仅当所有锁释放后才允许删除
- 锁有 TTL（默认 24 小时），超时自动失效

### 2.3 领域服务

| 领域服务 | 职责 | 跨聚合协调 |
|----------|------|-----------|
| VersionLockService | 锁定/解锁/续约的核心逻辑 | 协调 VersionLock 聚合与 Model 聚合的 isLocked 字段同步 |
| LockMonitoringService | 僵尸锁巡检、过期预警、续约模式异常检测 | 仅 Leader 节点执行 |

### 2.4 仓储接口

| 仓储 | 职责 |
|------|------|
| ModelRepository | Model 及 ModelVersion 的持久化（含版本状态更新、软删除管理） |
| CategoryRepository | Category 及 ModelType 的持久化 |
| TagRepository | Tag、ModelTag、ModelTypeTag 的持久化 |
| VersionLockRepository | VersionLock 的持久化（含过期锁查询） |

### 2.5 领域事件

| 事件 | 触发时机 |
|------|----------|
| ModelCreatedEvent | 创建模型及第一个版本成功后 |
| ModelModifiedEvent | 修改模型元数据后 |
| ModelSoftDeletedEvent | 软删除模型后 |
| ModelHardDeletedEvent | 硬删除模型后 |
| ModelRestoredEvent | 恢复模型后 |
| VersionCreatedEvent | 创建新版本后 |
| VersionStatusChangedEvent | 版本状态变更后 |
| VersionSoftDeletedEvent | 软删除版本后 |
| VersionHardDeletedEvent | 硬删除版本后 |
| VersionRestoredEvent | 恢复版本后 |
| CategoryCreatedEvent | 创建分类后 |
| CategoryDeletedEvent | 删除分类后 |
| ModelTypeCreatedEvent | 创建模型类型后 |
| ModelTypeDeletedEvent | 删除模型类型后 |
| TagCreatedEvent | 创建标签后 |
| TagDeletedEvent | 删除标签后（内置标签不允许删除） |
| TagAddedToModelEvent | 标签添加到模型后 |
| TagRemovedFromModelEvent | 标签从模型移除后 |
| TagAddedToModelTypeEvent | 标签添加到模型类型后 |
| TagRemovedFromModelTypeEvent | 标签从模型类型移除后 |
| VersionLockedEvent | 版本锁定后 |
| VersionUnlockedEvent | 版本解锁后 |
| VersionLockRenewedEvent | 锁续约后 |
| VersionLockRenewalFailedEvent | 锁续约失败后 |
| VersionLockExpiredEvent | 锁过期清理后 |
| VersionLockExpiringSoonEvent | 锁即将过期预警 |
| ExpiredLocksCleanedEvent | 过期锁批量清理后 |
| LockRenewalPatternAnomalyDetectedEvent | 续约模式异常检测 |
| ModelMovedToRecycleBinEvent | 模型移入回收站后 |
| VersionMovedToRecycleBinEvent | 版本移入回收站后 |

---

## 3. 权重任务上下文（WeightTask Context）

### 3.1 统一语言范围

此上下文中，语言围绕**任务、通道、校验**展开：

| 术语 | 含义 |
|------|------|
| UploadTask | 上传任务，跟踪从外部存储拷贝权重的异步过程 |
| ConvertTask | 转换任务，跟踪权重格式转换的异步过程 |
| SourcePath | 上传源路径（NFS/CIFS/PVC） |
| TaskStatus | 任务状态（Pending/Running/Paused/Completed/Failed/Cancelled） |
| Register | 纳管，注册外部路径不复制文件 |
| Archive | 归档，训练产出纳入版本管理 |
| WeightValidation | 权重完整性校验 |
| WeightTypeRecognition | 权重类型识别 |

### 3.2 聚合设计

#### 3.2.1 UploadTask 聚合

```mermaid
classDiagram
    class UploadTask {
        +UUID taskId
        +UUID modelId
        +UUID versionId
        +SourcePath sourcePath
        +String targetPath
        +Integer progress
        +TaskStatus status
        +String errorMessage
        +String createUser
        +Boolean deleted
        +DateTime createTime
        +DateTime updateTime
        +start() void
        +pause() void
        +resume() void
        +cancel() void
        +complete() void
        +fail(message) void
        +updateProgress(percent) void
    }

    class SourcePath {
        <<Value Object>>
        +SourceType sourceType
        +String path
        +CifsCredentials credentials
    }

    class SourceType {
        <<enumeration>>
        NFS
        CIFS
        PVC
    }

    class CifsCredentials {
        <<Value Object>>
        +String username
        +String password
    }

    class TaskStatus {
        <<Value Object>>
        +PENDING
        +RUNNING
        +PAUSED
        +COMPLETED
        +FAILED
        +CANCELLED
    }

    UploadTask --> SourcePath
    UploadTask --> TaskStatus
    SourcePath --> SourceType
    SourcePath --> CifsCredentials : optional
```

#### 3.2.2 ConvertTask 聚合

```mermaid
classDiagram
    class ConvertTask {
        +UUID taskId
        +UUID modelId
        +UUID sourceVersionId
        +UUID targetVersionId
        +String sourceFormat
        +String targetFormat
        +Integer progress
        +TaskStatus status
        +String errorMessage
        +String createUser
        +Boolean deleted
        +DateTime createTime
        +DateTime updateTime
        +start() void
        +complete() void
        +fail(message) void
    }

    class TaskStatus {
        <<Value Object>>
        +PENDING
        +RUNNING
        +COMPLETED
        +FAILED
    }

    ConvertTask --> TaskStatus
```

### 3.3 领域服务

| 领域服务 | 职责 |
|----------|------|
| WeightValidationService | 权重完整性校验：文件数量校验，未来可扩展 SHA256、脚本校验等 |
| WeightTypeRecognitionService | 权重类型识别：解析 config.json 识别数据精度类型（FP16、w8a8 等） |

### 3.4 仓储接口

| 仓储 | 职责 |
|------|------|
| UploadTaskRepository | UploadTask 的持久化 |
| ConvertTaskRepository | ConvertTask 的持久化 |

### 3.5 领域事件

| 事件 | 触发时机 |
|------|----------|
| WeightRegisteredEvent | 纳管操作完成后 |
| WeightUploadStartedEvent | 上传任务启动后 |
| WeightUploadCompletedEvent | 上传任务完成后 |
| WeightUploadFailedEvent | 上传任务失败后 |
| WeightValidationStartedEvent | 权重校验开始后 |
| WeightValidationCompletedEvent | 权重校验完成后 |
| WeightTypeRecognizedEvent | 权重类型识别完成后 |
| WeightTypeRecognitionFailedEvent | 权重类型识别失败后 |
| TrainingWeightArchivedEvent | 训练权重归档后 |
| WeightConversionStartedEvent | 转换任务启动后 |
| WeightConversionCompletedEvent | 转换任务完成后 |
| WeightConversionFailedEvent | 转换任务失败后 |
| UploadTaskCreatedEvent | 上传任务创建后 |
| UploadTaskPausedEvent | 上传任务暂停后 |
| UploadTaskResumedEvent | 上传任务恢复后 |
| UploadTaskCancelledEvent | 上传任务取消后 |
| UploadTaskDeletedEvent | 上传任务删除后 |
| ConvertTaskCreatedEvent | 转换任务创建后 |
| ConvertTaskDeletedEvent | 转换任务删除后 |

---

## 4. 上下文映射（Context Map）

### 4.1 上下文关系

```mermaid
graph LR
    subgraph "模型仓库服务"
        MWC["模型权重上下文<br/>ModelWeight"]
        WTC["权重任务上下文<br/>WeightTask"]
    end

    MWC <|.. WTC : "Customer-Supplier<br/>（客户-供应商）<br/>领域服务直接调用"
```

**映射模式**: **Customer-Supplier（客户-供应商）**

- **供应商 (Supplier)**: 模型权重上下文 — 提供版本创建、状态更新、锁管理等能力
- **客户 (Customer)**: 权重任务上下文 — 在权重导入流程中消费这些能力
- **协作方式**: 同一服务内，通过领域服务直接调用

### 4.2 协作接口

权重任务上下文调用模型权重上下文的以下能力：

| 协作场景 | 权重任务上下文调用 | 模型权重上下文提供 |
|----------|-------------------|-------------------|
| 纳管/上传/归档时创建新版本 | `ModelRepository.createVersion()` | 版本创建 |
| 上传/校验完成后更新版本状态 | `ModelRepository.updateVersionStatus()` | 状态更新 |
| 权重转换时锁定源版本 | `VersionLockService.lock()` | 版本锁定 |
| 转换完成/失败后解锁源版本 | `VersionLockService.unlock()` | 版本解锁 |

### 4.3 协作流程

#### 4.3.1 权重上传流程

```mermaid
sequenceDiagram
    participant User
    participant WT_CTX as 权重任务上下文
    participant MW_CTX as 模型权重上下文
    participant INF as 基础设施层

    User->>WT_CTX: 创建上传任务
    WT_CTX->>MW_CTX: 创建新版本 (status=NO_WEIGHT)
    MW_CTX-->>WT_CTX: 返回 versionId
    WT_CTX->>INF: 创建 K8s Job
    INF-->>WT_CTX: Job 状态回调
    WT_CTX->>MW_CTX: 更新版本状态 (UPLOADING)
    alt 上传成功
        WT_CTX->>MW_CTX: 更新版本状态 (AVAILABLE)
        WT_CTX->>WT_CTX: 触发校验和类型识别
        WT_CTX->>MW_CTX: 更新权重类型
    else 上传失败
        WT_CTX->>MW_CTX: 更新版本状态 (UPLOAD_FAILED)
    end
```

#### 4.3.2 权重格式转换流程

```mermaid
sequenceDiagram
    participant User
    participant WT_CTX as 权重任务上下文
    participant MW_CTX as 模型权重上下文

    User->>WT_CTX: 创建转换任务
    WT_CTX->>MW_CTX: 锁定源版本 (VersionLockService.lock)
    WT_CTX->>MW_CTX: 创建目标版本 (status=NO_WEIGHT)
    WT_CTX->>WT_CTX: 启动转换 (K8s Job)
    alt 转换成功
        WT_CTX->>MW_CTX: 更新目标版本状态 (AVAILABLE)
        WT_CTX->>MW_CTX: 解锁源版本 (VersionLockService.unlock)
    else 转换失败
        WT_CTX->>MW_CTX: 更新目标版本状态 (ERROR)
        WT_CTX->>MW_CTX: 解锁源版本 (VersionLockService.unlock)
    end
```

---

## 5. 代码包结构

### 5.1 总体结构

```
com.huawei.modellite.repository
│
├── modelweight/                              # 模型权重上下文
│   ├── domain/
│   │   ├── aggregate/
│   │   │   ├── model/                        # Model 聚合
│   │   │   │   ├── Model.java                # 聚合根
│   │   │   │   ├── ModelVersion.java         # 内部实体
│   │   │   │   ├── ModelName.java            # 值对象
│   │   │   │   ├── VersionNumber.java        # 值对象
│   │   │   │   ├── StoragePath.java          # 值对象
│   │   │   │   ├── VersionStatus.java        # 值对象（枚举）
│   │   │   │   ├── ResourceGroup.java        # 值对象
│   │   │   │   └── TrainingMetadata.java     # 值对象
│   │   │   │
│   │   │   ├── category/                     # Category 聚合
│   │   │   │   ├── Category.java             # 聚合根
│   │   │   │   └── ModelType.java            # 内部实体
│   │   │   │
│   │   │   ├── tag/                          # Tag 聚合
│   │   │   │   ├── Tag.java                  # 聚合根
│   │   │   │   ├── TagType.java              # 枚举（USER/CAPABILITY）
│   │   │   │   ├── ModelTag.java             # 值对象（Model-Tag 关联）
│   │   │   │   └── ModelTypeTag.java         # 值对象（ModelType-Tag 关联）
│   │   │   │
│   │   │   └── versionlock/                  # VersionLock 聚合
│   │   │       ├── VersionLock.java          # 聚合根
│   │   │       ├── LockType.java             # 枚举
│   │   │       └── LockOwner.java            # 值对象
│   │   │
│   │   ├── service/
│   │   │   ├── VersionLockService.java       # 锁定/解锁/续约
│   │   │   └── LockMonitoringService.java    # 巡检/预警/异常检测
│   │   │
│   │   ├── repository/                       # 仓储接口（领域层定义）
│   │   │   ├── ModelRepository.java
│   │   │   ├── CategoryRepository.java
│   │   │   ├── TagRepository.java
│   │   │   └── VersionLockRepository.java
│   │   │
│   │   └── event/                            # 领域事件
│   │       ├── ModelCreatedEvent.java
│   │       ├── ModelModifiedEvent.java
│   │       ├── ModelSoftDeletedEvent.java
│   │       ├── ModelHardDeletedEvent.java
│   │       ├── ModelRestoredEvent.java
│   │       ├── VersionCreatedEvent.java
│   │       ├── VersionStatusChangedEvent.java
│   │       ├── VersionSoftDeletedEvent.java
│   │       ├── VersionHardDeletedEvent.java
│   │       ├── VersionRestoredEvent.java
│   │       ├── CategoryCreatedEvent.java
│   │       ├── CategoryDeletedEvent.java
│   │       ├── ModelTypeCreatedEvent.java
│   │       ├── ModelTypeDeletedEvent.java
│   │       ├── TagCreatedEvent.java
│   │       ├── TagDeletedEvent.java
│   │       ├── TagAddedToModelEvent.java
│   │       ├── TagRemovedFromModelEvent.java
│   │       ├── TagAddedToModelTypeEvent.java
│   │       ├── TagRemovedFromModelTypeEvent.java
│   │       ├── VersionLockedEvent.java
│   │       ├── VersionUnlockedEvent.java
│   │       ├── VersionLockRenewedEvent.java
│   │       ├── VersionLockRenewalFailedEvent.java
│   │       ├── VersionLockExpiredEvent.java
│   │       ├── VersionLockExpiringSoonEvent.java
│   │       ├── ExpiredLocksCleanedEvent.java
│   │       ├── LockRenewalPatternAnomalyDetectedEvent.java
│   │       ├── ModelMovedToRecycleBinEvent.java
│   │       └── VersionMovedToRecycleBinEvent.java
│   │
│   └── application/
│       ├── ModelApplicationService.java      # 模型 CRUD 应用服务
│       ├── CategoryApplicationService.java   # 分类管理应用服务
│       ├── TagApplicationService.java        # 标签管理应用服务
│       └── LockApplicationService.java       # 锁管理应用服务
│
├── weighttask/                               # 权重任务上下文
│   ├── domain/
│   │   ├── aggregate/
│   │   │   ├── uploadtask/                   # UploadTask 聚合
│   │   │   │   ├── UploadTask.java           # 聚合根
│   │   │   │   ├── SourcePath.java           # 值对象
│   │   │   │   ├── SourceType.java           # 枚举（NFS/CIFS/PVC）
│   │   │   │   ├── CifsCredentials.java      # 值对象（可选）
│   │   │   │   └── TaskStatus.java           # 值对象（枚举）
│   │   │   │
│   │   │   └── converttask/                  # ConvertTask 聚合
│   │   │       ├── ConvertTask.java          # 聚合根
│   │   │       └── TaskStatus.java           # 值对象（枚举）
│   │   │
│   │   ├── service/
│   │   │   ├── WeightValidationService.java  # 权重完整性校验
│   │   │   └── WeightTypeRecognitionService.java # 权重类型识别
│   │   │
│   │   ├── repository/                       # 仓储接口
│   │   │   ├── UploadTaskRepository.java
│   │   │   └── ConvertTaskRepository.java
│   │   │
│   │   └── event/                            # 领域事件
│   │       ├── WeightRegisteredEvent.java
│   │       ├── WeightUploadStartedEvent.java
│   │       ├── WeightUploadCompletedEvent.java
│   │       ├── WeightUploadFailedEvent.java
│   │       ├── WeightValidationStartedEvent.java
│   │       ├── WeightValidationCompletedEvent.java
│   │       ├── WeightTypeRecognizedEvent.java
│   │       ├── WeightTypeRecognitionFailedEvent.java
│   │       ├── TrainingWeightArchivedEvent.java
│   │       ├── WeightConversionStartedEvent.java
│   │       ├── WeightConversionCompletedEvent.java
│   │       ├── WeightConversionFailedEvent.java
│   │       ├── UploadTaskCreatedEvent.java
│   │       ├── UploadTaskPausedEvent.java
│   │       ├── UploadTaskResumedEvent.java
│   │       ├── UploadTaskCancelledEvent.java
│   │       ├── UploadTaskDeletedEvent.java
│   │       ├── ConvertTaskCreatedEvent.java
│   │       └── ConvertTaskDeletedEvent.java
│   │
│   └── application/
│       ├── UploadTaskApplicationService.java  # 上传任务应用服务
│       └── ConvertTaskApplicationService.java # 转换任务应用服务
│
├── infrastructure/                           # 基础设施层（跨上下文共享）
│   ├── persistence/                          # 仓储实现
│   │   ├── mapper/                           # MyBatis Mapper
│   │   │   ├── ModelMapper.java
│   │   │   ├── ModelVersionMapper.java
│   │   │   ├── CategoryMapper.java
│   │   │   ├── ModelTypeMapper.java
│   │   │   ├── TagMapper.java
│   │   │   ├── ModelTagMapper.java
│   │   │   ├── ModelTypeTagMapper.java
│   │   │   ├── VersionLockMapper.java
│   │   │   ├── UploadTaskMapper.java
│   │   │   └── ConvertTaskMapper.java
│   │   │
│   │   ├── MyBatisModelRepository.java
│   │   ├── MyBatisCategoryRepository.java
│   │   ├── MyBatisTagRepository.java
│   │   ├── MyBatisVersionLockRepository.java
│   │   ├── MyBatisUploadTaskRepository.java
│   │   └── MyBatisConvertTaskRepository.java
│   │
│   ├── taskscheduler/                        # 任务调度
│   │   ├── K8sJobScheduler.java              # K8s Job 创建/监控/删除
│   │   ├── InformerManager.java              # fabric8 Informer 状态同步
│   │   └── LeaderElectionManager.java        # Leader Election 写一致性
│   │
│   ├── config/                               # 配置
│   │   ├── DataSourceConfig.java
│   │   ├── K8sConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebConfig.java
│   │
│   ├── security/                             # 安全
│   │   ├── SslConfig.java
│   │   └── M2mAuthFilter.java
│   │
│   └── log/                                  # 日志
│       ├── AuditLogAnnotation.java
│       └── LogReporter.java
│
└── api/                                      # 接口层（跨上下文共享）
    ├── user/                                 # 人机接口
    │   ├── ModelApi.java                     # → ModelApplicationService
    │   ├── VersionApi.java                   # → ModelApplicationService
    │   ├── UploadTaskApi.java                # → UploadTaskApplicationService
    │   ├── ConvertTaskApi.java               # → ConvertTaskApplicationService
    │   ├── CategoryApi.java                  # → CategoryApplicationService
    │   ├── TagApi.java                       # → TagApplicationService
    │   └── RecycleBinApi.java                # → ModelApplicationService
    │
    └── m2m/                                  # 机机接口
        ├── PathQueryApi.java                 # → ModelApplicationService
        ├── ArchiveApi.java                   # → UploadTaskApplicationService
        └── LockApi.java                      # → LockApplicationService
```

### 5.2 依赖方向规则

```mermaid
graph TB
    API["接口层<br/>api/"]
    APP_MW["应用层<br/>modelweight/application/"]
    APP_WT["应用层<br/>weighttask/application/"]
    DOMAIN_MW["领域层<br/>modelweight/domain/"]
    DOMAIN_WT["领域层<br/>weighttask/domain/"]
    INFRA["基础设施层<br/>infrastructure/"]

    API --> APP_MW
    API --> APP_WT
    APP_MW --> DOMAIN_MW
    APP_WT --> DOMAIN_WT
    APP_WT -.->|"跨上下文调用<br/>（通过接口）"| DOMAIN_MW
    INFRA --> DOMAIN_MW
    INFRA --> DOMAIN_WT
```

**依赖规则**：

| 规则 | 说明 |
|------|------|
| 接口层 → 应用层 | API 只调用应用服务，不直接访问领域层 |
| 应用层 → 领域层 | 应用服务协调领域对象和仓储 |
| 权重任务应用层 → 模型权重领域层 | 跨上下文调用，通过领域服务直接调用 |
| 基础设施层 → 领域层 | 基础设施实现领域层定义的仓储接口 |
| 领域层 ←/→ 领域层 | 同一上下文内聚合间可互相引用；跨上下文通过应用层协调 |

---

## 6. 决策记录

| 决策编号 | 决策内容 | 决策理由 |
|----------|----------|----------|
| BC-01 | 采用 2 个限界上下文 | 模型权重和权重任务有不同的语言体系（实体 vs 任务），适合独立上下文；2 个上下文足够清晰又不至于过度拆分 |
| BC-02 | VersionLock 归入模型权重上下文 | VersionLock 与 ModelVersion 关系紧密，且 isLocked 反规范化字段需要跨聚合一致性维护，在同一上下文中更易管理 |
| BC-03 | 上下文间通过领域服务直接调用 | 模型仓库是单一服务，不需要事件机制；领域服务直接调用足够简单有效 |
| BC-04 | 采用包级别分离上下文 | Spring Boot 项目惯例；同一服务内包名区分上下文，既保持边界清晰又方便互相调用 |
| BC-05 | 接口层跨上下文共享 | 接口层很薄（只做参数校验和格式封装），按上下文分离增加文件数但不增加清晰度；通过注释标注每个 API 指向的应用服务 |
| BC-06 | isLocked 为反规范化字段 | 高频查询场景（删除前检查锁）的性能优化；一致性由 VersionLockService 在同一事务中保证 |

---

## 7. 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-04-22 | 初始版本，定义 2 个限界上下文（模型权重、权重任务），含聚合设计、上下文映射、协作流程、代码包结构 | Prometheus |

---

## 8. 参考文档

- [ModelLite-模型仓库-需求规格说明书-v1.2.md](/docs/ModelLite-模型仓库-需求规格说明书-v1.2.md)
- [ModelLite-Repository-DDD-Ubiquitous-Language.md](./ModelLite-Repository-DDD-Ubiquitous-Language.md)
- [ModelLite-Repository-DDD-Domain-Events.md](./ModelLite-Repository-DDD-Domain-Events.md)
- [ModelLite-Repository-DDD-Subdomain-Modeling.md](./ModelLite-Repository-DDD-Subdomain-Modeling.md)
- [ModelLite-模型仓库-架构设计-v1.1.md](/docs/architecture/ModelLite-模型仓库-架构设计-v1.1.md)

---

**文档结束**
