# Draft: ModelLite 模型仓库架构设计

## 需求概览（来自需求规格说明书 v1.1）

### 核心功能域
- 模型权重生命周期管理（创建、查看、修改、删除、恢复）
- 权重纳管（Register - 注册已有路径，只读）
- 权重上传（Upload - 拷贝到平台PVC）
- 权重格式转换（Megatron → Safetensors）
- 模型分类管理（两层：Classification → Type）
- 分权分域（资源组可见性控制）
- 机机接口（M2M API - 查询路径、锁定/解锁、训练归档）

### 技术栈约束（用户已确定）
- Spring Boot 3.4.5 + Java 21
- PostgreSQL + MyBatis ORM + Druid 连接池
- SSL/TLS 双向认证
- Log4j2 + GC日志 + 日志轮转
- Kubernetes 部署

### 核心数据实体
- Model（模型）
- Model Version（版本）
- Model Classification（一级分类）
- Model Type（二级分类）
- Upload Task（上传任务）
- Convert Task（转换任务）

### 非功能需求关键点
- 性能：模型列表查询 ≤500ms，详情 ≤200ms，10并发上传，支持100GB单文件
- 可靠性：断点续传、任务恢复、事务一致性
- 可扩展性：预留AI资产扩展、权重校验扩展、资源组修改扩展

## 用户确认的技术栈
- [x] Spring Boot 3.4.5 + Java 21
- [x] PostgreSQL + MyBatis + Druid
- [x] SSL/TLS 双向认证
- [x] Log4j2 + GC日志轮转

## 待确认问题
- 项目目录结构是否已存在？
- 是否有现有的基础设施代码（如基础框架、公共模块）？
- ConfigMap 挂载方式在 K8s 部署中如何设计？
- 机机接口的 Pod URL 访问方式如何实现？

## 研究结果

### 1. 代码库现状
- 全新项目，仅有 docs/ 和 tmp/ 目录
- 无任何 Java 代码、构建配置或基础设施代码
- 需从零构建完整项目骨架

### 2. Spring Boot 分层架构模式
- 推荐 Controller-Service-Repository 分层
- MyBatis 3.0.x starter 匹配 Spring Boot 3.4.5
- Druid 1.2.19 starter 集成
- 包结构：controller/, service/, repository/, domain/, dto/, config/, common/, exception/

### 3. SSL/TLS 双向认证
- Spring Boot 3.x 使用 SSL Bundle 配置
- Spring Security X.509 模块提取客户端证书
- 通过证书 OU 字段区分 User API vs M2M API 客户端
- 多 Connector 支持不同端口不同 SSL 配置

### 4. Log4j2 配置
- RollingRandomAccessFile + Async 包装器
- JDK 21 使用 -Xlog 语法收集 GC 日志
- JsonTemplateLayout 支持 ECS 结构化日志
- K8s 友好的日志路径 /var/log/

### 5. 用户确认的技术决策
- 异步任务：K8s Job + fabric8 Informer 机制
- 版本锁：锁列表（关系表）
- 平台对接：有公共框架，需 mock
- ConfigMap：Properties 方式读取
- API版本化：URL路径版本化，版本号为 v2（代码重构）
- 数据库迁移：无迁移工具，手动SQL管理
- UML图：优先Mermaid，Mermaid不支持时用PlantUML
- 项目包名：com.huawei.modellite.repository
- 这是代码重构项目（原系统已有，现重构为新版本）

### 6. 安全与对接决策
- 用户认证：Gateway 通过 HTTP Header 透传用户信息（X-User-Id, X-User-Role, X-Resource-Group）
- 操作日志：使用 annotation + 同步 HTTP 调用上报平台日志服务（复用平台已有模式）
- 错误码：数字错误码格式，数字区间划分模块+错误类型
- 机机接口认证：SSL/TLS 双向证书认证，证书 OU 区分 User vs Service

### 7. Metis审查后的补充决策
- 软删除与唯一性：排除软删除记录，恢复时检查冲突
- 部署副本：多副本 + Leader Election
- 训练归档检测：模型仓提供刷新接口，前端触发，2s内同步校验
  - 需要解决：模型过多时校验耗时、高并发下防止线程爆炸、同一版本同时只有一个刷新任务
- ID生成：UUID
- 纳管版本的硬删除：不删除源文件（只删元数据），因为纳管文件不属于模块管理
- 版本锁清理：需要TTL或定期清理机制

## 架构设计关键决策点
1. 模块分层架构（Controller-Service-DAO 通用模式 vs 其他）
2. 异步任务处理机制（Spring异步、消息队列、还是独立Worker）
3. 文件存储抽象层设计（PVC/NFS统一抽象）
4. 软删除/硬删除机制设计
5. 版本锁机制设计（引用计数还是锁列表）
6. 错误码规范设计
7. API响应格式规范
8. 配置管理策略（ConfigMap如何映射到Spring配置）

## Open Questions
- 是否有平台级别的公共框架或基础设施代码需要继承？
- Gateway认证机制的具体实现是什么？模块需要对接什么接口？
- 操作日志上报接口的具体格式是什么？