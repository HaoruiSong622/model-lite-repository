package com.huawei.modellite.repository.api.user;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskCreateRequest;
import com.huawei.modellite.repository.weighttask.application.dto.UploadTaskResponse;
import com.huawei.modellite.repository.weighttask.application.service.UploadApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v2/ui/models/{modelId}/upload-tasks")
public class UploadTaskApi {

    private final UploadApplicationService uploadApplicationService;

    public UploadTaskApi(UploadApplicationService uploadApplicationService) {
        this.uploadApplicationService = uploadApplicationService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<UploadTaskResponse>> createUploadTask(
            @PathVariable UUID modelId,
            @RequestBody UploadTaskCreateRequest request,
            @RequestParam(required = false, defaultValue = "system") String createUser) {
        UploadTaskResponse response = uploadApplicationService.createUploadTask(modelId, request, createUser);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<BaseResponse<UploadTaskResponse>> getUploadTask(
            @PathVariable UUID modelId,
            @PathVariable UUID taskId) {
        UploadTaskResponse response = uploadApplicationService.getUploadTask(modelId, taskId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<UploadTaskResponse>>> listUploadTasks(
            @PathVariable UUID modelId,
            @RequestParam(required = false) String status) {
        List<UploadTaskResponse> response = uploadApplicationService.listUploadTasks(modelId, status);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping("/{taskId}/pause")
    public ResponseEntity<BaseResponse<Void>> pauseUploadTask(
            @PathVariable UUID modelId,
            @PathVariable UUID taskId) {
        uploadApplicationService.pauseUploadTask(modelId, taskId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PostMapping("/{taskId}/resume")
    public ResponseEntity<BaseResponse<Void>> resumeUploadTask(
            @PathVariable UUID modelId,
            @PathVariable UUID taskId) {
        uploadApplicationService.resumeUploadTask(modelId, taskId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<BaseResponse<Void>> cancelUploadTask(
            @PathVariable UUID modelId,
            @PathVariable UUID taskId) {
        uploadApplicationService.cancelUploadTask(modelId, taskId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<BaseResponse<Void>> deleteUploadTask(
            @PathVariable UUID modelId,
            @PathVariable UUID taskId) {
        uploadApplicationService.deleteUploadTask(modelId, taskId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
