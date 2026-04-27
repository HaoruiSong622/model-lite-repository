# Task 23: Tech Design 代码规范检查报告

> **检查日期**: 2026-04-27
> **对照文档**: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md` v1.3
> **检查范围**: 全部 `src/main` Java 源码、SQL 脚本、MyBatis XML

---

## 检查总览

| 检查项 | 状态 | 严重级别 |
|--------|------|----------|
| 包结构符合 DDD 规范 | ✅ PASS | — |
| 类命名符合规范 | ❌ FAIL | 🔴 HIGH |
| 无跨聚合 @Transactional | ❌ FAIL | 🔴 HIGH |
| 无 javax.validation 注解在 DTO 上 | ✅ PASS | — |
| 无独立 DTO 转换器类 | ✅ PASS | — |
| SQL 脚本按 DB 类型分目录 | ⚠️ WARN | 🟡 MEDIUM |
| SourceType 未被修改 | ✅ PASS | — |
| 0102024 标记为废弃 | ✅ PASS | — |
| 错误码格式 0102YYY | ✅ PASS | — |
| MyBatis XML mapper 位置 | ✅ PASS | — |

**总结**: 10 项检查中 **2 项 FAIL**, **1 项 WARN**, **7 项 PASS**。

---

## 1. 包结构检查 ✅ PASS

### 期望（Tech Design §4.1）

```
com.huawei.modellite.repository
├── modelweight/domain/aggregate/{model|category|tag|versionlock}
├── modelweight/domain/repository/
├── modelweight/domain/service/
├── modelweight/application/
├── weighttask/domain/aggregate/{uploadtask|converttask}
├── weighttask/domain/repository/
├── weighttask/domain/service/
├── infrastructure/persistence/mapper/
├── api/user/
├── common/
```

### 实际

```
modelweight/domain/aggregate/category/    ✅ 存在，含 Category.java, ModelType.java
modelweight/domain/aggregate/model/       ✅ 存在，含 Model.java, ModelVersion.java, SourcePathType.java, StoragePath.java, TrainingMetadata.java
modelweight/domain/aggregate/tag/         ✅ 存在，含 Tag.java
modelweight/domain/aggregate/versionlock/ ✅ 存在（空，仅 .gitkeep）
modelweight/domain/repository/            ✅ 含 CategoryRepository, ModelRepository, TagRepository
modelweight/domain/service/               ✅ 含 ModelDomainService
modelweight/domain/vo/                    ✅ 含 ModelQueryCondition, PageResult
modelweight/application/dto/              ✅ 含 12 个 DTO 类
modelweight/application/service/          ✅ 含 ModelApplicationService, CategoryApplicationService, TagApplicationService
weighttask/domain/aggregate/uploadtask/   ✅ 存在（空）
weighttask/domain/aggregate/converttask/  ✅ 存在（空）
weighttask/domain/repository/             ✅ 存在（空）
weighttask/domain/service/                ✅ 存在（空）
infrastructure/persistence/mapper/        ✅ 含 CategoryMapper, ModelMapper, TagMapper
infrastructure/persistence/typehandler/   ✅ 含 7 个 TypeHandler
infrastructure/config/                    ✅ 含 DruidConfig, FileSuffixConfig, MyBatisConfig
api/user/                                 ✅ 含 CategoryApi, ModelApi, TagApi
api/m2m/                                  ✅ 存在（空）
common/converter/                         ✅ 存在（空，仅 .gitkeep）
common/dto/response/                      ✅ 含 BaseResponse
common/enums/                             ✅ 含 ErrorCode, LockType, SourceType, TagType, TaskStatus, VersionStatus
common/exception/                         ✅ 含 GlobalExceptionHandler, ModelLiteException
```

**结论**: 包结构与 Tech Design 规范完全一致。

---

## 2. 类命名检查 ❌ FAIL

### 期望（Tech Design §4.2）

| 类型 | 命名规范 | 期望示例 |
|------|----------|----------|
| 聚合根/实体 | 无后缀 | `Model.java`, `Category.java` |
| 值对象 | 无后缀 | `StoragePath.java` |
| 领域服务 | 无后缀 + Service | `ModelDomainService.java` |
| 应用服务 | 后缀 `ApplicationService` | `ModelApplicationService.java` |
| 仓储接口 | 后缀 `Repository` | `ModelRepository.java` |
| **仓储实现** | **前缀 `MyBatis` + 后缀 `Repository`** | **`MyBatisModelRepository.java`** |
| Mapper 接口 | 后缀 `Mapper` | `ModelMapper.java` |
| 请求 DTO | 后缀 `Request` | `CreateModelRequest.java` |
| 响应 DTO | 后缀 `Response` | `ModelDetailResponse.java` |
| API 控制器 | 后缀 `Api` | `ModelApi.java` |

### 实际 vs 期望

| 文件 | 实际命名 | 期望命名 | 状态 |
|------|----------|----------|------|
| `CategoryRepositoryImpl.java` | `CategoryRepositoryImpl` | `MyBatisCategoryRepository` | ❌ |
| `ModelRepositoryImpl.java` | `ModelRepositoryImpl` | `MyBatisModelRepository` | ❌ |
| `TagRepositoryImpl.java` | `TagRepositoryImpl` | `MyBatisTagRepository` | ❌ |

**其余类均符合规范**：
- 聚合根：`Model`, `Category`, `Tag`, `ModelVersion` ✅
- 值对象：`StoragePath`, `SourcePathType`, `TrainingMetadata`, `ModelQueryCondition`, `PageResult` ✅
- 领域服务：`ModelDomainService` ✅
- 应用服务：`ModelApplicationService`, `CategoryApplicationService`, `TagApplicationService` ✅
- 仓储接口：`ModelRepository`, `CategoryRepository`, `TagRepository` ✅
- Mapper：`ModelMapper`, `CategoryMapper`, `TagMapper` ✅
- DTO：`ModelCreateRequest`, `ModelModifyRequest`, `ModelResponse`, `VersionCreateRequest`, `VersionResponse` 等 ✅
- API：`ModelApi`, `CategoryApi`, `TagApi` ✅

**违规详情**: 仓储实现类使用 `Impl` 后缀，违反 Tech Design §4.2 规定的 `MyBatis` 前缀 + `Repository` 后缀命名。

---

## 3. @Transactional 跨聚合检查 ❌ FAIL

### 期望（Tech Design §3.2）

> 跨聚合操作不用 `@Transactional` 包裹整个流程。每个聚合操作独立事务。

### 实际发现

**`ModelApplicationService.java`** — 3 处 `@Transactional`：

| 行号 | 方法 | 涉及聚合 | 分析 |
|------|------|----------|------|
| L51 | `createModel()` | Model + Tag | ❌ 同一事务中调用 `modelRepository.save(model)` + `tagRepository.addModelTag()` — **跨聚合事务** |
| L99 | `modifyModel()` | Model + Tag | ❌ 同一事务中调用 `modelRepository.update(model)` + `tagRepository.removeModelTag()` / `tagRepository.addModelTag()` — **跨聚合事务** |
| L159 | `createVersion()` | Model (单聚合) | ⚠️ 单聚合操作，`@Transactional` 使用合理 |

**`ModelDomainService.java`** — 1 处 `@Transactional(readOnly = true)`：

| 行号 | 类级别 | 分析 |
|------|--------|------|
| L17 | `@Transactional(readOnly = true)` | ⚠️ 领域服务是只读查询验证，使用 `readOnly=true` 可以接受，但标注在类级别意味着所有方法都参与事务 |

**违规详情**:
1. `createModel()` 在同一事务中操作 Model 和 Tag 两个聚合 — 违反 Tech Design §3.2
2. `modifyModel()` 在同一事务中操作 Model 和 Tag 两个聚合 — 违反 Tech Design §3.2
3. `ModelDomainService` 类级别 `@Transactional(readOnly = true)` — 领域服务不应直接使用 `@Transactional`

---

## 4. javax.validation 注解检查 ✅ PASS

### 期望（Tech Design Inherited Wisdom）

> 不在 DTO 上使用 javax.validation 注解

### 实际

搜索 `@Valid`, `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Size`, `@Min`, `@Max`, `@Pattern` 以及 `import jakarta.` / `import javax.validation` 在 DTO 目录下：**无任何匹配**。

**结论**: DTO 类上无任何 javax.validation 注解，符合规范。

---

## 5. 独立 DTO 转换器类检查 ✅ PASS

### 期望（Tech Design Inherited Wisdom）

> 不创建独立 DTO 转换器类

### 实际

- `common/converter/` 目录：仅含 `.gitkeep`，无 Java 文件
- 全局搜索 `class.*Converter|class.*Convertor`：**无匹配**

DTO 转换逻辑通过 `ModelApplicationService` 中的私有方法实现（`toModelResponse()`, `toModelListResponse()`, `toVersionResponse()` 等），符合规范。

**结论**: 无独立 DTO 转换器类，符合规范。

---

## 6. SQL 脚本目录结构检查 ⚠️ WARN

### 期望（Tech Design §6.4）

```
src/main/resources/sql/
├── schema/
│   ├── postgresql/V1__*.sql, V2__*.sql
│   └── h2/V1__*.sql
├── data/
│   └── V1__builtin_data.sql   （文档示例为根目录）
```

### 实际

```
src/main/resources/sql/
├── schema/
│   ├── postgresql/
│   │   ├── V1__create_core_tables.sql         ✅
│   │   └── V2__add_model_version_storage_columns.sql  ✅
│   └── h2/
│       └── V1__create_core_tables.sql          ✅
├── data/
│   ├── V1__builtin_data.sql                    ⚠️ 根目录有一份
│   └── postgresql/
│       └── V1__builtin_data.sql                ✅ postgresql 子目录有一份
```

**问题**:
- `sql/data/V1__builtin_data.sql` 存在于根目录，与按 DB 类型分目录的策略不一致
- `sql/data/postgresql/V1__builtin_data.sql` 同时存在于 postgresql 子目录 — 正确

**说明**: Tech Design 文档中 `data/` 目录的示例确实为 `V1__builtin_data.sql`（根目录），但如果遵循按 DB 类型分目录的策略，应统一到子目录中。根目录的副本可能是冗余文件。

---

## 7. SourceType 未修改检查 ✅ PASS

### 期望

> SourceType 未被修改（保持原有定义）

### 实际

```bash
$ git diff HEAD~3 -- src/main/**/SourceType.java
(no output — 无变更)
```

SourceType.java 内容保持原始定义：
```java
public enum SourceType {
    NFS("NFS", "NFS 存储"),
    CIFS("CIFS", "CIFS 存储"),
    PVC("PVC", "PVC 存储");
}
```

**结论**: SourceType 未被修改，符合规范。

---

## 8. 0102024 废弃标记检查 ✅ PASS

### 期望

> 0102024 标记为废弃

### 实际（ErrorCode.java L43）

```java
// 0102024: DEPRECATED - 原标签被引用禁止删除，现改为级联删除
```

**结论**: 0102024 已标记为 DEPRECATED，且无对应常量定义（正确跳过），符合规范。

---

## 9. 错误码格式检查 ✅ PASS

### 期望（Tech Design §4.4）

> 错误码格式为 `0102YYY`

### 实际

所有已定义错误码（ErrorCode.java）：

| 错误码 | 常量名 | 格式 |
|--------|--------|------|
| 0102001 | MODEL_NOT_FOUND | ✅ 0102YYY |
| 0102002 | MODEL_NAME_EXISTS | ✅ |
| 0102003 | MODEL_NAME_IMMUTABLE | ✅ |
| 0102004 | CATEGORY_HAS_MODELS | ✅ |
| 0102005 | MODEL_CAPACITY_EXCEEDED | ✅ |
| 0102006 | VERSION_NOT_FOUND | ✅ |
| 0102007 | VERSION_NUMBER_GAP | ✅ |
| 0102008 | VERSION_LOCKED | ✅ |
| 0102009 | VERSION_CAPACITY_EXCEEDED | ✅ |
| 0102010 | UPLOAD_TASK_NOT_FOUND | ✅ |
| 0102011 | FILE_SUFFIX_NOT_ALLOWED | ✅ |
| 0102012 | UPLOAD_TASK_CONCURRENT_LIMIT | ✅ |
| 0102013 | CONVERT_TASK_NOT_FOUND | ✅ |
| 0102014 | UNSUPPORTED_CONVERT_FORMAT | ✅ |
| 0102015 | CATEGORY_NOT_FOUND | ✅ |
| 0102016 | CATEGORY_NAME_EXISTS | ✅ |
| 0102017 | CATEGORY_BUILTIN | ✅ |
| 0102018 | MODEL_TYPE_NAME_EXISTS | ✅ |
| 0102019 | MODEL_TYPE_NOT_FOUND | ✅ |
| 0102020 | MODEL_TYPE_BUILTIN | ✅ |
| 0102021 | TAG_NAME_EXISTS | ✅ |
| 0102022 | TAG_NOT_FOUND | ✅ |
| 0102023 | TAG_BUILTIN | ✅ |
| 0102024 | (DEPRECATED) | ✅ 已废弃 |
| 0102025 | MODEL_TAG_LIMIT_EXCEEDED | ✅ |
| 0102026 | MODEL_TAG_NOT_FOUND | ✅ |
| 0102027 | MODEL_TYPE_NOT_BELONG_TO_CATEGORY | ✅ |
| 0102028 | MODEL_RESOURCE_GROUP_IMMUTABLE | ✅ |
| 0102029 | MODEL_GLOBAL_CAPACITY_EXCEEDED | ✅ |
| 0102030 | VERSION_INVALID_CREATE_MODE | ✅ |
| 0102031 | TAG_NOT_FOUND_F3 | ✅ |
| 0102032 | MODEL_TAG_COUNT_EXCEEDED | ✅ |
| 0102033 | STORAGE_PATH_PVC_NAME_REQUIRED | ✅ |
| 0102034 | STORAGE_PATH_NFS_REQUIRED | ✅ |

**结论**: 全部 34 个错误码均遵循 `0102YYY` 格式，符合规范。

---

## 10. MyBatis XML Mapper 位置检查 ✅ PASS

### 期望（Tech Design §6.2）

> Mapper 文件位置: `src/main/resources/mapper/`
> 文件命名: `{表名}Mapper.xml`

### 实际

```
src/main/resources/mapper/
├── CategoryMapper.xml    ✅
├── ModelMapper.xml       ✅
└── TagMapper.xml         ✅
```

**结论**: MyBatis XML Mapper 位置和命名均符合规范。

---

## 违规汇总与建议

### 🔴 HIGH — 仓储实现类命名不符（§4.2）

| 当前 | 应改为 |
|------|--------|
| `CategoryRepositoryImpl` | `MyBatisCategoryRepository` |
| `ModelRepositoryImpl` | `MyBatisModelRepository` |
| `TagRepositoryImpl` | `MyBatisTagRepository` |

**建议**: 重命名仓储实现类，遵循 `MyBatis{Aggregate}Repository` 命名规范。

### 🔴 HIGH — 跨聚合 @Transactional（§3.2）

| 方法 | 问题 |
|------|------|
| `ModelApplicationService.createModel()` | 同一事务操作 Model + Tag 聚合 |
| `ModelApplicationService.modifyModel()` | 同一事务操作 Model + Tag 聚合 |

**建议**: 将 Model 保存和 Tag 关联操作拆分为独立事务。可使用编程式事务（`TransactionTemplate`）或移除 `@Transactional` 让各 Repository 方法自行管理事务。

### 🟡 MEDIUM — SQL data 目录结构（§6.4）

| 文件 | 问题 |
|------|------|
| `sql/data/V1__builtin_data.sql` | 根目录冗余文件，应仅保留 `sql/data/postgresql/V1__builtin_data.sql` |

**建议**: 删除 `sql/data/V1__builtin_data.sql` 根目录文件，统一按 DB 类型分目录。

---

## 检查结论

| 级别 | 数量 | 状态 |
|------|------|------|
| ✅ PASS | 7 | 包结构、javax.validation、DTO转换器、SourceType、0102024、错误码、Mapper位置 |
| ⚠️ WARN | 1 | SQL data 目录冗余文件 |
| ❌ FAIL | 2 | 仓储实现命名、跨聚合事务 |

**总体评价**: 包结构设计合理，与 DDD Tech Design 高度一致。主要偏差在仓储实现类命名和跨聚合事务处理上，需要修复以完全符合规范。
