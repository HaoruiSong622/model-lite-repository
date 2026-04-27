package com.huawei.modellite.repository.api.user;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelListResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelModifyRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelResponse;
import com.huawei.modellite.repository.modelweight.application.dto.VersionCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.modelweight.application.service.ModelApplicationService;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v2/ui/models")
public class ModelApi {

    private final ModelApplicationService modelApplicationService;

    public ModelApi(ModelApplicationService modelApplicationService) {
        this.modelApplicationService = modelApplicationService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<ModelResponse>> createModel(@RequestBody ModelCreateRequest request) {
        ModelResponse model = modelApplicationService.createModel(request);
        return ResponseEntity.ok(BaseResponse.success(model));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ModelResponse>> getModel(@PathVariable UUID id) {
        ModelResponse model = modelApplicationService.getModel(id);
        return ResponseEntity.ok(BaseResponse.success(model));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BaseResponse<ModelResponse>> modifyModel(
            @PathVariable UUID id,
            @RequestBody ModelModifyRequest request) {
        ModelResponse model = modelApplicationService.modifyModel(id, request);
        return ResponseEntity.ok(BaseResponse.success(model));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResult<ModelListResponse>>> listModels(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) UUID tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        ModelQueryCondition condition = new ModelQueryCondition();
        condition.setCategoryId(categoryId);
        condition.setTypeId(typeId);
        condition.setTagId(tagId);
        condition.setKeyword(keyword);
        condition.setPage(page);
        condition.setPageSize(pageSize);
        condition.setSortBy(sortBy);
        condition.setSortOrder(sortOrder);

        PageResult<ModelListResponse> result = modelApplicationService.listModels(condition);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<BaseResponse<VersionResponse>> createVersion(
            @PathVariable UUID id,
            @RequestBody VersionCreateRequest request) {
        VersionResponse version = modelApplicationService.createVersion(id, request);
        return ResponseEntity.ok(BaseResponse.success(version));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public ResponseEntity<BaseResponse<VersionResponse>> getVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId) {
        VersionResponse version = modelApplicationService.getVersion(id, versionId);
        return ResponseEntity.ok(BaseResponse.success(version));
    }
}
