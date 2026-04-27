package com.huawei.modellite.repository.api.user;

import com.huawei.modellite.repository.common.dto.response.BaseResponse;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryRequest;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryResponse;
import com.huawei.modellite.repository.modelweight.application.service.CategoryApplicationService;
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
@RequestMapping("/v2/ui/categories")
public class CategoryApi {

    private final CategoryApplicationService categoryApplicationService;

    public CategoryApi(CategoryApplicationService categoryApplicationService) {
        this.categoryApplicationService = categoryApplicationService;
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryApplicationService.getAllCategories();
        return ResponseEntity.ok(BaseResponse.success(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        CategoryResponse category = categoryApplicationService.getCategoryById(id);
        return ResponseEntity.ok(BaseResponse.success(category));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse category = categoryApplicationService.createCategory(request);
        return ResponseEntity.ok(BaseResponse.success(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryApplicationService.deleteCategory(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PostMapping("/{id}/types")
    public ResponseEntity<BaseResponse<Void>> addModelTypeToCategory(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        categoryApplicationService.addModelTypeToCategory(id, name, description);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @DeleteMapping("/{id}/types/{typeId}")
    public ResponseEntity<BaseResponse<Void>> removeModelTypeFromCategory(
            @PathVariable UUID id,
            @PathVariable UUID typeId) {
        categoryApplicationService.removeModelTypeFromCategory(id, typeId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
