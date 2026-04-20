## repository模块接口级信息

```java
/**
 * 负责模型仓核心业务能力，包括模型的增删改查、版本管理、元数据维护、分类管理等功能
 */
public class ManageController {
    /**
     * 批量查询模型仓的模型列表信息
     * @param request 请求信息
     * @param queryModelRequest 批量查询模型仓请求体
     * @return 模型列表信息
     */
    public ResponseEntity<BaseResponse> queryModelList(HttpServletRequest request, QueryModelRequest queryModelRequest) {
        pass;
    }

    /**
     * 查询单个模型详情
     * @param request 请求信息
     * @param modelName 模型名称
     * @param receivedCycled 是否搜索回收站
     * @param modelOwnerGroupId 模型的资源组id
     * @return 查询单个模型详情响应
     */
    public ResponseEntity<BaseResponse> querySingleModelInfo(HttpServletRequest request, String modelName, Integer receivedCycled, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 软删除单个模型
     * @param request 请求信息
     * @param modelName 模型名称
     * @param modelOwnerGroupId 模型的资源组id
     * @return 软删除响应
     */
    public ResponseEntity<BaseResponse> deleteModel(HttpServletRequest request, String modelName, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 软删除单个模型的指定版本
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号
     * @param modelOwnerGroupId 模型的资源组id
     * @return 软删除单个模型响应
     */
    public ResponseEntity<BaseResponse> deleteModelVersion(HttpServletRequest request, String modelName, String versionNo, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 主动同步所有模型列表信息
     * @return 下发刷新任务响应
     */
    public ResponseEntity<BaseResponse> syncModelList() {
        pass;
    }

    /**
     * 查询同步任务的状态信息
     * @return 同步任务的状态信息响应
     */
    public ResponseEntity<BaseResponse> querySyncStatus() {
        pass;
    }

    /**
     * 查询模型模型去重后的字段
     * @param request 请求信息
     * @param parameterName 查询参数名称
     * @return 模型列表信息
     */
    public ResponseEntity<BaseResponse> queryModelList(HttpServletRequest request, String parameterName) {
        pass;
    }

    /**
     * 新增/更新模型元数据json文件
     * @param httpServletRequest 请求
     * @param modelName 模型名称
     * @param mergeModelRequest 新增/更新模型元数据请求体
     * @return 新增/更新模型元数据请求体
     */
    public ResponseEntity<BaseResponse> mergeModelJsonFile(HttpServletRequest httpServletRequest, String modelName, MergeModelRequest mergeModelRequest) {
        pass;
    }

    /**
     * versionmetaupsert
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号
     * @param mergeVersionRequest 新增/更新响应请求体
     * @return 新增/更新响应
     */
    public ResponseEntity<BaseResponse> mergeVersionJsonFile(HttpServletRequest request, String modelName, String versionNo, MergeVersionRequest mergeVersionRequest) {
        pass;
    }

    /**
     * 硬删除单个模型/单个模型的指定版本
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号
     * @param modelOwnerGroupId 模型的资源组id
     * @return 硬删除单个模型响应
     */
    public ResponseEntity<BaseResponse> hardDeleteModelVersion(HttpServletRequest request, String modelName, String versionNo, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 回收站模型/版本恢复接口
     * @param request 请求信息
     * @param modelName 模型名称
     * @param versionNo 版本号
     * @param modelOwnerGroupId 模型的资源组id
     * @return 回收站版本恢复接口
     */
    public ResponseEntity<BaseResponse> restoreVersionFromTrash(HttpServletRequest request, String modelName, String versionNo, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 查询支持的模型类型字段
     * @param modelCategoryList 支持功能列表
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> querySupportModelType(List<String> modelCategoryList) {
        pass;
    }

    /**
     * 根据模型类型查询支持的量化类型列表
     * @param modelType 模型类型
     * @return 支持的量化类型列表
     */
    public ResponseEntity<RestResponse<List<String>>> queryQuantTypes(String modelType) {
        pass;
    }

    /**
     * 创建模型量化任务
     * @param httpServletRequest http请求
     * @param quantCreateRequest 模型量化请求体
     * @return 量化任务id
     */
    public ResponseEntity<RestResponse<Long>> createModelQuantTask(HttpServletRequest httpServletRequest, QuantCreateRequest quantCreateRequest) {
        pass;
    }

    /**
     * 查询模型量化任务列表
     * @param request 模型量化请求体
     * @param httpServletRequest http请求
     * @return 量化任务列表
     */
    public ResponseEntity<RestResponse<PageResult<ModelQuantTaskVo>>> queryModelQuantTasks(QuantQueryRequest request, HttpServletRequest httpServletRequest) {
        pass;
    }

    /**
     * 删除量化任务
     * @param httpServletRequest 请求信息
     * @param taskId 任务id
     * @return ok
     */
    public ResponseEntity<RestResponse> deleteModelQuantTask(Long taskId, HttpServletRequest httpServletRequest) {
        pass;
    }

    /**
     * 停止量化任务
     * @param httpServletRequest 请求信息
     * @param taskId 任务id
     * @return ok
     */
    public ResponseEntity<RestResponse> stopModelQuantTask(Long taskId, HttpServletRequest httpServletRequest) {
        pass;
    }

    /**
     * 重试量化任务
     * @param httpServletRequest 请求信息
     * @param taskId 任务id
     * @return ok
     */
    public ResponseEntity<RestResponse> restartModelQuantTask(Long taskId, HttpServletRequest httpServletRequest) {
        pass;
    }

    /**
     * 查询所有模型分类
     * @param modelCategoryList 支持功能列表，为空则返回全部
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> querySupportModelCategory(List<String> modelCategoryList) {
        pass;
    }

    /**
     * 新增模型分类
     * @param modelCategory 新增的模型分类
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> addModelCategory(String modelCategory) {
        pass;
    }

    /**
     * 删除模型分类
     * @param modelCategory 要删除的模型分类
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> delModelCategory(String modelCategory) {
        pass;
    }

    /**
     * 新增模型类型
     * @param modelCategory 新增的模型类型所属的分类
     * @param modelType 新增的模型类型
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> addModelType(String modelCategory, String modelType) {
        pass;
    }

    /**
     * 删除模型类型
     * @param modelCategory 要删除的模型类型所属的分类
     * @param modelType 要删除的模型类型
     * @return 支持模型类型列表
     */
    public ResponseEntity<BaseResponse> delModelType(String modelCategory, String modelType) {
        pass;
    }

    /**
     * 根据资源组查询组下的资源[仅admin调用]
     * @param request 请求信息
     * @param queryModelRequest 批量查询模型仓请求体
     * @return 模型列表信息
     */
    public ResponseEntity<BaseResponse> queryModelListByResourceGruop(HttpServletRequest request, QueryModelRequest queryModelRequest) {
        pass;
    }

    /**
     * 健康检查
     * @param request 请求头
     * @return 健康检查
     */
    public ResponseEntity<String> healthCheck(HttpServletRequest request) {
        pass;
    }
}

/**
 * 负责模型管理核心业务逻辑，包括模型信息处理、版本控制、文件操作、元数据维护等
 */
public class ModelManageService {
    /**
     * 批量查询模型仓的模型列表信息
     * @param queryModelRequest 模型列表查询请求
     * @param request 请求信息
     * @param userResourceGroupId 用户资源组id
     * @return 模型列表信息
     */
    public QueryModelListResp queryModelInfo(QueryModelRequest queryModelRequest, HttpServletRequest request, String userResourceGroupId) {
        pass;
    }

    /**
     * 查询模型模型去重后的字段
     * @param parameterName 查询去重的参数名称
     * @param userResourceGroupId 用户资源组id
     * @return parameterName去重后的列表
     */
    public QueryDistinctNameListResp queryDistinctNamesList(String parameterName, String userResourceGroupId) {
        pass;
    }

    /**
     * 查询单个模型信息和对应的版本信息
     * @param request 请求信息
     * @param modelName 模型名称
     * @param receivedCycled 接收是否在回收站
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型实例的创建者资源组id（模型所属资源组id）
     * @return 单个模型信息和对应的版本信息
     */
    public BaseResponse querySingleModelInfo(HttpServletRequest request, String modelName, Integer receivedCycled, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 硬删除单个模型数据
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 硬删除响应
     */
    public BaseResponse hardDeleteModel(String modelName, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 软删除单个模型
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型实例的创建者资源组id（模型所属资源组id）
     * @return 软删除响应
     */
    public BaseResponse softDeleteModelByName(String modelName, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 新增/更新模型元数据json文件
     * @param modelName 模型名称
     * @param mergeModelRequest 请求体
     * @param userResourceGroupId 用户的资源组id
     * @return 增/更新响应
     */
    public CreateModelResp mergeModelJsonFile(String modelName, MergeModelRequest mergeModelRequest, String userResourceGroupId) {
        pass;
    }

    /**
     * 硬删除单条版本记录
     * @param versionNo 版本号
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 硬删除响应
     */
    public BaseResponse hardDeleteModelVersion(String versionNo, String modelName, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 软删除单个模型的指定版本
     * @param versionNo 版本号
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 软删除响应
     */
    public BaseResponse softDeleteByVersionAndModel(String versionNo, String modelName, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 新增/更新版本元数据json文件
     * @param versionNo 版本号
     * @param modelName 模型名称
     * @param mergeVersionRequest 请求体
     * @param userResourceGroupId 用户资源组id
     * @return 增/更新响应
     */
    public BaseResponse mergeVersionJsonFile(String versionNo, String modelName, MergeVersionRequest mergeVersionRequest, String userResourceGroupId) {
        pass;
    }

    /**
     * 回收站模型恢复
     * @param modelName 模型名
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 恢复响应
     */
    public BaseResponse restoreModelFromTrash(String modelName, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 回收站版本恢复
     * @param versionNo 模型版本
     * @param modelName 模型名称
     * @param userResourceGroupId 用户资源组id
     * @param modelOwnerGroupId 模型所属资源组id
     * @return 恢复响应
     */
    public BaseResponse restoreVersionFromTrash(String versionNo, String modelName, String userResourceGroupId, String modelOwnerGroupId) {
        pass;
    }

    /**
     * 查询支持的模型类型名称
     * @param modelCategoryList 支持功能列表,不填默认查询全部；多传查询并集（有一个即可）
     * @return 符合条件的模型类型列表
     */
    public QuerySupportModelTypeResp querySupportModelType(List<String> modelCategoryList) {
        pass;
    }

    /**
     * 查询模型所支持的量化类型
     * @param modelType 模型类型
     * @return 支持的量化类型列表
     */
    public List<String> querySupportQuantTypes(String modelType) {
        pass;
    }

    /**
     * 查询支持的模型分类列表
     * @param supportCategoryList 支持功能列表,不填默认查询全部；多传查询并集（有一个即可）
     * @return 符合条件的模型分类列表
     */
    public QuerySupportModelCategoryResp querySupportModelCategory(List<String> supportCategoryList) {
        pass;
    }

    /**
     * 新增模型分类
     * @param modelCategory 新增的模型分类名称
     * @return 是否新增成功响应体
     */
    public BaseResponse addModelCategory(String modelCategory) {
        pass;
    }

    /**
     * 删除模型分类
     * @param modelCategory 要删除的模型分类
     * @return 是否删除成功响应体
     */
    public BaseResponse delModelCategory(String modelCategory) {
        pass;
    }

    /**
     * 在模型分类下增加一个新的模型类型
     * @param modelCategory 要删除的模型类型所属分类
     * @param modelType 要删除的模型类型
     * @return 是否新增成功响应体
     */
    public BaseResponse addModelType(String modelCategory, String modelType) {
        pass;
    }

    /**
     * 在模型分类下删除一个模型类型
     * @param modelCategory 要删除的模型类型所属分类
     * @param modelType 要删除的模型类型
     * @return 是否删除成功响应体
     */
    public BaseResponse delModelType(String modelCategory, String modelType) {
        pass;
    }
}
```