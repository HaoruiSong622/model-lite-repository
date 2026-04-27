package com.huawei.modellite.repository.api.user;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.modelweight.application.dto.TagRequest;
import com.huawei.modellite.repository.modelweight.application.dto.TagResponse;
import com.huawei.modellite.repository.modelweight.application.service.TagApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v2/ui")
public class TagApi {

    private final TagApplicationService tagApplicationService;

    public TagApi(TagApplicationService tagApplicationService) {
        this.tagApplicationService = tagApplicationService;
    }

    @GetMapping("/tags")
    public ResponseEntity<BaseResponse<List<TagResponse>>> getAllTags() {
        List<TagResponse> tags = tagApplicationService.getAllTags();
        return ResponseEntity.ok(BaseResponse.success(tags));
    }

    @PostMapping("/tags")
    public ResponseEntity<BaseResponse<TagResponse>> createTag(@RequestBody TagRequest request) {
        TagResponse tag = tagApplicationService.createTag(request);
        return ResponseEntity.ok(BaseResponse.success(tag));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteTag(@PathVariable UUID id) {
        tagApplicationService.deleteTag(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PostMapping("/models/{modelId}/tags")
    public ResponseEntity<BaseResponse<Void>> addTagToModel(
            @PathVariable UUID modelId,
            @RequestBody List<UUID> tagIds) {
        tagApplicationService.addTagToModel(modelId, tagIds);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @DeleteMapping("/models/{modelId}/tags/{tagId}")
    public ResponseEntity<BaseResponse<Void>> removeTagFromModel(
            @PathVariable UUID modelId,
            @PathVariable UUID tagId) {
        tagApplicationService.removeTagFromModel(modelId, tagId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
