## 2. 每个模块的职责说明
- ai-executor：负责AI模型执行相关的管理功能
- common：包含项目通用的共享组件和工具
- controller-manager：负责Kubernetes控制器管理
- finetune：处理模型微调相关的功能
- frontend：提供前端用户界面
- gateway：作为系统的API网关和路由入口
- inference：负责模型推理服务的部署和管理
- modellite-utils：包含项目通用工具类和辅助功能
- repository：负责模型仓库管理和存储

## 3. 模块之间的依赖关系
- controller-manager 依赖 modellite-utils 提供的通用工具功能
- inference 依赖 controller-manager 进行Kubernetes资源管理
- finetune 依赖 repository 进行模型存储管理
- gateway 作为入口，会调用 finetune、inference 和 repository 模块的功能
- frontend 通过 gateway 与后端各模块进行交互
- ai-executor 为推理和微调提供底层执行支持