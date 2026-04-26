# Feature 2: 分类体系与标签管理 — 特性设计文档

> **文档类型**: 特性设计文档
> **文档版本**: v1.0
> **编写日期**: 2026-04-25
> **适用范围**: ModelLite 平台模型仓库模块 Feature 2
> **目标读者**: 后端开发工程师

---

## 1. 特性概述

### 1.1 目标

实现模型仓库的分类体系（两级分类：分类 + 模型类型）与标签管理能力，为模型提供结构化的组织方式。包括分类/类型的 CRUD、标签的 CRUD、模型与标签的关联管理、以及内置预设数据的填充机制。

### 1.2 范围

**IN（包含）**:
- Category 聚合根的领域模型实现（含 ModelType 实体）
- Tag 聚合根的领域模型实现（含 ModelTag、ModelTypeTag 值对象）
- CategoryRepository 仓储接口与 MyBatis 实现
- TagRepository 仓储接口与 MyBatis 实现
- CategoryApplicationService 应用服务
- TagApplicationService 应用服务
- 分类/类型管理的人机接口（CRUD）
- 标签管理的人机接口（CRUD + 模型关联）
- 内置预设数据填充机制（SQL 脚本 + 应用启动校验）
- 全局异常处理器（分类/标签相关错误处理）

**OUT（不包含）**:
- 模型 CRUD 接口（Feature 3）
- 模型列表查询中的分类/类型/标签筛选逻辑（Feature 3）
- 版本锁管理（Feature 5）
- M2M 接口（Feature 8）
- 具体内置预设数据清单（另起文档补充）
- 操作日志上报（Feature 8）

### 1.3 依赖关系

| 依赖项 | 类型 | 说明 |
|--------|------|------|
| Feature 1: 基础设施与通用能力 | 特性 | 数据库 Schema（category, model_type, tag, model_tag, model_type_tag 表）、枚举定义（TagType）、错误码定义、MyBatis 配置 |
| com.huawei.modellite.common 公共模块 | 外部依赖 | 提供 ModelLiteException、BaseResponse 等 |

---

## 2. 数据库设计

### 2.1 新增/变更表 DDL

> 本特性涉及的 5 张表已在 Feature 1 中创建空表，DDL 不变更。此处仅补充数据字典和业务约束说明。

#### category（分类表）

**DDL**: 见 Feature 1 §2.1。

**本特性新增业务规则**:
- `is_builtin = TRUE` 的分类不允许删除
- 删除分类前须确保 `model` 表中无 `category_id` 引用（检查 `WHERE category_id = ? AND deleted = FALSE`）

#### model_type（模型类型表）

**DDL**: 见 Feature 1 §2.1。

**本特性新增业务规则**:
- `is_builtin = TRUE` 的类型不允许删除
- 删除类型前须确保 `model` 表中无 `type_id` 引用（检查 `WHERE type_id = ? AND deleted = FALSE`）

#### tag（标签表）

**DDL**: 见 Feature 1 §2.1。

**本特性新增业务规则**:
- `is_builtin = TRUE` 的标签不允许删除
- 删除标签时级联清理 `model_tag` 和 `model_type_tag` 表中 `tag_id` 对应的关联记录

#### model_tag（模型-标签关联表）

**DDL**: 见 Feature 1 §2.1。本特性无 DDL 变更。

#### model_type_tag（模型类型-标签关联表）

**DDL**: 见 Feature 1 §2.1。本特性无 DDL 变更。

### 2.2 表关系图（ER 图）

```mermaid
erDiagram
    category {
        UUID id "分类ID"
        VARCHAR name "分类名称（唯一）"
        VARCHAR description "分类描述"
        BOOLEAN is_builtin "是否内置"
    }
    
    model_type {
        UUID id "类型ID"
        UUID category_id "所属分类ID"
        VARCHAR name "类型名称"
        VARCHAR description "类型描述"
        BOOLEAN is_builtin "是否内置"
    }
    
    tag {
        UUID id "标签ID"
        VARCHAR name "标签名称（唯一）"
        VARCHAR tag_type "USER / CAPABILITY"
        BOOLEAN is_builtin "是否内置"
    }
    
    model_tag {
        UUID id "关联ID"
        UUID model_id "模型ID"
        UUID tag_id "标签ID"
    }
    
    model_type_tag {
        UUID id "关联ID"
        UUID type_id "类型ID"
        UUID tag_id "标签ID"
    }
    
    category ||--o{ model_type : "包含多个类型"
    model_type ||--o{ model_type_tag : "关联能力标签"
    tag ||--o{ model_tag : "被模型引用"
    tag ||--o{ model_type_tag : "被类型引用"
```

### 2.3 索引设计

> 索引已在 Feature 1 §2.3 中定义，本特性无新增索引。

### 2.4 数据字典

#### category 表

| 字段名 | 类型 | 是否必填 | 默认值 | 取值范围/说明 |
|--------|------|----------|--------|---------------|
| id | UUID | Y | 应用侧生成 | 分类 ID，UUID v4 |
| name | VARCHAR(100) | Y | — | 分类名称，全局唯一，长度 1-100 |
| description | VARCHAR(500) | N | '' | 分类描述，长度 0-500 |
| is_builtin | BOOLEAN | Y | FALSE | 是否内置分类（内置分类不可删除） |
| create_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 创建时间 |
| update_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 更新时间 |

#### model_type 表

| 字段名 | 类型 | 是否必填 | 默认值 | 取值范围/说明 |
|--------|------|----------|--------|---------------|
| id | UUID | Y | 应用侧生成 | 类型 ID，UUID v4 |
| category_id | UUID | Y | — | 所属分类 ID（外键引用 category.id） |
| name | VARCHAR(100) | Y | — | 类型名称，同一分类下唯一，长度 1-100 |
| description | VARCHAR(500) | N | '' | 类型描述，长度 0-500 |
| is_builtin | BOOLEAN | Y | FALSE | 是否内置类型（内置类型不可删除） |
| create_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 创建时间 |
| update_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 更新时间 |

#### tag 表

| 字段名 | 类型 | 是否必填 | 默认值 | 取值范围/说明 |
|--------|------|----------|--------|---------------|
| id | UUID | Y | 应用侧生成 | 标签 ID，UUID v4 |
| name | VARCHAR(50) | Y | — | 标签名称，全局唯一，长度 1-50 |
| tag_type | VARCHAR(20) | Y | — | 标签类型：USER=用户自定义标签，CAPABILITY=能力标签 |
| is_builtin | BOOLEAN | Y | FALSE | 是否内置标签（内置标签不可删除） |
| create_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 创建时间 |
| update_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 更新时间 |

#### model_tag 表

| 字段名 | 类型 | 是否必填 | 默认值 | 取值范围/说明 |
|--------|------|----------|--------|---------------|
| id | UUID | Y | 应用侧生成 | 关联 ID，UUID v4 |
| model_id | UUID | Y | — | 模型 ID（外键引用 model.id） |
| tag_id | UUID | Y | — | 标签 ID（外键引用 tag.id） |
| create_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 创建时间 |

#### model_type_tag 表

| 字段名 | 类型 | 是否必填 | 默认值 | 取值范围/说明 |
|--------|------|----------|--------|---------------|
| id | UUID | Y | 应用侧生成 | 关联 ID，UUID v4 |
| type_id | UUID | Y | — | 类型 ID（外键引用 model_type.id） |
| tag_id | UUID | Y | — | 标签 ID（外键引用 tag.id） |
| create_time | TIMESTAMP WITH TIME ZONE | Y | NOW() | 创建时间 |

### 2.5 内置预设数据填充机制

**填充方式**: SQL 脚本 `src/main/resources/sql/data/V1__builtin_data.sql`

**执行时机**: 数据库初始化阶段，由运维/CI 手动执行

**预设数据框架**:
- 内置分类：`is_builtin = TRUE`，覆盖平台默认模型分类域
- 内置类型：`is_builtin = TRUE`，各分类下的默认模型架构
- 内置标签：`is_builtin = TRUE`，至少包含 `supportFinetune`（CAPABILITY 类型）
- 内置能力标签关联：`model_type_tag` 表中关联内置类型与内置能力标签

**具体预设数据清单**: 另起文档补充，本特性仅实现填充机制和 `is_builtin` 保护逻辑。

**应用侧校验（可选增强）**: 应用启动时检查内置数据是否存在，如缺失则打印告警日志（不自动补数据，保持幂等）。

---

## 3. 领域模型设计

### 3.1 类图

#### Category 聚合

```mermaid
classDiagram
    class Category {
        +UUID categoryId
        +String name
        +String description
        +Boolean isBuiltIn
        +DateTime createTime
        +DateTime updateTime
        +List~ModelType~ modelTypes
        +addModelType(name, description) ModelType
        +removeModelType(typeId, hasModelReference) void
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

#### Tag 聚合

```mermaid
classDiagram
    class Tag {
        +UUID tagId
        +String name
        +TagType tagType
        +Boolean isBuiltIn
        +DateTime createTime
        +DateTime updateTime
    }

    class TagType {
        <<enumeration>>
        USER
        CAPABILITY
    }

    class ModelTag {
        <<ValueObject>>
        +UUID modelId
        +UUID tagId
        +DateTime createTime
    }

    class ModelTypeTag {
        <<ValueObject>>
        +UUID typeId
        +UUID tagId
        +DateTime createTime
    }

    Tag --> TagType
    Tag ..> ModelTag : creates
    Tag ..> ModelTypeTag : creates
```

### 3.2 核心类定义

#### Category（聚合根）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| categoryId | UUID | 分类唯一标识 | 创建后不可修改 |
| name | String | 分类名称 | 长度 1-100，全局唯一，创建后不可修改 |
| description | String | 分类描述 | 长度 0-500 |
| isBuiltIn | Boolean | 是否内置分类 | 内置分类不可删除 |
| createTime | DateTime | 创建时间 | 自动填充 |
| updateTime | DateTime | 更新时间 | 自动填充 |
| modelTypes | List\<ModelType\> | 分类下的类型列表 | 聚合内实体，延迟加载 |

| 方法名 | 参数 | 返回类型 | 说明 | 业务规则 |
|--------|------|----------|------|----------|
| addModelType | name: String, description: String | ModelType | 在分类下添加模型类型 | 前置：name 在本分类下唯一；后置：新增 ModelType 实体，isBuiltIn=FALSE |
| removeModelType | typeId: UUID, hasModelReference: boolean | void | 删除分类下的模型类型 | 前置：typeId 存在于本分类下；前置：isBuiltIn=FALSE；前置：hasModelReference=FALSE（无模型引用） |

#### ModelType（实体）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| typeId | UUID | 类型唯一标识 | 创建后不可修改 |
| name | String | 类型名称 | 长度 1-100，同一分类下唯一，创建后不可修改 |
| description | String | 类型描述 | 长度 0-500 |
| isBuiltIn | Boolean | 是否内置类型 | 内置类型不可删除 |

#### Tag（聚合根）

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| tagId | UUID | 标签唯一标识 | 创建后不可修改 |
| name | String | 标签名称 | 长度 1-50，全局唯一 |
| tagType | TagType | 标签类型 | USER 或 CAPABILITY |
| isBuiltIn | Boolean | 是否内置标签 | 内置标签不可删除 |
| createTime | DateTime | 创建时间 | 自动填充 |
| updateTime | DateTime | 更新时间 | 自动填充 |

> Tag 聚合根本身不持有 ModelTag / ModelTypeTag 列表。关联关系通过 CategoryApplicationService / TagApplicationService 编排，由 CategoryRepository / TagRepository 管理关联表的持久化。

#### 关键方法伪代码

**Category.addModelType**:
```java
public ModelType addModelType(String name, String description) {
    // 前置条件：名称在本分类下唯一
    boolean nameExists = modelTypes.stream()
            .anyMatch(mt -> mt.getName().equals(name));
    if (nameExists) {
        throw new ModelLiteException(ErrorCode.MODEL_TYPE_NAME_EXISTS, 
                "分类下已存在同名模型类型: " + name);
    }
    
    // 创建实体
    ModelType modelType = new ModelType(
            UUID.randomUUID(), name, description, false);
    modelTypes.add(modelType);
    return modelType;
}
```

**Category.removeModelType**:
```java
public void removeModelType(UUID typeId, boolean hasModelReference) {
    ModelType modelType = modelTypes.stream()
            .filter(mt -> mt.getTypeId().equals(typeId))
            .findFirst()
            .orElseThrow(() -> new ModelLiteException(
                    ErrorCode.MODEL_TYPE_NOT_FOUND, "模型类型不存在"));
    
    if (modelType.getIsBuiltIn()) {
        throw new ModelLiteException(ErrorCode.MODEL_TYPE_BUILTIN, 
                "内置模型类型不允许删除");
    }
    
    if (hasModelReference) {
        throw new ModelLiteException(ErrorCode.CATEGORY_HAS_MODELS, 
                "模型类型下存在模型，禁止删除");
    }
    
    modelTypes.remove(modelType);
}
```

**Tag 创建（静态工厂方法）**:
```java
public static Tag createUserTag(String name) {
    return new Tag(UUID.randomUUID(), name, TagType.USER, false);
}

public static Tag createCapabilityTag(String name) {
    return new Tag(UUID.randomUUID(), name, TagType.CAPABILITY, false);
}
```

### 3.3 值对象定义

| 值对象名 | 字段名 | 类型 | 说明 | 校验规则 |
|----------|--------|------|------|----------|
| TagType | — | 枚举 | 标签类型 | USER / CAPABILITY，由 TypeHandler 转换 |
| ModelTag | modelId | UUID | 模型 ID | 非空，引用有效模型 |
| | tagId | UUID | 标签 ID | 非空，引用有效标签 |
| | createTime | DateTime | 创建时间 | 自动填充 |
| ModelTypeTag | typeId | UUID | 类型 ID | 非空，引用有效模型类型 |
| | tagId | UUID | 标签 ID | 非空，引用有效标签 |
| | createTime | DateTime | 创建时间 | 自动填充 |

### 3.4 领域服务

本特性无需独立领域服务。Category 和 Tag 聚合的业务逻辑均在聚合根内完成。

### 3.5 仓储接口

#### CategoryRepository

| 方法名 | 参数 | 返回类型 | 说明 |
|--------|------|----------|------|
| save | Category | void | 保存分类（含级联保存 ModelType） |
| findById | UUID categoryId | Optional\<Category\> | 按 ID 查找分类（不含 ModelType 列表） |
| findByIdWithTypes | UUID categoryId | Optional\<Category\> | 按 ID 查找分类（含 ModelType 列表） |
| findAll | — | List\<Category\> | 查询全部分类（不含 ModelType 列表） |
| findAllWithTypes | — | List\<Category\> | 查询全部分类（含 ModelType 列表） |
| existsByName | String name | boolean | 检查分类名称是否存在 |
| hasModelReference | UUID categoryId | boolean | 检查分类下是否有模型 |
| hasModelReferenceByTypeId | UUID typeId | boolean | 检查类型下是否有模型 |
| deleteById | UUID categoryId | void | 删除分类（含级联删除 ModelType） |
| deleteModelTypeById | UUID typeId | void | 删除指定模型类型 |

#### TagRepository

| 方法名 | 参数 | 返回类型 | 说明 |
|--------|------|----------|------|
| save | Tag | void | 保存标签 |
| findById | UUID tagId | Optional\<Tag\> | 按 ID 查找标签 |
| findAll | — | List\<Tag\> | 查询全部标签 |
| findByTagType | TagType tagType | List\<Tag\> | 按标签类型筛选 |
| existsByName | String name | boolean | 检查标签名称是否存在 |
| hasReference | UUID tagId | boolean | 检查标签是否被模型或类型引用 |
| deleteById | UUID tagId | void | 删除标签 |
| removeModelTagsByTagId | UUID tagId | int | 级联删除指定标签的所有 model_tag 关联，返回删除行数 |
| removeModelTypeTagsByTagId | UUID tagId | int | 级联删除指定标签的所有 model_type_tag 关联，返回删除行数 |
| addModelTag | UUID modelId, UUID tagId | void | 为模型添加标签 |
| removeModelTag | UUID modelId, UUID tagId | void | 从模型移除标签 |
| findTagsByModelId | UUID modelId | List\<Tag\> | 查询模型关联的标签列表 |
| findModelIdsByTagId | UUID tagId | List\<UUID\> | 查询关联了指定标签的模型 ID 列表 |
| addModelTypeTag | UUID typeId, UUID tagId | void | 为模型类型添加能力标签 |
| removeModelTypeTag | UUID typeId, UUID tagId | void | 从模型类型移除能力标签 |
| findTagsByModelTypeId | UUID typeId | List\<Tag\> | 查询模型类型关联的能力标签列表 |

### 3.6 业务不变量

| 不变量名 | 说明 | 强制方式 |
|----------|------|----------|
| 分类名称唯一 | 分类名称全局唯一 | 数据库唯一约束 `uk_category_name` + 代码校验 |
| 类型名称唯一 | 同一分类下类型名称唯一 | 数据库唯一约束 `uk_model_type_name` + 代码校验 |
| 标签名称唯一 | 标签名称全局唯一 | 数据库唯一约束 `uk_tag_name` + 代码校验 |
| 内置分类不可删除 | is_builtin=TRUE 的分类不允许删除 | 代码校验 |
| 内置类型不可删除 | is_builtin=TRUE 的类型不允许删除 | 代码校验 |
| 内置标签不可删除 | is_builtin=TRUE 的标签不允许删除 | 代码校验 |
| 分类下无模型时才可删除 | 删除分类前须确保下无模型引用 | 代码校验（查询 model 表） |
| 类型下无模型时才可删除 | 删除类型前须确保下无模型引用 | 代码校验（查询 model 表） |
| 标签删除级联清理关联 | 删除标签时级联清理 model_tag 和 model_type_tag 关联 | 代码实现（先删关联再删标签） |
| 模型标签上限 | 同一模型最多 20 个标签 | 代码校验 |

---

## 4. 接口设计

### 4.1 人机接口（User API）

#### 4.1.1 查询分类列表

| 属性 | 值 |
|------|-----|
| URL | `GET /v2/ui/categories` |
| Method | GET |
| 描述 | 查询所有分类列表（含各分类下的模型类型列表） |

**Request Parameters**: 无

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": [
        {
            "id": "uuid-xxx",
            "name": "TextGeneration",
            "description": "文本生成模型",
            "isBuiltin": true,
            "modelTypes": [
                {
                    "id": "uuid-yyy",
                    "name": "glm-5",
                    "description": "GLM-5 模型",
                    "isBuiltin": true
                }
            ],
            "createTime": "2026-04-25T10:00:00Z",
            "updateTime": "2026-04-25T10:00:00Z"
        }
    ],
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 无 | — | 此接口不返回业务错误码 |

---

#### 4.1.2 查询分类详情

| 属性 | 值 |
|------|-----|
| URL | `GET /v2/ui/categories/{categoryId}` |
| Method | GET |
| 描述 | 查询指定分类详情及其下的模型类型列表 |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| categoryId | UUID | Y | 分类 ID |

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "id": "uuid-xxx",
        "name": "TextGeneration",
        "description": "文本生成模型",
        "isBuiltin": true,
        "modelTypes": [
            {
                "id": "uuid-yyy",
                "name": "glm-5",
                "description": "GLM-5 模型",
                "isBuiltin": true
            }
        ],
        "createTime": "2026-04-25T10:00:00Z",
        "updateTime": "2026-04-25T10:00:00Z"
    },
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102015 | 404 | 分类不存在 |

---

#### 4.1.3 添加分类

| 属性 | 值 |
|------|-----|
| URL | `POST /v2/ui/categories` |
| Method | POST |
| 描述 | 创建新的模型分类（用户自定义分类，isBuiltin=false） |

**Request Body**:
```json
{
    "name": "MyCategory",          // 分类名称，必填，1-100字符
    "description": "自定义分类"     // 描述，可选，0-500字符
}
```

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "id": "uuid-new",
        "name": "MyCategory",
        "description": "自定义分类",
        "isBuiltin": false,
        "modelTypes": [],
        "createTime": "2026-04-25T10:30:00Z",
        "updateTime": "2026-04-25T10:30:00Z"
    },
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102016 | 409 | 分类名称已存在 |

**业务规则**:
- **前置条件**: 分类名称全局唯一
- **校验规则**: name 长度 1-100，description 长度 0-500
- **后置条件**: 新建分类 isBuiltin=false，无下级类型

---

#### 4.1.4 删除分类

| 属性 | 值 |
|------|-----|
| URL | `DELETE /v2/ui/categories/{categoryId}` |
| Method | DELETE |
| 描述 | 删除模型分类（真删除） |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| categoryId | UUID | Y | 分类 ID |

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": null,
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102015 | 404 | 分类不存在 |
| 0102017 | 400 | 内置分类不允许删除 |
| 0102004 | 400 | 分类下存在模型，禁止删除 |

**业务规则**:
- **前置条件**: 分类存在、isBuiltin=false、分类下无模型引用
- **后置条件**: 分类及其下所有模型类型被物理删除
- **级联删除**: 删除分类时，级联删除该分类下的所有 model_type 记录（仅当无模型引用时才允许走到此步）

---

#### 4.1.5 添加模型类型

| 属性 | 值 |
|------|-----|
| URL | `POST /v2/ui/categories/{categoryId}/types` |
| Method | POST |
| 描述 | 在指定分类下创建新的模型类型 |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| categoryId | UUID | Y | 分类 ID |

**Request Body**:
```json
{
    "name": "MyModelType",         // 类型名称，必填，1-100字符
    "description": "自定义类型"     // 描述，可选，0-500字符
}
```

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "id": "uuid-new",
        "name": "MyModelType",
        "description": "自定义类型",
        "isBuiltin": false,
        "createTime": "2026-04-25T10:30:00Z",
        "updateTime": "2026-04-25T10:30:00Z"
    },
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102015 | 404 | 分类不存在 |
| 0102018 | 409 | 同分类下类型名称已存在 |

**业务规则**:
- **前置条件**: 分类存在
- **校验规则**: name 长度 1-100，同一分类下唯一
- **后置条件**: 新建类型 isBuiltin=false

---

#### 4.1.6 删除模型类型

| 属性 | 值 |
|------|-----|
| URL | `DELETE /v2/ui/categories/{categoryId}/types/{typeId}` |
| Method | DELETE |
| 描述 | 删除指定模型类型（真删除） |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| categoryId | UUID | Y | 分类 ID |
| typeId | UUID | Y | 类型 ID |

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": null,
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102015 | 404 | 分类不存在 |
| 0102019 | 404 | 模型类型不存在 |
| 0102020 | 400 | 内置模型类型不允许删除 |
| 0102004 | 400 | 类型下存在模型，禁止删除 |

**业务规则**:
- **前置条件**: 分类存在、类型存在且属于该分类、isBuiltin=false、类型下无模型引用
- **级联处理**: 删除类型时，同步清理 `model_type_tag` 中该类型的关联记录

---

#### 4.1.7 查询标签列表

| 属性 | 值 |
|------|-----|
| URL | `GET /v2/ui/tags` |
| Method | GET |
| 描述 | 查询全部标签列表 |

**Request Parameters**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| tagType | String | N | — | 标签类型筛选：USER / CAPABILITY |

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": [
        {
            "id": "uuid-xxx",
            "name": "supportFinetune",
            "tagType": "CAPABILITY",
            "isBuiltin": true,
            "createTime": "2026-04-25T10:00:00Z",
            "updateTime": "2026-04-25T10:00:00Z"
        },
        {
            "id": "uuid-yyy",
            "name": "experimental",
            "tagType": "USER",
            "isBuiltin": false,
            "createTime": "2026-04-25T10:00:00Z",
            "updateTime": "2026-04-25T10:00:00Z"
        }
    ],
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**: 无

---

#### 4.1.8 添加标签

| 属性 | 值 |
|------|-----|
| URL | `POST /v2/ui/tags` |
| Method | POST |
| 描述 | 创建新的用户自定义标签（tagType=USER） |

**Request Body**:
```json
{
    "name": "experimental"          // 标签名称，必填，1-50字符
}
```

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "id": "uuid-new",
        "name": "experimental",
        "tagType": "USER",
        "isBuiltin": false,
        "createTime": "2026-04-25T10:30:00Z",
        "updateTime": "2026-04-25T10:30:00Z"
    },
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102021 | 409 | 标签名称已存在 |

**业务规则**:
- **前置条件**: 标签名称全局唯一
- **校验规则**: name 长度 1-50
- **后置条件**: 新建标签 tagType=USER，isBuiltin=false

---

#### 4.1.9 删除标签

| 属性 | 值 |
|------|-----|
| URL | `DELETE /v2/ui/tags/{tagId}` |
| Method | DELETE |
| 描述 | 删除标签（仅用户自定义标签可删除） |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tagId | UUID | Y | 标签 ID |

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": null,
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102022 | 404 | 标签不存在 |
| 0102023 | 400 | 内置标签不允许删除 |

**业务规则**:
- **前置条件**: 标签存在、isBuiltin=false
- **级联处理**: 删除标签时，级联清理 `model_tag` 和 `model_type_tag` 表中该标签的关联记录
- **执行顺序**: 先删 `model_tag` → 再删 `model_type_tag` → 最后删 `tag`
- **幂等性**: 若关联表无记录，DELETE 0 行不报错

---

#### 4.1.10 为模型添加标签

| 属性 | 值 |
|------|-----|
| URL | `POST /v2/ui/models/{modelId}/tags` |
| Method | POST |
| 描述 | 为指定模型添加标签 |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| modelId | UUID | Y | 模型 ID |

**Request Body**:
```json
{
    "tagIds": ["uuid-tag1", "uuid-tag2"]   // 标签 ID 列表，必填
}
```

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": null,
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102001 | 404 | 模型不存在 |
| 0102022 | 404 | 标签不存在 |
| 0102025 | 400 | 模型标签数量超过上限（20个） |

**业务规则**:
- **前置条件**: 模型存在、标签存在
- **校验规则**: 添加后模型总标签数 ≤ 20；已存在的标签关联跳过（幂等）
- **后置条件**: model_tag 表新增关联记录

---

#### 4.1.11 从模型移除标签

| 属性 | 值 |
|------|-----|
| URL | `DELETE /v2/ui/models/{modelId}/tags/{tagId}` |
| Method | DELETE |
| 描述 | 从指定模型移除标签 |

**Path Parameters**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| modelId | UUID | Y | 模型 ID |
| tagId | UUID | Y | 标签 ID |

**Response Body**（成功）:
```json
{
    "code": 0,
    "message": "success",
    "data": null,
    "timestamp": "2026-04-25T10:30:00Z",
    "requestId": "req-uuid-xxx"
}
```

**错误码**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102001 | 404 | 模型不存在 |
| 0102022 | 404 | 标签不存在 |
| 0102026 | 404 | 模型与标签无关联 |

**业务规则**:
- **前置条件**: 模型存在、关联存在
- **后置条件**: model_tag 表删除对应关联记录

---

### 4.2 机机接口（M2M API）

本特性不涉及 M2M 接口。

---

## 5. 核心业务流程

### 5.1 分类/类型创建流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Api as CategoryApi
    participant AppSvc as CategoryApplicationService
    participant Category as Category聚合
    participant Repo as CategoryRepository
    participant DB as 数据库
    
    User->>Api: POST /v2/ui/categories {name, description}
    Api->>AppSvc: createCategory(name, description)
    AppSvc->>Repo: existsByName(name)
    Repo->>DB: SELECT COUNT(*) FROM category WHERE name = ?
    DB-->>Repo: 0
    AppSvc->>Category: new Category(id, name, description, false)
    AppSvc->>Repo: save(category)
    Repo->>DB: INSERT INTO category (...)
    DB-->>Repo: OK
    Repo-->>AppSvc: void
    AppSvc-->>Api: category
    Api-->>User: BaseResponse.success(category)
    
    alt 名称已存在
        AppSvc->>Repo: existsByName(name)
        Repo-->>AppSvc: true
        AppSvc-->>User: BaseResponse.error(0102016, "分类名称已存在")
    end
```

**流程说明**:
1. 用户提交分类名称和描述
2. 应用服务校验名称唯一性
3. 创建 Category 聚合根（isBuiltin=false）
4. 通过 Repository 持久化到数据库

### 5.2 分类/类型删除流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Api as CategoryApi
    participant AppSvc as CategoryApplicationService
    participant Repo as CategoryRepository
    participant DB as 数据库
    
    User->>Api: DELETE /v2/ui/categories/{categoryId}
    Api->>AppSvc: deleteCategory(categoryId)
    AppSvc->>Repo: findById(categoryId)
    Repo->>DB: SELECT * FROM category WHERE id = ?
    DB-->>Repo: category
    AppSvc->>Category: 检查 isBuiltIn
    alt isBuiltIn = true
        AppSvc-->>User: BaseResponse.error(0102017, "内置分类不允许删除")
    end
    AppSvc->>Repo: hasModelReference(categoryId)
    Repo->>DB: SELECT COUNT(*) FROM model WHERE category_id = ? AND deleted = FALSE
    DB-->>Repo: count > 0
    alt 有模型引用
        AppSvc-->>User: BaseResponse.error(0102004, "分类下存在模型，禁止删除")
    end
    AppSvc->>Repo: deleteById(categoryId)
    Repo->>DB: DELETE FROM model_type_tag WHERE type_id IN (SELECT id FROM model_type WHERE category_id = ?)
    Repo->>DB: DELETE FROM model_type WHERE category_id = ?
    Repo->>DB: DELETE FROM category WHERE id = ?
    DB-->>Repo: OK
    AppSvc-->>User: BaseResponse.success(null)
```

**流程说明**:
1. 查找分类，验证存在性
2. 检查 isBuiltIn（内置不可删除）
3. 检查是否有模型引用
4. 级联清理 model_type_tag 关联
5. 级联删除 model_type 记录
6. 删除 category 记录

### 5.3 标签添加到模型流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Api as TagApi
    participant AppSvc as TagApplicationService
    participant TagRepo as TagRepository
    participant ModelRepo as ModelRepository
    participant DB as 数据库
    
    User->>Api: POST /v2/ui/models/{modelId}/tags {tagIds}
    Api->>AppSvc: addTagsToModel(modelId, tagIds)
    AppSvc->>ModelRepo: existsById(modelId)
    ModelRepo-->>AppSvc: true
    AppSvc->>TagRepo: findTagsByModelId(modelId)
    TagRepo-->>AppSvc: existingTags (当前标签列表)
    AppSvc->>AppSvc: 计算添加后标签总数 ≤ 20
    
    loop 每个 tagId
        AppSvc->>TagRepo: findById(tagId)
        TagRepo-->>AppSvc: tag
        AppSvc->>TagRepo: addModelTag(modelId, tagId)
        TagRepo->>DB: INSERT INTO model_tag (id, model_id, tag_id) VALUES (...)
        Note over TagRepo,DB: 已存在的关联跳过（幂等，INSERT ON CONFLICT DO NOTHING）
    end
    
    AppSvc-->>User: BaseResponse.success(null)
```

**流程说明**:
1. 校验模型存在
2. 获取模型当前标签列表，计算添加后总数 ≤ 20
3. 逐个添加标签关联（幂等：已存在则跳过）

### 5.4 标签级联删除流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Api as TagApi
    participant AppSvc as TagApplicationService
    participant TagRepo as TagRepository
    participant DB as 数据库
    
    User->>Api: DELETE /v2/ui/tags/{tagId}
    Api->>AppSvc: deleteTag(tagId)
    AppSvc->>TagRepo: findById(tagId)
    TagRepo->>DB: SELECT * FROM tag WHERE id = ?
    DB-->>TagRepo: tag
    AppSvc->>Tag: 检查 isBuiltIn
    alt isBuiltIn = true
        AppSvc-->>User: BaseResponse.error(0102023, "内置标签不允许删除")
    end
    Note over AppSvc,DB: 级联清理关联（先删关联再删标签）
    AppSvc->>TagRepo: removeModelTagsByTagId(tagId)
    TagRepo->>DB: DELETE FROM model_tag WHERE tag_id = ?
    DB-->>TagRepo: 删除 N 行
    AppSvc->>TagRepo: removeModelTypeTagsByTagId(tagId)
    TagRepo->>DB: DELETE FROM model_type_tag WHERE tag_id = ?
    DB-->>TagRepo: 删除 M 行
    AppSvc->>TagRepo: deleteById(tagId)
    TagRepo->>DB: DELETE FROM tag WHERE id = ?
    DB-->>TagRepo: OK
    AppSvc-->>User: BaseResponse.success(null)
```

**流程说明**:
1. 查找标签，验证存在性
2. 检查 isBuiltIn（内置不可删除）
3. **级联删除 model_tag 关联**：清除所有使用该标签的模型关联
4. **级联删除 model_type_tag 关联**：清除所有使用该标签的类型关联
5. 删除 tag 记录
6. 事务保证：以上操作在同一事务内完成，要么全部成功，要么全部回滚

---

## 6. 测试用例

### 6.1 单元测试（领域模型）

#### 6.1.1 Category.addModelType — 成功添加

**Given**:
- 已创建一个 Category 聚合根，categoryId = "cat-uuid"

**When**:
- 调用 `category.addModelType("NewType", "新类型描述")`

**Then**:
- 返回 ModelType 实体，name = "NewType"，isBuiltin = false
- category.getModelTypes() 包含新增的 ModelType

---

#### 6.1.2 Category.addModelType — 名称重复拒绝

**Given**:
- 已创建一个 Category 聚合根，且 modelTypes 中已有 name="glm-5" 的类型

**When**:
- 调用 `category.addModelType("glm-5", "重复名称")`

**Then**:
- 抛出 ModelLiteException，错误码 = 0102018
- 异常消息包含 "分类下已存在同名模型类型"

---

#### 6.1.3 Category.removeModelType — 成功删除

**Given**:
- 已创建一个 Category 聚合根，且 modelTypes 中有 name="glm-5"，isBuiltin=false 的类型

**When**:
- 调用 `category.removeModelType(typeId, false)`（hasModelReference=false）

**Then**:
- modelTypes 中不再包含该类型
- category.getModelTypes().size() 减 1

---

#### 6.1.4 Category.removeModelType — 内置类型拒绝删除

**Given**:
- 已创建一个 Category 聚合根，且 modelTypes 中有 name="glm-5"，isBuiltin=true 的类型

**When**:
- 调用 `category.removeModelType(typeId, false)`

**Then**:
- 抛出 ModelLiteException，错误码 = 0102020
- 异常消息包含 "内置模型类型不允许删除"

---

#### 6.1.5 Category.removeModelType — 有模型引用拒绝删除

**Given**:
- 已创建一个 Category 聚合根，且 modelTypes 中有 isBuiltin=false 的类型

**When**:
- 调用 `category.removeModelType(typeId, true)`（hasModelReference=true）

**Then**:
- 抛出 ModelLiteException，错误码 = 0102004
- 异常消息包含 "模型类型下存在模型，禁止删除"

---

#### 6.1.6 Category.removeModelType — 类型不存在

**Given**:
- 已创建一个 Category 聚合根，modelTypes 中无 typeId = "non-existent-uuid" 的类型

**When**:
- 调用 `category.removeModelType(UUID.randomUUID(), false)`

**Then**:
- 抛出 ModelLiteException，错误码 = 0102019
- 异常消息包含 "模型类型不存在"

---

#### 6.1.7 Tag.createUserTag — 创建用户标签

**Given**:
- 无前置条件

**When**:
- 调用 `Tag.createUserTag("experimental")`

**Then**:
- 返回 Tag 实体，name = "experimental"，tagType = USER，isBuiltin = false

---

#### 6.1.8 Tag.createCapabilityTag — 创建能力标签

**Given**:
- 无前置条件

**When**:
- 调用 `Tag.createCapabilityTag("supportFinetune")`

**Then**:
- 返回 Tag 实体，name = "supportFinetune"，tagType = CAPABILITY，isBuiltin = false

---

#### 6.1.9 Category 构造 — name 为空拒绝

**Given**:
- 无前置条件

**When**:
- 调用 `new Category(UUID.randomUUID(), "", "", false)`

**Then**:
- 抛出异常（名称不可为空）

---

#### 6.1.10 Category 构造 — name 超长拒绝

**Given**:
- name 为 101 字符的字符串

**When**:
- 调用 `new Category(UUID.randomUUID(), "a".repeat(101), "", false)`

**Then**:
- 抛出异常（名称长度超限，最大 100）

---

#### 6.1.11 Tag 构造 — name 为空拒绝

**Given**:
- 无前置条件

**When**:
- 调用 `new Tag(UUID.randomUUID(), "", TagType.USER, false)`

**Then**:
- 抛出异常（名称不可为空）

---

#### 6.1.12 Tag 构造 — name 超长拒绝

**Given**:
- name 为 51 字符的字符串

**When**:
- 调用 `new Tag(UUID.randomUUID(), "a".repeat(51), TagType.USER, false)`

**Then**:
- 抛出异常（名称长度超限，最大 50）

---

### 6.2 集成测试（仓储）

#### 6.2.1 CategoryRepository.save — 保存分类含类型

**Given**:
- H2 内存数据库已初始化

**When**:
- 调用 `categoryRepository.save(category)` 保存一个含 2 个 ModelType 的分类
- 再调用 `categoryRepository.findByIdWithTypes(categoryId)`

**Then**:
- 查询结果存在
- 查询结果的 modelTypes 包含 2 个 ModelType
- 各字段值与保存时一致（name, description, isBuiltin）

---

#### 6.2.2 CategoryRepository.findById — 仅查询分类不含类型

**Given**:
- H2 数据库中已有分类（含 2 个 ModelType）

**When**:
- 调用 `categoryRepository.findById(categoryId)`

**Then**:
- 查询结果存在
- 查询结果的 modelTypes 为空列表（延迟加载）

---

#### 6.2.3 CategoryRepository.findById — 分类不存在

**Given**:
- H2 数据库中无目标分类

**When**:
- 调用 `categoryRepository.findById(UUID.randomUUID())`

**Then**:
- 返回 Optional.empty()

---

#### 6.2.4 CategoryRepository.findAll — 查询全部分类

**Given**:
- H2 数据库中已有 3 个分类

**When**:
- 调用 `categoryRepository.findAll()`

**Then**:
- 返回 3 个分类
- 每个分类的 modelTypes 为空（不含类型）

---

#### 6.2.5 CategoryRepository.findAllWithTypes — 查询全部分类含类型

**Given**:
- H2 数据库中已有 2 个分类，各含 1-2 个 ModelType

**When**:
- 调用 `categoryRepository.findAllWithTypes()`

**Then**:
- 返回 2 个分类
- 每个分类的 modelTypes 包含完整的类型列表

---

#### 6.2.6 CategoryRepository.existsByName — 名称存在

**Given**:
- H2 数据库中已有 name="TextGeneration" 的分类

**When**:
- 调用 `categoryRepository.existsByName("TextGeneration")`

**Then**:
- 返回 true

**When**:
- 调用 `categoryRepository.existsByName("NotExist")`

**Then**:
- 返回 false

---

#### 6.2.7 CategoryRepository.hasModelReference — 有模型引用

**Given**:
- H2 数据库中已有分类和 model 记录（category_id = 分类 ID）

**When**:
- 调用 `categoryRepository.hasModelReference(categoryId)`

**Then**:
- 返回 true

**When**:
- 调用 `categoryRepository.hasModelReference(无引用的分类ID)`

**Then**:
- 返回 false

---

#### 6.2.8 CategoryRepository.hasModelReferenceByTypeId — 类型有模型引用

**Given**:
- H2 数据库中已有 model 记录（type_id = 类型 ID）

**When**:
- 调用 `categoryRepository.hasModelReferenceByTypeId(typeId)`

**Then**:
- 返回 true

**When**:
- 调用 `categoryRepository.hasModelReferenceByTypeId(无引用的类型ID)`

**Then**:
- 返回 false

---

#### 6.2.9 CategoryRepository.deleteById — 删除分类级联删除类型

**Given**:
- H2 数据库中已有分类和其下 2 个 model_type 记录

**When**:
- 调用 `categoryRepository.deleteById(categoryId)`

**Then**:
- category 表中该记录被删除
- model_type 表中该分类下的记录也被删除

---

#### 6.2.10 CategoryRepository.deleteModelTypeById — 删除单个类型

**Given**:
- H2 数据库中已有分类，其下有 2 个 model_type 记录

**When**:
- 调用 `categoryRepository.deleteModelTypeById(typeId1)`

**Then**:
- model_type 表中仅删除 typeId1 的记录
- 另一个 model_type 记录仍存在

---

#### 6.2.11 TagRepository.save + findById — 保存和查询标签

**Given**:
- H2 内存数据库已初始化

**When**:
- 调用 `tagRepository.save(tag)` 保存一个 USER 类型标签
- 再调用 `tagRepository.findById(tagId)`

**Then**:
- 查询结果存在
- tag.name, tag.tagType, tag.isBuiltin 与保存时一致

---

#### 6.2.12 TagRepository.findById — 标签不存在

**Given**:
- H2 数据库中无目标标签

**When**:
- 调用 `tagRepository.findById(UUID.randomUUID())`

**Then**:
- 返回 Optional.empty()

---

#### 6.2.13 TagRepository.findAll — 查询全部标签

**Given**:
- H2 数据库中已有 3 个标签（2 个 USER + 1 个 CAPABILITY）

**When**:
- 调用 `tagRepository.findAll()`

**Then**:
- 返回 3 个标签

---

#### 6.2.14 TagRepository.findByTagType — 按类型筛选

**Given**:
- H2 数据库中已有 2 个 USER 标签和 1 个 CAPABILITY 标签

**When**:
- 调用 `tagRepository.findByTagType(TagType.USER)`

**Then**:
- 返回 2 个 USER 类型标签

**When**:
- 调用 `tagRepository.findByTagType(TagType.CAPABILITY)`

**Then**:
- 返回 1 个 CAPABILITY 类型标签

---

#### 6.2.15 TagRepository.existsByName — 名称存在检查

**Given**:
- H2 数据库中已有 name="NLP" 的标签

**When**:
- 调用 `tagRepository.existsByName("NLP")`

**Then**:
- 返回 true

**When**:
- 调用 `tagRepository.existsByName("NotExist")`

**Then**:
- 返回 false

---

#### 6.2.16 TagRepository.hasReference — 标签被模型引用

**Given**:
- H2 数据库中已有 tag (id=tagId) 和 model_tag 关联记录 (tag_id=tagId)

**When**:
- 调用 `tagRepository.hasReference(tagId)`

**Then**:
- 返回 true

**When**:
- 调用 `tagRepository.hasReference(未被引用的标签ID)`

**Then**:
- 返回 false

---

#### 6.2.17 TagRepository.hasReference — 标签被类型引用

**Given**:
- H2 数据库中已有 tag (id=tagId) 和 model_type_tag 关联记录 (tag_id=tagId)

**When**:
- 调用 `tagRepository.hasReference(tagId)`

**Then**:
- 返回 true（标签被模型类型引用也算被引用）

---

#### 6.2.18 TagRepository.deleteById — 删除标签

**Given**:
- H2 数据库中已有 tag (id=tagId)

**When**:
- 调用 `tagRepository.deleteById(tagId)`

**Then**:
- tag 表中该记录被删除
- 调用 `tagRepository.findById(tagId)` 返回 Optional.empty()

---

#### 6.2.18a TagRepository.removeModelTagsByTagId — 级联删除模型标签关联

**Given**:
- H2 数据库中已有 tag (id=tagId)
- model_tag 表中有 3 条 tag_id=tagId 的关联记录（关联到 model-1、model-2、model-3）

**When**:
- 调用 `tagRepository.removeModelTagsByTagId(tagId)`

**Then**:
- 返回删除行数 = 3
- model_tag 表中不再存在 tag_id=tagId 的记录
- 调用 `tagRepository.findTagsByModelId(model-1)` 返回空列表

---

#### 6.2.18b TagRepository.removeModelTagsByTagId — 无关联时返回 0

**Given**:
- H2 数据库中已有 tag (id=tagId)
- model_tag 表中无 tag_id=tagId 的记录

**When**:
- 调用 `tagRepository.removeModelTagsByTagId(tagId)`

**Then**:
- 返回删除行数 = 0
- 不抛出异常

---

#### 6.2.18c TagRepository.removeModelTypeTagsByTagId — 级联删除类型标签关联

**Given**:
- H2 数据库中已有 tag (id=tagId)
- model_type_tag 表中有 2 条 tag_id=tagId 的关联记录（关联到 type-1、type-2）

**When**:
- 调用 `tagRepository.removeModelTypeTagsByTagId(tagId)`

**Then**:
- 返回删除行数 = 2
- model_type_tag 表中不再存在 tag_id=tagId 的记录
- 调用 `tagRepository.findTagsByModelTypeId(type-1)` 返回空列表

---

#### 6.2.18d TagRepository.removeModelTypeTagsByTagId — 无关联时返回 0

**Given**:
- H2 数据库中已有 tag (id=tagId)
- model_type_tag 表中无 tag_id=tagId 的记录

**When**:
- 调用 `tagRepository.removeModelTypeTagsByTagId(tagId)`

**Then**:
- 返回删除行数 = 0
- 不抛出异常

---

#### 6.2.19 TagRepository.addModelTag — 添加模型标签关联

**Given**:
- H2 数据库中已有 model (id=modelId) 和 tag (id=tagId)

**When**:
- 调用 `tagRepository.addModelTag(modelId, tagId)`
- 再调用 `tagRepository.findTagsByModelId(modelId)`

**Then**:
- 返回的标签列表包含刚添加的标签

---

#### 6.2.20 TagRepository.addModelTag — 重复添加幂等

**Given**:
- H2 数据库中已有 model_tag 关联记录 (modelId, tagId)

**When**:
- 再次调用 `tagRepository.addModelTag(modelId, tagId)`

**Then**:
- 不抛出异常（幂等处理）
- model_tag 表中仍只有 1 条关联记录

---

#### 6.2.21 TagRepository.removeModelTag — 移除模型标签关联

**Given**:
- H2 数据库中已有 model_tag 关联记录 (modelId, tagId)

**When**:
- 调用 `tagRepository.removeModelTag(modelId, tagId)`
- 再调用 `tagRepository.findTagsByModelId(modelId)`

**Then**:
- 返回的标签列表不包含已移除的标签

---

#### 6.2.22 TagRepository.removeModelTag — 移除不存在的关联

**Given**:
- H2 数据库中无 (modelId, tagId) 的关联记录

**When**:
- 调用 `tagRepository.removeModelTag(modelId, tagId)`

**Then**:
- 不抛出异常（幂等处理）

---

#### 6.2.23 TagRepository.findModelIdsByTagId — 按标签查模型

**Given**:
- H2 数据库中已有 tag (id=tagId)，2 个模型关联了该标签

**When**:
- 调用 `tagRepository.findModelIdsByTagId(tagId)`

**Then**:
- 返回 2 个模型 ID

---

#### 6.2.24 TagRepository.addModelTypeTag — 添加类型标签关联

**Given**:
- H2 数据库中已有 model_type (id=typeId) 和 tag (id=tagId)

**When**:
- 调用 `tagRepository.addModelTypeTag(typeId, tagId)`
- 再调用 `tagRepository.findTagsByModelTypeId(typeId)`

**Then**:
- 返回的标签列表包含刚添加的标签

---

#### 6.2.25 TagRepository.removeModelTypeTag — 移除类型标签关联

**Given**:
- H2 数据库中已有 model_type_tag 关联记录 (typeId, tagId)

**When**:
- 调用 `tagRepository.removeModelTypeTag(typeId, tagId)`
- 再调用 `tagRepository.findTagsByModelTypeId(typeId)`

**Then**:
- 返回的标签列表不包含已移除的标签

---

### 6.3 API 测试（接口层）

#### 6.3.1 GET /v2/ui/categories — 查询分类列表成功

**Given**:
- 数据库中已有 2 个分类，各含 1-2 个 model_type

**When**:
- 调用 `GET /v2/ui/categories`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data 包含 2 个分类
- 每个分类包含 modelTypes 列表
- 分类按创建时间排序

---

#### 6.3.2 GET /v2/ui/categories — 空数据库返回空列表

**Given**:
- 数据库中无任何分类

**When**:
- 调用 `GET /v2/ui/categories`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data 为空列表 []

---

#### 6.3.3 GET /v2/ui/categories/{categoryId} — 查询分类详情成功

**Given**:
- 数据库中已有分类（id="cat-1"，含 2 个 model_type）

**When**:
- 调用 `GET /v2/ui/categories/cat-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data.id = "cat-1"
- Response.data.name 正确
- Response.data.modelTypes.size() = 2

---

#### 6.3.4 GET /v2/ui/categories/{categoryId} — 分类不存在 404

**Given**:
- 数据库中无 id="non-existent" 的分类

**When**:
- 调用 `GET /v2/ui/categories/non-existent`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102015
- Response.message 包含 "分类不存在"

---

#### 6.3.5 POST /v2/ui/categories — 创建分类成功

**Given**:
- 数据库中无名为 "TestCategory" 的分类

**When**:
- 调用 `POST /v2/ui/categories` body: `{"name": "TestCategory", "description": "测试分类"}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data.name = "TestCategory"
- Response.data.isBuiltin = false
- Response.data.modelTypes 为空列表

---

#### 6.3.6 POST /v2/ui/categories — 创建分类名称重复 409

**Given**:
- 数据库中已有名为 "TestCategory" 的分类

**When**:
- 调用 `POST /v2/ui/categories` body: `{"name": "TestCategory"}`

**Then**:
- HTTP 状态码 = 409
- Response.code = 0102016
- Response.message 包含 "分类名称已存在"

---

#### 6.3.7 POST /v2/ui/categories — 创建分类名称为空 400

**Given**:
- 无前置条件

**When**:
- 调用 `POST /v2/ui/categories` body: `{"name": ""}`

**Then**:
- HTTP 状态码 = 400
- Response.code 非零
- Response.message 包含参数校验错误信息

---

#### 6.3.8 POST /v2/ui/categories — 创建分类名称超长 400

**Given**:
- name 为 101 字符

**When**:
- 调用 `POST /v2/ui/categories` body: `{"name": "a".repeat(101)}`

**Then**:
- HTTP 状态码 = 400
- Response.code 非零
- Response.message 包含参数校验错误信息

---

#### 6.3.9 DELETE /v2/ui/categories/{categoryId} — 删除分类成功

**Given**:
- 数据库中已有分类（id="cat-1"，is_builtin=false，其下有 1 个 model_type）
- 该分类下无模型引用

**When**:
- 调用 `DELETE /v2/ui/categories/cat-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- category 表中 cat-1 记录被删除
- model_type 表中该分类下的记录也被级联删除

---

#### 6.3.10 DELETE /v2/ui/categories/{categoryId} — 分类不存在 404

**Given**:
- 数据库中无 id="non-existent" 的分类

**When**:
- 调用 `DELETE /v2/ui/categories/non-existent`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102015

---

#### 6.3.11 DELETE /v2/ui/categories/{categoryId} — 内置分类拒绝删除 400

**Given**:
- 数据库中已有 id="cat-builtin"，is_builtin=true 的分类

**When**:
- 调用 `DELETE /v2/ui/categories/cat-builtin`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102017
- Response.message 包含 "内置分类不允许删除"

---

#### 6.3.12 DELETE /v2/ui/categories/{categoryId} — 有模型引用拒绝删除 400

**Given**:
- 数据库中已有分类（id="cat-1"，is_builtin=false）
- model 表中存在 category_id="cat-1" 的模型记录

**When**:
- 调用 `DELETE /v2/ui/categories/cat-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102004
- Response.message 包含 "分类下存在模型，禁止删除"

---

#### 6.3.13 POST /v2/ui/categories/{categoryId}/types — 添加模型类型成功

**Given**:
- 数据库中已有分类（id="cat-1"）
- 该分类下无名为 "NewType" 的类型

**When**:
- 调用 `POST /v2/ui/categories/cat-1/types` body: `{"name": "NewType", "description": "新类型"}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data.name = "NewType"
- Response.data.isBuiltin = false

---

#### 6.3.14 POST /v2/ui/categories/{categoryId}/types — 分类不存在 404

**Given**:
- 数据库中无 id="non-existent" 的分类

**When**:
- 调用 `POST /v2/ui/categories/non-existent/types` body: `{"name": "NewType"}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102015

---

#### 6.3.15 POST /v2/ui/categories/{categoryId}/types — 类型名称重复 409

**Given**:
- 数据库中已有分类（id="cat-1"），其下已有 name="glm-5" 的模型类型

**When**:
- 调用 `POST /v2/ui/categories/cat-1/types` body: `{"name": "glm-5"}`

**Then**:
- HTTP 状态码 = 409
- Response.code = 0102018
- Response.message 包含 "同分类下类型名称已存在"

---

#### 6.3.16 DELETE /v2/ui/categories/{categoryId}/types/{typeId} — 删除类型成功

**Given**:
- 数据库中已有分类（id="cat-1"）和其下模型类型（id="type-1"，is_builtin=false）
- 该类型下无模型引用

**When**:
- 调用 `DELETE /v2/ui/categories/cat-1/types/type-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_type 表中 type-1 记录被删除
- model_type_tag 表中 type-1 的关联记录也被级联删除

---

#### 6.3.17 DELETE /v2/ui/categories/{categoryId}/types/{typeId} — 分类不存在 404

**Given**:
- 数据库中无 id="non-existent" 的分类

**When**:
- 调用 `DELETE /v2/ui/categories/non-existent/types/type-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102015

---

#### 6.3.18 DELETE /v2/ui/categories/{categoryId}/types/{typeId} — 类型不存在 404

**Given**:
- 数据库中已有分类（id="cat-1"）
- 数据库中无 id="non-existent" 的类型

**When**:
- 调用 `DELETE /v2/ui/categories/cat-1/types/non-existent`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102019

---

#### 6.3.19 DELETE /v2/ui/categories/{categoryId}/types/{typeId} — 内置类型拒绝删除 400

**Given**:
- 数据库中已有分类（id="cat-1"）和其下模型类型（id="type-builtin"，is_builtin=true）

**When**:
- 调用 `DELETE /v2/ui/categories/cat-1/types/type-builtin`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102020
- Response.message 包含 "内置模型类型不允许删除"

---

#### 6.3.20 DELETE /v2/ui/categories/{categoryId}/types/{typeId} — 类型下有模型引用 400

**Given**:
- 数据库中已有分类（id="cat-1"）和其下模型类型（id="type-1"，is_builtin=false）
- model 表中存在 type_id="type-1" 的模型记录

**When**:
- 调用 `DELETE /v2/ui/categories/cat-1/types/type-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102004
- Response.message 包含 "类型下存在模型，禁止删除"

---

#### 6.3.21 GET /v2/ui/tags — 查询全部标签

**Given**:
- 数据库中已有 3 个标签（2 个 USER + 1 个 CAPABILITY）

**When**:
- 调用 `GET /v2/ui/tags`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data 包含 3 个标签

---

#### 6.3.22 GET /v2/ui/tags — 按类型筛选 CAPABILITY

**Given**:
- 数据库中已有 2 个 USER 标签和 1 个 CAPABILITY 标签

**When**:
- 调用 `GET /v2/ui/tags?tagType=CAPABILITY`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data 包含 1 个标签
- Response.data[0].tagType = "CAPABILITY"

---

#### 6.3.23 GET /v2/ui/tags — 按类型筛选 USER

**Given**:
- 数据库中已有 2 个 USER 标签和 1 个 CAPABILITY 标签

**When**:
- 调用 `GET /v2/ui/tags?tagType=USER`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data 包含 2 个标签
- 所有标签的 tagType = "USER"

---

#### 6.3.24 POST /v2/ui/tags — 创建标签成功

**Given**:
- 数据库中无名为 "experimental" 的标签

**When**:
- 调用 `POST /v2/ui/tags` body: `{"name": "experimental"}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- Response.data.name = "experimental"
- Response.data.tagType = "USER"
- Response.data.isBuiltin = false

---

#### 6.3.25 POST /v2/ui/tags — 标签名称重复 409

**Given**:
- 数据库中已有名为 "NLP" 的标签

**When**:
- 调用 `POST /v2/ui/tags` body: `{"name": "NLP"}`

**Then**:
- HTTP 状态码 = 409
- Response.code = 0102021
- Response.message 包含 "标签名称已存在"

---

#### 6.3.26 POST /v2/ui/tags — 标签名称为空 400

**Given**:
- 无前置条件

**When**:
- 调用 `POST /v2/ui/tags` body: `{"name": ""}`

**Then**:
- HTTP 状态码 = 400
- Response.code 非零

---

#### 6.3.27 POST /v2/ui/tags — 标签名称超长 400

**Given**:
- name 为 51 字符

**When**:
- 调用 `POST /v2/ui/tags` body: `{"name": "a".repeat(51)}`

**Then**:
- HTTP 状态码 = 400
- Response.code 非零

---

#### 6.3.28 DELETE /v2/ui/tags/{tagId} — 删除标签成功

**Given**:
- 数据库中已有标签（id="tag-1"，is_builtin=false，无模型或类型引用）

**When**:
- 调用 `DELETE /v2/ui/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- tag 表中 tag-1 记录被删除

---

#### 6.3.29 DELETE /v2/ui/tags/{tagId} — 标签不存在 404

**Given**:
- 数据库中无 id="non-existent" 的标签

**When**:
- 调用 `DELETE /v2/ui/tags/non-existent`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102022

---

#### 6.3.30 DELETE /v2/ui/tags/{tagId} — 内置标签拒绝删除 400

**Given**:
- 数据库中已有标签（id="tag-builtin"，is_builtin=true）

**When**:
- 调用 `DELETE /v2/ui/tags/tag-builtin`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102023
- Response.message 包含 "内置标签不允许删除"

---

#### 6.3.31 DELETE /v2/ui/tags/{tagId} — 标签被模型引用时级联删除关联

**Given**:
- 数据库中已有标签（id="tag-1"，is_builtin=false）
- model_tag 表中存在 2 条 tag_id="tag-1" 的关联记录（关联到 model-1、model-2）

**When**:
- 调用 `DELETE /v2/ui/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_tag 表中 tag_id="tag-1" 的 2 条关联记录被删除
- tag 表中 id="tag-1" 的记录被删除
- 调用 `GET /v2/ui/models/model-1/tags`，返回列表不含 tag-1

---

#### 6.3.32 DELETE /v2/ui/tags/{tagId} — 标签被类型引用时级联删除关联

**Given**:
- 数据库中已有标签（id="tag-1"，is_builtin=false）
- model_type_tag 表中存在 tag_id="tag-1" 的关联记录（关联到 type-1）

**When**:
- 调用 `DELETE /v2/ui/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_type_tag 表中 tag_id="tag-1" 的关联记录被删除
- tag 表中 id="tag-1" 的记录被删除

---

#### 6.3.32a DELETE /v2/ui/tags/{tagId} — 标签同时被模型和类型引用时级联删除所有关联

**Given**:
- 数据库中已有标签（id="tag-1"，is_builtin=false）
- model_tag 表中存在 2 条 tag_id="tag-1" 的关联记录（关联到 model-1、model-2）
- model_type_tag 表中存在 1 条 tag_id="tag-1" 的关联记录（关联到 type-1）

**When**:
- 调用 `DELETE /v2/ui/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_tag 表中 tag_id="tag-1" 的 2 条关联记录被删除
- model_type_tag 表中 tag_id="tag-1" 的 1 条关联记录被删除
- tag 表中 id="tag-1" 的记录被删除
- 调用 `GET /v2/ui/models/model-1/tags`，返回列表不含 tag-1
- 调用 `GET /v2/ui/categories/{catId}/types/{typeId}/tags`，返回列表不含 tag-1

---

#### 6.3.33 POST /v2/ui/models/{modelId}/tags — 为模型添加标签成功

**Given**:
- 数据库中已有 model (id="model-1") 和 tag (id="tag-1")
- model-1 当前无标签

**When**:
- 调用 `POST /v2/ui/models/model-1/tags` body: `{"tagIds": ["tag-1"]}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_tag 表中新增 1 条关联记录

---

#### 6.3.34 POST /v2/ui/models/{modelId}/tags — 添加多个标签成功

**Given**:
- 数据库中已有 model (id="model-1") 和 3 个标签 (id="tag-1","tag-2","tag-3")
- model-1 当前无标签

**When**:
- 调用 `POST /v2/ui/models/model-1/tags` body: `{"tagIds": ["tag-1", "tag-2", "tag-3"]}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_tag 表中新增 3 条关联记录

---

#### 6.3.35 POST /v2/ui/models/{modelId}/tags — 重复添加幂等

**Given**:
- 数据库中已有 model (id="model-1") 和 tag (id="tag-1")
- model-1 已关联 tag-1

**When**:
- 调用 `POST /v2/ui/models/model-1/tags` body: `{"tagIds": ["tag-1"]}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0（幂等成功）
- model_tag 表中仍只有 1 条 (model-1, tag-1) 关联

---

#### 6.3.36 POST /v2/ui/models/{modelId}/tags — 模型不存在 404

**Given**:
- 数据库中无 id="non-existent" 的模型

**When**:
- 调用 `POST /v2/ui/models/non-existent/tags` body: `{"tagIds": ["tag-1"]}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102001

---

#### 6.3.37 POST /v2/ui/models/{modelId}/tags — 标签不存在 404

**Given**:
- 数据库中已有 model (id="model-1")
- 数据库中无 id="tag-nonexistent" 的标签

**When**:
- 调用 `POST /v2/ui/models/model-1/tags` body: `{"tagIds": ["tag-nonexistent"]}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102022

---

#### 6.3.38 POST /v2/ui/models/{modelId}/tags — 标签数量超过上限 400

**Given**:
- 数据库中已有 model (id="model-1")
- model-1 已有 20 个标签关联

**When**:
- 调用 `POST /v2/ui/models/model-1/tags` body: `{"tagIds": ["tag-new"]}`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102025
- Response.message 包含 "模型标签数量超过上限"

---

#### 6.3.39 DELETE /v2/ui/models/{modelId}/tags/{tagId} — 移除标签成功

**Given**:
- 数据库中已有 model (id="model-1") 和 tag (id="tag-1")
- model_tag 表中存在 (model-1, tag-1) 关联

**When**:
- 调用 `DELETE /v2/ui/models/model-1/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0
- model_tag 表中该关联记录被删除

---

#### 6.3.40 DELETE /v2/ui/models/{modelId}/tags/{tagId} — 模型不存在 404

**Given**:
- 数据库中无 id="non-existent" 的模型

**When**:
- 调用 `DELETE /v2/ui/models/non-existent/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102001

---

#### 6.3.41 DELETE /v2/ui/models/{modelId}/tags/{tagId} — 标签不存在 404

**Given**:
- 数据库中已有 model (id="model-1")
- 数据库中无 id="tag-nonexistent" 的标签

**When**:
- 调用 `DELETE /v2/ui/models/model-1/tags/tag-nonexistent`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102022

---

#### 6.3.42 DELETE /v2/ui/models/{modelId}/tags/{tagId} — 关联不存在 404

**Given**:
- 数据库中已有 model (id="model-1") 和 tag (id="tag-1")
- model_tag 表中无 (model-1, tag-1) 关联

**When**:
- 调用 `DELETE /v2/ui/models/model-1/tags/tag-1`

**Then**:
- HTTP 状态码 = 200
- Response.code = 0102026
- Response.message 包含 "模型与标签无关联"

---

## 附录：新增错误码定义

> 以下错误码在 Feature 1 定义的 ErrorCode 基础上扩展。错误码格式遵循 `0102YYY`，全局递增。

```java
// ===== 分类相关 =====
public static final String CATEGORY_NOT_FOUND = "0102015";             // 分类不存在
public static final String CATEGORY_NAME_EXISTS = "0102016";           // 分类名称已存在
public static final String CATEGORY_BUILTIN = "0102017";               // 内置分类不允许删除

// ===== 模型类型相关 =====
public static final String MODEL_TYPE_NAME_EXISTS = "0102018";         // 同分类下类型名称已存在
public static final String MODEL_TYPE_NOT_FOUND = "0102019";           // 模型类型不存在
public static final String MODEL_TYPE_BUILTIN = "0102020";             // 内置模型类型不允许删除

// ===== 标签相关 =====
public static final String TAG_NAME_EXISTS = "0102021";                // 标签名称已存在
public static final String TAG_NOT_FOUND = "0102022";                  // 标签不存在
public static final String TAG_BUILTIN = "0102023";                    // 内置标签不允许删除
// 0102024: 已废弃（原"标签仍被引用，禁止删除"，现改为级联删除关联后删除标签）
public static final String MODEL_TAG_LIMIT_EXCEEDED = "0102025";       // 模型标签数量超过上限
public static final String MODEL_TAG_NOT_FOUND = "0102026";            // 模型与标签无关联
```

**新增错误码与 HTTP 状态码映射**:

| 错误码 | HTTP 状态码 | 说明 |
|--------|-------------|------|
| 0102015 | 404 | 分类不存在 |
| 0102016 | 409 | 分类名称冲突 |
| 0102017 | 400 | 内置分类保护 |
| 0102018 | 409 | 类型名称冲突 |
| 0102019 | 404 | 模型类型不存在 |
| 0102020 | 400 | 内置类型保护 |
| 0102021 | 409 | 标签名称冲突 |
| 0102022 | 404 | 标签不存在 |
| 0102023 | 400 | 内置标签保护 |
| ~~0102024~~ | ~~400~~ | ~~已废弃：原"标签被引用"，现改为级联删除~~ |
| 0102025 | 400 | 标签数量超限 |
| 0102026 | 404 | 标签关联不存在 |

---

**文档结束**