## repository模块对外部系统的依赖信息

- model_io_manager
  - 用途：获取正在部署的模型列表信息
  - 输入：无
  - 输出：模型名称列表、模型状态

- MODEL_MANAGER_URL环境变量
  - 用途：提供model_io_manager服务的访问地址
  - 输入：环境变量配置的URL地址
  - 输出：服务访问端点