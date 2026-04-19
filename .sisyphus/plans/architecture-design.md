# 工作计划：ModelLite模型仓库架构设计文档生成

## TL;DR

> **目标**：根据已确认的所有架构决策，生成完整的《ModelLite模型仓库架构设计文档》到 `docs/architecture/` 目录。
>
> **交付物**：一份完备的架构设计Markdown文档，包含UML图、数据模型、API规范、安全架构等，使后续特性设计能够仅根据该文档+需求文档进行。
>
> **技术栈**：Spring Boot 3.4.5 + Java 21 + PostgreSQL + MyBatis + Druid + K8s Job + fabric8
>
> **并行执行**：NO — 这是一个文档生成任务，顺序执行
> **关键路径**：决策确认 → 文档骨架 → 各章节内容 → UML图 → 审核

---

## Context

### 原始请求
用户要求根据《ModelLite-模型仓库-需求规格说明书-v1.1.md》进行架构设计，生成架构设计文档。

### 已确认的架构决策（来自Interview）
1. **异步任务**：K8s Job + fabric8 Informer 机制
2. **版本锁**：锁列表（关系表）+ TTL 防止孤儿锁
3. **平台对接**：有公共框架，需 mock
4. **ConfigMap**：Properties 方式读取白名单
5. **API版本化**：URL路径版本化，版本号为 v2（代码重构）
6. **数据库迁移**：无迁移工具，手动SQL管理
7. **UML图**：优先Mermaid，Mermaid不支持时用PlantUML
8. **项目包名**：com.huawei.modellite.repository
9. **用户认证**：Gateway 通过 HTTP Header 透传（X-User-Id, X-User-Role, X-Resource-Group）
10. **操作日志**：annotation + 同步 HTTP 调用上报
11. **错误码**：数字错误码格式（1XXYYY）
12. **机机接口认证**：SSL/TLS 双向证书认证，证书 OU 区分 User vs Service
13. **软删除与唯一性**：排除软删除记录，恢复时检查冲突
14. **部署副本**：多副本 + Leader Election
15. **训练归档检测**：回调接口（同步2s内），前端触发刷新
16. **ID生成**：UUID v4
17. **纳管版本硬删除**：不删除源文件（只删元数据）

### Metis审查要点
- 所有31个需求必须追溯到架构章节
- 必须包含10条Guardrails防止范围蔓延
- 必须定义公共约定（响应格式、分页、错误码、软删除、ID生成）
- 必须包含可扩展性设计点（AI资产接口、校验扩展点）

---

## Work Objectives

### Core Objective
生成一份完备的架构设计文档，使得后续特性设计工程师能够仅根据该文档 + 需求规格说明书 v1.1 即可完成各特性的详细设计，无需额外的架构决策。

### Concrete Deliverables
- `docs/architecture/ModelLite-模型仓库-架构设计-v1.0.md` — 架构设计主文档

### Definition of Done
- [ ] 文档包含所有14个章节（见TODOs）
- [ ] 所有31个REQ-*需求都有架构追溯
- [ ] 包含至少5个Mermaid/PlantUML图
- [ ] 包含完整的表结构SQL
- [ ] 包含错误码定义表
- [ ] 包含API URL模式定义
- [ ] 包含部署架构说明

### Must Have
- 系统上下文图（Mermaid组件图）
- 模块分层架构图（Mermaid流程图）
- 部署架构图（K8s拓扑）
- ER图（Mermaid ER图）
- 数据表结构（完整SQL）
- API设计规范（URL、请求/响应、分页、错误码）
- 安全架构（SSL/TLS双向认证）
- 异步任务架构（K8s Job + Informer序列图）
- 公共约定（日志、配置、异常）
- 范围边界与约束
- 可扩展性设计点

### Must NOT Have (Guardrails)
- 不包含具体的功能实现代码
- 不包含前端UI设计
- 不包含性能测试方案
- 不包含详细的接口字段定义（留给特性设计）
- 不引入Redis、Kafka、Elasticsearch等未确认技术
- 不设计插件框架（AI资产只定义一个接口）

---

## Verification Strategy

### QA Policy
本任务为文档生成任务，QA通过文档完整性检查完成：
- 检查文档是否包含所有必需章节
- 检查所有31个REQ需求是否都有追溯
- 检查UML图语法是否正确（Mermaid/PlantUML）
- 检查SQL语法是否正确

---

## Execution Strategy

### 执行顺序

```
Wave 1 (文档骨架):
├── Task 1: 创建文档骨架（标题、版本信息、目录结构）
├── Task 2: 编写系统上下文章节（组件图、接口分类）
├── Task 3: 编写模块分层架构章节（分层图、包结构、职责规范）
└── Task 4: 编写部署架构章节（K8s拓扑、多副本、资源需求）

Wave 2 (核心设计):
├── Task 5: 编写数据模型章节（ER图、所有表SQL、数据约定）
├── Task 6: 编写API设计规范章节（URL模式、响应格式、分页、错误码）
├── Task 7: 编写安全架构章节（SSL配置、认证、鉴权）
└── Task 8: 编写异步任务架构章节（Job流、Informer、Leader Election）

Wave 3 (公共约定与边界):
├── Task 9: 编写公共约定章节（日志、配置、异常处理）
├── Task 10: 编写范围边界与约束章节
├── Task 11: 编写可扩展性设计点章节
└── Task 12: 编写关键决策总结、需求追溯矩阵、附录

Wave FINAL (审核):
├── Task F1: 文档完整性审核（检查所有章节、需求追溯、UML语法）
└── Task F2: 用户确认与交付
```

---

## TODOs

- [x] 1. 创建文档骨架

  **What to do**:
  - 在 `docs/architecture/` 目录下创建 `ModelLite-模型仓库-架构设计-v1.0.md`
  - 写入文档标题、版本信息、目录结构
  - 定义文档目标和范围

  **Must NOT do**:
  - 不写入具体章节内容（后续任务处理）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocks**: Tasks 2-12

  **Acceptance Criteria**:
  - [ ] 文件创建成功
  - [ ] 包含标准文档头（版本、日期、适用范围）
  - [ ] 包含完整的目录大纲

- [x] 2. 编写系统上下文章节

  **What to do**:
  - 编写系统边界图（Mermaid组件图）
  - 定义接口分类（User API / M2M API / 管理接口）
  - 定义模块职责边界（负责 vs 不负责）

  **Must NOT do**:
  - 不包含具体API端点定义（Task 6处理）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO（依赖Task 1）
  - **Blocked By**: Task 1
  - **Blocks**: Task 3

  **Acceptance Criteria**:
  - [ ] Mermaid组件图语法正确
  - [ ] 包含所有外部系统（Gateway、推理、训练、评测、日志服务、PVC）
  - [ ] 接口分类表格完整

- [x] 3. 编写模块分层架构章节

  **What to do**:
  - 绘制分层架构图（Mermaid流程图：Presentation → Business → DataAccess → Infrastructure）
  - 定义包结构规范（完整的目录树）
  - 定义分层职责规范（允许/禁止矩阵）

  **Must NOT do**:
  - 不包含具体类定义（留给特性设计）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 2
  - **Blocks**: Task 4

  **Acceptance Criteria**:
  - [ ] 包结构包含所有必要目录（controller/service/domain/repository/dto/infrastructure/common/config）
  - [ ] 分层职责表格清晰

- [x] 4. 编写部署架构章节

  **What to do**:
  - 绘制K8s部署拓扑图（Mermaid流程图）
  - 描述多副本 + Leader Election 机制
  - 定义资源需求表格

  **Must NOT do**:
  - 不包含具体的K8s YAML文件（留给部署文档）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 3
  - **Blocks**: Task 5

  **Acceptance Criteria**:
  - [ ] 部署图包含Deployment、Job、PVC、ConfigMap、Secret
  - [ ] Leader Election机制描述清晰
  - [ ] 资源需求表格完整

- [x] 5. 编写数据模型章节

  **What to do**:
  - 绘制ER图（Mermaid ER图）
  - 编写所有8张表的完整SQL（model, model_version, version_lock, upload_task, convert_task, model_classification, model_type, model_tag）
  - 定义数据约定（ID生成、软删除、审计字段、状态枚举）

  **Must NOT do**:
  - 不包含索引优化细节（除非影响功能）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 4
  - **Blocks**: Task 6

  **Acceptance Criteria**:
  - [ ] ER图包含所有实体和关系
  - [ ] 8张表的SQL语法正确
  - [ ] 唯一约束正确定义（排除软删除）
  - [ ] 状态枚举定义完整

- [x] 6. 编写API设计规范章节

  **What to do**:
  - 定义URL设计规范（RESTful模式）
  - 定义标准响应格式（code/message/data/traceId）
  - 定义分页请求/响应格式
  - 定义错误码规范（1XXYYY结构）
  - 列出User API和M2M API概览表格

  **Must NOT do**:
  - 不包含具体请求/响应字段定义（留给特性设计）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 5
  - **Blocks**: Task 7

  **Acceptance Criteria**:
  - [ ] URL模式表格完整
  - [ ] 响应格式JSON示例正确
  - [ ] 错误码按模块分层定义
  - [ ] API概览包含所有主要接口

- [x] 7. 编写安全架构章节

  **What to do**:
  - 绘制证书体系图（Mermaid流程图）
  - 编写Spring Boot SSL配置示例
  - 描述证书挂载方式
  - 定义客户端类型识别逻辑（OU字段）
  - 描述Gateway Header透传机制
  - 定义权限控制规则

  **Must NOT do**:
  - 不包含证书生成命令（留给运维文档）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 6
  - **Blocks**: Task 8

  **Acceptance Criteria**:
  - [ ] 证书体系图包含RootCA、子CA、服务证书
  - [ ] SSL配置包含application.yml示例
  - [ ] Header透传表格完整

- [x] 8. 编写异步任务架构章节

  **What to do**:
  - 绘制K8s Job任务流序列图（Mermaid序列图）
  - 描述Job模板管理（ConfigMap存储）
  - 描述fabric8 Informer机制
  - 描述Leader Election实现

  **Must NOT do**:
  - 不包含具体的Job业务逻辑（留给特性设计）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 7
  - **Blocks**: Task 9

  **Acceptance Criteria**:
  - [ ] 序列图包含User/Service/DB/Leader/K8s/Job/PVC/Informer
  - [ ] Job模板ConfigMap示例正确
  - [ ] Leader Election机制描述清晰

- [x] 9. 编写公共约定章节

  **What to do**:
  - 编写Log4j2配置示例（log4j2-spring.xml）
  - 编写GC日志JVM参数
  - 编写操作日志注解和AOP切面示例
  - 编写ConfigMap读取示例
  - 编写异常层次结构和全局异常处理示例

  **Must NOT do**:
  - 不包含具体的业务异常（留给特性设计）

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 8
  - **Blocks**: Task 10

  **Acceptance Criteria**:
  - [ ] Log4j2配置包含RollingFileAppender和轮转策略
  - [ ] 操作日志注解定义完整
  - [ ] 异常层次结构图清晰

- [x] 10. 编写范围边界与约束章节

  **What to do**:
  - 列出明确包含的功能（31个REQ需求）
  - 列出明确排除的功能（量化、收藏、导入导出等）
  - 定义架构约束（技术约束、设计约束）

  **Must NOT do**:
  - 不重复需求文档中的功能描述，只列需求编号和名称

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 9
  - **Blocks**: Task 11

  **Acceptance Criteria**:
  - [ ] 包含表格列出所有31个REQ需求
  - [ ] 排除功能列表完整
  - [ ] 约束条件明确

- [x] 11. 编写可扩展性设计点章节

  **What to do**:
  - 定义AI资产抽象接口（AIAsset）
  - 定义权重校验扩展点（WeightValidator）
  - 描述资源组迁移预留

  **Must NOT do**:
  - 不实现预留的扩展点，只定义接口

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 10
  - **Blocks**: Task 12

  **Acceptance Criteria**:
  - [ ] AIAsset接口定义完整
  - [ ] WeightValidator接口定义完整
  - [ ] 资源组迁移预留说明清晰

- [x] 12. 编写关键决策总结、需求追溯矩阵、附录

  **What to do**:
  - 编写关键设计决策总结表格（决策点、决策内容、理由）
  - 编写需求追溯矩阵（REQ编号→架构章节）
  - 编写附录（术语表、参考文档、变更记录）

  **Must NOT do**:
  - 不引入新的架构决策

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 11
  - **Blocks**: Tasks F1-F2

  **Acceptance Criteria**:
  - [ ] 决策总结表格包含所有关键决策
  - [ ] 追溯矩阵覆盖所有31个REQ需求
  - [ ] 附录完整

- [x] F1. 文档完整性审核

  **What to do**:
  - 检查文档是否包含所有14个章节
  - 检查所有31个REQ需求是否都有追溯
  - 检查Mermaid/PlantUML图语法
  - 检查SQL语法

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task 12

  **Acceptance Criteria**:
  - [ ] 14个章节全部存在
  - [ ] 31个REQ全部追溯
  - [ ] 至少5个UML图
  - [ ] SQL语法检查通过

- [x] F2. 用户确认与交付

  **What to do**:
  - 向用户呈现文档摘要
  - 获取用户确认或修改意见
  - 根据反馈修改文档

  **Recommended Agent Profile**:
  - **Category**: `unspecified-low`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Blocked By**: Task F1

  **Acceptance Criteria**:
  - [ ] 用户确认文档满足需求
  - [ ] 所有修改意见已处理

---

## Final Verification Wave

- [x] F1. **文档完整性审核** — `unspecified-high`
  检查文档结构、需求追溯、UML语法、SQL语法
  Output: `章节 [14/14] | 需求追溯 [31/31] | UML图 [16/5+] | SQL检查 [PASS]`

- [x] F2. **用户确认** — `unspecified-low`
  呈现文档摘要，获取用户确认
  Output: `用户确认 [PENDING]`

---

## Commit Strategy

- **1**: `docs(architecture): add ModelLite repository architecture design v1.0`
  - Files: `docs/architecture/ModelLite-模型仓库-架构设计-v1.0.md`

---

## Success Criteria

### Verification Commands
```bash
# 检查文档是否存在
ls docs/architecture/ModelLite-模型仓库-架构设计-v1.0.md

# 检查文档行数（应大于1000行）
wc -l docs/architecture/ModelLite-模型仓库-架构设计-v1.0.md

# 检查Mermaid图数量
grep -c "```mermaid" docs/architecture/ModelLite-模型仓库-架构设计-v1.0.md
```

### Final Checklist
- [x] 文档文件存在
- [x] 包含14个章节
- [x] 包含至少5个Mermaid/PlantUML图
- [x] 包含8张表的SQL定义
- [x] 包含错误码定义
- [x] 包含API URL模式
- [x] 所有31个REQ需求已追溯
- [ ] 用户已确认
