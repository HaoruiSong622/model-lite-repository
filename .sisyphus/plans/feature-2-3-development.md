# Feature 1 同步更新 + Feature 2/3 TDD 开发计划

## TL;DR

> **核心目标**: 同步 Feature 1 数据库变更（model_version 新增 3 字段 + 新增 SourcePathType 枚举 + 新增错误码 0102015-0102034 + 全局异常处理器），然后使用 TDD 模式分别开发 Feature 2（分类体系与标签管理）和 Feature 3（模型与版本生命周期管理）。
> 
> **交付物**:
> - Feature 1 代码同步（DDL、枚举、错误码、异常处理器）
> - Feature 2 完整实现：Category/Tag 聚合根 + Repository + ApplicationService + 11 个 API 端点
> - Feature 3 完整实现：Model 聚合根 + ModelVersion 实体 + 值对象 + 领域服务 + Repository + ApplicationService + 6 个 API 端点
> - 全套 TDD 测试用例（单元 + 集成 + API）
> 
> **预计工作量**: Large
> **并行执行**: YES - 5 Waves
> **关键路径**: Task 1 → Task 5 → Task 8-9 → Task 14-16 → Task 22-25 → Verification

---

## Context

### Original Request
用户已完成 Feature 2/3 的特性文档书写，Feature 1 数据库表定义有变化。需要：
1. 同步 Feature 1 代码变更
2. 分别 TDD 开发 Feature 2 和 Feature 3
3. 开发过程中间节点 git commit
4. 完成后对照需求文档和设计描述检查遗漏
5. 对照 tech design 文档检查代码规范

### Interview Summary
**Key Discussions**:
- TDD 模式：先写类/方法声明和测试用例，再补充方法实现
- Git 策略：中间节点提交，不一次性 commit
- 依赖关系：Feature 3 依赖 Feature 2 的 CategoryRepository/TagRepository

**Research Findings**:
- model_version 表 DDL 缺失 source_type、nfs_server、nfs_path 三个字段
- 需要新增 SourcePathType 枚举（PVC/NFS），不同于已有 SourceType（NFS/CIFS/PVC）
- 无 Mockito 依赖，需要添加
- 无 MyBatis XML mapper 文件
- 无全局异常处理器

### Metis Review
**Identified Gaps** (addressed):
- SourcePathType 与 SourceType 是不同枚举，需分别处理
- 错误码 0102024 已废弃但需保留注释
- Feature 2 的 model-tag 端点涉及 Model 聚合根（F3 范围），F2 只用 TagRepository 处理
- RBAC 暂用请求参数模拟，不实现真实认证
- Model+ModelVersion 属于同一聚合，单聚合操作可用 @Transactional
- 需要新增 V2 迁移脚本（PostgreSQL ALTER TABLE）+ 更新 V1（H2）

---

## Work Objectives

### Core Objective
按照 DDD 架构和 TDD 开发模式，完成 Feature 1 同步更新和 Feature 2/3 的完整代码实现，所有测试通过，代码符合 tech design 规范。

### Concrete Deliverables
- `sql/schema/h2/V1__create_core_tables.sql` — 更新（model_version 新增 3 字段）
- `sql/schema/postgresql/V2__add_model_version_storage_columns.sql` — 新增
- `SourcePathType.java` + `SourcePathTypeTypeHandler.java` — 新增
- `ErrorCode.java` — 更新（新增 0102015-0102034）
- `GlobalExceptionHandler.java` — 新增
- `Category.java`, `ModelType.java`, `Tag.java` — 新增（领域模型）
- `CategoryRepository.java` + MyBatis impl + XML mapper — 新增
- `TagRepository.java` + MyBatis impl + XML mapper — 新增
- `CategoryApplicationService.java`, `TagApplicationService.java` — 新增
- `CategoryApi.java`, `TagApi.java` — 新增（11 个端点）
- `Model.java`, `ModelVersion.java`, `StoragePath.java`, `TrainingMetadata.java` — 新增
- `ModelDomainService.java` — 新增
- `ModelRepository.java` + MyBatis impl + XML mapper — 新增
- `ModelApplicationService.java` — 新增
- `ModelApi.java` — 新增（6 个端点）
- `ModelQueryCondition.java`, `PageResult.java` — 新增
- 内置数据 SQL 脚本 — 新增
- 全套测试文件

### Definition of Done
- [ ] `mvn test` 全部通过（0 failures）
- [ ] 所有 API 端点可通过 MockMvc 测试
- [ ] 所有领域逻辑有单元测试覆盖
- [ ] 所有 Repository 有集成测试覆盖（H2）
- [ ] 代码符合 tech design 规范（包结构、命名、架构分层）

### Must Have
- Feature 1 DDL 同步（H2 + PostgreSQL）
- SourcePathType 枚举 + TypeHandler
- ErrorCode 0102015-0102034（0102024 标注废弃）
- GlobalExceptionHandler
- Feature 2: Category/Tag 聚合根完整实现 + 测试
- Feature 2: CategoryRepository/TagRepository 接口 + MyBatis 实现 + 测试
- Feature 2: ApplicationService + API Controller + 测试
- Feature 2: 内置预设数据 SQL
- Feature 3: Model/ModelVersion 聚合根完整实现 + 测试
- Feature 3: StoragePath/TrainingMetadata 值对象 + 测试
- Feature 3: ModelDomainService 领域服务 + 测试
- Feature 3: ModelRepository 接口 + MyBatis 实现 + 测试
- Feature 3: ModelApplicationService + API Controller + 测试
- 中间节点 git commit

### Must NOT Have (Guardrails)
- ❌ 不修改已有 SourceType 枚举或其 TypeHandler
- ❌ 不添加 model_type_tag 的 CRUD API 端点（仅 Repository 方法）
- ❌ 不添加 Category/ModelType/Tag 的 PUT/PATCH 更新端点
- ❌ 不实现软删除逻辑（Feature 6 范围）
- ❌ 不在跨聚合边界使用 @Transactional（单聚合内可用）
- ❌ 不实现真实认证/授权 — RBAC 用请求参数模拟
- ❌ 不创建独立 DTO 转换类 — 在 ApplicationService/Controller 内联映射
- ❌ 不在 DTO 上使用 javax.validation 注解 — 校验在领域模型工厂方法中
- ❌ 不过度抽象（不要为了复用创建不必要的基类/工具类）

---

## Verification Strategy

> **零人工介入** — 所有验证由 agent 执行。
> 验收标准中禁止出现"用户手动测试/确认"。

### Test Decision
- **Infrastructure exists**: YES（JUnit5 + H2 + spring-boot-starter-test）
- **Automated tests**: YES (TDD)
- **Framework**: JUnit5 + Mockito + H2 integration
- **TDD 流程**: RED（写失败测试）→ GREEN（最小实现）→ REFACTOR（重构）

### QA Policy
每个任务必须包含 agent 执行的 QA 场景。
证据保存到 `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`。

- **领域模型**: Bash（mvn test）— 运行单元测试，断言通过
- **Repository 集成**: Bash（mvn test）— 运行 H2 集成测试，验证 CRUD
- **API 端点**: Bash（mvn test）— 运行 MockMvc 测试，验证请求/响应
- **代码规范**: Bash（grep/ast_grep）— 检查包结构、命名、分层

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Foundation — Feature 1 同步 + 公共组件):
├── Task 1: Feature 1 DDL 同步 + SourcePathType 枚举 + TypeHandler [unspecified-high]
├── Task 2: ErrorCode 扩展 (0102015-0102034) [quick]
├── Task 3: GlobalExceptionHandler 全局异常处理器 [quick]
├── Task 4: Mockito 依赖添加 + 测试基础设施验证 [quick]
└── Task 5: Feature 2/3 DTO/VO 公共类定义 [quick]

Wave 2 (Feature 2 领域层 — TDD RED+GREEN):
├── Task 6: Category 聚合根 + ModelType 实体 (TDD) [deep]
├── Task 7: Tag 聚合根 (TDD) [deep]
└── Task 8: CategoryRepository 接口 + MyBatis 实现 + 集成测试 (TDD) [unspecified-high]
└── Task 9: TagRepository 接口 + MyBatis 实现 + 集成测试 (TDD) [unspecified-high]

Wave 3 (Feature 2 应用层 + API — Feature 3 领域层):
├── Task 10: CategoryApplicationService + TagApplicationService (TDD) [unspecified-high]
├── Task 11: CategoryApi + TagApi 控制器 + API 测试 (TDD) [unspecified-high]
├── Task 12: 内置预设数据 SQL 脚本 [quick]
├── Task 13: Model 聚合根 + ModelVersion 实体 (TDD) [deep]
└── Task 14: StoragePath + TrainingMetadata 值对象 (TDD) [quick]

Wave 4 (Feature 3 持久化 + 领域服务 + 应用层):
├── Task 15: ModelRepository 接口 + MyBatis 实现 + 集成测试 (TDD) [unspecified-high]
├── Task 16: ModelDomainService 领域服务 (TDD) [deep]
├── Task 17: ModelApplicationService 应用服务 (TDD) [unspecified-high]
└── Task 18: ModelApi 控制器 + API 测试 (TDD) [unspecified-high]

Wave 5 (Git Commit + 验证):
├── Task 19: Git commit — Feature 1 同步 [quick]
├── Task 20: Git commit — Feature 2 完整 [quick]
├── Task 21: Git commit — Feature 3 完整 [quick]
├── Task 22: 需求文档对照检查 [deep]
└── Task 23: Tech Design 代码规范检查 [deep]

Wave FINAL (Final Verification):
├── F1: Plan Compliance Audit — oracle
├── F2: Code Quality Review — unspecified-high
├── F3: Real Manual QA — unspecified-high
└── F4: Scope Fidelity Check — deep
→ Present results → Get explicit user okay

Critical Path: Task 1 → Task 5 → Task 6 → Task 8 → Task 10 → Task 11 → Task 13 → Task 15 → Task 16 → Task 17 → Task 18 → Task 22-23 → FINAL
Max Concurrent: 5 (Wave 1)
```

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| 1 | — | 6, 8, 9, 13, 14 | 1 |
| 2 | — | 3, 6, 7 | 1 |
| 3 | 2 | 10, 11, 17, 18 | 1 |
| 4 | — | 6, 7, 8, 9 | 1 |
| 5 | 1, 2 | 10, 11, 13, 17, 18 | 1 |
| 6 | 1, 2, 4 | 8, 10 | 2 |
| 7 | 1, 2, 4 | 9, 10 | 2 |
| 8 | 1, 4, 5, 6 | 10, 16 | 2 |
| 9 | 1, 4, 5, 7 | 10, 16 | 2 |
| 10 | 3, 6, 7, 8, 9 | 11 | 3 |
| 11 | 3, 5, 10 | 20 | 3 |
| 12 | 2, 8, 9 | 20 | 3 |
| 13 | 1, 2, 4, 5 | 15, 16 | 3 |
| 14 | 1, 13 | 15 | 3 |
| 15 | 5, 13, 14 | 17 | 4 |
| 16 | 8, 9, 13 | 17 | 4 |
| 17 | 3, 15, 16 | 18 | 4 |
| 18 | 3, 5, 17 | 21 | 4 |
| 19 | 1, 2, 3, 4, 5 | — | 5 |
| 20 | 11, 12 | — | 5 |
| 21 | 18 | — | 5 |
| 22 | 19, 20, 21 | FINAL | 5 |
| 23 | 19, 20, 21 | FINAL | 5 |

### Agent Dispatch Summary

- **Wave 1**: 5 — T1 `unspecified-high`, T2 `quick`, T3 `quick`, T4 `quick`, T5 `quick`
- **Wave 2**: 4 — T6 `deep`, T7 `deep`, T8 `unspecified-high`, T9 `unspecified-high`
- **Wave 3**: 5 — T10 `unspecified-high`, T11 `unspecified-high`, T12 `quick`, T13 `deep`, T14 `quick`
- **Wave 4**: 4 — T15 `unspecified-high`, T16 `deep`, T17 `unspecified-high`, T18 `unspecified-high`
- **Wave 5**: 5 — T19 `quick`, T20 `quick`, T21 `quick`, T22 `deep`, T23 `deep`
- **FINAL**: 4 — F1 `oracle`, F2 `unspecified-high`, F3 `unspecified-high`, F4 `deep`

---

## TODOs

- [x] 1. Feature 1 DDL 同步 + SourcePathType 枚举 + TypeHandler

  **What to do**:
  - 更新 `sql/schema/h2/V1__create_core_tables.sql`：在 model_version 表中添加 `source_type VARCHAR(20) DEFAULT NULL`, `nfs_server VARCHAR(255) DEFAULT NULL`, `nfs_path VARCHAR(1024) DEFAULT NULL` 三个字段（放在 `internal_path` 之后）
  - 新建 `sql/schema/postgresql/V2__add_model_version_storage_columns.sql`：`ALTER TABLE model_version ADD COLUMN source_type VARCHAR(20) DEFAULT NULL;` 等三条 ALTER TABLE 语句 + COMMENT ON COLUMN
  - 在 `modelweight/domain/aggregate/model/` 包下创建 `SourcePathType.java` 枚举（PVC, NFS），遵循现有枚举模式（`ENUM_NAME("dbValue", "displayName")` + `fromDbValue()`）
  - 在 `infrastructure/persistence/typehandler/` 下创建 `SourcePathTypeTypeHandler.java`，遵循现有 TypeHandler 模式
  - 在 `MyBatisConfig.java` 中注册新 TypeHandler
  - 运行 `mvn test` 验证 H2 Schema 迁移测试通过

  **Must NOT do**:
  - 不修改现有 `SourceType` 枚举或其 TypeHandler
  - 不添加 `upload_task` 表的任何变更
  - 不修改 PostgreSQL 的 V1 脚本（用 V2 迁移脚本）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 涉及多文件变更（SQL + 枚举 + TypeHandler + 配置），需要理解现有模式并保持一致性
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4, 5)
  - **Blocks**: Tasks 6, 8, 9, 13, 14
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `src/main/java/com/huawei/modellite/repository/common/enums/TagType.java` — 枚举模式参考（字段、构造函数、fromDbValue）
  - `src/main/java/com/huawei/modellite/repository/infrastructure/persistence/typehandler/TagTypeTypeHandler.java` — TypeHandler 模式参考
  - `src/main/java/com/huawei/modellite/repository/infrastructure/config/MyBatisConfig.java` — TypeHandler 注册方式

  **API/Type References**:
  - `sql/schema/h2/V1__create_core_tables.sql` — H2 DDL，需要添加 3 个字段到 model_version 表
  - `sql/schema/postgresql/V1__create_core_tables.sql` — PostgreSQL DDL 模式参考（COMMENT ON COLUMN 格式）
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 298-309 — SourcePathType 枚举定义（PVC/NFS 两个值）
  - `.sisyphus/drafts/feature-1-infrastructure-design.md` lines 155-193 — model_version 表完整 DDL（含新增的 3 个字段）

  **External References**:
  - 无需外部参考，完全遵循项目内现有模式

  **WHY Each Reference Matters**:
  - TagType.java 提供枚举的精确代码结构，确保新枚举风格一致
  - H2 DDL 是需要修改的目标文件，必须精确知道插入位置
  - F3 设计文档定义了 SourcePathType 的精确值和含义

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: H2 Schema 迁移成功
    Tool: Bash
    Preconditions: V1 DDL 已更新，添加了 3 个新字段
    Steps:
      1. mvn test -Dtest=SchemaMigrationTest
      2. 检查输出 BUILD SUCCESS
    Expected Result: SchemaMigrationTest 通过，model_version 表包含 source_type, nfs_server, nfs_path 列
    Failure Indicators: SQL 语法错误、表已存在、列名冲突
    Evidence: .sisyphus/evidence/task-1-h2-schema-migration.txt

  Scenario: SourcePathType 枚举正确性验证
    Tool: Bash
    Preconditions: SourcePathType.java 已创建
    Steps:
      1. mvn test -Dtest=SourcePathTypeTest (如已有通用枚举测试)
      2. 或编写快速验证：检查 PVC.fromDbValue("PVC") 返回 PVC, NFS.fromDbValue("NFS") 返回 NFS
    Expected Result: 枚举值正确，fromDbValue 正常工作，无效值抛 IllegalArgumentException
    Failure Indicators: 编译错误、枚举值缺失、fromDbValue 返回 null
    Evidence: .sisyphus/evidence/task-1-source-path-type-enum.txt

  Scenario: TypeHandler 注册验证
    Tool: Bash
    Preconditions: SourcePathTypeTypeHandler 已创建，MyBatisConfig 已注册
    Steps:
      1. mvn test -Dtest=TypeHandlerIntegrationTest
    Expected Result: TypeHandler 集成测试通过（如有通用测试）或 mvn compile 成功
    Failure Indicators: MyBatisConfig 编译错误、TypeHandler 未注册
    Evidence: .sisyphus/evidence/task-1-typehandler-registration.txt
  ```

  **Commit**: YES (groups with Task 19)
  - Message: `feat(infrastructure): sync Feature 1 — add model_version storage columns, SourcePathType enum`
  - Files: `sql/schema/**, **/SourcePathType.java, **/SourcePathTypeTypeHandler.java, **/MyBatisConfig.java`
  - Pre-commit: `mvn test`

- [x] 2. ErrorCode 扩展 (0102015-0102034)

  **What to do**:
  - 在 `common/enums/ErrorCode.java` 中添加 Feature 2 错误码：0102015 (CATEGORY_NOT_FOUND), 0102016 (CATEGORY_NAME_EXISTS), 0102017 (CATEGORY_BUILTIN), 0102018 (MODEL_TYPE_NAME_EXISTS), 0102019 (MODEL_TYPE_NOT_FOUND), 0102020 (MODEL_TYPE_BUILTIN), 0102021 (TAG_NAME_EXISTS), 0102022 (TAG_NOT_FOUND), 0102023 (TAG_BUILTIN), 0102024 (已废弃，保留注释 `// 0102024: DEPRECATED - 原标签被引用禁止删除，现改为级联删除`), 0102025 (MODEL_TAG_LIMIT_EXCEEDED), 0102026 (MODEL_TAG_NOT_FOUND)
  - 添加 Feature 3 错误码：0102027 (MODEL_TYPE_NOT_BELONG_TO_CATEGORY), 0102028 (MODEL_RESOURCE_GROUP_IMMUTABLE), 0102029 (MODEL_GLOBAL_CAPACITY_EXCEEDED), 0102030 (VERSION_INVALID_CREATE_MODE), 0102031 (MODEL_TAG_NOT_FOUND), 0102032 (MODEL_TAG_COUNT_EXCEEDED), 0102033 (STORAGE_PATH_PVC_NAME_REQUIRED), 0102034 (STORAGE_PATH_NFS_REQUIRED)
  - 添加错误码与 HTTP 状态码的映射（供 GlobalExceptionHandler 使用）
  - 编写/更新 ErrorCode 单元测试验证所有新增常量

  **Must NOT do**:
  - 不修改已有 0102001-0102014 的定义
  - 不跳过 0102024（必须保留为废弃注释）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 单文件修改，模式清晰（在现有常量后追加新常量）
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 4, 5)
  - **Blocks**: Tasks 3, 6, 7
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `src/main/java/com/huawei/modellite/repository/common/enums/ErrorCode.java` — 现有错误码格式，新增代码需遵循相同风格

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 2393-2408 — Feature 2 错误码完整定义（含 0102024 废弃说明）
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 600-609 — Feature 3 错误码完整定义

  **WHY Each Reference Matters**:
  - ErrorCode.java 是需要修改的目标文件，必须了解现有格式
  - F2/F3 设计文档提供了每个错误码的精确常量名、值和含义

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 错误码完整性验证
    Tool: Bash
    Preconditions: ErrorCode.java 已更新
    Steps:
      1. mvn compile — 验证编译通过
      2. grep -c "01020" src/main/java/**/ErrorCode.java — 计算错误码数量
      3. 验证包含 0102015-0102034 所有常量（34 个总计）
      4. 验证 0102024 存在但标记为 DEPRECATED
    Expected Result: 编译成功，34 个错误码常量（含 0102024 注释）
    Failure Indicators: 缺少常量、重复常量、编译错误
    Evidence: .sisyphus/evidence/task-2-error-codes-complete.txt

  Scenario: 错误码单元测试
    Tool: Bash
    Preconditions: 测试文件已创建
    Steps:
      1. mvn test -Dtest=ErrorCodeTest (或编写新测试)
      2. 验证每个新增常量的值和格式正确
    Expected Result: 所有测试通过
    Failure Indicators: 常量值不匹配、格式错误
    Evidence: .sisyphus/evidence/task-2-error-codes-test.txt
  ```

  **Commit**: YES (groups with Task 19)
  - Message: `feat(common): add error codes 0102015-0102034 for Feature 2 and Feature 3`
  - Files: `**/ErrorCode.java`
  - Pre-commit: `mvn test`

- [x] 3. GlobalExceptionHandler 全局异常处理器

  **What to do**:
  - 在 `common/exception/` 或 `infrastructure/` 下创建 `GlobalExceptionHandler.java`（`@RestControllerAdvice`）
  - 处理 `ModelLiteException` → 提取 code + message → 返回 `BaseResponse.error(code, message)`，并根据错误码映射 HTTP 状态码（404 for 0102*001/006/015/019/022/026/031, 409 for 0102*002/008/016/018/021, 400 for 其他）
  - 处理通用 `Exception` → 返回 500 + BaseResponse.error("0105001", "内部服务错误")
  - 编写单元测试：模拟各种异常，验证返回的 HTTP 状态码和 BaseResponse 格式
  - TDD 流程：先写测试定义期望行为，再实现异常处理器

  **Must NOT do**:
  - 不过度设计（不要添加 i18n、日志链路等复杂逻辑）
  - 不在异常处理器中做业务逻辑判断

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准的 Spring @RestControllerAdvice 模式，代码量小
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (但依赖 Task 2 的错误码定义)
  - **Parallel Group**: Wave 1 (with Tasks 1, 4, 5) — 但需 Task 2 完成后开始
  - **Blocks**: Tasks 10, 11, 17, 18
  - **Blocked By**: Task 2

  **References**:

  **Pattern References**:
  - `src/main/java/com/huawei/modellite/repository/common/exception/ModelLiteException.java` — 需要处理的异常类，了解其 code/message 结构
  - `src/main/java/com/huawei/modellite/repository/common/dto/response/BaseResponse.java` — 返回类型，了解 error() 工厂方法签名

  **API/Type References**:
  - `.sisyphus/drafts/feature-1-infrastructure-design.md` lines 497-514 — 错误码与 HTTP 状态码映射表
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 2415-2426 — Feature 2 错误码 HTTP 映射
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 587-609 — Feature 3 错误码 HTTP 映射

  **WHY Each Reference Matters**:
  - ModelLiteException 是需要捕获的核心异常类型
  - BaseResponse.error() 是标准返回格式
  - 设计文档提供了精确的 HTTP 状态码映射规则

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: ModelLiteException 正确映射
    Tool: Bash
    Preconditions: GlobalExceptionHandler 已实现
    Steps:
      1. mvn test -Dtest=GlobalExceptionHandlerTest
      2. 验证: MODEL_NOT_FOUND(0102001) → 404, MODEL_NAME_EXISTS(0102002) → 409, CATEGORY_BUILTIN(0102017) → 400
    Expected Result: 每种错误码映射到正确的 HTTP 状态码，response body 包含 code + message
    Failure Indicators: 错误的状态码、response 格式不符合 BaseResponse
    Evidence: .sisyphus/evidence/task-3-exception-handler.txt

  Scenario: 未知异常返回 500
    Tool: Bash
    Preconditions: GlobalExceptionHandler 已实现
    Steps:
      1. 抛出 RuntimeException("unexpected error")
      2. 验证返回 HTTP 500 + BaseResponse.error("0105001", "内部服务错误")
    Expected Result: 500 状态码，标准错误响应
    Failure Indicators: 暴露异常堆栈到客户端、非标准响应格式
    Evidence: .sisyphus/evidence/task-3-exception-handler-500.txt
  ```

  **Commit**: YES (groups with Task 19)
  - Message: `feat(common): add GlobalExceptionHandler with error code HTTP status mapping`
  - Files: `**/GlobalExceptionHandler.java, **/GlobalExceptionHandlerTest.java`
  - Pre-commit: `mvn test`

- [x] 4. Mockito 依赖添加 + 测试基础设施验证

  **What to do**:
  - 在 `pom.xml` 中添加 `mockito-core` 依赖（如 spring-boot-starter-test 未自动包含）或确认 Mockito 已通过 spring-boot-starter-test 引入
  - 验证 `spring-boot-starter-test` 包含的 Mockito 版本（Spring Boot 3.4.5 应包含 Mockito 5.x）
  - 创建一个简单的 Mockito 测试示例验证可用性（如 mock 一个 List，验证 interaction）
  - 确认 `mybatis-spring-boot-starter-test` 可用于 MyBatis 集成测试
  - 运行 `mvn test` 验证所有现有测试仍然通过

  **Must NOT do**:
  - 不升级 spring-boot 版本
  - 不修改现有测试

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 简单的依赖确认和验证
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 5)
  - **Blocks**: Tasks 6, 7, 8, 9
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `pom.xml` — 现有 Maven 依赖配置

  **API/Type References**:
  - `src/test/java/com/huawei/modellite/repository/integration/AbstractIntegrationTest.java` — 现有集成测试基类模式

  **WHY Each Reference Matters**:
  - pom.xml 确认 Mockito 是否已通过 spring-boot-starter-test 引入
  - AbstractIntegrationTest 提供了集成测试的基础模式

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Mockito 可用性验证
    Tool: Bash
    Preconditions: pom.xml 已检查
    Steps:
      1. mvn dependency:tree | grep mockito — 确认 Mockito 在依赖树中
      2. mvn test — 所有现有测试通过
    Expected Result: Mockito 在依赖树中可见，所有测试通过
    Failure Indicators: Mockito 缺失、测试失败
    Evidence: .sisyphus/evidence/task-4-mockito-available.txt
  ```

  **Commit**: YES (groups with Task 19)
  - Message: `chore(test): verify Mockito dependency for TDD workflow`
  - Files: `pom.xml` (if changed)
  - Pre-commit: `mvn test`

- [x] 5. Feature 2/3 DTO/VO 公共类定义

  **What to do**:
  - 创建 `modelweight/application/` 下的请求/响应 DTO 类：
    - CategoryDTO: CategoryRequest (name, description), CategoryResponse (id, name, description, isBuiltin, modelTypes, createTime, updateTime)
    - ModelTypeDTO: ModelTypeResponse (id, name, description, isBuiltin)
    - TagDTO: TagRequest (name, tagType), TagResponse (id, name, tagType, isBuiltin, createTime, updateTime)
    - ModelDTO: ModelCreateRequest, ModelModifyRequest, ModelResponse, ModelListResponse, VersionCreateRequest, VersionResponse
  - 创建 `ModelQueryCondition.java` 值对象（categoryId, typeId, tagId, keyword, resourceGroups, page, pageSize, sortBy, sortOrder）
  - 创建 `PageResult<T>.java` 通用分页结果（items, total, page, pageSize, totalPages）
  - 所有 DTO 只包含字段定义 + getter/setter（或 Lombok），不含业务逻辑
  - 编译验证通过

  **Must NOT do**:
  - 不在 DTO 上添加 javax.validation 注解（校验在领域模型中）
  - 不创建独立的 DTO 转换器类
  - 不在 DTO 中包含业务逻辑

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 纯数据类定义，无业务逻辑
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (依赖 Task 1, 2 的枚举和错误码)
  - **Parallel Group**: Wave 1 (with Tasks 3, 4) — 需 Task 1, 2 完成后开始
  - **Blocks**: Tasks 10, 11, 13, 17, 18
  - **Blocked By**: Task 1, Task 2

  **References**:

  **Pattern References**:
  - `src/main/java/com/huawei/modellite/repository/common/dto/response/BaseResponse.java` — DTO 响应包装格式

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 443-899 — 所有 Feature 2 API 端点的 Request/Response 定义
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 613-1100 — 所有 Feature 3 API 端点的 Request/Response 定义
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 543-565 — ModelQueryCondition 和 PageResult 完整定义

  **WHY Each Reference Matters**:
  - 设计文档提供了每个 DTO 的精确字段名、类型和约束
  - BaseResponse.java 提供了响应包装的标准模式

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: DTO 编译验证
    Tool: Bash
    Preconditions: 所有 DTO 类已创建
    Steps:
      1. mvn compile
      2. 验证无编译错误
    Expected Result: BUILD SUCCESS，所有 DTO 类编译通过
    Failure Indicators: 类型引用错误、缺少导入
    Evidence: .sisyphus/evidence/task-5-dto-compile.txt
  ```

  **Commit**: YES (groups with Task 19)
  - Message: `feat(dto): add request/response DTOs for Feature 2 and Feature 3`
  - Files: `**/dto/**, **/ModelQueryCondition.java, **/PageResult.java`
  - Pre-commit: `mvn compile`

- [x] 6. Category 聚合根 + ModelType 实体 (TDD)

  **What to do**:
  - TDD RED: 先编写 Category 和 ModelType 的测试用例
    - Category 构造：正常创建、名称为空/null 应抛异常
    - Category.addModelType：正常添加、重名应抛 MODEL_TYPE_NAME_EXISTS
    - Category.removeModelType：正常移除、不存在抛 MODEL_TYPE_NOT_FOUND、内置类型抛 MODEL_TYPE_BUILTIN、有模型引用抛 CATEGORY_HAS_MODELS
    - ModelType 构造：正常创建、字段正确性验证
  - TDD GREEN: 实现 Category 聚合根和 ModelType 实体
    - Category: categoryId, name, description, isBuiltIn, modelTypes (List\<ModelType\>), addModelType(), removeModelType()
    - ModelType: typeId, name, description, isBuiltIn
    - 构造函数校验：name 非空且长度限制
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不添加 @Transactional 注解
  - 不依赖 Repository 或数据库
  - 不添加 Category 的 update 方法（设计文档无此需求）

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 领域模型设计需要理解聚合根模式、业务不变量，TDD 要求先写测试再实现
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 7, 8, 9)
  - **Blocks**: Tasks 8, 10
  - **Blocked By**: Tasks 1, 2, 4

  **References**:

  **Pattern References**:
  - `src/main/java/com/huawei/modellite/repository/common/enums/ErrorCode.java` — 需要引用的错误码常量
  - `src/main/java/com/huawei/modellite/repository/common/exception/ModelLiteException.java` — 异常类构造方式

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 210-359 — Category/ModelType 类图、字段定义、方法伪代码
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 426-439 — 业务不变量定义
  - `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md` — 包结构规范 `modelweight.domain.aggregate.category`

  **Test References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 1600-1700 (approx) — Category 单元测试用例定义

  **WHY Each Reference Matters**:
  - 类图和伪代码提供了精确的方法签名和业务规则
  - 业务不变量定义了所有需要校验的规则
  - Tech Design 文档确定了包路径和命名规范

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Category 领域模型单元测试全部通过
    Tool: Bash
    Preconditions: Category.java, ModelType.java, CategoryTest.java 已创建
    Steps:
      1. mvn test -Dtest=CategoryTest
      2. 验证所有测试通过
    Expected Result: 所有测试通过（至少包含：正常创建、addModelType 正常/重名、removeModelType 正常/不存在/内置/有引用）
    Failure Indicators: 测试失败、缺少测试场景
    Evidence: .sisyphus/evidence/task-6-category-domain-test.txt

  Scenario: ModelType 构造验证
    Tool: Bash
    Preconditions: ModelType.java 已创建
    Steps:
      1. mvn test -Dtest=ModelTypeTest
    Expected Result: ModelType 字段正确性验证通过
    Failure Indicators: 字段类型或访问器错误
    Evidence: .sisyphus/evidence/task-6-modeltype-test.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 7. Tag 聚合根 (TDD)

  **What to do**:
  - TDD RED: 先编写 Tag 测试用例
    - Tag.createUserTag：正常创建、验证 tagType=USER
    - Tag.createCapabilityTag：正常创建、验证 tagType=CAPABILITY
    - Tag 构造校验：name 非空且长度限制（1-50）、tagType 非空
    - Tag 名字唯一性由 Repository 保证（领域模型不校验）
  - TDD GREEN: 实现 Tag 聚合根
    - Tag: tagId, name, tagType (TagType 枚举), isBuiltIn, createTime, updateTime
    - 静态工厂方法：createUserTag(name), createCapabilityTag(name)
    - 构造函数校验：name 非空且 ≤50 字符
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不在 Tag 聚合根中持有 ModelTag/ModelTypeTag 列表（关联由 Repository 管理）
  - 不添加 Tag update 方法

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 聚合根 TDD 开发
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 8, 9)
  - **Blocks**: Tasks 9, 10
  - **Blocked By**: Tasks 1, 2, 4

  **References**:

  **Pattern References**:
  - `src/main/java/com/huawei/modellite/repository/common/enums/TagType.java` — TagType 枚举

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 238-386 — Tag 类图、字段定义、静态工厂方法
  - `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md` — 包结构规范 `modelweight.domain.aggregate.tag`

  **Test References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 1700-1800 (approx) — Tag 单元测试用例

  **WHY Each Reference Matters**:
  - Tag 类图提供了字段和工厂方法的精确定义
  - 静态工厂方法 createUserTag/createCapabilityTag 是核心设计决策

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Tag 领域模型单元测试
    Tool: Bash
    Preconditions: Tag.java, TagTest.java 已创建
    Steps:
      1. mvn test -Dtest=TagTest
    Expected Result: 所有测试通过（createUserTag 验证 USER 类型, createCapabilityTag 验证 CAPABILITY 类型, name 校验）
    Failure Indicators: 测试失败、tagType 错误
    Evidence: .sisyphus/evidence/task-7-tag-domain-test.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 8. CategoryRepository 接口 + MyBatis 实现 + 集成测试 (TDD)

  **What to do**:
  - TDD RED: 先编写 CategoryRepository 集成测试
    - 测试 save（含级联保存 ModelType）、findById、findByIdWithTypes、findAll、findAllWithTypes、existsByName、hasModelReference、hasModelReferenceByTypeId、deleteById、deleteModelTypeById
    - 继承 AbstractIntegrationTest 使用 H2 内存数据库
  - TDD GREEN: 实现完整 Repository 层
    - 定义 `CategoryRepository` 接口（10 个方法）在 `modelweight/domain/repository/`
    - 创建 `CategoryRepositoryImpl` 在 `infrastructure/persistence/` 实现 MyBatis 映射
    - 创建 `CategoryMapper.xml` 在 `resources/mapper/`
    - 创建 `CategoryMapper.java` 接口在 `infrastructure/persistence/mapper/`
    - ResultMap 映射：Category + 嵌套 ModelType 集合（findByIdWithTypes/findAllWithTypes）
  - TDD REFACTOR: 优化 SQL、抽取公共 SQL 片段

  **Must NOT do**:
  - 不在 Repository 中实现业务逻辑
  - 不使用 @Transactional 跨聚合
  - 不添加 Category update 方法

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: MyBatis XML 配置 + 集成测试需要理解 SQL 映射和 ResultMap 嵌套查询
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 7, 9)
  - **Blocks**: Tasks 10, 16
  - **Blocked By**: Tasks 1, 4, 5, 6

  **References**:

  **Pattern References**:
  - `src/test/java/com/huawei/modellite/repository/integration/AbstractIntegrationTest.java` — H2 集成测试基类
  - `src/test/java/com/huawei/modellite/repository/integration/TypeHandlerIntegrationTest.java` — MyBatis 集成测试模式
  - `src/main/java/com/huawei/modellite/repository/infrastructure/config/MyBatisConfig.java` — mapper 扫描配置

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 388-403 — CategoryRepository 接口方法签名（10 个方法）
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 136-157 — category 和 model_type 表数据字典
  - `sql/schema/h2/V1__create_core_tables.sql` — H2 DDL 表结构

  **External References**:
  - MyBatis XML mapper 文档 — ResultMap 嵌套映射（collection 标签）

  **WHY Each Reference Matters**:
  - AbstractIntegrationTest 提供 H2 测试的启动方式（@MybatisTest 等）
  - 设计文档定义了所有 10 个 Repository 方法的精确签名
  - H2 DDL 是 SQL 编写的目标表结构

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: CategoryRepository 集成测试全通过
    Tool: Bash
    Preconditions: CategoryRepository 接口、MyBatis impl、XML mapper、测试类已创建
    Steps:
      1. mvn test -Dtest=CategoryRepositoryTest
      2. 验证所有 CRUD 操作正确
    Expected Result: 所有 10 个方法测试通过（save 级联保存 ModelType, findById 返回正确, existsByName 正确判断, deleteById 级联删除 ModelType）
    Failure Indicators: SQL 错误、ResultMap 映射错误、级联保存/删除失败
    Evidence: .sisyphus/evidence/task-8-category-repo-test.txt

  Scenario: findByIdWithTypes 正确加载嵌套集合
    Tool: Bash
    Steps:
      1. 保存 Category + 2 个 ModelType
      2. 调用 findByIdWithTypes
      3. 验证返回的 Category.modelTypes 包含 2 个元素
    Expected Result: modelTypes 列表正确填充
    Failure Indicators: 空列表、重复数据
    Evidence: .sisyphus/evidence/task-8-nested-loading.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 9. TagRepository 接口 + MyBatis 实现 + 集成测试 (TDD)

  **What to do**:
  - TDD RED: 先编写 TagRepository 集成测试（16 个方法）
    - 测试 CRUD: save, findById, findAll, findByTagType, existsByName, deleteById
    - 测试关联管理: addModelTag, removeModelTag, findTagsByModelId, findModelIdsByTagId, addModelTypeTag, removeModelTypeTag, findTagsByModelTypeId
    - 测试级联删除: removeModelTagsByTagId, removeModelTypeTagsByTagId, hasReference
  - TDD GREEN: 实现完整 Repository 层
    - 定义 `TagRepository` 接口（16 个方法）在 `modelweight/domain/repository/`
    - 创建 `TagRepositoryImpl` 在 `infrastructure/persistence/`
    - 创建 `TagMapper.xml` 在 `resources/mapper/`
    - 创建 `TagMapper.java` 接口
  - TDD REFACTOR: 优化 SQL

  **Must NOT do**:
  - 不添加 model_type_tag 的 API 端点（仅 Repository 方法）
  - 不在 Repository 中实现业务逻辑

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 16 个方法的 MyBatis 实现 + 集成测试，工作量较大
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 7, 8)
  - **Blocks**: Tasks 10, 16
  - **Blocked By**: Tasks 1, 4, 5, 7

  **References**:

  **Pattern References**:
  - `src/test/java/com/huawei/modellite/repository/integration/AbstractIntegrationTest.java` — H2 集成测试基类
  - Task 8 的 CategoryRepository 模式 — 遵循相同模式

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 405-424 — TagRepository 接口方法签名（16 个方法）
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 159-186 — tag, model_tag, model_type_tag 表数据字典

  **WHY Each Reference Matters**:
  - TagRepository 有最多的方法（16 个），需要精确的方法签名
  - 三张关联表的数据字典是 SQL 编写的基础

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: TagRepository 集成测试全通过
    Tool: Bash
    Preconditions: TagRepository 完整实现
    Steps:
      1. mvn test -Dtest=TagRepositoryTest
    Expected Result: 所有 16 个方法测试通过
    Failure Indicators: SQL 错误、关联表操作失败
    Evidence: .sisyphus/evidence/task-9-tag-repo-test.txt

  Scenario: 标签级联删除验证
    Tool: Bash
    Steps:
      1. 创建 Tag, 创建关联的 model_tag 和 model_type_tag 记录
      2. 调用 removeModelTagsByTagId + removeModelTypeTagsByTagId + deleteById
      3. 验证 tag、model_tag、model_type_tag 中对应记录全部删除
    Expected Result: 级联删除正确执行
    Failure Indicators: 残留关联记录
    Evidence: .sisyphus/evidence/task-9-cascade-delete.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 10. CategoryApplicationService + TagApplicationService (TDD)

  **What to do**:
  - TDD RED: 先编写 ApplicationService 测试（使用 Mockito mock Repository）
    - CategoryApplicationService 测试：
      - createCategory: 正常创建、名称重复抛 CATEGORY_NAME_EXISTS
      - deleteCategory: 正常删除、不存在抛 CATEGORY_NOT_FOUND、内置分类抛 CATEGORY_BUILTIN、有模型引用抛 CATEGORY_HAS_MODELS
      - addModelType: 正常添加、分类不存在抛 CATEGORY_NOT_FOUND、类型名重复抛 MODEL_TYPE_NAME_EXISTS
      - deleteModelType: 正常删除、类型不存在抛 MODEL_TYPE_NOT_FOUND、内置类型抛 MODEL_TYPE_BUILTIN、有模型引用抛 CATEGORY_HAS_MODELS
    - TagApplicationService 测试：
      - createTag: 正常创建、名称重复抛 TAG_NAME_EXISTS
      - deleteTag: 正常删除（含级联清理 model_tag + model_type_tag）、不存在抛 TAG_NOT_FOUND、内置标签抛 TAG_BUILTIN
      - addModelTag: 正常添加、标签不存在抛 TAG_NOT_FOUND、标签数超限抛 MODEL_TAG_LIMIT_EXCEEDED
      - removeModelTag: 正常移除、关联不存在抛 MODEL_TAG_NOT_FOUND
  - TDD GREEN: 实现 CategoryApplicationService 和 TagApplicationService
    - 注入 CategoryRepository, TagRepository, ModelRepository（仅用于 hasModelReference 检查）
    - 每个方法实现完整的业务编排逻辑
  - TDD REFACTOR: 提取公共校验方法

  **Must NOT do**:
  - 不使用 @Transactional（跨聚合独立事务）
  - 不在 ApplicationService 中做数据库操作（委托给 Repository）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 两个 ApplicationService 各有多个方法，需要 Mockito 编排 + 业务逻辑实现
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 11, 12, 13, 14)
  - **Blocks**: Task 11
  - **Blocked By**: Tasks 3, 6, 7, 8, 9

  **References**:

  **Pattern References**:
  - Task 6/7 的 Category/Tag 领域模型 — ApplicationService 需要调用的方法
  - Task 8/9 的 Repository 接口 — ApplicationService 需要注入的依赖

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 950-1200 (approx) — ApplicationService 序列图和业务流程
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 1120-1240 — 关键业务流程伪代码（删除分类、删除标签级联、添加模型类型等）

  **WHY Each Reference Matters**:
  - 序列图展示了 ApplicationService 编排 Repository 调用的完整流程
  - 业务流程伪代码提供了精确的实现逻辑

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: CategoryApplicationService 单元测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=CategoryApplicationServiceTest
    Expected Result: 所有测试通过（create, delete, addModelType, deleteModelType 正常 + 异常场景）
    Failure Indicators: 业务逻辑错误、未捕获异常场景
    Evidence: .sisyphus/evidence/task-10-category-appsvc-test.txt

  Scenario: TagApplicationService 单元测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=TagApplicationServiceTest
    Expected Result: 所有测试通过（create, delete 含级联, addModelTag, removeModelTag 正常 + 异常场景）
    Failure Indicators: 级联删除逻辑缺失、标签数量限制未检查
    Evidence: .sisyphus/evidence/task-10-tag-appsvc-test.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 11. CategoryApi + TagApi 控制器 + API 测试 (TDD)

  **What to do**:
  - TDD RED: 先编写 API 测试（使用 MockMvc）
    - CategoryApi 测试（6 个端点）:
      - GET /v2/ui/categories — 返回分类列表（含 types）
      - GET /v2/ui/categories/{id} — 返回分类详情 / 404
      - POST /v2/ui/categories — 创建成功 / 409 名称重复
      - DELETE /v2/ui/categories/{id} — 删除成功 / 404 / 400 内置
      - POST /v2/ui/categories/{id}/types — 添加类型 / 404 / 409
      - DELETE /v2/ui/categories/{id}/types/{typeId} — 删除类型 / 404 / 400
    - TagApi 测试（5 个端点）:
      - GET /v2/ui/tags — 返回标签列表
      - POST /v2/ui/tags — 创建成功 / 409
      - DELETE /v2/ui/tags/{id} — 删除成功 / 404 / 400 内置
      - POST /v2/ui/models/{modelId}/tags — 为模型添加标签 / 404 / 400 超限
      - DELETE /v2/ui/models/{modelId}/tags/{tagId} — 移除标签 / 404
  - TDD GREEN: 实现 CategoryApi 和 TagApi 控制器（@RestController）
    - 注入对应 ApplicationService
    - DTO → 领域模型转换（内联映射）
    - 返回 BaseResponse 包装
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不在 Controller 中做业务逻辑
  - 不添加 PUT/PATCH 端点
  - 不使用 @Valid 注解（校验在领域模型中）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 11 个 API 端点 + MockMvc 测试，工作量较大
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 12, 13, 14)
  - **Blocks**: Task 20
  - **Blocked By**: Tasks 3, 5, 10

  **References**:

  **Pattern References**:
  - Task 10 的 ApplicationService — Controller 需要调用的方法
  - Task 5 的 DTO 定义 — Controller 使用和返回的数据结构
  - `src/main/java/com/huawei/modellite/repository/common/dto/response/BaseResponse.java` — 响应包装

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 443-899 — 全部 11 个 API 端点的完整定义（URL, Method, Request/Response, 错误码）
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 1760-2382 — 全部 API 测试用例定义

  **WHY Each Reference Matters**:
  - API 端点定义提供了每个接口的精确 URL、HTTP 方法、请求/响应格式
  - API 测试用例定义了需要覆盖的所有测试场景

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Feature 2 全部 API 测试通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=CategoryApiTest,TagApiTest
    Expected Result: 所有 11 个端点测试通过（正常 + 异常场景）
    Failure Indicators: HTTP 状态码错误、响应格式不符、缺少端点
    Evidence: .sisyphus/evidence/task-11-api-tests.txt

  Scenario: API 响应格式符合 BaseResponse 规范
    Tool: Bash
    Steps:
      1. 检查每个成功响应包含 code=0, message="success", data, timestamp, requestId
      2. 检查每个错误响应包含 code=错误码, message=描述
    Expected Result: 所有响应格式正确
    Failure Indicators: 缺少字段、格式不一致
    Evidence: .sisyphus/evidence/task-11-response-format.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 12. 内置预设数据 SQL 脚本

  **What to do**:
  - 创建 `sql/data/V1__builtin_data.sql`（H2 测试用）和 `sql/data/postgresql/V1__builtin_data.sql`（生产用）
  - 插入内置分类（如 TextGeneration, ImageTextToText 等，is_builtin=TRUE）
  - 插入内置模型类型（如 glm-5, Qwen2.5-VL 等，is_builtin=TRUE）
  - 插入内置标签（如 supportFinetune, CAPABILITY 类型，is_builtin=TRUE）
  - 插入内置 model_type_tag 关联
  - 具体数据清单参考设计文档的描述
  - 验证 H2 测试环境可执行此 SQL

  **Must NOT do**:
  - 不硬编码到 Java 代码中（用 SQL 脚本）
  - 不在应用启动时自动执行（由运维/CI 手动执行）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 纯 SQL 数据插入，模式简单
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 10, 11, 13, 14)
  - **Blocks**: Task 20
  - **Blocked By**: Tasks 2, 8, 9

  **References**:

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` lines 188-200 — 内置预设数据机制说明
  - `sql/schema/h2/V1__create_core_tables.sql` — 表结构参考

  **WHY Each Reference Matters**:
  - 设计文档说明了填充机制和数据框架
  - DDL 确保插入数据的表结构正确

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 内置数据 SQL 可执行
    Tool: Bash
    Steps:
      1. mvn test -Dtest=SchemaMigrationTest（验证 SQL 不会破坏 H2 初始化）
      2. 检查 SQL 语法正确性
    Expected Result: H2 可执行内置数据 SQL，无语法错误
    Failure Indicators: SQL 语法错误、外键约束违反
    Evidence: .sisyphus/evidence/task-12-builtin-data.txt
  ```

  **Commit**: NO (groups with Task 20)

- [x] 13. Model 聚合根 + ModelVersion 实体 (TDD)

  **What to do**:
  - TDD RED: 先编写 Model 和 ModelVersion 测试用例
    - Model.createModel（静态工厂）：
      - 正常创建：验证字段正确、自动创建 v1 版本（status=NoWeight, isRegistered=false）
      - name 校验：非空、长度 1-255、字符集 `^[a-zA-Z0-9\u4e00-\u9fa5_-]+$`
      - resourceGroup 校验：非空、≤100
    - Model.modifyMetadata：
      - 正常修改 description, author, seriesName, modelSize, maxSeqLength
      - name 不可修改（由 API 层保证，领域模型不检查）
      - resourceGroup 不可修改
    - Model.createVersion：
      - 正常创建：版本号自动递增
      - 注册模式：status=Available
      - 非注册模式：status=NoWeight
      - 版本容量校验（≤50）
    - Model.getLatestVersionNumber：空列表返回 0
    - Model.getModelVersion：存在/不存在
    - ModelVersion.createFirstVersion：验证 v1 字段
  - TDD GREEN: 实现 Model 聚合根和 ModelVersion 实体
    - Model: modelId, name, description, categoryId, typeId, resourceGroup, createUser, author, seriesName, modelSize, maxSeqLength, versions (List\<ModelVersion\>), tagIds (List\<UUID\>)
    - ModelVersion: versionId, versionNumber, storagePath (StoragePath), weightType, status, isRegistered, isLocked, trainingMetadata (TrainingMetadata)
    - 静态工厂方法 createModel, createVersion
    - 实例方法 modifyMetadata, getLatestVersionNumber, getModelVersion
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不在 Model 中注入 Repository（跨聚合校验由 ModelDomainService 处理）
  - 不实现软删除逻辑
  - 不在 Model 中直接管理 tag 关联（tagIds 仅维护 ID 列表）

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 最复杂的聚合根，包含静态工厂、版本管理、多个业务不变量，TDD 需要仔细设计测试
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 10, 11, 12, 14)
  - **Blocks**: Tasks 15, 16
  - **Blocked By**: Tasks 1, 2, 4, 5

  **References**:

  **Pattern References**:
  - Task 6 的 Category 聚合根模式 — 聚合根+实体的 TDD 模式参考
  - `src/main/java/com/huawei/modellite/repository/common/enums/ErrorCode.java` — 错误码常量

  **API/Type References**:
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 256-440 — Model/ModelVersion 完整类定义、方法伪代码
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 467-481 — 内联校验规则（name 字符集、resourceGroup）
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 567-581 — 所有业务不变量

  **WHY Each Reference Matters**:
  - Model 是最核心的聚合根，方法伪代码提供了精确实现逻辑
  - 内联校验规则定义了 name 的精确正则表达式和约束
  - 业务不变量列表确保不遗漏任何校验

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Model 领域模型单元测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=ModelTest
    Expected Result: 所有测试通过（createModel 正常+校验失败, modifyMetadata 正常, createVersion 注册/非注册+容量校验, getLatestVersionNumber, getModelVersion 存在/不存在）
    Failure Indicators: 校验逻辑缺失、版本号不连续、首版本未自动创建
    Evidence: .sisyphus/evidence/task-13-model-domain-test.txt

  Scenario: ModelVersion 创建验证
    Tool: Bash
    Steps:
      1. mvn test -Dtest=ModelVersionTest
    Expected Result: createFirstVersion 字段正确（versionNumber=1, status=NoWeight, isRegistered=false）
    Failure Indicators: 字段默认值错误
    Evidence: .sisyphus/evidence/task-13-version-entity-test.txt
  ```

  **Commit**: NO (groups with Task 21)

- [x] 14. StoragePath + TrainingMetadata 值对象 (TDD)

  **What to do**:
  - TDD RED: 先编写值对象测试
    - StoragePath.ofPvc(pvcName, internalPath): sourceType=PVC, nfsServer=null, nfsPath=null, pvcName 非空校验
    - StoragePath.ofNfs(nfsServer, nfsPath): sourceType=NFS, pvcName=null, nfsServer 非空, nfsPath 非空
    - StoragePath.empty(): 所有字段 null
    - TrainingMetadata.empty(): 所有字段 null
    - StoragePath PVC 模式 pvcName 为空抛 STORAGE_PATH_PVC_NAME_REQUIRED
    - StoragePath NFS 模式 nfsServer 或 nfsPath 为空抛 STORAGE_PATH_NFS_REQUIRED
  - TDD GREEN: 实现两个值对象
    - StoragePath: sourceType (SourcePathType), pvcName, internalPath, nfsServer, nfsPath
    - TrainingMetadata: trainFrame, trainType, trainStrategy, trainTime, finalLoss, sourceVersion
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不把 StoragePath 放在 common 包（属于 model 聚合内）
  - 不添加不必要的 getter/setter（使用不可变对象模式）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 两个简单值对象，逻辑清晰
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 10, 11, 12, 13)
  - **Blocks**: Task 15
  - **Blocked By**: Tasks 1, 13

  **References**:

  **Pattern References**:
  - Task 1 的 SourcePathType 枚举 — StoragePath 引用的枚举类型

  **API/Type References**:
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 296-329 — StoragePath/TrainingMetadata 类图
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 442-481 — 值对象完整定义（字段、校验规则、工厂方法）
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 607-609 — 错误码 STORAGE_PATH_PVC_NAME_REQUIRED / STORAGE_PATH_NFS_REQUIRED

  **WHY Each Reference Matters**:
  - 值对象定义提供了所有字段类型和校验规则
  - 工厂方法（ofPvc, ofNfs, empty）是核心 API

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 值对象单元测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=StoragePathTest,TrainingMetadataTest
    Expected Result: 所有测试通过（ofPvc, ofNfs, empty + 校验失败场景）
    Failure Indicators: PVC/NFS 模式校验缺失、工厂方法返回错误字段值
    Evidence: .sisyphus/evidence/task-14-value-objects-test.txt
  ```

  **Commit**: NO (groups with Task 21)

- [x] 15. ModelRepository 接口 + MyBatis 实现 + 集成测试 (TDD)

  **What to do**:
  - TDD RED: 先编写 ModelRepository 集成测试（12 个方法）
    - save: 保存 Model（含级联保存 ModelVersion + model_tag）
    - findById: 按 ID 查找（不含版本列表）
    - findByIdWithVersions: 含版本列表（按版本号降序）
    - findVersionById: 按模型 ID + 版本 ID 查找
    - findAll: 查询全部
    - findByCondition: 条件查询（分页、分类/类型/标签/名称筛选、排序）
    - findByResourceGroups: 按资源组列表过滤（RBAC 核心）
    - existsByCategoryAndTypeAndName: 名称唯一性检查
    - countByResourceGroup: 资源组模型计数
    - countAll: 全局模型计数
    - update: 更新模型元数据
    - updateVersion: 更新版本信息
    - findTagIdsByModelId: 查询标签 ID 列表
  - TDD GREEN: 实现完整 Repository 层
    - 定义 `ModelRepository` 接口在 `modelweight/domain/repository/`
    - 创建 `ModelRepositoryImpl` 在 `infrastructure/persistence/`
    - 创建 `ModelMapper.xml` — 复杂 SQL（多表关联、分页、条件查询、RBAC 过滤）
    - 创建 `ModelMapper.java` 接口
    - ResultMap: Model 基本映射 + ModelVersion 嵌套集合映射
  - TDD REFACTOR: 优化 SQL、抽取公共 SQL 片段、提取分页模板

  **Must NOT do**:
  - 不在 SQL 中使用 PostgreSQL 特有语法（H2 需兼容）
  - 不在 Repository 中实现业务逻辑
  - 不使用 @Transactional 跨聚合

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 12 个方法的 MyBatis 实现 + 复杂条件查询 SQL + 分页 + 集成测试，工作量大
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 16, 17, 18)
  - **Blocks**: Task 17
  - **Blocked By**: Tasks 5, 13, 14

  **References**:

  **Pattern References**:
  - Task 8 的 CategoryRepository MyBatis 模式 — 遵循相同模式
  - `src/test/java/com/huawei/modellite/repository/integration/AbstractIntegrationTest.java` — 集成测试基类

  **API/Type References**:
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 521-565 — ModelRepository 接口完整方法签名 + ModelQueryCondition/PageResult 定义
  - `sql/schema/h2/V1__create_core_tables.sql` — model, model_version 表结构

  **WHY Each Reference Matters**:
  - ModelRepository 是最复杂的 Repository（12 方法 + 分页 + RBAC），方法签名必须精确
  - ModelQueryCondition 和 PageResult 定义了分页查询的输入输出格式

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: ModelRepository 集成测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=ModelRepositoryTest
    Expected Result: 所有 12 个方法测试通过
    Failure Indicators: SQL 错误、分页计算错误、RBAC 过滤错误
    Evidence: .sisyphus/evidence/task-15-model-repo-test.txt

  Scenario: 分页查询正确性验证
    Tool: Bash
    Steps:
      1. 插入 10 个模型（不同分类/类型/资源组）
      2. 测试按 categoryId 筛选 → 返回正确子集
      3. 测试分页 page=1, pageSize=5 → 返回 5 条 + total=10
      4. 测试按 resourceGroups 过滤 → 只返回匹配的模型
    Expected Result: 分页、筛选、RBAC 过滤全部正确
    Failure Indicators: 分页计算错误、筛选条件无效、RBAC 未生效
    Evidence: .sisyphus/evidence/task-15-pagination-test.txt
  ```

  **Commit**: NO (groups with Task 21)

- [x] 16. ModelDomainService 领域服务 (TDD)

  **What to do**:
  - TDD RED: 先编写 ModelDomainService 测试（使用 Mockito mock CategoryRepository, TagRepository, ModelRepository）
    - validateModelCreation:
      - 分类不存在 → CATEGORY_NOT_FOUND
      - 类型不属于该分类 → MODEL_TYPE_NOT_BELONG_TO_CATEGORY
      - 名称重复 → MODEL_NAME_EXISTS
      - 资源组容量超限（≥100）→ MODEL_CAPACITY_EXCEEDED
      - 全局容量超限（≥1000）→ MODEL_GLOBAL_CAPACITY_EXCEEDED
      - 全部通过 → 正常返回
    - validateModelModification:
      - 模型不存在 → MODEL_NOT_FOUND
      - categoryId 变更 + 新类型不属于新分类 → MODEL_TYPE_NOT_BELONG_TO_CATEGORY
      - categoryId 变更 + 新分类+类型下名称重复 → MODEL_NAME_EXISTS
      - 全部通过 → 正常返回
  - TDD GREEN: 实现 ModelDomainService
    - 注入 CategoryRepository, ModelRepository, TagRepository
    - 实现 validateModelCreation, validateModelModification
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不在领域服务中做持久化操作
  - 不使用 @Transactional
  - 不在领域服务中创建 Model 实例（只做校验）

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 跨聚合校验逻辑复杂，需要理解多个聚合的交互规则
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 15, 17, 18)
  - **Blocks**: Task 17
  - **Blocked By**: Tasks 8, 9, 13

  **References**:

  **Pattern References**:
  - Task 8/9 的 CategoryRepository/TagRepository 接口 — 需要调用的方法
  - Task 10 的 ApplicationService 模式 — 类似的 Mockito 测试模式

  **API/Type References**:
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 484-519 — ModelDomainService 完整方法定义和伪代码
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 567-581 — 所有业务不变量

  **WHY Each Reference Matters**:
  - 伪代码提供了跨聚合校验的精确步骤和错误抛出条件
  - 业务不变量确保所有校验规则不遗漏

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: ModelDomainService 单元测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=ModelDomainServiceTest
    Expected Result: 所有校验场景测试通过（创建校验 5 种异常 + 修改校验 3 种异常 + 正常流程）
    Failure Indicators: 校验逻辑遗漏、错误码不匹配
    Evidence: .sisyphus/evidence/task-16-domain-service-test.txt
  ```

  **Commit**: NO (groups with Task 21)

- [x] 17. ModelApplicationService 应用服务 (TDD)

  **What to do**:
  - TDD RED: 先编写 ApplicationService 测试（使用 Mockito）
    - createModel: 正常创建 + 各种校验失败场景
    - getModel: 正常获取 / MODEL_NOT_FOUND
    - modifyModel: 正常修改 / MODEL_NOT_FOUND / MODEL_RESOURCE_GROUP_IMMUTABLE
    - listModels: 正常分页查询 + RBAC 过滤
    - createVersion: 注册模式（Available）/ 非注册模式（NoWeight） / VERSION_CAPACITY_EXCEEDED / STORAGE_PATH 校验
    - getVersion: 正常获取 / VERSION_NOT_FOUND
  - TDD GREEN: 实现 ModelApplicationService
    - 注入 ModelRepository, ModelDomainService, TagRepository, CategoryRepository
    - createModel: 调用 DomainService 校验 → Model.createModel → repository.save → 创建 model_tag 关联
    - modifyModel: 调用 DomainService 校验 → model.modifyMetadata → repository.update → 更新 model_tag
    - listModels: 根据 resourceGroups 调用 repository.findByResourceGroups 或 findByCondition
    - createVersion: model.createVersion → repository.save（NFS 模式生成 PVC 名称）
    - getVersion: repository.findVersionById
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不使用 @Transactional 跨聚合
  - 不在 ApplicationService 中直接操作数据库
  - 不实现真实 RBAC（用参数模拟 resourceGroup）

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 6 个核心方法，编排多个 Repository 和 DomainService，逻辑较复杂
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 4 (sequential after Tasks 15, 16)
  - **Blocks**: Task 18
  - **Blocked By**: Tasks 3, 15, 16

  **References**:

  **Pattern References**:
  - Task 10 的 CategoryApplicationService 模式 — 类似的编排逻辑
  - Task 13 的 Model 聚合根方法 — 需要调用的工厂方法和实例方法
  - Task 16 的 ModelDomainService — 需要调用的校验方法

  **API/Type References**:
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 1150-1350 (approx) — ApplicationService 序列图和业务流程
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 365-418 — Model 方法伪代码（createModel, createVersion, modifyMetadata）

  **WHY Each Reference Matters**:
  - 序列图展示了完整的编排流程（校验 → 创建 → 保存 → 关联）
  - Model 方法伪代码提供了 ApplicationService 需要调用的精确方法签名

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: ModelApplicationService 单元测试全通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=ModelApplicationServiceTest
    Expected Result: 所有 6 个方法测试通过（正常 + 异常场景）
    Failure Indicators: 编排逻辑错误、校验未调用、保存失败
    Evidence: .sisyphus/evidence/task-17-model-appsvc-test.txt

  Scenario: 创建模型含首版本验证
    Tool: Bash
    Steps:
      1. 调用 createModel
      2. 验证返回的 Model 包含 versionNumber=1 的 ModelVersion（status=NoWeight）
    Expected Result: 模型创建后自动包含首版本
    Failure Indicators: 首版本缺失、版本号错误、状态错误
    Evidence: .sisyphus/evidence/task-17-first-version.txt
  ```

  **Commit**: NO (groups with Task 21)

- [x] 18. ModelApi 控制器 + API 测试 (TDD)

  **What to do**:
  - TDD RED: 先编写 API 测试（使用 MockMvc）
    - POST /v2/ui/models — 创建成功 / 400 校验失败 / 409 名称冲突
    - GET /v2/ui/models/{id} — 获取详情 / 404
    - PATCH /v2/ui/models/{id} — 修改成功 / 404 / 400 resourceGroup 不可修改
    - GET /v2/ui/models — 分页查询 + 分类/类型/标签筛选 + RBAC
    - POST /v2/ui/models/{id}/versions — 创建版本（注册/非注册） / 404 / 400 容量超限
    - GET /v2/ui/models/{id}/versions/{versionId} — 获取版本详情 / 404
  - TDD GREEN: 实现 ModelApi 控制器（@RestController）
    - 注入 ModelApplicationService
    - DTO ↔ 领域模型转换（内联映射）
    - 返回 BaseResponse 包装
  - TDD REFACTOR: 代码清理

  **Must NOT do**:
  - 不在 Controller 中做业务逻辑
  - 不使用 @Valid 注解
  - 不暴露 name 和 resourceGroup 修改字段

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: 6 个 API 端点 + MockMvc 测试
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 4 (sequential after Task 17)
  - **Blocks**: Task 21
  - **Blocked By**: Tasks 3, 5, 17

  **References**:

  **Pattern References**:
  - Task 11 的 CategoryApi/TagApi 模式 — 遵循相同模式

  **API/Type References**:
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 613-1100 — 全部 6 个 API 端点完整定义
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` lines 1800-2100 (approx) — 全部 API 测试用例

  **WHY Each Reference Matters**:
  - API 端点定义提供了精确的 URL、HTTP 方法、请求/响应格式
  - API 测试用例定义了完整的测试场景

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Feature 3 全部 API 测试通过
    Tool: Bash
    Steps:
      1. mvn test -Dtest=ModelApiTest
    Expected Result: 所有 6 个端点测试通过（正常 + 异常场景）
    Failure Indicators: HTTP 状态码错误、响应格式不符
    Evidence: .sisyphus/evidence/task-18-model-api-tests.txt

  Scenario: PATCH 不暴露 name/resourceGroup
    Tool: Bash
    Steps:
      1. 发送 PATCH /v2/ui/models/{id} 包含 name 和 resourceGroup 字段
      2. 验证这两个字段被忽略或返回 400 MODEL_RESOURCE_GROUP_IMMUTABLE
    Expected Result: name/resourceGroup 不可通过 API 修改
    Failure Indicators: name 或 resourceGroup 被修改
    Evidence: .sisyphus/evidence/task-18-immutable-fields.txt
  ```

  **Commit**: NO (groups with Task 21)

- [x] 19. Git commit — Feature 1 同步

  **What to do**:
  - 使用 `/git-master` skill 创建 commit
  - 验证所有 Feature 1 相关变更已暂存：DDL 更新、SourcePathType 枚举 + TypeHandler、ErrorCode 扩展、GlobalExceptionHandler、DTO 公共类、pom.xml
  - 运行 `mvn test` 确保所有测试通过
  - 创建 commit: `feat(infrastructure): sync Feature 1 — add model_version storage columns, SourcePathType enum, error codes 0102015-0102034, GlobalExceptionHandler`
  - 此 commit 应在 Feature 2/3 开发之前创建，作为干净的基线

  **Must NOT do**:
  - 不包含 Feature 2/3 的代码
  - 不 force push

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: git commit 操作
  - **Skills**: [`git-master`]
    - `git-master`: git 操作规范

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 20, 21)
  - **Blocks**: Tasks 22, 23
  - **Blocked By**: Tasks 1, 2, 3, 4, 5

  **References**:

  **Pattern References**:
  - 无需代码模式参考

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Feature 1 commit 验证
    Tool: Bash
    Steps:
      1. git log -1 — 验证最新 commit 消息正确
      2. git diff HEAD~1 --stat — 验证变更文件列表正确
      3. git stash && mvn test && git stash pop — 验证测试通过
    Expected Result: commit 消息包含 feat(infrastructure)，变更文件包含 DDL、枚举、TypeHandler、ErrorCode、GlobalExceptionHandler
    Evidence: .sisyphus/evidence/task-19-f1-commit.txt
  ```

  **Commit**: YES
  - Message: `feat(infrastructure): sync Feature 1 — add model_version storage columns, SourcePathType enum, error codes 0102015-0102034, GlobalExceptionHandler`
  - Files: Wave 1 所有变更文件
  - Pre-commit: `mvn test`

- [x] 20. Git commit — Feature 2 完整

  **What to do**:
  - 验证所有 Feature 2 相关变更已暂存：Category/Tag 聚合根、Repository 接口 + MyBatis 实现 + XML mapper、ApplicationService、API Controller、内置数据 SQL
  - 运行 `mvn test` 确保所有测试通过
  - 创建 commit: `feat(category-tag): implement Feature 2 — Category/Tag aggregates, repositories, application services, API controllers, builtin data`

  **Must NOT do**:
  - 不包含 Feature 3 的代码
  - 不 force push

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`]

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 19, 21)
  - **Blocks**: Tasks 22, 23
  - **Blocked By**: Tasks 11, 12

  **References**: 无

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Feature 2 commit 验证
    Tool: Bash
    Steps:
      1. git log -1 — 验证 commit 消息
      2. git diff HEAD~1 --stat — 验证变更文件包含 category/, tag/, CategoryRepository*, TagRepository*, *ApplicationService*, *Api*, builtin data SQL
      3. mvn test — 全部通过
    Expected Result: commit 正确，测试全通过
    Evidence: .sisyphus/evidence/task-20-f2-commit.txt
  ```

  **Commit**: YES
  - Message: `feat(category-tag): implement Feature 2 — Category/Tag aggregates, repositories, application services, API controllers, builtin data`
  - Files: Wave 2-3 中 Feature 2 相关所有文件
  - Pre-commit: `mvn test`

- [x] 21. Git commit — Feature 3 完整

  **What to do**:
  - 验证所有 Feature 3 相关变更已暂存：Model 聚合根、ModelVersion 实体、值对象、ModelDomainService、Repository、ApplicationService、API Controller
  - 运行 `mvn test` 确保所有测试通过
  - 创建 commit: `feat(model-version): implement Feature 3 — Model aggregate, ModelVersion entity, domain service, repository, application service, API controller`

  **Must NOT do**:
  - 不 force push

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`]

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 19, 20)
  - **Blocks**: Tasks 22, 23
  - **Blocked By**: Task 18

  **References**: 无

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Feature 3 commit 验证
    Tool: Bash
    Steps:
      1. git log -1 — 验证 commit 消息
      2. git diff HEAD~1 --stat — 验证变更文件包含 model/, ModelDomainService*, ModelRepository*, *ApplicationService*, *Api*, *QueryCondition*, *PageResult*
      3. mvn test — 全部通过
    Expected Result: commit 正确，测试全通过
    Evidence: .sisyphus/evidence/task-21-f3-commit.txt
  ```

  **Commit**: YES
  - Message: `feat(model-version): implement Feature 3 — Model aggregate, ModelVersion entity, domain service, repository, application service, API controller`
  - Files: Wave 3-4 中 Feature 3 相关所有文件
  - Pre-commit: `mvn test`

- [x] 22. 需求文档对照检查

  **What to do**:
  - 逐一对照 `.sisyphus/drafts/feature-2-category-tag-design.md` 和 `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md`
  - 检查清单：
    - Feature 2: 11 个 API 端点是否全部实现
    - Feature 2: CategoryRepository 10 个方法是否全部实现
    - Feature 2: TagRepository 16 个方法是否全部实现
    - Feature 2: 所有业务不变量是否已强制执行
    - Feature 2: 内置数据保护（is_builtin）是否生效
    - Feature 2: 标签级联删除是否正确
    - Feature 3: 6 个 API 端点是否全部实现
    - Feature 3: ModelRepository 12 个方法是否全部实现
    - Feature 3: ModelDomainService 2 个校验方法是否完整
    - Feature 3: 容量限制（100/50/1000）是否执行
    - Feature 3: 版本号递增无间隔是否保证
    - Feature 3: 资源组可见性（RBAC）是否实现
    - Feature 3: StoragePath PVC/NFS 模式校验是否正确
  - 输出检查报告到 `.sisyphus/evidence/task-22-requirement-check.md`

  **Must NOT do**:
  - 不做代码修改（仅检查和报告）

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 需要仔细对照两份设计文档的所有细节
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Task 23)
  - **Blocks**: FINAL
  - **Blocked By**: Tasks 19, 20, 21

  **References**:

  **API/Type References**:
  - `.sisyphus/drafts/feature-2-category-tag-design.md` — Feature 2 完整设计文档
  - `.sisyphus/drafts/feature-3-model-version-lifecycle-design.md` — Feature 3 完整设计文档

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 需求覆盖率检查
    Tool: Bash
    Steps:
      1. 遍历设计文档中的每个 API 端点，验证代码中存在对应实现
      2. 遍历设计文档中的每个 Repository 方法，验证代码中存在对应实现
      3. 遍历设计文档中的每个业务不变量，验证代码中有对应校验逻辑
    Expected Result: 100% 需求覆盖，无遗漏
    Failure Indicators: 缺少端点、缺少方法、缺少校验
    Evidence: .sisyphus/evidence/task-22-requirement-check.md
  ```

  **Commit**: NO (仅检查报告)

- [x] 23. Tech Design 代码规范检查

  **What to do**:
  - 对照 `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md` 检查代码规范
  - 检查清单：
    - 包结构是否符合 DDD 规范（modelweight.domain.aggregate.{category|tag|model}）
    - 类命名是否正确（聚合根无后缀、ApplicationService 后缀、MyBatis 前缀 + Repository 后缀）
    - MyBatis XML mapper 是否在 `resources/mapper/` 目录
    - 错误码格式是否为 0102YYY
    - 是否存在跨聚合 @Transactional
    - 是否存在 javax.validation 注解在 DTO 上
    - 是否存在独立的 DTO 转换器类
    - SQL 脚本是否按 DB 类型分目录
    - 是否修改了已有 SourceType 枚举
    - 0102024 是否标记为废弃
  - 输出检查报告到 `.sisyphus/evidence/task-23-techdesign-check.md`

  **Must NOT do**:
  - 不做代码修改（仅检查和报告）

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 需要全面检查代码规范，使用 grep/ast_grep 等工具
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Task 22)
  - **Blocks**: FINAL
  - **Blocked By**: Tasks 19, 20, 21

  **References**:

  **API/Type References**:
  - `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md` — Tech Design 完整文档

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 代码规范检查
    Tool: Bash
    Steps:
      1. 检查包结构：find src/main -type f -name "*.java" | grep -E "aggregate|repository|application|api" 
      2. 检查 @Transactional：grep -r "@Transactional" src/main
      3. 检查 @Valid：grep -r "@Valid\|@NotNull\|@NotBlank" src/main
      4. 检查 SourceType 未被修改：git diff HEAD~3 src/main/**/SourceType.java
      5. 检查 0102024 注释：grep "0102024" src/main/**/ErrorCode.java
    Expected Result: 所有规范检查通过
    Failure Indicators: 包结构错误、禁止使用的注解、SourceType 被修改
    Evidence: .sisyphus/evidence/task-23-techdesign-check.md
  ```

  **Commit**: NO (仅检查报告)

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .sisyphus/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `mvn compile` + `mvn test`. Review all changed files for: anti-patterns, empty catches, console.log in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names. Verify TDD compliance: test files exist for every domain class.
  Output: `Build [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high`
  Start from clean state (`mvn clean test`). Execute EVERY QA scenario from EVERY task. Test cross-feature integration (model with category/tag references working). Save to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff. Verify 1:1 — everything in spec was built, nothing beyond spec was built. Check "Must NOT do" compliance. Detect cross-task contamination. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **Task 19 (Feature 1 Sync)**: `feat(infrastructure): sync Feature 1 — add model_version storage columns, SourcePathType enum, error codes 0102015-0102034, GlobalExceptionHandler` — `sql/schema/**, **/SourcePathType.java, **/SourcePathTypeTypeHandler.java, **/ErrorCode.java, **/GlobalExceptionHandler.java, pom.xml`
  Pre-commit: `mvn test`
  
- **Task 20 (Feature 2 Complete)**: `feat(category-tag): implement Feature 2 — Category/Tag aggregates, repositories, application services, API controllers, builtin data` — `**/category/**, **/tag/**, **/CategoryRepository*, **/TagRepository*, **/CategoryApplicationService*, **/TagApplicationService*, **/CategoryApi*, **/TagApi*, sql/data/**`
  Pre-commit: `mvn test`

- **Task 21 (Feature 3 Complete)**: `feat(model-version): implement Feature 3 — Model aggregate, ModelVersion entity, domain service, repository, application service, API controller` — `**/model/**, **/ModelDomainService*, **/ModelRepository*, **/ModelApplicationService*, **/ModelApi*, **/ModelQueryCondition*, **/PageResult*`
  Pre-commit: `mvn test`

---

## Success Criteria

### Verification Commands
```bash
mvn clean test                    # Expected: BUILD SUCCESS, 0 failures
mvn compile                       # Expected: BUILD SUCCESS
grep -r "@Transactional" src/     # Expected: only within single-aggregate scopes
grep -r "0102024" src/            # Expected: only as deprecated comment
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] All tests pass (`mvn test`)
- [ ] Feature 2: 11 API endpoints functional
- [ ] Feature 3: 6 API endpoints functional
- [ ] Error codes 0102015-0102034 defined with correct HTTP status mapping
- [ ] SourcePathType enum separate from SourceType
- [ ] GlobalExceptionHandler catches ModelLiteException → BaseResponse.error()
- [ ] TDD: test files exist for every domain/service class
- [ ] 3 git commits with descriptive messages
- [ ] Code follows tech design package structure and naming conventions
