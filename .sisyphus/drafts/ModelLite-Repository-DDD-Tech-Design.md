# ModelLite 模型仓库 - 技术设计规范

> **文档类型**: 技术设计规范
> **文档版本**: v1.1
> **编写日期**: 2026-04-22
> **适用范围**: ModelLite 平台模型仓库模块 DDD 架构实现
> **目标读者**: 架构师、后端开发工程师

---

## 1. 概述

### 1.1 文档目的

本文档定义模型仓库模块的技术实现规范，包括：
- 技术栈选型与版本
- DDD 技术实践规范
- 代码规范
- API 规范
- 数据库规范
- 测试规范
- 配置管理规范

### 1.2 参考文档

| 文档 | 说明 |
|------|------|
| 架构设计 v1.1 | 原有架构设计，部分决策沿用 |
| 限界上下文设计 | DDD 聚合、包结构设计 |
| 需求规格说明书 v1.2 | 功能需求参照 |

---

## 2. 技术栈

### 2.1 核心技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **开发框架** | Spring Boot | 3.4.5 | 核心框架 |
| **编程语言** | Java | 21 LTS | 使用虚拟线程等新特性 |
| **数据库** | PostgreSQL | — | 关系型数据库 |
| **ORM** | MyBatis | — | 轻量级 ORM，XML 方式定义 SQL |
| **连接池** | Druid | — | 阿里巴巴开源连接池 |
| **构建工具** | Maven | — | 依赖管理与构建 |
| **单元测试** | JUnit 5 | — | 测试框架 |
| **Mock 测试** | Mockito | — | Mock 框架 |
| **集成测试** | Testcontainers | — | Docker 化测试环境 |
| **ID 生成** | UUID v4 | — | `gen_random_uuid()` |

### 2.2 Kubernetes 相关

| 类别 | 技术 | 说明 |
|------|------|------|
| **部署** | Kubernetes Deployment | 多副本部署 |
| **异步任务** | K8s Job + fabric8 Informer | 上传/转换任务执行与状态同步 |
| **Leader Election** | Kubernetes原生机制 | 写操作一致性 |
| **配置挂载** | ConfigMap | 文件后缀白名单 |
| **敏感配置** | K8s Secret | SSL 证书等（如需要） |

---

## 3. DDD 技术实践规范

### 3.1 领域事件处理

**决策**: 领域事件仅作为**文档记录**，代码中**不实现**事件发布/订阅机制。

**理由**:
- 当前架构采用领域服务直接调用，非事件驱动
- 跨上下文协作通过直接调用完成，无需事件解耦
- 审计日志在应用服务中直接调用 LogReporter 上报

**实现方式**:
- 领域事件的定义（事件数据结构）记录在文档中
- 不引入 Spring Event 或类似机制
- 不实现事件处理器（如 AuditLogHandler 等）

### 3.2 聚合事务边界

**决策**: 跨聚合操作采用**最终一致性 + 补偿机制**，不用 `@Transactional` 包裹整个流程。

**聚合内一致性**:
- 单个聚合内的操作在一个事务中完成
- 聚合根通过 Repository 加载/保存，Repository 内部使用事务

**跨聚合一致性**:
- 不用 `@Transactional` 包裹跨聚合操作
- 通过应用服务编排，每个聚合操作独立事务
- 失败时通过补偿机制处理（如转换失败后解锁源版本）

**示例：权重格式转换流程**：

```
1. ConvertTask 创建（独立事务）
2. 锁定源版本（独立事务）
3. 创建目标版本（独立事务）
4. 执行转换（K8s Job 异步）
5. 成功：解锁源版本（独立事务） + 更新目标版本状态（独立事务）
6. 失败：解锁源版本（独立事务） + 标记任务失败（独立事务）
```

### 3.3 方法命名规范

**决策**: 领域方法使用**业务术语**，而非技术术语。

| 正确示例 | 错误示例 |
|----------|----------|
| `softDelete()` | `updateDeletedFlag()` |
| `restore()` | `setDeletedFalse()` |
| `lock()` | `createVersionLockRecord()` |
| `createVersion()` | `insertModelVersion()` |
| `archiveWeight()` | `saveTrainingMetadata()` |

**理由**: 代码应表达业务意图，而非技术实现细节。

---

## 4. 代码规范

### 4.1 包结构

**决策**: 包名沿用 `com.huawei.modellite.repository`。

**包结构**（基于限界上下文设计）：

```
com.huawei.modellite.repository
│
├── modelweight/                  # 模型权重上下文
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
│
├── weighttask/                   # 权重任务上下文
│   ├── domain/
│   │   ├── aggregate/
│   │   │   ├── uploadtask/
│   │   │   └── converttask/
│   │   ├── service/
│   │   ├── repository/
│   │   └── event/
│   └── application/
│
├── infrastructure/               # 基础设施层
│   ├── persistence/
│   │   ├── mapper/               # MyBatis Mapper XML
│   │   └── MyBatisXxxRepository.java
│   ├── taskscheduler/
│   ├── config/
│   ├── security/
│   └── log/
│
├── api/                          # 接口层
│   ├── user/                     # 人机接口 /v2/ui/
│   └── m2m/                      # 机机接口 /v2/
│
└── common/                       # 公共模块
    ├── dto/
    │   ├── request/
    │   └── response/
    ├── converter/
    └── util/
```

### 4.2 类命名规范

**决策**: 采用 **DDD 标准命名**。

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| **聚合根 / 实体** | 无后缀，领域术语 | `Model.java`, `UploadTask.java` |
| **值对象** | 无后缀，领域术语 | `ModelName.java`, `VersionStatus.java` |
| **领域服务** | 无后缀，领域术语 + Service | `VersionLockService.java` |
| **应用服务** | 后缀 `ApplicationService` | `ModelApplicationService.java` |
| **仓储接口** | 后缀 `Repository` | `ModelRepository.java` |
| **仓储实现** | 前缀 `MyBatis` + 后缀 `Repository` | `MyBatisModelRepository.java` |
| **Mapper 接口** | 后缀 `Mapper` | `ModelMapper.java` |
| **请求 DTO** | 后缀 `Request` | `CreateModelRequest.java` |
| **响应 DTO** | 后缀 `Response` | `ModelDetailResponse.java` |
| **API 控制器** | 后缀 `Api` | `ModelApi.java` |
| **枚举** | 无后缀，领域术语 | `VersionStatus.java`, `TaskStatus.java` |

### 4.3 异常体系

**决策**: 使用项目公共模块已定义的 `ModelLiteException`，不另建异常类。

**使用方式**:
- 所有业务异常抛出 `ModelLiteException`
- 异常构造时传入错误码和消息
- 全局异常处理器统一处理，返回标准错误响应

**示例**:
```java
// 抛出业务异常
throw new ModelLiteException("0102001", "模型不存在");

// 全局异常处理器返回
{
    "code": 0102001,
    "message": "模型不存在",
    "data": null,
    "timestamp": "2026-04-22T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

### 4.4 错误码规范

**决策**: 错误码格式为 `0102YYY`。

| 组成 | 含义 | 取值 |
|------|------|------|
| **前两位** | 项目编号 | `01` |
| **中间两位** | 模块码 | `02`（模型仓模块） |
| **后三位** | 错误序号 | `001` ~ `999`，全局递增，不按业务域分段 |

**错误码示例**:

| 错误码 | 错误信息 | HTTP 状态码 |
|--------|----------|-------------|
| 0102001 | 模型不存在 | 404 |
| 0102002 | 模型名称已存在 | 409 |
| 0102003 | 模型名称不可修改 | 400 |
| 0102004 | 分类下存在模型，禁止删除 | 400 |
| 0102005 | 单资源组模型数量超限 | 400 |
| 0102006 | 版本不存在 | 404 |
| 0102007 | 版本号不连续 | 400 |
| 0102008 | 版本已被锁定 | 409 |
| 0102009 | 单模型版本数量超限 | 400 |
| 0102010 | 上传任务不存在 | 404 |
| 0102011 | 文件后缀不在白名单 | 400 |
| 0102012 | 并发上传任务数超限 | 400 |
| 0102013 | 转换任务不存在 | 404 |
| 0102014 | 不支持的转换格式 | 400 |

---

## 5. API 规范

### 5.1 URL 规范

**决策**:
- 人机接口：`/v2/ui/{resource}`
- 机机接口：`/v2/{resource}`（不加 `/ui` 和 `/m2m`）

**人机接口示例**:

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/v2/ui/models` | 创建模型 |
| GET | `/v2/ui/models` | 查询模型列表 |
| GET | `/v2/ui/models/{modelId}` | 查看模型详情 |
| PATCH | `/v2/ui/models/{modelId}` | 修改模型 |
| DELETE | `/v2/ui/models/{modelId}` | 软删除模型 |
| POST | `/v2/ui/models/{modelId}/versions` | 创建版本 |
| GET | `/v2/ui/upload-tasks` | 查询上传任务列表 |
| GET | `/v2/ui/classifications` | 查询分类列表 |

**机机接口示例**:

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/v2/models/{modelId}/versions/{versionNumber}/path` | 查询权重路径 |
| POST | `/v2/models/{modelId}/archive` | 训练权重归档 |
| POST | `/v2/versions/{versionId}/lock` | 锁定版本 |
| DELETE | `/v2/versions/{versionId}/lock` | 解锁版本 |

### 5.2 请求响应格式

**统一响应格式**:

```json
// 成功响应
{
    "code": 0,
    "message": "success",
    "data": { ... },
    "timestamp": "2026-04-22T10:30:00Z",
    "requestId": "req-uuid-xxx"
}

// 分页响应
{
    "code": 0,
    "message": "success",
    "data": {
        "items": [ ... ],
        "page": 1,
        "pageSize": 50,
        "total": 100,
        "totalPages": 2
    },
    "timestamp": "2026-04-22T10:30:00Z",
    "requestId": "req-uuid-xxx"
}

// 错误响应
{
    "code": 0102001,
    "message": "模型不存在",
    "data": null,
    "timestamp": "2026-04-22T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

### 5.3 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，从 1 开始 |
| pageSize | int | 50 | 每页条数，最大 100 |

### 5.4 排序参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| sortBy | string | createTime | 排序字段 |
| sortOrder | string | desc | 排序方向（asc / desc） |

---

## 6. 数据库规范

### 6.1 表名与字段名

**决策**: 沿用架构设计 v1.1 规范。

| 规范 | 说明 |
|------|------|
| **表名** | 蛇形命名，小写，如 `model`, `model_version` |
| **字段名** | 蛇形命名，小写，如 `create_time`, `resource_group` |
| **主键** | UUID，使用 `gen_random_uuid()` 自动生成 |
| **软删除** | `deleted` 布尔字段 + 部分索引 |
| **时间戳** | `create_time`, `update_time`，类型为 `TIMESTAMP WITH TIME ZONE` |

### 6.2 MyBatis Mapper 规范

**决策**: 使用 **XML 方式**。

**Mapper 文件位置**: `src/main/resources/mapper/`

**文件命名**: `{表名}Mapper.xml`，如 `ModelMapper.xml`

**示例**:

```xml
<!-- ModelMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.huawei.modellite.repository.infrastructure.persistence.mapper.ModelMapper">
    
    <resultMap id="ModelResultMap" type="com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model">
        <id property="modelId" column="id"/>
        <result property="name.value" column="name"/>
        <result property="description" column="description"/>
        <result property="categoryId" column="classification_id"/>
        <result property="typeId" column="type_id"/>
        <result property="resourceGroup.value" column="resource_group"/>
        <result property="createUser" column="create_user"/>
        <result property="author" column="author"/>
        <result property="deleted" column="deleted"/>
        <result property="createTime" column="create_time"/>
        <result property="updateTime" column="update_time"/>
    </resultMap>
    
    <select id="findById" resultMap="ModelResultMap">
        SELECT * FROM model WHERE id = #{modelId} AND deleted = FALSE
    </select>
    
    <insert id="insert">
        INSERT INTO model (id, name, description, classification_id, type_id, resource_group, create_user, author, deleted, create_time, update_time)
        VALUES (#{modelId}, #{name.value}, #{description}, #{categoryId}, #{typeId}, #{resourceGroup.value}, #{createUser}, #{author}, #{deleted}, #{createTime}, #{updateTime})
    </insert>
    
</mapper>
```

### 6.3 事务隔离级别

**决策**: 使用 **READ COMMITTED**（PostgreSQL 默认）。

---

## 7. 测试规范

### 7.1 测试策略

**决策**: 采用 **TDD（Test-Driven Development）**。

**流程**:
1. 先编写失败测试（表达期望行为）
2. 编写最小实现使测试通过
3. 重构代码

### 7.2 测试框架

| 框架 | 用途 |
|------|------|
| JUnit 5 | 单元测试框架 |
| Mockito | Mock 依赖 |
| Testcontainers | 集成测试（PostgreSQL 容器） |

### 7.3 测试层次

| 层次 | 说明 | 覆盖目标 |
|------|------|----------|
| **单元测试** | 领域层（聚合根、值对象、领域服务） | 80%+ |
| **集成测试** | 仓储实现、应用服务（带真实数据库） | 60%+ |
| **API 测试** | 接口层（可选） | 关键场景 |

### 7.4 测试命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| 测试类 | 后缀 `Test` | `ModelTest.java`, `ModelRepositoryTest.java` |
| 测试方法 | `should_期望行为_when_条件` | `should_throwException_when_modelNotFound()` |

### 7.5 测试示例

**单元测试示例**:

```java
class ModelTest {
    
    @Test
    void should_createModelWithFirstVersion_when_validInput() {
        // Given
        ModelName name = new ModelName("test-model");
        ResourceGroup resourceGroup = new ResourceGroup("default");
        UUID categoryId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        
        // When
        Model model = Model.createModel(name, "description", categoryId, typeId, resourceGroup, "user", WeightSource.register(...));
        
        // Then
        assertThat(model.getName()).isEqualTo(name);
        assertThat(model.getResourceGroup()).isEqualTo(resourceGroup);
        assertThat(model.getVersions()).hasSize(1);
    }
    
    @Test
    void should_throwException_when_createDuplicateVersion() {
        // Given
        Model model = createTestModel();
        
        // When/Then
        assertThatThrownBy(() -> model.createVersion(1, ...)) // 重复版本号
            .isInstanceOf(ModelLiteException.class)
            .hasMessageContaining("版本号不连续");
    }
}
```

**集成测试示例**:

```java
@Testcontainers
class MyBatisModelRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    private MyBatisModelRepository repository;
    
    @Test
    void should_saveAndLoadModel() {
        // Given
        Model model = createTestModel();
        
        // When
        repository.save(model);
        Model loaded = repository.findById(model.getModelId());
        
        // Then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo(model.getName());
    }
}
```

---

## 8. 配置管理规范

### 8.1 多环境配置

**决策**: 使用 **Spring Profile**。

**配置文件**:
- `application.yml` - 公共配置
- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

**激活方式**:
- 开发：`spring.profiles.active=dev`
- 测试：环境变量或 JVM 参数
- 生产：K8s 环境变量注入

### 8.2 配置结构

```yaml
# application.yml（公共配置）
server:
  port: 8080

spring:
  application:
    name: model-lite-repository

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/modellite_dev
    username: dev_user
    password: dev_pass
    driver-class-name: org.postgresql.Driver

logging:
  level:
    com.huawei.modellite: DEBUG

# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/modellite
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

logging:
  level:
    com.huawei.modellite: INFO
```

### 8.3 敏感配置

**方式**: 通过环境变量注入，不写入配置文件。

| 配置项 | 环境变量 |
|--------|----------|
| 数据库密码 | `DB_PASSWORD` |
| SSL 证书路径 | `SSL_CERT_PATH` |

---

## 9. 决策记录

| 决策编号 | 决策内容 | 决策理由 |
|----------|----------|----------|
| T-01 | 领域事件仅作文档记录 | 当前架构采用直接调用，非事件驱动；无事件解耦需求 |
| T-02 | 错误码格式 `0102YYY` | 统一格式便于识别和处理；模块码区分不同业务域 |
| T-03 | 跨聚合用最终一致性 + 补偿 | 避免大事务；符合聚合边界设计原则；失败可补偿 |
| T-04 | 包名沿用 `com.huawei.modellite.repository` | 与项目现有包名一致 |
| T-05 | 领域方法用业务术语 | 代码表达业务意图，而非技术实现 |
| T-06 | 人机 `/v2/ui/...`，机机 `/v2/...` | 清晰区分两类接口；机机接口不加额外前缀 |
| T-07 | 使用公共模块 `ModelLiteException` | 统一异常处理，不另建异常类体系 |
| T-08 | MyBatis 用 XML 方式 | 适合复杂查询和动态 SQL；便于 DBA 审查 |
| T-09 | 采用 TDD | 先写测试表达期望行为，再写实现 |
| T-10 | M2M 不加 `/m2m` 前缀 | 简化 URL；通过不同 Service 路由区分 |
| T-11 | 沿用架构设计 v1.1 数据库规范 | 蛇形命名、UUID 主键、deleted 软删除已确定 |
| T-12 | Spring Profile 多环境配置 | Spring Boot 标准，便于切换环境 |
| T-13 | DDD 标准类命名 | 聚合根无后缀、DTO Request/Response、仓储实现 MyBatis 前缀 |
| T-14 | READ COMMITTED 事务隔离 | PostgreSQL 默认，适合大多数场景 |

---

## 10. 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-04-22 | 初始版本，记录技术栈、DDD 实践、代码规范、API 规范、数据库规范、测试规范、配置管理规范 | Prometheus |
| v1.1 | 2026-04-22 | 删除 6.2 新增表、6.3 tag 表更新、6.4 model_type 表更新（设计内容不属于代码规范文档） | Prometheus |

---

## 11. 参考文档

- [ModelLite-模型仓库-架构设计-v1.1.md](/docs/architecture/ModelLite-模型仓库-架构设计-v1.1.md)
- [ModelLite-Repository-DDD-Bounded-Context.md](./ModelLite-Repository-DDD-Bounded-Context.md)
- [ModelLite-模型仓库-需求规格说明书-v1.2.md](/docs/ModelLite-模型仓库-需求规格说明书-v1.2.md)

---

**文档结束**