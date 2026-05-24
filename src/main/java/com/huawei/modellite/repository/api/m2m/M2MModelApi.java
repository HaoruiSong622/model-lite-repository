package com.huawei.modellite.repository.api.m2m;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.weighttask.application.dto.ArchiveRequest;
import com.huawei.modellite.repository.weighttask.application.service.UploadApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v2/models/{modelId}/versions")
public class M2MModelApi {

    private final UploadApplicationService uploadApplicationService;

    public M2MModelApi(UploadApplicationService uploadApplicationService) {
        this.uploadApplicationService = uploadApplicationService;
    }

    @PostMapping("/archive")
    public ResponseEntity<BaseResponse<VersionResponse>> archiveTrainingWeight(
            @PathVariable UUID modelId,
            @RequestBody ArchiveRequest request) {
        VersionResponse version = uploadApplicationService.archiveTrainingWeight(modelId, request);
        return ResponseEntity.ok(BaseResponse.success(version));
    }
}
