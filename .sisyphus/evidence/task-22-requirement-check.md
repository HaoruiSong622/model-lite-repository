# Task 22: 需求文档对照检查报告

> **检查日期**: 2026-04-27
> **检查范围**: Feature 2（分类体系与标签管理）、Feature 3（模型与版本生命周期管理）
> **结论**: 存在若干偏差与遗漏，详见下方清单

---

## 一、Feature 2 检查结果

### 1.1 API 端点对照（设计文档要求 11 个）

| # | 设计文档端点 | 设计方法 | 代码实现 | 状态 | 备注 |
|---|-------------|---------|---------|------|------|
| 1 | `GET /v2/ui/categories` | 查询分类列表 | `CategoryApi.getAllCategories()` | ✅ 匹配 | — |
| 2 | `GET /v2/ui/categories/{categoryId}` | 查询分类详情 | `CategoryApi.getCategoryById(UUID)` | ✅ 匹配 | — |
| 3 | `POST /v2/ui/categories` | 添加分类 | `CategoryApi.createCategory(CategoryRequest)` | ✅ 匹配 | — |
| 4 | `DELETE /v2/ui/categories/{categoryId}` | 删除分类 | `CategoryApi.deleteCategory(UUID)` | ✅ 匹配 | — |
| 5 | `POST /v2/ui/categories/{categoryId}/types` | 添加模型类型 | `CategoryApi.addModelTypeToCategory(UUID, UUID)` | ⚠️ 偏差 | 见下方说明 |
| 6 | `DELETE /v2/ui/categories/{categoryId}/types/{typeId}` | 删除模型类型 | `CategoryApi.removeModelTypeFromCategory(UUID, UUID)` | ✅ 匹配 | — |
| 7 | `GET /v2/ui/tags` | 查询标签列表 | `TagApi.getAllTags()` | ⚠️ 偏差 | 见下方说明 |
| 8 | `POST /v2/ui/tags` | 添加标签 | `TagApi.createTag(TagRequest)` | ✅ 匹配 | — |
| 9 | `DELETE /v2/ui/tags/{tagId}` | 删除标签 | `TagApi.deleteTag(UUID)` | ✅ 匹配 | — |
| 10 | `POST /v2/ui/models/{modelId}/tags` | 为模型添加标签 | `TagApi.addTagToModel(UUID, UUID)` | ⚠️ 偏差 | 见下方说明 |
| 11 | `DELETE /v2/ui/models/{modelId}/tags/{tagId}` | 从模型移除标签 | `TagApi.removeTagFromModel(UUID, UUID)` | ✅ 匹配 | — |

**端点偏差详情:**

#### ⚠️ 偏差 1: 端点 #5 — 添加模型类型 API 签名不匹配
- **设计文档**: `POST /v2/ui/categories/{categoryId}/types`，Request Body: `{"name": "MyModelType", "description": "自定义类型"}`
- **实际代码**: `CategoryApi.addModelTypeToCategory(UUID id, UUID typeId)`，接收 `UUID typeId` 作为 RequestBody
- **影响**: API 接口与设计文档不一致。设计文档要求前端传入 name+description 创建新类型；实际代码接收一个 typeId UUID，且实现直接抛出 `UnsupportedOperationException`
- **严重度**: **高** — 该端点完全不可用

#### ⚠️ 偏差 2: 端点 #7 — 标签列表查询缺少 tagType 筛选参数
- **设计文档**: `GET /v2/ui/tags?tagType=USER`，支持按 tagType 筛选
- **实际代码**: `TagApi.getAllTags()` 无 `tagType` 查询参数，调用 `tagApplicationService.getAllTags()` 返回全部标签
- **影响**: 不支持按标签类型筛选
- **严重度**: **中** — 功能缺失但不影响基础 CRUD

#### ⚠️ 偏差 3: 端点 #10 — 为模型添加标签 API 签名不匹配
- **设计文档**: `POST /v2/ui/models/{modelId}/tags`，Request Body: `{"tagIds": ["uuid-tag1", "uuid-tag2"]}` — 批量添加
- **实际代码**: `TagApi.addTagToModel(UUID modelId, UUID tagId)` — 单个添加，RequestBody 为单个 UUID
- **影响**: 无法批量添加标签；且缺少模型存在性校验和标签上限校验（20个）
- **严重度**: **高** — API 语义不匹配，缺少标签上限校验

### 1.2 CategoryRepository 方法对照（设计文档要求 10 个）

| # | 方法名 | 参数 | 返回类型 | 代码实现 | 状态 |
|---|--------|------|---------|---------|------|
| 1 | save | Category | void | ✅ | — |
| 2 | findById | UUID categoryId | Optional\<Category\> | ✅ | — |
| 3 | findByIdWithTypes | UUID categoryId | Optional\<Category\> | ✅ | — |
| 4 | findAll | — | List\<Category\> | ✅ | — |
| 5 | findAllWithTypes | — | List\<Category\> | ✅ | — |
| 6 | existsByName | String name | boolean | ✅ | — |
| 7 | hasModelReference | UUID categoryId | boolean | ✅ | — |
| 8 | hasModelReferenceByTypeId | UUID typeId | boolean | ✅ | — |
| 9 | deleteById | UUID categoryId | void | ✅ | — |
| 10 | deleteModelTypeById | UUID typeId | void | ✅ | — |

**结论**: CategoryRepository 10/10 方法全部实现 ✅

### 1.3 TagRepository 方法对照（设计文档要求 16 个）

| # | 方法名 | 参数 | 返回类型 | 代码实现 | 状态 |
|---|--------|------|---------|---------|------|
| 1 | save | Tag | void | ✅ | — |
| 2 | findById | UUID tagId | Optional\<Tag\> | ✅ | — |
| 3 | findAll | — | List\<Tag\> | ✅ | — |
| 4 | findByTagType | TagType tagType | List\<Tag\> | ✅ | — |
| 5 | existsByName | String name | boolean | ✅ | — |
| 6 | hasReference | UUID tagId | boolean | ✅ | — |
| 7 | deleteById | UUID tagId | void | ✅ | — |
| 8 | removeModelTagsByTagId | UUID tagId | int | ✅ | — |
| 9 | removeModelTypeTagsByTagId | UUID tagId | int | ✅ | — |
| 10 | addModelTag | UUID modelId, UUID tagId | void | ✅ | — |
| 11 | removeModelTag | UUID modelId, UUID tagId | void | ✅ | — |
| 12 | findTagsByModelId | UUID modelId | List\<Tag\> | ✅ | — |
| 13 | findModelIdsByTagId | UUID tagId | List\<UUID\> | ✅ | — |
| 14 | addModelTypeTag | UUID typeId, UUID tagId | void | ✅ | — |
| 15 | removeModelTypeTag | UUID typeId, UUID tagId | void | ✅ | — |
| 16 | findTagsByModelTypeId | UUID typeId | List\<Tag\> | ✅ | — |

**结论**: TagRepository 16/16 方法全部实现 ✅

### 1.4 业务不变量检查

| # | 不变量名 | 强制方式 | 代码实现 | 状态 | 备注 |
|---|----------|---------|---------|------|------|
| 1 | 分类名称唯一 | 数据库 + 代码校验 | `CategoryApplicationService.createCategory()` 调用 `existsByName()` | ✅ | — |
| 2 | 类型名称唯一 | 数据库 + 代码校验 | `Category.addModelType()` 检查 `nameExists` | ✅ | — |
| 3 | 标签名称唯一 | 数据库 + 代码校验 | `TagApplicationService.createTag()` 调用 `existsByName()` | ✅ | — |
| 4 | 内置分类不可删除 | 代码校验 | `CategoryApplicationService.deleteCategory()` 检查 `isBuiltIn` | ✅ | — |
| 5 | 内置类型不可删除 | 代码校验 | `Category.removeModelType()` 检查 `isBuiltIn` | ✅ | — |
| 6 | 内置标签不可删除 | 代码校验 | `TagApplicationService.deleteTag()` 检查 `isBuiltIn` | ✅ | — |
| 7 | 分类下无模型时才可删除 | 代码校验 | `CategoryApplicationService.deleteCategory()` 调用 `hasModelReference()` | ✅ | — |
| 8 | 类型下无模型时才可删除 | 代码校验 | `CategoryApplicationService.removeModelTypeFromCategory()` 调用 `hasModelReferenceByTypeId()` | ✅ | — |
| 9 | 标签删除级联清理关联 | 代码实现 | `TagApplicationService.deleteTag()` 调用 `removeModelTagsByTagId()` → `removeModelTypeTagsByTagId()` → `deleteById()` | ✅ | 顺序正确 |
| 10 | 模型标签上限（20个） | 代码校验 | `TagApplicationService.addTagToModel()` 中 **未实现** | ❌ | 缺少上限校验 |

### 1.5 内置数据保护（is_builtin）检查

| 检查项 | 实现状态 | 代码位置 |
|--------|---------|---------|
| 分类 is_builtin 删除保护 | ✅ | `CategoryApplicationService.deleteCategory()` L43-46 |
| 类型 is_builtin 删除保护 | ✅ | `Category.removeModelType()` L73-76 |
| 标签 is_builtin 删除保护 | ✅ | `TagApplicationService.deleteTag()` L42-45 |

### 1.6 标签级联删除检查

| 检查项 | 实现状态 | 代码位置 |
|--------|---------|---------|
| 先删 model_tag 关联 | ✅ | `TagApplicationService.deleteTag()` L47 |
| 再删 model_type_tag 关联 | ✅ | `TagApplicationService.deleteTag()` L48 |
| 最后删 tag 本身 | ✅ | `TagApplicationService.deleteTag()` L49 |
| 执行顺序正确 | ✅ | model_tag → model_type_tag → tag |

---

## 二、Feature 3 检查结果

### 2.1 API 端点对照（设计文档要求 6 个）

| # | 设计文档端点 | 设计方法 | 代码实现 | 状态 | 备注 |
|---|-------------|---------|---------|------|------|
| 1 | `POST /v2/ui/models` | 创建模型 | `ModelApi.createModel(ModelCreateRequest)` | ✅ 匹配 | — |
| 2 | `GET /v2/ui/models/{modelId}` | 查询模型详情 | `ModelApi.getModel(UUID)` | ✅ 匹配 | — |
| 3 | `PATCH /v2/ui/models/{modelId}` | 修改模型元数据 | `ModelApi.modifyModel(UUID, ModelModifyRequest)` | ⚠️ 偏差 | 见下方说明 |
| 4 | `GET /v2/ui/models` | 查询模型列表 | `ModelApi.listModels(...)` | ⚠️ 偏差 | 见下方说明 |
| 5 | `POST /v2/ui/models/{modelId}/versions` | 创建版本 | `ModelApi.createVersion(UUID, VersionCreateRequest)` | ⚠️ 偏差 | 见下方说明 |
| 6 | `GET /v2/ui/models/{modelId}/versions/{versionId}` | 查询版本详情 | `ModelApi.getVersion(UUID, UUID)` | ✅ 匹配 | — |

**端点偏差详情:**

#### ⚠️ 偏差 4: 端点 #3 — 修改模型元数据缺少不可变字段拦截
- **设计文档**: 请求若包含 `name` 或 `resourceGroup` 字段应返回错误（0102003 / 0102028）
- **实际代码**: `ModelModifyRequest` 中虽然不包含 `name` 和 `resourceGroup` 字段（Lombok @Data 无这些属性），但未主动拦截
- **分析**: DTO 层面不暴露这两个字段，相当于隐式实现。但设计文档要求"请求中若包含这些字段则返回错误"，实际代码只是忽略，无显式错误返回
- **严重度**: **低** — 行为等价，但缺少显式错误提示

#### ⚠️ 偏差 5: 端点 #4 — 模型列表查询缺少 RBAC 资源组过滤
- **设计文档**: RBAC 过滤：普通用户只能看到自己资源组 + "public" 资源组的模型；管理员可看到全部。需通过 `findByResourceGroups()` 实现
- **实际代码**: `ModelApi.listModels()` 将查询条件直接传给 `ModelApplicationService.listModels(condition)`，由 `condition.getResourceGroups()` 是否为空决定走 `findByResourceGroups` 还是 `findByCondition`。但 API 层 **未从用户上下文构建可见资源组列表**
- **影响**: RBAC 资源组可见性过滤未在应用服务层自动构建。需依赖前端/中间件传入 `resourceGroups` 参数，若未传入则返回全部模型（无权限隔离）
- **严重度**: **高** — 安全性漏洞

#### ⚠️ 偏差 6: 端点 #5 — 创建版本缺少 isRegistered 字段和版本容量校验
- **设计文档**: Request Body 含 `isRegistered` 字段；注册模式 `status=Available`，非注册模式 `status=NoWeight`
- **实际代码**: `VersionCreateRequest` 无 `isRegistered` 字段。`ModelApplicationService.createVersion()` 通过 `storagePath != null && sourceType != null` 推断状态为 `AVAILABLE`，否则为 `NO_WEIGHT`。创建版本时 `isRegistered` 硬编码为 `false`
- **影响**: 无法区分注册模式和上传模式；NFS 模式下 PVC 名称自动生成规则 `pvc-{modelId}-v{versionNumber}` 未实现
- **严重度**: **中** — 版本创建模式逻辑不完整

### 2.2 ModelRepository 方法对照（设计文档要求 12 个）

| # | 方法名 | 参数 | 返回类型 | 代码实现 | 状态 |
|---|--------|------|---------|---------|------|
| 1 | save | Model | void | ✅ | — |
| 2 | findById | UUID modelId | Optional\<Model\> | ✅ | — |
| 3 | findByIdWithVersions | UUID modelId | Optional\<Model\> | ✅ | — |
| 4 | findVersionById | UUID modelId, UUID versionId | Optional\<ModelVersion\> | ✅ | — |
| 5 | findAll | — | List\<Model\> | ✅ | — |
| 6 | findByCondition | ModelQueryCondition | PageResult\<Model\> | ✅ | — |
| 7 | findByResourceGroups | List\<String\>, ModelQueryCondition | PageResult\<Model\> | ✅ | — |
| 8 | existsByCategoryAndTypeAndName | UUID, UUID, String | boolean | ✅ | — |
| 9 | countByResourceGroup | String resourceGroup | long | ✅ | — |
| 10 | countAll | — | long | ✅ | — |
| 11 | update | Model | void | ✅ | — |
| 12 | updateVersion | ModelVersion | void | ✅ | — |

**附加方法（设计文档有提及但不在 12 个核心方法中）:**

| 方法名 | 代码实现 | 状态 |
|--------|---------|------|
| findTagIdsByModelId | ✅ | 设计文档 §3.5 提及，已实现 |

**结论**: ModelRepository 12/12 核心方法全部实现 ✅，附加方法也已实现

### 2.3 ModelDomainService 方法检查（设计文档要求 2 个）

| # | 方法名 | 设计文档要求 | 代码实现 | 状态 |
|---|--------|-------------|---------|------|
| 1 | validateModelCreation | 校验分类存在、类型存在且属于该分类、名称唯一、资源组容量<100、全局容量<1000 | ✅ 完整实现 | — |
| 2 | validateModelModification | 校验模型存在；若 categoryId/typeId 变更，校验新分类+类型下名称唯一、新类型属于新分类 | ✅ 完整实现 | — |

**结论**: ModelDomainService 2/2 方法全部实现 ✅

### 2.4 容量限制检查

| 检查项 | 设计值 | 代码实现 | 状态 | 代码位置 |
|--------|-------|---------|------|---------|
| 单资源组模型上限 | 100 | `MAX_MODELS_PER_RESOURCE_GROUP = 100` | ✅ | `ModelDomainService` L20 |
| 全局模型上限 | 1000 | `MAX_MODELS_GLOBAL = 1000` | ✅ | `ModelDomainService` L21 |
| 单模型版本上限 | 50 | `MAX_VERSIONS = 50` | ✅ | `Model.createVersion()` L109, `Model` L15 |

### 2.5 版本号递增无间隔检查

| 检查项 | 实现状态 | 代码位置 | 备注 |
|--------|---------|---------|------|
| 版本号 = max(现有) + 1 | ✅ | `Model.createVersion()` L114: `getLatestVersionNumber() + 1` | — |
| 首版本号 = 1 | ✅ | `ModelVersion.createInitialVersion()` L38: `versionNumber=1` | — |
| getLatestVersionNumber() | ⚠️ | `Model.getLatestVersionNumber()` L130 | 假设 versions 非空列表；空列表会抛 IndexOutOfBoundsException |

### 2.6 资源组可见性（RBAC）检查

| 检查项 | 设计文档要求 | 代码实现 | 状态 | 备注 |
|--------|-------------|---------|------|------|
| 查询时 RBAC 过滤 | `findByResourceGroups()` | Repository 层有实现 | ⚠️ | API/应用服务层未构建用户可见资源组列表 |
| 模型详情 RBAC | 无权限返回 404 | 未实现 | ❌ | `getModel()` 无 RBAC 校验 |
| 修改模型 RBAC | 仅所属资源组用户可修改 | 未实现 | ❌ | `modifyModel()` 无 RBAC 校验 |
| 创建版本 RBAC | 仅所属资源组用户可创建 | 未实现 | ❌ | `createVersion()` 无 RBAC 校验 |
| 公共资源组 "public" | 对所有用户可见 | 未在应用层体现 | ❌ | 无 `isPublicResourceGroup()` 逻辑 |

**RBAC 总体评估**: Repository 层已提供 `findByResourceGroups()` 方法，但应用服务层和 API 层未实现 RBAC 逻辑。所有模型操作缺乏权限控制。

### 2.7 StoragePath PVC/NFS 模式校验检查

| 检查项 | 设计文档要求 | 代码实现 | 状态 |
|--------|-------------|---------|------|
| PVC 模式 pvcName 必填 | `StoragePath.ofPvc()` 校验 | ✅ | `StoragePath.ofPvc()` L30-32 |
| NFS 模式 nfsServer+nfsPath 必填 | `StoragePath.ofNfs()` 校验 | ✅ | `StoragePath.ofNfs()` L38-39 |
| PVC 模式 nfsServer/nfsPath = null | 构造时设置 | ✅ | `StoragePath.ofPvc()` L34 |
| NFS 模式 pvcName = null | 构造时设置 | ✅ | `StoragePath.ofNfs()` L42 |
| NFS 模式 PVC 自动命名 `pvc-{modelId}-v{versionNumber}` | 应用服务层生成 | ❌ | 未实现自动 PVC 命名 |
| `StoragePath.empty()` 静态方法 | 设计文档要求 | ✅ | `StoragePath.empty()` L46 |

### 2.8 其他 Feature 3 检查项

| 检查项 | 设计文档要求 | 代码实现 | 状态 |
|--------|-------------|---------|------|
| Model.createModel 静态工厂 | 创建模型 + 首版本 | ✅ | `Model.createModel()` L56-81 |
| 首版本 status=NoWeight | 初始版本无权重 | ✅ | `ModelVersion.createInitialVersion()` L41 |
| 首版本 isRegistered=false | 初始版本非注册 | ✅ | `ModelVersion.createInitialVersion()` L43 |
| Model.modifyMetadata | 不可修改 name/resourceGroup | ⚠️ | DTO 层不暴露这些字段，但未主动拦截请求 |
| 标签全量替换 | tagIds 全量替换语义 | ✅ | `ModelApplicationService.modifyModel()` L116-131 |
| categoryName/typeName 返回 | 详情/列表响应补充名称 | ❌ | `toModelResponse()` 未填充 categoryName/typeName |
| tags 完整信息返回 | 返回 Tag 对象含 id+name | ❌ | `toTagResponses()` 仅返回 id，无 name |
| 标签存在性校验(创建模型) | tagIds 中的标签必须存在 | ❌ | `createModel()` 未校验标签存在性 |
| 标签上限校验(创建/修改模型) | tagIds ≤ 20 | ❌ | 未实现 |
| 版本列表降序排列 | 按 version_number DESC | ⚠️ | 取决于 Repository 实现，API 层未显式排序 |
| createVersion 返回 isRegistered | 响应中包含 isRegistered | ✅ | `VersionResponse` 有 isRegistered 字段 |
| 模型名称字符集校验 | `^[a-zA-Z0-9\u4e00-\u9fa5_-]+$` | ❌ | `Model.validateName()` 仅检查非空和长度，未校验字符集 |
| 模型名称长度上限 | 255 | ⚠️ | `Model.validateName()` 限制 100，设计文档要求 255 |
| 模型名称 trim | trim 后校验 | ❌ | 未 trim |
| resourceGroup 长度 ≤100 | 设计文档要求 | ❌ | `Model.validateResourceGroup()` 仅检查非空 |
| 模型名称不可修改 | API 层不暴露 name | ⚠️ | DTO 无 name 字段，但未显式拦截 |
| 资源组不可修改 | API 层不暴露 resourceGroup | ⚠️ | DTO 无 resourceGroup 字段，但未显式拦截 |
| ModelQueryCondition 默认值 | page=1, pageSize=50, sortBy=createTime, sortOrder=desc | ❌ | DTO 字段均为 null，无默认值 |

---

## 三、遗漏项汇总

### 高严重度遗漏（影响核心功能）

| # | 所属 Feature | 遗漏项 | 说明 |
|---|-------------|--------|------|
| 1 | F2 | 添加模型类型 API 不可用 | `addModelTypeToCategory()` 抛出 UnsupportedOperationException，Request Body 应为 `{name, description}` 而非 UUID |
| 2 | F2 | 为模型添加标签缺少批量支持 | 设计要求批量添加 `tagIds` 数组，实际为单个 UUID |
| 3 | F2 | 标签上限校验未实现 | 添加标签时应校验 ≤ 20 |
| 4 | F3 | RBAC 资源组可见性未在应用层实现 | 无用户上下文到可见资源组的映射逻辑 |
| 5 | F3 | 模型详情/修改/创建版本缺少 RBAC 权限校验 | 任何用户可操作任意模型 |
| 6 | F3 | 标签存在性校验未实现 | 创建/修改模型时传入的 tagIds 未校验标签是否存在 |
| 7 | F3 | 模型名称字符集校验未实现 | 设计要求 `^[a-zA-Z0-9\u4e00-\u9fa5_-]+$` |

### 中严重度遗漏（影响完整性）

| # | 所属 Feature | 遗漏项 | 说明 |
|---|-------------|--------|------|
| 8 | F2 | 标签列表查询缺少 tagType 筛选参数 | `GET /v2/ui/tags` 不支持 tagType 查询参数 |
| 9 | F3 | NFS 模式 PVC 自动命名未实现 | 设计要求 `pvc-{modelId}-v{versionNumber}` |
| 10 | F3 | VersionCreateRequest 缺少 isRegistered 字段 | 无法区分注册/上传模式 |
| 11 | F3 | categoryName/typeName 未在响应中填充 | 详情和列表响应缺少分类名称和类型名称 |
| 12 | F3 | tags 响应仅含 id 不含 name | `toTagResponses()` 仅返回 TagResponse(id) |
| 13 | F3 | ModelQueryCondition 无默认值 | page/pageSize/sortBy/sortOrder 应有默认值 |
| 14 | F3 | 标签上限校验(创建/修改模型)未实现 | tagIds ≤ 20 |

### 低严重度遗漏（影响一致性）

| # | 所属 Feature | 遗漏项 | 说明 |
|---|-------------|--------|------|
| 15 | F2 | Category 名称长度校验上限不精确 | 设计 1-100，代码实现为空或>100 拒绝 |
| 16 | F3 | Model 名称长度上限为 100，设计要求 255 | `Model.validateName()` L84 |
| 17 | F3 | 资源组长度校验 ≤100 未实现 | `Model.validateResourceGroup()` 仅检查非空 |
| 18 | F3 | Model 名称未做 trim | 设计要求 trim 后校验 |
| 19 | F3 | Model.getLatestVersionNumber() 空列表风险 | versions 为空时抛 IndexOutOfBoundsException |
| 20 | F3 | 修改模型时 categoryId/typeId 变更未传入 modifyMetadata | `modifyMetadata()` 不接受 categoryId/typeId，但 validateModelModification 检查了 |
| 21 | F3 | 模型名称不可修改未显式拦截 | 请求中包含 name 应返回错误 |
| 22 | F3 | 资源组不可修改未显式拦截 | 请求中包含 resourceGroup 应返回错误 |
| 23 | F3 | createTime/updateTime 未在 Model 响应中返回 | 设计文档 Response Body 包含时间字段 |

---

## 四、覆盖率统计

### Feature 2 覆盖率

| 检查维度 | 设计要求 | 已实现 | 覆盖率 |
|----------|---------|--------|--------|
| API 端点 | 11 | 8 (完全匹配) + 3 (存在偏差) | **72.7%** (严格) / 100% (端点存在) |
| CategoryRepository 方法 | 10 | 10 | **100%** |
| TagRepository 方法 | 16 | 16 | **100%** |
| 业务不变量 | 10 | 9 | **90%** (标签上限校验缺失) |
| 内置数据保护 | 3 | 3 | **100%** |
| 级联删除 | 4 | 4 | **100%** |

### Feature 3 覆盖率

| 检查维度 | 设计要求 | 已实现 | 覆盖率 |
|----------|---------|--------|--------|
| API 端点 | 6 | 3 (完全匹配) + 3 (存在偏差) | **50%** (严格) / 100% (端点存在) |
| ModelRepository 方法 | 12 | 12 | **100%** |
| ModelDomainService 方法 | 2 | 2 | **100%** |
| 容量限制 (100/50/1000) | 3 | 3 | **100%** |
| 版本号递增无间隔 | 2 | 2 | **100%** |
| RBAC 资源组可见性 | 5 | 1 (Repository层) | **20%** |
| StoragePath 校验 | 6 | 5 | **83.3%** |
| 响应完整性 | 4 | 0 | **0%** (categoryName/typeName/tags名称/时间字段) |

### 总体覆盖率

| 指标 | 数值 |
|------|------|
| Feature 2 总体 | **~90%** (Repository 层完整，API 层有偏差) |
| Feature 3 总体 | **~65%** (Repository/DomainService 完整，应用服务层和 RBAC 存在较大遗漏) |
| **综合覆盖率** | **~77%** |

---

## 五、建议修复优先级

### P0 — 必须修复（功能不可用/安全漏洞）
1. 修复添加模型类型 API（端点 #5）— 接收 `{name, description}` 创建新类型
2. 实现 RBAC 资源组可见性过滤
3. 实现模型详情/修改/创建版本的 RBAC 权限校验
4. 修复为模型添加标签 API — 支持批量 `tagIds` 数组

### P1 — 应该修复（功能缺失）
5. 实现标签上限校验（≤ 20）— 添加标签和创建/修改模型时
6. 实现标签存在性校验 — 创建/修改模型时
7. 实现模型名称字符集校验 `^[a-zA-Z0-9\u4e00-\u9fa5_-]+$`
8. 补充 categoryName/typeName 到模型响应
9. 补充 tags 完整信息（id + name）到响应
10. 标签列表查询增加 tagType 筛选参数

### P2 — 可以修复（一致性和健壮性）
11. 修正 Model 名称长度上限为 255
12. Model 名称 trim 处理
13. 资源组长度校验 ≤100
14. ModelQueryCondition 添加默认值
15. VersionCreateRequest 增加 isRegistered 字段
16. NFS 模式 PVC 自动命名
17. 修复 Model.getLatestVersionNumber() 空列表风险
18. Model.modifyMetadata 支持 categoryId/typeId 变更
19. 模型名称/资源组不可修改显式拦截
20. Model 响应补充 createTime/updateTime

---

**报告结束**
