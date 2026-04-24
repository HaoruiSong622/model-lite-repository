# Feature 1: 基础设施与通用能力 — 详细设计文档

> **文档类型**: 特性详细设计
> **文档版本**: v1.0
> **编写日期**: 2026-04-24
> **适用范围**: ModelLite 平台模型仓库模块 Feature 1
> **目标读者**: 架构师、后端开发工程师

---

## TL;DR

> **Quick Summary**: Feature 1 是模型仓库模块的技术底座，包含数据库 Schema、项目骨架、公共模块 Mock、枚举定义、异常体系、配置管理等基础设施能力。
>
> **Deliverables**:
> - 数据库 Schema（DDL + 初始化脚本）
> - 项目骨架代码（包结构、Maven 配置）
> - Mock 公共模块（BaseResponse）
> - 版本状态枚举 + 任务状态枚举 + 锁类型枚举
> - 错误码定义（0102YYY）
> - MyBatis 配置 + Druid 连接池配置
> - 日志轮转配置（Logback）
> - 文件后缀白名单 ConfigMap
> - SSL/TLS 基础设施配置
> - K8s Deployment 基础配置
>
> **Estimated Effort**: Large
> **Parallel Execution**: YES - 4 waves
> **Critical Path**: T1 → T3 → T4 → T7 → T8

---

## Context

### Original Request

用户需要对 ModelLite 模型仓库模块进行重构/重写，采用 DDD 架构。Feature 1 作为第一个特性，负责搭建整个模块的技术底座，为后续特性（分类体系、模型管理、权重导入等）提供基础设施能力。

### Interview Summary

**Key Discussions**:
- 技术栈: Java 21 + Spring Boot 3.4.5 + PostgreSQL + MyBatis + Druid
- 包名: `com.huawei.modellite.repository`
- 错误码格式: `0102YYY`
- 公共模块: 已有 `com.huawei.modellite.common`，需要在本仓库 Mock BaseResponse
- 数据库迁移: 手动 SQL 脚本
- 内置数据: Feature 1 先留空，Feature 2 再填充
- 领域事件: 仅作文档记录，不实现事件发布/订阅

**Research Findings**:
- 旧版本状态码（0-7 整数）→ 新版本状态枚举字符串
- 旧版本 String ID → 新版本 UUID
- 旧版本无版本锁 → 新版本新增 version_lock 表
- 旧版本无纳管概念 → 新版本 ModelVersion 增加 is_registered 字段
- Tag 在新版本提升为独立聚合

### Metis Review

**Identified Gaps** (addressed):
- **公共模块 Mock**: 需要在本仓库创建 Mock 模块模拟 BaseResponse → 已确认方案
- **初始化数据**: Feature 1 先留空 → 已确认
- **边缘情况**: Model 表唯一约束需要考虑软删除 → 在 DDL 中使用 WHERE deleted = FALSE 的部分索引
- **枚举 TypeHandler**: MyBatis 需要自定义 TypeHandler 处理枚举 → 在设计中包含

---

## Work Objectives

### Core Objective

搭建模型仓库模块的技术底座，提供数据库 Schema、项目骨架、公共能力（枚举、异常、配置），为后续特性开发提供可复用的基础设施。

### Concrete Deliverables

1. **数据库 Schema**: 8 张核心表的 DDL 脚本
2. **项目骨架**: Maven 项目结构 + Spring Boot 配置
3. **Mock 公共模块**: BaseResponse 类
4. **枚举定义**: VersionStatus, TaskStatus, LockType, SourceType, TagType
5. **错误码定义**: 0102001-0105002 共 12 个错误码
6. **MyBatis 配置**: mapper-locations + TypeHandler + 配置文件
7. **Druid 配置**: 连接池 + 监控（生产环境关闭 stat-view-servlet）
8. **Logback 配置**: 日志轮转 + 级别配置
9. **ConfigMap**: 文件后缀白名单配置
10. **SSL 配置**: SSL/TLS 双向证书认证基础设施
11. **K8s Deployment**: Deployment YAML + Service YAML

### Definition of Done

- [ ] 数据库 Schema 脚本可执行，所有表创建成功
- [ ] 项目骨架可启动，健康检查接口返回 200
- [ ] Mock 模块可被 repository 模块引用
- [ ] 所有枚举类编译通过，包含完整的值列表
- [ ] 错误码文档与代码定义一致
- [ ] MyBatis 可连接数据库，执行简单查询
- [ ] Druid 连接池配置生效，监控端点可访问（仅开发环境）
- [ ] 日志文件按配置轮转，不无限增长
- [ ] ConfigMap 模板文件存在，可被 K8s 挂载
- [ ] SSL 配置文件存在，证书路径可配置
- [ ] K8s Deployment YAML 可部署成功

### Must Have

- 数据库 Schema 包含所有 8 张核心表
- 项目骨架遵循 DDD 包结构（modelweight/weighttask/infrastructure/api/common）
- VersionStatus 枚举包含 NoWeight/Uploading/Available/UploadFailed/ValidationFailed/Error
- 错误码覆盖模型、版本、上传、转换 4 个业务域
- MyBatis 使用 XML 方式
- Logback 配置日志轮转

### Must NOT Have (Guardrails)

- **不创建领域事件代码**: 领域事件仅作文档记录，不创建 Event 类或 Handler
- **不创建 Repository 接口**: Feature 1 不涉及仓储接口定义（Feature 3 才开始）
- **不创建 Application Service**: Feature 1 不涉及应用服务代码
- **不创建 API Controller**: Feature 1 不涉及接口层代码（仅健康检查）
- **不配置全局异常处理器**: Feature 1 仅定义异常体系，不实现 GlobalExceptionHandler（Feature 3）
- **不配置 Security Filter Chain**: SSL 仅基础设施配置，不实现 Spring Security（Feature 3）
- **不实现审计日志上报**: 仅配置 LogReporter 接口预留位置，不实现（Feature 8）
- **不填充内置数据**: 分类/类型/标签数据在 Feature 2 填充，Feature 1 Schema 先留空
- **不创建 K8s Job 相关配置**: 任务调度配置在 Feature 4，Feature 1 仅 Deployment

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed. No exceptions.

### Test Decision

- **Infrastructure exists**: NO（新建项目）
- **Automated tests**: NO（Feature 1 主要是配置和骨架，单元测试价值有限）
- **Framework**: 无（Feature 1 不需要测试框架）
- **Agent-Executed QA**: ALWAYS（通过命令验证配置生效）

### QA Policy

Feature 1 的验证主要通过命令验证配置生效：
- 数据库 Schema: 执行 SQL 脚本后查询表结构
- 项目骨架: 启动后访问健康检查接口
- 枚举定义: 编译检查
- MyBatis: 执行简单查询验证连接
- Druid: 访问监控端点（开发环境）
- Logback: 检查日志文件生成和轮转

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - 项目骨架 + Mock):
├── Task 1: Maven 项目结构 + pom.xml 配置 [quick]
├── Task 2: Spring Boot 主类 + application.yml [quick]
├── Task 3: Mock 公共模块（BaseResponse） [quick]
└── Task 4: 包结构创建（空目录 + package-info.java） [quick]

Wave 2 (After Wave 1 - 枚举 + 异常体系):
├── Task 5: VersionStatus 枚举 + TaskStatus 枚举 + 其他枚举 [quick]
├── Task 6: 错误码定义（ErrorCode 类） [quick]
├── Task 7: ModelLiteException 扩展（如需） [quick]
└── Task 8: 全局异常处理器预留位置 [quick]

Wave 3 (After Wave 2 - 数据库 + MyBatis):
├── Task 9: 数据库 Schema DDL（8 张表） [unspecified-high]
├── Task 10: MyBatis 配置 + TypeHandler [quick]
├── Task 11: Druid 连接池配置 [quick]
└── Task 12: 数据库连接测试 [quick]

Wave 4 (After Wave 3 - 配置 + 运维):
├── Task 13: Logback 日志轮转配置 [quick]
├── Task 14: 文件后缀白名单 ConfigMap [quick]
├── Task 15: SSL/TLS 基础设施配置 [quick]
├── Task 16: K8s Deployment + Service YAML [quick]
└── Task 17: 健康检查接口 [quick]

Wave FINAL (After ALL tasks — 4 parallel reviews):
├── Task F1: Schema DDL 完整性检查（oracle）
├── Task F2: 配置一致性检查（unspecified-high）
├── Task F3: 编译与启动验证（unspecified-high）
└── Task F4: DDD 包结构合规检查（deep）
-> Present results -> Get explicit user okay

Critical Path: T1 → T3 → T9 → T10 → T12 → F3
Parallel Speedup: ~60% faster than sequential
Max Concurrent: 4 (Wave 1 & 2)
```

### Dependency Matrix

| Task | Depends On | Blocks |
|------|------------|--------|
| 1 | - | 2, 3, 4, 5, 6, 9, 10, 13, 14, 15, 16 |
| 2 | 1 | 17, F3 |
| 3 | 1 | 7 |
| 4 | 1 | F4 |
| 5 | 1 | 10 |
| 6 | 1 | 7 |
| 7 | 3, 6 | F3 |
| 8 | 1 | - |
| 9 | 1 | 10, 12 |
| 10 | 1, 5, 9 | 12 |
| 11 | 1 | 12 |
| 12 | 9, 10, 11 | F3 |
| 13 | 1 | - |
| 14 | 1 | - |
| 15 | 1 | - |
| 16 | 1 | F3 |
| 17 | 2 | F3 |

### Agent Dispatch Summary

- **Wave 1**: 4 tasks → `quick` (T1-T4)
- **Wave 2**: 4 tasks → `quick` (T5-T8)
- **Wave 3**: 4 tasks → `unspecified-high` (T9), `quick` (T10-T12)
- **Wave 4**: 5 tasks → `quick` (T13-T17)
- **FINAL**: 4 tasks → `oracle` (F1), `unspecified-high` (F2, F3), `deep` (F4)

---

## TODOs

> Implementation + Test = ONE Task. Never separate.
> EVERY task MUST have: Recommended Agent Profile + Parallelization info + QA Scenarios.

- [ ] 1. Maven 项目结构 + pom.xml 配置

  **What to do**:
  - 创建 Maven 项目根目录结构
  - 编写根 pom.xml，引入 Spring Boot 3.4.5 parent
  - 定义 properties: `<java.version>21</java.version>`
  - 引入核心依赖:
    - `spring-boot-starter-web`
    - `spring-boot-starter-actuator`
    - `mybatis-spring-boot-starter`（版本兼容 Spring Boot 3.4.5）
    - `druid-spring-boot-starter`
    - `postgresql` driver
    - `spring-boot-starter-test`（test scope）
    - `mockito-core`（test scope）
    - `testcontainers` + `postgresql` testcontainer（test scope）
    - `lombok`（可选，如果团队使用）
  - 引入 `com.huawei.modellite.common` 公共模块依赖
  - 配置 Maven Compiler Plugin: source/target 21
  - 模块结构: 先单模块（repository），mock-common 作为同级目录

  **Must NOT do**:
  - 不创建多个 Maven 子模块（mock-common 目录仅做模拟，不作为正式子模块）
  - 不引入 Spring Data JPA（使用 MyBatis）
  - 不引入 spring-boot-starter-security（Feature 3 再配置）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 纯配置文件创建，无复杂业务逻辑
  - **Skills**: []
  - **Skills Evaluated but Omitted**:
    - `git-master`: 不涉及 git 操作

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4)
  - **Blocks**: Tasks 2, 3, 4, 5, 6, 9, 10, 13, 14, 15, 16
  - **Blocked By**: None (can start immediately)

  **References**:
  **Pattern References**:
  - 旧版本项目结构: `docs/project-understanding/project-understanding-summary.md:9-38` — Maven 项目结构参考

  **API/Type References**:
  - 技术栈版本: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:36-49` — Spring Boot 3.4.5, Java 21, MyBatis, Druid, Testcontainers
  - 包名决策: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:123-125` — `com.huawei.modellite.repository`
  - 公共模块依赖: `com.huawei.modellite.common` — Maven coordinates

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Maven 项目编译成功
    Tool: Bash
    Preconditions: Java 21 已安装, Maven 已安装
    Steps:
      1. 执行 `mvn clean compile`
      2. 检查输出包含 "BUILD SUCCESS"
    Expected Result: 编译成功，无错误
    Failure Indicators: 编译失败, 依赖下载失败
    Evidence: .sisyphus/evidence/task-1-maven-compile.txt

  Scenario: pom.xml 依赖版本正确
    Tool: Bash
    Preconditions: pom.xml 已创建
    Steps:
      1. 执行 `mvn dependency:tree | grep spring-boot-starter`
      2. 验证 spring-boot 版本为 3.4.5
      3. 执行 `mvn dependency:tree | grep mybatis`
      4. 验证 mybatis-spring-boot-starter 存在
      5. 执行 `mvn dependency:tree | grep druid`
      6. 验证 druid-spring-boot-starter 存在
      7. 执行 `mvn dependency:tree | grep postgresql`
      8. 验证 postgresql driver 存在
    Expected Result: 所有核心依赖正确引入
    Evidence: .sisyphus/evidence/task-1-dependency-tree.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `feat(infra): add project skeleton and maven configuration`
  - Files: `pom.xml, src/main/java/com/huawei/modellite/repository/`
  - Pre-commit: `mvn clean compile`

- [ ] 2. Spring Boot 主类 + application.yml

  **What to do**:
  - 创建 Spring Boot 主类 `RepositoryApplication.java`，放在 `com.huawei.modellite.repository` 包下
  - 创建 `application.yml` 公共配置:
    ```yaml
    server:
      port: 8080
    spring:
      application:
        name: model-lite-repository
    mybatis:
      mapper-locations: classpath:mapper/*.xml
      configuration:
        map-underscore-to-camel-case: true
    ```
  - 创建 `application-dev.yml`:
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/modellite_dev
        username: dev_user
        password: dev_pass
        driver-class-name: org.postgresql.Driver
        type: com.alibaba.druid.pool.DruidDataSource
    logging:
      level:
        com.huawei.modellite: DEBUG
    ```
  - 创建 `application-prod.yml`:
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://${DB_HOST}:5432/modellite
        username: ${DB_USER}
        password: ${DB_PASSWORD}
        driver-class-name: org.postgresql.Driver
        type: com.alibaba.druid.pool.DruidDataSource
    logging:
      level:
        com.huawei.modellite: INFO
    ```
  - 创建 `application-test.yml`:
    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/modellite_test
        username: test_user
        password: test_pass
        driver-class-name: org.postgresql.Driver
    ```

  **Must NOT do**:
  - 不在 application.yml 中配置具体业务参数
  - 不配置 spring.datasource（放在 profile 文件中）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准 Spring Boot 配置文件创建
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 4)
  - **Blocks**: Tasks 17, F3
  - **Blocked By**: Task 1

  **References**:
  **Pattern References**:
  - 配置结构: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:509-548` — Spring Profile 配置模板

  **API/Type References**:
  - MyBatis 配置: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:345-349` — mapper-locations, map-underscore-to-camel-case

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: application.yml 配置文件格式正确
    Tool: Bash
    Preconditions: Task 1 完成
    Steps:
      1. 读取 application.yml，验证包含 server.port=8080
      2. 读取 application.yml，验证包含 mybatis.mapper-locations
      3. 读取 application-dev.yml，验证包含 spring.datasource.url
      4. 读取 application-prod.yml，验证包含 ${DB_HOST} 环境变量
      5. 读取 application-test.yml，验证包含 spring.datasource.url
    Expected Result: 所有配置文件格式正确，key-value 完整
    Evidence: .sisyphus/evidence/task-2-config-verify.txt

  Scenario: Spring Boot 主类存在
    Tool: Bash
    Preconditions: Task 1 完成
    Steps:
      1. 验证文件 src/main/java/com/huawei/modellite/repository/RepositoryApplication.java 存在
      2. 验证文件包含 @SpringBootApplication 注解
      3. 验证文件包含 main 方法
    Expected Result: 主类存在且格式正确
    Evidence: .sisyphus/evidence/task-2-mainclass.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: (grouped with Task 1)
  - Files: `RepositoryApplication.java, application*.yml`

- [ ] 3. Mock 公共模块（BaseResponse）

  **What to do**:
  - 在项目根目录创建 `mock-common/` 目录（不是 Maven 子模块，仅做模拟）
  - 创建 `com.huawei.modellite.common` 包下的 `BaseResponse.java`:
    ```java
    package com.huawei.modellite.common;

    /**
     * 统一响应格式（Mock）
     * 真实实现在 com.huawei.modellite.common 模块中
     */
    public class BaseResponse<T> {
        private int code;
        private String message;
        private T data;
        private String timestamp;
        private String requestId;

        // 构造方法
        public static <T> BaseResponse<T> success(T data) { ... }
        public static <T> BaseResponse<T> error(int code, String message) { ... }

        // getter/setter
    }
    ```
  - 在根 pom.xml 中添加 mock-common 作为本地依赖或 system scope
  - 或者在 pom.xml 中添加本地文件依赖指向 mock-common
  - **推荐方式**: 将 mock-common 作为 Maven 子模块，但标记为 provided scope（最终会被真实 common 替换）

  **Must NOT do**:
  - 不创建完整的 common 模块（只模拟 BaseResponse）
  - 不实现 ModelLiteException（假设公共模块已有）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 简单的 Mock 类创建
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 4)
  - **Blocks**: Task 7
  - **Blocked By**: Task 1

  **References**:
  **API/Type References**:
  - 统一响应格式: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:276-310` — BaseResponse 结构（code, message, data, timestamp, requestId）
  - 公共模块异常: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:193-213` — ModelLiteException 使用方式

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: BaseResponse Mock 类可用
    Tool: Bash
    Preconditions: Task 1, 3 完成
    Steps:
      1. 执行 `mvn clean compile` 验证编译成功
      2. 验证 BaseResponse.java 文件存在于 mock-common 目录
      3. 验证 BaseResponse 包含 success() 和 error() 静态方法
      4. 验证 BaseResponse 包含 code, message, data, timestamp, requestId 字段
    Expected Result: Mock 类可编译，字段和方法完整
    Evidence: .sisyphus/evidence/task-3-mock-common.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: (grouped with Task 1)

- [ ] 4. 包结构创建（空目录 + package-info.java）

  **What to do**:
  - 创建完整 DDD 包结构:
    ```
    com.huawei.modellite.repository
    ├── modelweight/
    │   ├── domain/
    │   │   ├── aggregate/
    │   │   │   ├── model/
    │   │   │   ├── category/
    │   │   │   ├── tag/
    │   │   │   └── versionlock/
    │   │   ├── service/
    │   │   ├── repository/
    │   │   └── event/
    │   └── application/
    ├── weighttask/
    │   ├── domain/
    │   │   ├── aggregate/
    │   │   │   ├── uploadtask/
    │   │   │   └── converttask/
    │   │   ├── service/
    │   │   ├── repository/
    │   │   └── event/
    │   └── application/
    ├── infrastructure/
    │   ├── persistence/
    │   │   └── mapper/
    │   ├── taskscheduler/
    │   ├── config/
    │   ├── security/
    │   └── log/
    ├── api/
    │   ├── user/
    │   └── m2m/
    └── common/
        ├── dto/
        │   ├── request/
        │   └── response/
        ├── converter/
        └── util/
    ```
  - 每个包下创建 `package-info.java`，使用 Javadoc 注释说明包的职责
  - 创建 `src/main/resources/mapper/` 目录（MyBatis XML 存放位置）

  **Must NOT do**:
  - 不创建任何领域事件类（event/ 目录仅创建 package-info.java）
  - 不创建任何 Repository 接口（repository/ 目录仅创建 package-info.java）
  - 不创建任何 Application Service（application/ 目录仅创建 package-info.java）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 纯目录和占位文件创建
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3)
  - **Blocks**: F4
  - **Blocked By**: Task 1

  **References**:
  **Pattern References**:
  - 包结构设计: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:644-824` — 完整包结构定义
  - 技术设计包结构: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:126-171` — 包结构确认

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: DDD 包结构完整
    Tool: Bash
    Preconditions: Task 4 完成
    Steps:
      1. 验证 modelweight/domain/aggregate/model/ 目录存在
      2. 验证 modelweight/domain/aggregate/category/ 目录存在
      3. 验证 modelweight/domain/aggregate/tag/ 目录存在
      4. 验证 modelweight/domain/aggregate/versionlock/ 目录存在
      5. 验证 weighttask/domain/aggregate/uploadtask/ 目录存在
      6. 验证 weighttask/domain/aggregate/converttask/ 目录存在
      7. 验证 infrastructure/persistence/mapper/ 目录存在
      8. 验证 api/user/ 和 api/m2m/ 目录存在
      9. 验证 common/dto/request/ 和 common/dto/response/ 目录存在
      10. 验证 src/main/resources/mapper/ 目录存在
    Expected Result: 所有 DDD 包目录存在
    Failure Indicators: 任一关键目录缺失
    Evidence: .sisyphus/evidence/task-4-package-structure.txt

  Scenario: package-info.java 职责注释正确
    Tool: Bash
    Preconditions: Task 4 完成
    Steps:
      1. 读取 modelweight/domain/aggregate/model/package-info.java
      2. 验证包含 "Model 聚合根，模型元数据及版本" 等描述
      3. 读取 infrastructure/config/package-info.java
      4. 验证包含 "基础设施配置" 描述
    Expected Result: 每个 package-info.java 有明确的中文职责描述
    Evidence: .sisyphus/evidence/task-4-package-info.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: (grouped with Task 1)

- [ ] 5. 枚举定义（VersionStatus, TaskStatus, LockType, SourceType, TagType）

  **What to do**:
  - 创建 `VersionStatus` 枚举（`modelweight/domain/aggregate/model/VersionStatus.java`）:
    ```java
    public enum VersionStatus {
        NO_WEIGHT("NoWeight", "无权重"),
        UPLOADING("Uploading", "上传中"),
        AVAILABLE("Available", "可用"),
        UPLOAD_FAILED("UploadFailed", "上传失败"),
        VALIDATION_FAILED("ValidationFailed", "校验失败"),
        ERROR("Error", "异常");

        private final String dbValue;   // 数据库存储值
        private final String displayName; // 显示名称
    }
    ```
  - 创建 `TaskStatus` 枚举（`weighttask/domain/aggregate/uploadtask/TaskStatus.java`）:
    ```java
    public enum TaskStatus {
        PENDING("Pending", "待执行"),
        RUNNING("Running", "执行中"),
        PAUSED("Paused", "已暂停"),
        COMPLETED("Completed", "已完成"),
        FAILED("Failed", "失败"),
        CANCELLED("Cancelled", "已取消");

        private final String dbValue;
        private final String displayName;
    }
    ```
  - 创建 `LockType` 枚举（`modelweight/domain/aggregate/versionlock/LockType.java`）:
    ```java
    public enum LockType {
        INFERENCE("Inference", "推理服务"),
        TRAINING("Training", "训练任务"),
        EVALUATION("Evaluation", "评测任务"),
        DEVELOPMENT("Development", "模型开发");

        private final String dbValue;
        private final String displayName;
    }
    ```
  - 创建 `SourceType` 枚举（`weighttask/domain/aggregate/uploadtask/SourceType.java`）:
    ```java
    public enum SourceType {
        NFS("NFS", "NFS 存储"),
        CIFS("CIFS", "CIFS 存储"),
        PVC("PVC", "PVC 存储");

        private final String dbValue;
        private final String displayName;
    }
    ```
  - 创建 `TagType` 枚举（`modelweight/domain/aggregate/tag/TagType.java`）:
    ```java
    public enum TagType {
        USER("USER", "用户自定义标签"),
        CAPABILITY("CAPABILITY", "能力标签");

        private final String dbValue;
        private final String displayName;
    }
    ```
  - 每个枚举提供 `fromDbValue(String)` 静态方法，用于 MyBatis 反序列化

  **Must NOT do**:
  - 不创建业务逻辑方法（枚举仅定义值和转换方法）
  - 不使用 Java 整数值作为枚举序号（使用字符串存储）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准枚举类创建，逻辑简单
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 7, 8)
  - **Blocks**: Task 10（TypeHandler 需要枚举类）
  - **Blocked By**: Task 1

  **References**:
  **API/Type References**:
  - VersionStatus: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:148-157` — NO_WEIGHT/UPLOADING/AVAILABLE/UPLOAD_FAILED/VALIDATION_FAILED/Error
  - TaskStatus: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:470-478` — PENDING/RUNNING/PAUSED/COMPLETED/FAILED/CANCELLED
  - LockType: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:329-335` — INFERENCE/TRAINING/EVALUATION/DEVELOPMENT
  - SourceType: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:458-462` — NFS/CIFS/PVC
  - TagType: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:268-274` — USER/CAPABILITY
  - 统一语言: `.sisyphus/drafts/ModelLite-Repository-DDD-Ubiquitous-Language.md:44-53` — 版本状态术语

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 枚举类编译通过且值完整
    Tool: Bash
    Preconditions: Task 5 完成
    Steps:
      1. 执行 `mvn clean compile`
      2. 验证编译成功
      3. 验证 VersionStatus 有 6 个值: NO_WEIGHT, UPLOADING, AVAILABLE, UPLOAD_FAILED, VALIDATION_FAILED, ERROR
      4. 验证 TaskStatus 有 6 个值: PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
      5. 验证 LockType 有 4 个值: INFERENCE, TRAINING, EVALUATION, DEVELOPMENT
      6. 验证 SourceType 有 3 个值: NFS, CIFS, PVC
      7. 验证 TagType 有 2 个值: USER, CAPABILITY
      8. 验证每个枚举都有 fromDbValue() 方法
    Expected Result: 所有枚举编译通过，值完整
    Evidence: .sisyphus/evidence/task-5-enums.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `feat(infra): add enums and error codes`
  - Files: `VersionStatus.java, TaskStatus.java, LockType.java, SourceType.java, TagType.java`

- [ ] 6. 错误码定义（ErrorCode 类）

  **What to do**:
  - 创建 `common/ErrorCode.java`:
    ```java
    package com.huawei.modellite.repository.common;

    /**
     * 模型仓库模块错误码定义
     * 格式: 0102YYY (01=前缀, 02=模型仓模块, YYY=错误序号)
     */
    public final class ErrorCode {
        private ErrorCode() {}

        // ===== 模型相关 0102001-0102099 =====
        public static final String MODEL_NOT_FOUND = "0102001";
        public static final String MODEL_NAME_EXISTS = "0102002";
        public static final String MODEL_NAME_IMMUTABLE = "0102003";
        public static final String CATEGORY_HAS_MODELS = "0102004";
        public static final String MODEL_CAPACITY_EXCEEDED = "0102005";  // 单资源组模型数量上限

        // ===== 版本相关 0103001-0103099 =====
        public static final String VERSION_NOT_FOUND = "0103001";
        public static final String VERSION_NUMBER_GAP = "0103002";
        public static final String VERSION_LOCKED = "0103003";
        public static final String VERSION_CAPACITY_EXCEEDED = "0103004";  // 单模型版本数量上限

        // ===== 上传任务相关 0104001-0104099 =====
        public static final String UPLOAD_TASK_NOT_FOUND = "0104001";
        public static final String FILE_SUFFIX_NOT_ALLOWED = "0104002";
        public static final String UPLOAD_TASK_CONCURRENT_LIMIT = "0104003";  // 并发上传上限

        // ===== 转换任务相关 0105001-0105099 =====
        public static final String CONVERT_TASK_NOT_FOUND = "0105001";
        public static final String UNSUPPORTED_CONVERT_FORMAT = "0105002";
    }
    ```
  - 错误码与 HTTP 状态码映射:
    - 0102001 (MODEL_NOT_FOUND) → 404
    - 0102002 (MODEL_NAME_EXISTS) → 409
    - 0102003 (MODEL_NAME_IMMUTABLE) → 400
    - 0102004 (CATEGORY_HAS_MODELS) → 400
    - 0103003 (VERSION_LOCKED) → 409
    - 0104002 (FILE_SUFFIX_NOT_ALLOWED) → 400

  **Must NOT do**:
  - 不实现错误码到 HTTP 状态码的自动映射（Feature 3 全局异常处理器中实现）
  - 不在 ErrorCode 中包含业务逻辑

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 常量类定义，无复杂逻辑
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 5, 7, 8)
  - **Blocks**: Task 7
  - **Blocked By**: Task 1

  **References**:
  **API/Type References**:
  - 错误码格式: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:216-240` — 0102YYY 格式，错误码示例列表

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 错误码定义完整且格式正确
    Tool: Bash
    Preconditions: Task 6 完成
    Steps:
      1. 验证 ErrorCode.java 存在于 common/ 包下
      2. 验证包含 0102001-0102005 (模型相关)
      3. 验证包含 0103001-0103004 (版本相关)
      4. 验证包含 0104001-0104003 (上传任务相关)
      5. 验证包含 0105001-0105002 (转换任务相关)
      6. 验证所有错误码格式为 "0102" + 3位数字
      7. 执行 `mvn clean compile` 验证编译成功
    Expected Result: 所有错误码格式正确，编译通过
    Evidence: .sisyphus/evidence/task-6-error-codes.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: (grouped with Task 5)

- [ ] 7. ModelLiteException 使用验证

  **What to do**:
  - 验证 `com.huawei.modellite.common` 公共模块中 ModelLiteException 可被引用
  - 如果 ModelLiteException 不可用（mock-common 中未包含），在 mock-common 中补充:
    ```java
    package com.huawei.modellite.common;

    /**
     * 业务异常（Mock）
     */
    public class ModelLiteException extends RuntimeException {
        private final String errorCode;

        public ModelLiteException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
    ```
  - 验证 ErrorCode 常量可与 ModelLiteException 配合使用:
    ```java
    // 示例用法（不创建实际代码，仅在测试中验证）
    throw new ModelLiteException(ErrorCode.MODEL_NOT_FOUND, "模型不存在");
    ```

  **Must NOT do**:
  - 不修改 ModelLiteException 的签名（保持与公共模块一致）
  - 不创建自定义异常子类

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 简单的异常类 Mock 和编译验证
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (depends on Task 3 + Task 6)
  - **Blocks**: F3
  - **Blocked By**: Task 3 (Mock 模块), Task 6 (ErrorCode)

  **References**:
  **API/Type References**:
  - 异常体系: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:193-213` — ModelLiteException 使用方式
  - 公共模块决策: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:569-571` — T-07 决策

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: ModelLiteException 可与 ErrorCode 配合使用
    Tool: Bash
    Preconditions: Task 3, 6, 7 完成
    Steps:
      1. 验证 ModelLiteException.java 存在于 mock-common
      2. 验证构造方法接受 (String errorCode, String message)
      3. 执行 `mvn clean compile` 验证编译成功
      4. 编写简单测试验证 new ModelLiteException(ErrorCode.MODEL_NOT_FOUND, "test") 可编译
    Expected Result: 异常类可用，与 ErrorCode 配合编译通过
    Evidence: .sisyphus/evidence/task-7-exception.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: (grouped with Task 5)

- [ ] 8. 全局异常处理器预留位置

  **What to do**:
  - 在 `infrastructure/config/` 下创建 `GlobalExceptionHandler.java` 骨架:
    ```java
    package com.huawei.modellite.repository.infrastructure.config;

    import org.springframework.web.bind.annotation.RestControllerAdvice;

    /**
     * 全局异常处理器（骨架）
     * TODO: Feature 3 实现完整异常处理逻辑
     */
    @RestControllerAdvice
    public class GlobalExceptionHandler {
        // Feature 3 填充:
        // - ModelLiteException → BaseResponse.error(code, message)
        // - MethodArgumentNotValidException → BaseResponse.error(400, validation errors)
        // - Exception → BaseResponse.error(500, internal error)
    }
    ```

  **Must NOT do**:
  - 不实现异常处理逻辑（Feature 3）
  - 不处理具体异常类型

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 单文件骨架创建
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 5, 6, 7)
  - **Blocks**: None
  - **Blocked By**: Task 1

  **References**: None (骨架文件)

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 全局异常处理器骨架存在
    Tool: Bash
    Preconditions: Task 8 完成
    Steps:
      1. 验证 GlobalExceptionHandler.java 存在于 infrastructure/config/
      2. 验证包含 @RestControllerAdvice 注解
      3. 验证包含 TODO 注释说明 Feature 3 实现
      4. 执行 `mvn clean compile` 验证编译成功
    Expected Result: 骨架类存在，可编译
    Evidence: .sisyphus/evidence/task-8-exception-handler.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: (grouped with Task 5)

- [ ] 9. 数据库 Schema DDL（8 张核心表）

  **What to do**:
  - 创建 `src/main/resources/db/schema.sql`（手动 SQL 脚本），包含 8 张核心表的 DDL:

  **表 1: model（模型表）**
  ```sql
  CREATE TABLE model (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      name                VARCHAR(255) NOT NULL,
      description         VARCHAR(2000) DEFAULT '',
      classification_id   UUID NOT NULL REFERENCES classification(id),
      type_id             UUID NOT NULL REFERENCES model_type(id),
      resource_group      VARCHAR(100) NOT NULL,
      create_user         VARCHAR(100) NOT NULL,
      author              VARCHAR(100) DEFAULT NULL,
      deleted             BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

      -- 同一分类+类型组合下，未删除的模型名称唯一
      CONSTRAINT uk_model_name UNIQUE (name, classification_id, type_id)
          WHERE (deleted = FALSE)
  );
  COMMENT ON TABLE model IS '模型表';
  COMMENT ON COLUMN model.id IS '模型ID（UUID）';
  COMMENT ON COLUMN model.name IS '模型名称，创建后不可修改';
  COMMENT ON COLUMN model.deleted IS '软删除标记';
  ```

  **表 2: model_version（模型版本表）**
  ```sql
  CREATE TABLE model_version (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      model_id            UUID NOT NULL REFERENCES model(id),
      version_number      INTEGER NOT NULL,
      pvc_name            VARCHAR(255) DEFAULT NULL,
      internal_path       VARCHAR(1024) DEFAULT NULL,
      weight_type         VARCHAR(50) DEFAULT NULL,
      is_registered       BOOLEAN NOT NULL DEFAULT FALSE,
      status              VARCHAR(30) NOT NULL DEFAULT 'NoWeight',
      is_locked           BOOLEAN NOT NULL DEFAULT FALSE,
      -- 训练元数据（归档版本才有）
      train_frame         VARCHAR(100) DEFAULT NULL,
      train_type          VARCHAR(100) DEFAULT NULL,
      train_strategy      VARCHAR(100) DEFAULT NULL,
      train_time          BIGINT DEFAULT NULL,
      final_loss          VARCHAR(100) DEFAULT NULL,
      source_version      VARCHAR(50) DEFAULT NULL,
      deleted             BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

      -- 同一模型下版本号唯一
      CONSTRAINT uk_model_version UNIQUE (model_id, version_number)
  );
  COMMENT ON TABLE model_version IS '模型版本表';
  COMMENT ON COLUMN model_version.status IS '版本状态: NoWeight/Uploading/Available/UploadFailed/ValidationFailed/Error';
  COMMENT ON COLUMN model_version.is_locked IS '是否被锁定（反规范化字段，由 version_lock 表驱动）';
  COMMENT ON COLUMN model_version.is_registered IS '是否为纳管版本（纳管版本只读挂载）';
  ```

  **表 3: classification（分类表）**
  ```sql
  CREATE TABLE classification (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      name                VARCHAR(100) NOT NULL UNIQUE,
      description         VARCHAR(500) DEFAULT '',
      is_builtin          BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
  );
  COMMENT ON TABLE classification IS '模型分类表（一级分类）';
  COMMENT ON COLUMN classification.is_builtin IS '是否内置分类（内置分类不可删除）';
  ```

  **表 4: model_type（模型类型表）**
  ```sql
  CREATE TABLE model_type (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      classification_id   UUID NOT NULL REFERENCES classification(id),
      name                VARCHAR(100) NOT NULL,
      description         VARCHAR(500) DEFAULT '',
      is_builtin          BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

      -- 同一分类下类型名称唯一
      CONSTRAINT uk_model_type_name UNIQUE (classification_id, name)
  );
  COMMENT ON TABLE model_type IS '模型类型表（二级分类）';
  ```

  **表 5: tag（标签表）**
  ```sql
  CREATE TABLE tag (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      name                VARCHAR(50) NOT NULL UNIQUE,
      tag_type            VARCHAR(20) NOT NULL,  -- USER / CAPABILITY
      is_builtin          BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
  );
  COMMENT ON TABLE tag IS '标签表';
  COMMENT ON COLUMN tag.tag_type IS '标签类型: USER=用户自定义标签, CAPABILITY=能力标签';
  COMMENT ON COLUMN tag.is_builtin IS '是否内置标签（内置标签不允许删除）';
  ```

  **表 6: model_tag（模型-标签关联表）**
  ```sql
  CREATE TABLE model_tag (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      model_id            UUID NOT NULL REFERENCES model(id),
      tag_id              UUID NOT NULL REFERENCES tag(id),
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

      -- 同一模型下同一标签唯一
      CONSTRAINT uk_model_tag UNIQUE (model_id, tag_id)
  );
  COMMENT ON TABLE model_tag IS '模型-标签关联表';
  ```

  **表 7: model_type_tag（模型类型-标签关联表）**
  ```sql
  CREATE TABLE model_type_tag (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      type_id             UUID NOT NULL REFERENCES model_type(id),
      tag_id              UUID NOT NULL REFERENCES tag(id),
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

      -- 同一模型类型下同一标签唯一
      CONSTRAINT uk_model_type_tag UNIQUE (type_id, tag_id)
  );
  COMMENT ON TABLE model_type_tag IS '模型类型-标签关联表（能力标签）';
  ```

  **表 8: version_lock（版本锁表）**
  ```sql
  CREATE TABLE version_lock (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      version_id          UUID NOT NULL REFERENCES model_version(id),
      locker_id           VARCHAR(200) NOT NULL,
      lock_type           VARCHAR(30) NOT NULL,  -- INFERENCE/TRAINING/EVALUATION/DEVELOPMENT
      expire_time         TIMESTAMP WITH TIME ZONE NOT NULL,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
  );
  COMMENT ON TABLE version_lock IS '版本锁表';
  COMMENT ON COLUMN version_lock.expire_time IS '锁过期时间（默认创建时间+24小时）';
  ```

  **额外表（后续特性使用，Feature 1 一并创建）**:

  **表 9: upload_task（上传任务表）**
  ```sql
  CREATE TABLE upload_task (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      model_id            UUID NOT NULL REFERENCES model(id),
      version_id          UUID NOT NULL REFERENCES model_version(id),
      source_type         VARCHAR(20) NOT NULL,  -- NFS/CIFS/PVC
      source_path         VARCHAR(1024) NOT NULL,
      cifs_username       VARCHAR(200) DEFAULT NULL,
      cifs_password       VARCHAR(200) DEFAULT NULL,
      target_path         VARCHAR(1024) NOT NULL,
      progress            INTEGER DEFAULT 0,
      status              VARCHAR(20) NOT NULL DEFAULT 'Pending',
      error_message       VARCHAR(2000) DEFAULT NULL,
      create_user         VARCHAR(100) NOT NULL,
      deleted             BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
  );
  COMMENT ON TABLE upload_task IS '上传任务表';
  ```

  **表 10: convert_task（转换任务表）**
  ```sql
  CREATE TABLE convert_task (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      model_id            UUID NOT NULL REFERENCES model(id),
      source_version_id   UUID NOT NULL REFERENCES model_version(id),
      target_version_id   UUID DEFAULT NULL REFERENCES model_version(id),
      source_format       VARCHAR(50) NOT NULL,
      target_format       VARCHAR(50) NOT NULL,
      progress            INTEGER DEFAULT 0,
      status              VARCHAR(20) NOT NULL DEFAULT 'Pending',
      error_message       VARCHAR(2000) DEFAULT NULL,
      create_user         VARCHAR(100) NOT NULL,
      deleted             BOOLEAN NOT NULL DEFAULT FALSE,
      create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
  );
  COMMENT ON TABLE convert_task IS '转换任务表';
  ```

  - 创建索引:
  ```sql
  -- model 表索引
  CREATE INDEX idx_model_classification ON model(classification_id) WHERE deleted = FALSE;
  CREATE INDEX idx_model_type ON model(type_id) WHERE deleted = FALSE;
  CREATE INDEX idx_model_resource_group ON model(resource_group) WHERE deleted = FALSE;
  CREATE INDEX idx_model_create_time ON model(create_time) WHERE deleted = FALSE;

  -- model_version 表索引
  CREATE INDEX idx_version_model ON model_version(model_id);
  CREATE INDEX idx_version_status ON model_version(status) WHERE deleted = FALSE;

  -- version_lock 表索引
  CREATE INDEX idx_lock_version ON version_lock(version_id);
  CREATE INDEX idx_lock_expire ON version_lock(expire_time);
  CREATE INDEX idx_lock_locker ON version_lock(locker_id, lock_type);

  -- upload_task 表索引
  CREATE INDEX idx_upload_model ON upload_task(model_id) WHERE deleted = FALSE;
  CREATE INDEX idx_upload_status ON upload_task(status) WHERE deleted = FALSE;

  -- convert_task 表索引
  CREATE INDEX idx_convert_model ON convert_task(model_id) WHERE deleted = FALSE;
  CREATE INDEX idx_convert_status ON convert_task(status) WHERE deleted = FALSE;

  -- tag 表索引
  CREATE INDEX idx_tag_type ON tag(tag_type);

  -- model_tag 表索引
  CREATE INDEX idx_model_tag_tag ON model_tag(tag_id);

  -- model_type_tag 表索引
  CREATE INDEX idx_model_type_tag_tag ON model_type_tag(tag_id);
  ```

  **关键设计决策说明**:

  | 决策 | 说明 |
  |------|------|
  | UUID 主键 | 使用 `gen_random_uuid()`，与旧版 String ID 区分 |
  | 软删除部分索引 | `WHERE deleted = FALSE` 条件确保软删除后名称可复用 |
  | version_status 为 VARCHAR(30) | 存储枚举的 dbValue 字符串，与旧版 int 状态码区分 |
  | is_locked 反规范化 | model_version.is_locked 由 version_lock 表驱动，不独立修改 |
  | CIFS 密码明文存储 | 当前 MVP 阶段明文，后续可加密（需求未明确要求加密） |
  | classification 无 deleted 字段 | 分类/类型采用真删除（确保下无模型才允许删除） |
  | model_type_tag 唯一约束 | 同一模型类型下同一标签只能关联一次 |

  **Must NOT do**:
  - 不创建初始化数据（Feature 2 填充）
  - 不创建迁移脚本（手动 SQL 管理）
  - 不创建视图或存储过程

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: DDL 设计需要仔细考虑字段类型、约束、索引，有一定复杂度
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO (核心任务，其他任务依赖此任务的输出)
  - **Parallel Group**: Wave 3 (after Wave 1)
  - **Blocks**: Tasks 10, 12
  - **Blocked By**: Task 1

  **References**:
  **Pattern References**:
  - 旧版本数据模型: `docs/project-understanding/project-understanding-summary.md:476-658` — 字段对照参考
  - 数据库规范: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:329-388` — 蛇形命名、UUID 主键、deleted 软删除

  **API/Type References**:
  - Model 聚合字段: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:183-210` — 字段类型和约束
  - VersionLock 聚合: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:314-345` — 锁表字段
  - UploadTask 聚合: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:425-483` — 上传任务字段
  - ConvertTask 聚合: `.sisyphus/drafts/ModelLite-Repository-DDD-Bounded-Context.md:489-518` — 转换任务字段

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: DDL 脚本可执行
    Tool: Bash
    Preconditions: PostgreSQL 数据库可访问
    Steps:
      1. 连接数据库: `psql -h localhost -U postgres -d modellite_dev`
      2. 执行 `\i src/main/resources/db/schema.sql`
      3. 执行 `\dt` 验证 10 张表创建成功
      4. 验证表: model, model_version, classification, model_type, tag, model_tag, model_type_tag, version_lock, upload_task, convert_task
    Expected Result: 10 张表全部创建成功
    Failure Indicators: SQL 执行报错, 表缺失
    Evidence: .sisyphus/evidence/task-9-ddl-execute.txt

  Scenario: 唯一约束和索引正确
    Tool: Bash
    Preconditions: DDL 已执行
    Steps:
      1. 查询 model 表约束: `SELECT conname FROM pg_constraint WHERE conrelid = 'model'::regclass`
      2. 验证 uk_model_name 约束存在（含 WHERE deleted = FALSE）
      3. 查询索引: `SELECT indexname FROM pg_indexes WHERE tablename = 'model'`
      4. 验证 idx_model_classification, idx_model_resource_group 等索引存在
      5. 验证 model_version 表 uk_model_version 约束存在
      6. 验证 version_lock 表 idx_lock_expire 索引存在
    Expected Result: 所有约束和索引正确创建
    Evidence: .sisyphus/evidence/task-9-constraints.txt

  Scenario: 外键约束正确
    Tool: Bash
    Preconditions: DDL 已执行
    Steps:
      1. 查询外键: `SELECT conname, conrelid::regclass, confrelid::regclass FROM pg_constraint WHERE contype = 'f'`
      2. 验证 model.classification_id → classification.id
      3. 验证 model.type_id → model_type.id
      4. 验证 model_version.model_id → model.id
      5. 验证 version_lock.version_id → model_version.id
    Expected Result: 所有必要外键约束存在
    Evidence: .sisyphus/evidence/task-9-foreign-keys.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat(infra): add database schema and MyBatis config`
  - Files: `src/main/resources/db/schema.sql`

- [ ] 10. MyBatis 配置 + TypeHandler

  **What to do**:
  - 创建 `infrastructure/config/MyBatisConfig.java`:
    ```java
    @Configuration
    @MapperScan("com.huawei.modellite.repository.infrastructure.persistence.mapper")
    public class MyBatisConfig {
        // 扫描 Mapper 接口
    }
    ```
  - 创建通用 TypeHandler（`infrastructure/persistence/typehandler/`）:
    - `VersionStatusTypeHandler.java`: VersionStatus 枚举 ↔ VARCHAR(30) 转换
    - `TaskStatusTypeHandler.java`: TaskStatus 枚举 ↔ VARCHAR(20) 转换
    - `LockTypeTypeHandler.java`: LockType 枚举 ↔ VARCHAR(30) 转换
    - `SourceTypeTypeHandler.java`: SourceType 枚举 ↔ VARCHAR(20) 转换
    - `TagTypeTypeHandler.java`: TagType 枚举 ↔ VARCHAR(20) 转换
  - 每个枚举的 TypeHandler 使用 `@MappedTypes` 注解自动注册
  - 在 application.yml 中配置 TypeHandler 包扫描:
    ```yaml
    mybatis:
      type-handlers-package: com.huawei.modellite.repository.infrastructure.persistence.typehandler
    ```
  - 创建 `src/main/resources/mapper/` 目录（已在 Task 4 创建，此处确认存在）

  **Must NOT do**:
  - 不创建任何 Mapper XML 文件（Feature 3 再创建）
  - 不创建任何 Mapper 接口（Feature 3 再创建）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准配置和 TypeHandler 创建
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (与 Task 11 并行)
  - **Parallel Group**: Wave 3 (with Tasks 9, 11, 12)
  - **Blocks**: Task 12
  - **Blocked By**: Task 1 (骨架), Task 5 (枚举类), Task 9 (确认配置一致)

  **References**:
  **Pattern References**:
  - MyBatis 配置: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:345-384` — mapper-locations, XML 方式, ResultMap 示例

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: TypeHandler 编译通过
    Tool: Bash
    Preconditions: Task 5, 10 完成
    Steps:
      1. 执行 `mvn clean compile`
      2. 验证编译成功
      3. 验证 VersionStatusTypeHandler.java 存在
      4. 验证 VersionStatusTypeHandler 使用 @MappedTypes(VersionStatus.class) 注解
      5. 验证所有 5 个 TypeHandler 文件存在
    Expected Result: 所有 TypeHandler 编译通过
    Evidence: .sisyphus/evidence/task-10-mybatis-config.txt

  Scenario: MyBatis 配置正确
    Tool: Bash
    Preconditions: Task 2, 10 完成
    Steps:
      1. 读取 application.yml，验证 mybatis.mapper-locations = classpath:mapper/*.xml
      2. 读取 application.yml，验证 mybatis.type-handlers-package 包路径正确
      3. 验证 MyBatisConfig.java 包含 @MapperScan 注解
    Expected Result: MyBatis 配置完整正确
    Evidence: .sisyphus/evidence/task-10-mybatis-config-verify.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: (grouped with Task 9)

- [ ] 11. Druid 连接池配置

  **What to do**:
  - 在 `application-dev.yml` 中添加 Druid 配置:
    ```yaml
    spring:
      datasource:
        druid:
          initial-size: 5
          min-idle: 5
          max-active: 20
          max-wait: 60000
          time-between-eviction-runs-millis: 60000
          min-evictable-idle-time-millis: 300000
          validation-query: SELECT 1
          test-while-idle: true
          test-on-borrow: false
          test-on-return: false
          # 开发环境启用监控
          stat-view-servlet:
            enabled: true
            url-pattern: /druid/*
          web-stat-filter:
            enabled: true
    ```
  - 在 `application-prod.yml` 中添加 Druid 配置（**关闭 stat-view-servlet**）:
    ```yaml
    spring:
      datasource:
        druid:
          initial-size: 10
          min-idle: 10
          max-active: 50
          max-wait: 60000
          time-between-eviction-runs-millis: 60000
          min-evictable-idle-time-millis: 300000
          validation-query: SELECT 1
          test-while-idle: true
          test-on-borrow: false
          test-on-return: false
          # 生产环境关闭监控
          stat-view-servlet:
            enabled: false
          web-stat-filter:
            enabled: false
    ```

  **Must NOT do**:
  - 不在生产环境开启 Druid 监控页面（安全风险）
  - 不硬编码数据库密码（使用环境变量）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 配置文件编辑
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 9, 10, 12)
  - **Blocks**: Task 12
  - **Blocked By**: Task 1

  **References**:
  **Pattern References**:
  - Druid 配置: `.sisyphus/drafts/ModelLite-Repository-DDD-Tech-Design.md:509-548` — 多环境配置结构

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Druid 开发环境配置正确
    Tool: Bash
    Preconditions: Task 11 完成
    Steps:
      1. 读取 application-dev.yml
      2. 验证包含 spring.datasource.druid.initial-size
      3. 验证包含 spring.datasource.druid.stat-view-servlet.enabled = true
      4. 读取 application-prod.yml
      5. 验证包含 spring.datasource.druid.stat-view-servlet.enabled = false
    Expected Result: 开发环境启用监控，生产环境关闭监控
    Evidence: .sisyphus/evidence/task-11-druid-config.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: (grouped with Task 9)

- [ ] 12. 数据库连接测试

  **What to do**:
  - 创建 `src/test/java/.../DatabaseConnectionTest.java`:
    ```java
    @SpringBootTest
    @ActiveProfiles("test")
    class DatabaseConnectionTest {
        @Autowired
        private DataSource dataSource;

        @Test
        void should_connectToDatabase() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                assertThat(conn.isValid(5)).isTrue();
            }
        }

        @Test
        void should_queryModelTableExists() throws SQLException {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'model'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }
    ```
  - **注意**: 此测试需要数据库实例（本地 PostgreSQL 或 Testcontainers），如果不可用则跳过

  **Must NOT do**:
  - 不创建业务测试（仅连接验证）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 简单的集成测试
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 3 (last task)
  - **Blocks**: F3
  - **Blocked By**: Tasks 9 (Schema), 10 (MyBatis), 11 (Druid)

  **References**: None

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 数据库连接测试通过
    Tool: Bash
    Preconditions: PostgreSQL 数据库可访问, Schema 已执行
    Steps:
      1. 执行 `mvn test -Dtest=DatabaseConnectionTest`
      2. 验证测试通过（如果数据库不可用，允许跳过并记录原因）
    Expected Result: 数据库连接成功，model 表存在
    Failure Indicators: 连接超时, 表不存在
    Evidence: .sisyphus/evidence/task-12-db-connection.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: (grouped with Task 9)

- [ ] 13. Logback 日志轮转配置

  **What to do**:
  - 创建 `src/main/resources/logback-spring.xml`:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
        <property name="LOG_PATH" value="logs"/>
        <property name="APP_NAME" value="modellite-repository"/>

        <!-- 控制台输出 -->
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            </encoder>
        </appender>

        <!-- 文件输出 + 轮转 -->
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>${LOG_PATH}/${APP_NAME}.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            </encoder>
        </appender>

        <!-- 开发环境 -->
        <springProfile name="dev">
            <root level="INFO">
                <appender-ref ref="CONSOLE"/>
                <appender-ref ref="FILE"/>
            </root>
            <logger name="com.huawei.modellite" level="DEBUG"/>
        </springProfile>

        <!-- 生产环境 -->
        <springProfile name="prod">
            <root level="WARN">
                <appender-ref ref="FILE"/>
            </root>
            <logger name="com.huawei.modellite" level="INFO"/>
        </springProfile>

        <!-- 测试环境 -->
        <springProfile name="test">
            <root level="INFO">
                <appender-ref ref="CONSOLE"/>
            </root>
            <logger name="com.huawei.modellite" level="DEBUG"/>
        </springProfile>
    </configuration>
    ```

  **Must NOT do**:
  - 不使用 Log4j（需求文档提到 Log4j，但 Spring Boot 默认使用 Logback，Logback 与 Log4j 2 都满足日志轮转需求）
  - 不在控制台输出敏感信息（密码等）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准 Logback 配置
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 14, 15, 16, 17)
  - **Blocks**: None
  - **Blocked By**: Task 1

  **References**:
  **Pattern References**:
  - 日志需求: 需求规格说明书 REQ-GENERAL-001 — 使用 Log4j 进行日志轮转（实际使用 Logback，兼容 Spring Boot 默认）

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Logback 配置文件存在且格式正确
    Tool: Bash
    Preconditions: Task 13 完成
    Steps:
      1. 验证 logback-spring.xml 存在于 src/main/resources/
      2. 验证包含 RollingFileAppender
      3. 验证包含 maxFileSize = 100MB
      4. 验证包含 maxHistory = 30
      5. 验证包含 springProfile name="dev" 和 "prod"
    Expected Result: 日志轮转配置正确
    Evidence: .sisyphus/evidence/task-13-logback.txt

  Scenario: 日志文件可生成
    Tool: Bash
    Preconditions: 应用可启动
    Steps:
      1. 启动应用（dev profile）
      2. 验证 logs/modellite-repository.log 文件生成
      3. 验证日志文件包含启动日志
    Expected Result: 日志文件成功生成
    Evidence: .sisyphus/evidence/task-13-log-file.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat(infra): add logging, ssl and k8s config`

- [ ] 14. 文件后缀白名单 ConfigMap

  **What to do**:
  - 创建 ConfigMap 模板文件 `k8s/configmap-whitelist.yaml`:
    ```yaml
    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: model-repo-whitelist
      namespace: modellite
    data:
      allowed_suffixes: |
        .safetensors
        .bin
        .pt
        .pth
        .onnx
        .json
        .txt
        .model
        .index.json
        .config.json
        .tokenizer.json
        .vocab.json
        .merges.txt
        .special_tokens_map.json
        .tokenizer_config.json
        .generation_config.json
    ```
  - 创建白名单加载类 `infrastructure/config/WhitelistConfig.java`:
    ```java
    @Configuration
    public class WhitelistConfig {
        @Value("${model-repo.whitelist-path:}")
        private String whitelistPath;

        private Set<String> allowedSuffixes;

        @PostConstruct
        public void loadWhitelist() {
            // 从 ConfigMap 挂载文件或 classpath 默认值加载白名单
            // 如果 whitelistPath 为空，从 classpath:whitelist.txt 加载默认值
        }

        public boolean isAllowed(String filename) {
            return allowedSuffixes.stream()
                .anyMatch(filename::endsWith);
        }

        public Set<String> getAllowedSuffixes() {
            return Collections.unmodifiableSet(allowedSuffixes);
        }
    }
    ```
  - 创建 `src/main/resources/whitelist.txt` 作为默认白名单（开发环境使用）
  - 在 application.yml 中添加配置:
    ```yaml
    model-repo:
      whitelist-path: ${WHITELIST_PATH:}
    ```

  **Must NOT do**:
  - 不将白名单存储到数据库（需求明确使用 ConfigMap）
  - 不实现文件内容安全检查（仅后缀白名单）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 配置类 + ConfigMap 模板
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 13, 15, 16, 17)
  - **Blocks**: None
  - **Blocked By**: Task 1

  **References**:
  **API/Type References**:
  - 文件后缀白名单: 需求规格说明书 REQ-SECURITY-001 — ConfigMap 挂载，不存储于数据库
  - 可扩展性: REQ-NFR-011 — 支持动态调整

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 白名单配置文件存在
    Tool: Bash
    Preconditions: Task 14 完成
    Steps:
      1. 验证 k8s/configmap-whitelist.yaml 存在
      2. 验证包含 .safetensors, .bin, .json 等常见后缀
      3. 验证 src/main/resources/whitelist.txt 存在
      4. 验证 WhitelistConfig.java 存在于 infrastructure/config/
    Expected Result: 白名单配置文件和加载类存在
    Evidence: .sisyphus/evidence/task-14-whitelist.txt

  Scenario: 白名单加载逻辑可用
    Tool: Bash
    Preconditions: Task 14 完成, 应用可启动
    Steps:
      1. 启动应用
      2. 验证 WhitelistConfig Bean 初始化成功（日志无 ERROR）
      3. 验证 WhitelistConfig.isAllowed("model.safetensors") 返回 true
      4. 验证 WhitelistConfig.isAllowed("malware.exe") 返回 false
    Expected Result: 白名单加载成功，后缀判断正确
    Evidence: .sisyphus/evidence/task-14-whitelist-test.txt
  ```

  **Commit**: YES (groups with Wave 4)

- [ ] 15. SSL/TLS 基础设施配置

  **What to do**:
  - 创建 SSL 配置模板 `infrastructure/security/SslConfig.java`:
    ```java
    @Configuration
    public class SslConfig {
        @Value("${server.ssl.enabled:false}")
        private boolean sslEnabled;

        @Value("${server.ssl.key-store:}")
        private String keyStorePath;

        @Value("${server.ssl.key-store-password:}")
        private String keyStorePassword;

        @Value("${server.ssl.trust-store:}")
        private String trustStorePath;

        @Value("${server.ssl.trust-store-password:}")
        private String trustStorePassword;

        // SSL 配置由 Spring Boot 自动加载
        // 此类仅记录配置项，不实现自定义 SSL 逻辑
    }
    ```
  - 在 `application-prod.yml` 中添加 SSL 配置:
    ```yaml
    server:
      ssl:
        enabled: true
        key-store: ${SSL_KEY_STORE_PATH}
        key-store-password: ${SSL_KEY_STORE_PASSWORD}
        key-store-type: PKCS12
        trust-store: ${SSL_TRUST_STORE_PATH}
        trust-store-password: ${SSL_TRUST_STORE_PASSWORD}
        trust-store-type: PKCS12
        client-auth: want  # 双向证书认证
    ```
  - 在 `application-dev.yml` 中 SSL 默认关闭:
    ```yaml
    server:
      ssl:
        enabled: false
    ```
  - 创建 M2M 认证过滤器骨架 `infrastructure/security/M2mAuthFilter.java`（仅骨架，Feature 3 实现）

  **Must NOT do**:
  - 不实现 Spring Security（Feature 3 配置）
  - 不生成自签名证书（生产环境使用正式证书）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 配置类 + 模板文件
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 13, 14, 16, 17)
  - **Blocks**: None
  - **Blocked By**: Task 1

  **References**:
  **API/Type References**:
  - SSL 需求: 需求规格说明书 REQ-SECURITY-002 — SSL/TLS 双向证书认证
  - M2M 接口认证: 需求规格说明书 5.1 — 机机接口通过 SSL 证书校验进行认证

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: SSL 配置文件存在
    Tool: Bash
    Preconditions: Task 15 完成
    Steps:
      1. 验证 SslConfig.java 存在于 infrastructure/security/
      2. 验证 application-prod.yml 包含 server.ssl.enabled = true
      3. 验证 application-prod.yml 包含 ${SSL_KEY_STORE_PATH} 环境变量
      4. 验证 application-dev.yml 包含 server.ssl.enabled = false
      5. 验证 M2mAuthFilter.java 骨架存在
    Expected Result: SSL 配置模板正确
    Evidence: .sisyphus/evidence/task-15-ssl.txt

  Scenario: 开发环境无 SSL 可启动
    Tool: Bash
    Preconditions: Task 15 完成
    Steps:
      1. 使用 dev profile 启动应用
      2. 验证启动成功，无 SSL 相关错误
      3. 访问 http://localhost:8080/actuator/health → 200 OK
    Expected Result: 开发环境无需证书即可启动
    Evidence: .sisyphus/evidence/task-15-ssl-dev.txt
  ```

  **Commit**: YES (groups with Wave 4)

- [ ] 16. K8s Deployment + Service YAML

  **What to do**:
  - 创建 `k8s/deployment.yaml`:
    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: model-repository
      namespace: modellite
      labels:
        app: model-repository
    spec:
      replicas: 2
      selector:
        matchLabels:
          app: model-repository
      template:
        metadata:
          labels:
            app: model-repository
        spec:
          containers:
            - name: model-repository
              image: modellite/model-repository:latest
              ports:
                - containerPort: 8080
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: "prod"
                - name: DB_HOST
                  valueFrom:
                    secretKeyRef:
                      name: db-credentials
                      key: host
                - name: DB_USER
                  valueFrom:
                    secretKeyRef:
                      name: db-credentials
                      key: username
                - name: DB_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: db-credentials
                      key: password
                - name: SSL_KEY_STORE_PATH
                  value: /etc/ssl/keystore.p12
                - name: SSL_KEY_STORE_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: ssl-credentials
                      key: keystore-password
                - name: SSL_TRUST_STORE_PATH
                  value: /etc/ssl/truststore.p12
                - name: SSL_TRUST_STORE_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: ssl-credentials
                      key: truststore-password
              volumeMounts:
                - name: whitelist-config
                  mountPath: /etc/config/whitelist
                - name: ssl-certs
                  mountPath: /etc/ssl
                  readOnly: true
              livenessProbe:
                httpGet:
                  path: /actuator/health/liveness
                  port: 8080
                initialDelaySeconds: 30
                periodSeconds: 10
              readinessProbe:
                httpGet:
                  path: /actuator/health/readiness
                  port: 8080
                initialDelaySeconds: 10
                periodSeconds: 5
              resources:
                requests:
                  cpu: "500m"
                  memory: "512Mi"
                limits:
                  cpu: "2"
                  memory: "2Gi"
          volumes:
            - name: whitelist-config
              configMap:
                name: model-repo-whitelist
            - name: ssl-certs
              secret:
                secretName: ssl-certs
    ```
  - 创建 `k8s/service.yaml`:
    ```yaml
    apiVersion: v1
    kind: Service
    metadata:
      name: model-repository
      namespace: modellite
    spec:
      selector:
        app: model-repository
      ports:
        - name: https
          port: 8443
          targetPort: 8080
      type: ClusterIP
    ```

  **Must NOT do**:
  - 不创建 K8s Job 模板（Feature 4 再创建）
  - 不创建 Ingress 配置（由 Gateway 管理）
  - 不创建 LeaderElection 配置（Feature 5 再创建）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: K8s YAML 模板创建
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 13, 14, 15, 17)
  - **Blocks**: F3
  - **Blocked By**: Task 1

  **References**: None (标准 K8s 配置)

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: K8s YAML 文件存在且格式正确
    Tool: Bash
    Preconditions: Task 16 完成
    Steps:
      1. 验证 k8s/deployment.yaml 存在
      2. 验证 k8s/service.yaml 存在
      3. 验证 deployment.yaml 包含 replicas: 2
      4. 验证 deployment.yaml 包含 livenessProbe 和 readinessProbe
      5. 验证 deployment.yaml 包含 ConfigMap volume (whitelist-config)
      6. 验证 deployment.yaml 包含 SSL volume (ssl-certs)
      7. 验证 service.yaml 包含 ClusterIP type
      8. 验证 YAML 语法正确（使用 yamllint 或类似工具）
    Expected Result: K8s 配置完整且格式正确
    Evidence: .sisyphus/evidence/task-16-k8s.txt
  ```

  **Commit**: YES (groups with Wave 4)

- [ ] 17. 健康检查接口

  **What to do**:
  - 在 `application.yml` 中启用 Actuator:
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: health, info
      endpoint:
        health:
          show-details: when-authorized
    ```
  - 如果 Spring Boot Actuator 默认的健康检查端点不足，可创建自定义健康指示器:
    ```java
    @Component
    public class DatabaseHealthIndicator extends AbstractHealthIndicator {
        @Autowired
        private DataSource dataSource;

        @Override
        protected void doHealthCheck(Health.Builder builder) throws Exception {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    builder.up().withDetail("database", "PostgreSQL");
                } else {
                    builder.down().withDetail("database", "Connection invalid");
                }
            }
        }
    }
    ```

  **Must NOT do**:
  - 不暴露所有 Actuator 端点（仅 health 和 info）
  - 不创建业务相关的健康检查（如模型数量等）

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 标准 Actuator 配置
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 13, 14, 15, 16)
  - **Blocks**: F3
  - **Blocked By**: Task 2 (application.yml)

  **References**: None

  **Acceptance Criteria**:

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: 健康检查端点可用
    Tool: Bash
    Preconditions: 应用已启动（dev profile）
    Steps:
      1. 执行 `curl -s http://localhost:8080/actuator/health`
      2. 验证返回 JSON 包含 "status":"UP"
      3. 执行 `curl -s http://localhost:8080/actuator/health/liveness`
      4. 验证返回 200 OK
      5. 执行 `curl -s http://localhost:8080/actuator/health/readiness`
      6. 验证返回 200 OK
    Expected Result: 所有健康检查端点返回正常
    Failure Indicators: 端点返回 404, status=DOWN
    Evidence: .sisyphus/evidence/task-17-health.txt

  Scenario: 不暴露敏感端点
    Tool: Bash
    Preconditions: 应用已启动
    Steps:
      1. 执行 `curl -s http://localhost:8080/actuator/env`
      2. 验证返回 404
      3. 执行 `curl -s http://localhost:8080/actuator/beans`
      4. 验证返回 404
    Expected Result: 敏感端点不可访问
    Evidence: .sisyphus/evidence/task-17-health-security.txt
  ```

  **Commit**: YES (groups with Wave 4)

- [ ] F1. **Schema DDL 完整性检查** — `oracle`
  Read DDL script end-to-end. Verify: 1) All 8 tables exist. 2) UUID primary keys with gen_random_uuid(). 3) deleted boolean + partial indexes. 4) Foreign key constraints correct. 5) No missing NOT NULL on required columns. 6) UNIQUE constraints with deleted=FALSE condition.
  Output: `Tables [8/8] | PKs [UUID all] | SoftDelete [all] | FKs [N correct] | VERDICT: APPROVE/REJECT`

- [ ] F2. **配置一致性检查** — `unspecified-high`
  Compare all config files against Tech Design: 1) MyBatis XML mapper-locations matches Tech Design. 2) Druid config matches Tech Design (stat-view-servlet disabled in prod). 3) Logback config matches Tech Design (rotation, level). 4) application.yml profiles match Tech Design. 5) pom.xml versions match Tech Design (Spring Boot 3.4.5, Java 21).
  Output: `MyBatis [MATCH/NOT] | Druid [MATCH/NOT] | Logback [MATCH/NOT] | Spring [MATCH/NOT] | VERDICT`

- [ ] F3. **编译与启动验证** — `unspecified-high`
  Execute: 1) `mvn clean compile` → BUILD SUCCESS. 2) `mvn spring-boot:run` (dev profile) → Application started. 3) `curl http://localhost:8080/actuator/health` → {"status":"UP"}. 4) Check logs: no ERROR/FATAL on startup. 5) Execute simple DB query via MyBatis test → connection OK.
  Output: `Compile [PASS/FAIL] | Start [PASS/FAIL] | Health [PASS/FAIL] | DB [PASS/FAIL] | Logs [CLEAN/ISSUES] | VERDICT`

- [ ] F4. **DDD 包结构合规检查** — `deep`
  Read project structure. Verify: 1) Package `com.huawei.modellite.repository` exists. 2) Sub-packages: modelweight/, weighttask/, infrastructure/, api/, common/ exist. 3) Each sub-package has domain/, application/ (where applicable). 4) No domain event files exist (T-01 guardrail). 5) No repository interfaces exist (guardrail). 6) No application services exist (guardrail). 7) No API controllers exist except health check (guardrail).
  Output: `Structure [DDD/NOT] | Guardrails [ALL PASS/VIOLATED] | VERDICT: APPROVE/REJECT`

---

## Commit Strategy

- **Wave 1 Completion**: `feat(infra): add project skeleton and mock common module`
  - Files: pom.xml, src/main/java/..., mock-common/...
  - Pre-commit: `mvn clean compile`

- **Wave 2 Completion**: `feat(infra): add enums and error codes`
  - Files: VersionStatus.java, TaskStatus.java, ErrorCode.java, ...
  - Pre-commit: `mvn clean compile`

- **Wave 3 Completion**: `feat(infra): add database schema and MyBatis config`
  - Files: schema.sql, MyBatisConfig.java, DruidConfig.java, ...
  - Pre-commit: `mvn clean compile + DB connection test`

- **Wave 4 Completion**: `feat(infra): add logging, ssl and k8s config`
  - Files: logback-spring.xml, application-ssl.yml, k8s/*.yaml, ...
  - Pre-commit: `mvn clean compile`

---

## Success Criteria

### Verification Commands

```bash
# 数据库 Schema 验证
psql -h localhost -U postgres -d modellite_dev -c "\dt"  # Expected: 8 tables listed

# 项目骨架验证
mvn clean compile  # Expected: BUILD SUCCESS
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Expected: Application started

# 健康检查验证
curl http://localhost:8080/actuator/health  # Expected: {"status":"UP"}

# MyBatis 连接验证（通过测试）
mvn test -Dtest=MyBatisConnectionTest  # Expected: Tests run: 1, Failures: 0

# 日志验证
ls logs/*.log  # Expected: modellite-repository.log exists
ls logs/*.log.*  # Expected: rolled files exist if log > threshold

# Druid 监控验证（仅开发环境）
curl http://localhost:8080/druid/index.html  # Expected: Druid Stat View HTML
```

### Final Checklist

- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] Database Schema executable
- [ ] Project can start successfully
- [ ] All configs match Tech Design
- [ ] DDD package structure correct
- [ ] All guardrails satisfied