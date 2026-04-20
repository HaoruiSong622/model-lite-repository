## 数据模型/领域对象结构信息

```java
class ModelInfo {
    modelId: String // 模型ID
    modelName: String // 模型具体名称
    modelType: String // 模型类型
    author: String // 模型作者
    seriesName: String // 模型系列名称
    modelDescription: String // 模型描述
    versionNum: Integer // 版本数量
    modelSize: BigDecimal // 模型大小，填写小数，单位为B
    maxSeqLength: Integer // 最大序列长度，单位为tokens
    deprecated: Boolean // 是否已废弃；true为在回收站，false为正常使用
    weightStatus: int // 权重状态: 0无权重 1上传中 2已暂停 3上传中断 4空间不足导致上传中断 5取消中 6有权重
    createTime: String // 创建时间
    updateTime: String // 更新时间
    supportFunctionList: List<String> // 模型类型支持的用途
    userResourceGroupId: String // 用户资源组id
    userResourceGroupName: String // 用户资源组名称
}

class VersionInfo {
    versionId: String // 版本id
    modelName: String // 版本对应模型的名称
    versionNo: String // 版本号
    versionPath: String // 版本存放路径
    versionDescription: String // 版本描述
    createTime: String // 更新时间（版本创建时间）
    trainFrame: String // 训练框架
    trainType: String // 训练类型
    trainStrategy: String // 训练策略
    trainTime: Long // 训练时长,单位为毫秒
    finalLoss: String // 最终loss
    deprecated: Boolean // 是否已废弃；true为在回收站，false为正常使用
    weightStatue: int // 状态 0无权重 1上传中 2已暂停 3上传中断 4空间不足导致上传中断 5取消中 6有权重
    weightType: String // 模型权重类型
    sourceVersion: String // 模型权重来源版本
    userResourceGroupId: String // 权重模型的用户资源组id
}

class ModelInfoJson {
    modelName: String // 模型具体名称
    modelType: String // 模型类型
    author: String // 模型作者
    seriesName: String // 模型系列名称
    modelDescription: String // 模型描述
    modelSize: BigDecimal // 模型大小，填写小数，单位为B
    maxSeqLength: Integer // 最大序列长度，单位为tokens
    deprecated: Boolean // 是否已废弃；true为在回收站，false为正常使用
    createTime: String // 创建时间
    updateTime: String // 更新时间
    userResourceGroupId: String // 用户资源组id
    userResourceGroupName: String // 用户资源组Name
}

class VersionInfoJson {
    modelName: String // 版本对应模型的名称
    versionNo: String // 版本号
    versionDescription: String // 版本描述
    updateTime: String // 更新时间
    trainFrame: String // 训练框架
    trainType: String // 训练类型
    trainStrategy: String // 训练策略
    trainTime: Long // 训练时长,单位为毫秒
    finalLoss: String // 最终loss
    deprecated: Boolean // 是否已废弃；true为在回收站，false为正常使用
    weightType: String // 模型权重类型
    sourceVersion: String // 模型权重来源版本
    userResourceGroupId: String // 模型的资源组Id
}

class ModelInfoEntry {
    modelId: String // 模型ID
    modelName: String // 模型具体名称
    modelType: String // 模型类型
    author: String // 模型作者
    seriesName: String // 模型系列名称
    modelDescription: String // 模型描述
    versionNum: Integer // 版本数量
    modelSize: BigDecimal // 模型大小，填写小数，单位为B
    maxSeqLength: Integer // 最大序列长度，单位为tokens
    deprecated: Boolean // 是否已废弃；true为在回收站，false为正常使用
    createTime: String // 创建时间
    updateTime: String // 更新时间
    userResourceGroupId: String // 用户资源组id
    userResourceGroupName: String // 用户资源组Name
}

class VersionInfoEntry {
    versionId: String // 版本id
    modelName: String // 版本对应模型的名称
    versionNo: String // 版本号
    versionPath: String // 版本所在路径
    versionDescription: String // 版本描述
    updateTime: String // 创建时间
    trainFrame: String // 训练框架
    trainType: String // 训练类型
    trainStrategy: String // 训练策略
    trainTime: Long // 训练时长,单位为毫秒
    finalLoss: String // 最终loss
    deprecated: Boolean // 是否已废弃；true为在回收站，false为正常使用
    weightType: String // 模型权重类型
    sourceVersion: String // 权重来源版本
    userResourceGroupId: String // 权重模型的用户资源组id
}

class ModelCategoryTypeInfo {
    singleCategoryTypeList: List<ModelTypeEntry> // 该分类下模型类型列表
    modelCategoryName: String // 模型类型列表
    internalBuilt: boolean // 模型具体名称
}

class ModelConfig {
    modelInfo: ModelInfo // 模型信息
    configJson: String // 配置JSON字符串
    versionInfoEntryList: List<VersionInfo> // 版本信息列表
    versionListFilesMap: Map<String, List<String>> // 版本文件映射
    versionSetWeightMissingIndexMap: Map<String, Set<Integer>> // 版本权重缺失索引映射
}

class PageResult<T> {
    count: long // 总数
    result: List<T> // 查询结果列表
}

class SynTaskRecordEntity {
    id: String // 同步任务id
    state: Integer // 同步任务状态（0：执行成功；1：执行中；2：失败）
    startTime: String // 开始时间
    endTime: String // 结束时间
    createType: String // 创建类型（0：定时任务；1：接口触发）
    failReason: String // 失败原因
}

class ModelCategoryEntry {
    modelCategory: String // 模型分类
    internalBuilt: boolean // 是否为内置
}

class ModelTypeEntry {
    modelType: String // 模型类型
    supportFinetune: boolean // 支持精调
    supportQuantify: boolean // 支持量化
    modelCategory: String // 模型分类
    internalBuilt: boolean // 是否内置
}

class SynTaskRecordEntry {
    id: String // 同步任务id
    state: Integer // 同步任务状态（0：执行成功；1：执行中；2：失败）
    startTime: String // 开始时间
    endTime: String // 结束时间
    createType: String // 创建类型（0：定时任务；1：接口触发）
    failReason: String // 失败原因
}

class UploadWeightTaskEntry {
    modelName: String // 模型名称，用于确定任务
    versionNo: String // 版本名称，用于确定任务
    sharedPath: String // 远程共享目录
    status: int // 传输状态 0无权重 1上传中 2已暂停 3上传中断 4空间不足导致上传中断 5取消中 6有权重
    progress: float // 百分比进度,默认0.00
    beginDate: LocalDate // 任务创建时间
    taskId: String // 任务ID
    userResourceGroupId: String // 任务的用户资源组id
}

class UploadWeightMountRemoteEntry {
    sharedPath: String // 远程共享目录
    agreement: String // 使用的协议
    userName: String // CIFS用户名
    password: String // CIFS密码
    mountPath: String // 本地挂载点
    mountDate: LocalDate // 第一次挂载时间
    recentUploadDate: LocalDate // 最近的使用时间
}
```