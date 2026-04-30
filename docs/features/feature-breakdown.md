# Draft: ModelLite 模型仓库 — 特性分解决策

## 需求来源
- 需求规格说明书: `docs/ModelLite-模型仓库-需求规格说明书-v1.2.md`
- DDD 架构设计: `.sisyphus/drafts/` 下的 5 份 DDD 设计文档

## 特性分解结果

### 开发模式
- **MVP 优先模式**: 先开发 P0+P1 特性形成最小可用产品，后续迭代其他特性
- **逐特性详细设计**: 每个特性单独编写详细设计文档，确认后开发

### 特性列表（按优先级）

#### P0（MVP 基础）
1. **Feature 1: 基础设施与通用能力**
   - 子域: 基础设施层
   - 需求: REQ-GENERAL-001/002, REQ-SECURITY-001/002, 非功能需求
   - 交付: 数据库 Schema, 项目骨架, 配置, 异常体系, 版本状态枚举
   - 依赖: 无

2. **Feature 2: 分类体系与标签管理**
   - 子域: 模型权重子域（分类体系能力模块）
   - 需求: REQ-CATEGORY-001/002, REQ-TAG-001
   - 交付: Category 聚合, Tag 聚合, 内置预设数据, 人机接口
   - 依赖: Feature 1

#### P1（MVP 核心）
3. **Feature 3: 模型与版本生命周期**
   - 子域: 模型权重子域（核心）
   - 需求: REQ-MODEL-001/002/003, REQ-VERSION-001/002, REQ-QUERY-001, REQ-RBAC-001/002/003, 性能/容量需求
   - 交付: Model 聚合(含 ModelVersion), 模型 CRUD, 版本管理, 列表查询, 资源组控制
   - 依赖: Feature 1, Feature 2

4. **Feature 4: 权重导入（纳管/上传）**
   - 子域: 权重任务子域（核心）
   - 需求: REQ-REGISTER-001/002, REQ-UPLOAD-001/002, REQ-M2M-002
   - 交付: UploadTask 聚合, 纳管, 上传(异步+K8s Job), 上传任务管理, 训练归档
   - 依赖: Feature 1, Feature 3

#### P2（核心完善）
5. **Feature 5: 版本锁管理**
   - 子域: 版本锁子域（支撑域）
   - 需求: REQ-M2M-003
   - 交付: VersionLock 聚合, 锁定/解锁/续约, 过期清理, 预警, M2M 接口
   - 依赖: Feature 1, Feature 3

6. **Feature 6: 删除恢复与回收站**
   - 子域: 模型权重子域（删除与恢复）
   - 需求: REQ-DELETE-001/002/003, REQ-RECYCLE-001
   - 交付: 软删除/硬删除/恢复, 回收站管理, 删除前锁校验
   - 依赖: Feature 3, Feature 5

#### P3（功能增强）
7. **Feature 7: 校验识别与转换**
   - 子域: 权重任务子域（校验与识别 + 格式转换）
   - 需求: REQ-INFO-001/002, REQ-CONVERT-001/002
   - 交付: 校验, 类型识别, 格式转换(Megatron→Safetensors), 转换任务管理
   - 依赖: Feature 1, Feature 4, Feature 5

8. **Feature 8: 机机接口与运维**
   - 子域: 跨子域（接口层）
   - 需求: REQ-M2M-001, REQ-LOG-001
   - 交付: 权重路径查询 M2M 接口, 操作日志上报, 运维监控
   - 依赖: Feature 1, 3, 4, 5

## 设计交付模式
- 逐特性详细设计: 为每个特性编写单独的详细设计文档
- 确认后开始开发

## 特性设计文档模板规范

### 模板偏好（已确认）
- **UML 图格式**: Mermaid（在 Markdown 中直接渲染）
- **代码示例**: 关键伪代码，小而精
- **测试用例粒度**: 场景级（Given-When-Then 格式）
- **详细程度**: 精确到字段级（开发者拿到即可编码）

## Open Questions
- 无

## Scope Boundaries
- INCLUDE: 8 个特性覆盖需求规格说明书中的所有功能需求
- EXCLUDE: 模型量化任务管理、模型收藏/订阅、模型导入导出、模型存储同步机制
