package com.huawei.modellite.repository.modelweight.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryRequest;
import com.huawei.modellite.repository.modelweight.application.dto.CategoryResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelTypeResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryApplicationService {

    private final CategoryRepository categoryRepository;

    public CategoryApplicationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ModelLiteException(ErrorCode.CATEGORY_NAME_EXISTS,
                    "分类名称已存在: " + request.getName());
        }

        Category category = new Category(request.getName(), request.getDescription());
        categoryRepository.save(category);

        return toResponse(category);
    }

    public void deleteCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.CATEGORY_NOT_FOUND,
                        "分类不存在"));

        if (Boolean.TRUE.equals(category.getIsBuiltIn())) {
            throw new ModelLiteException(ErrorCode.CATEGORY_BUILTIN,
                    "内置分类不允许删除");
        }

        if (categoryRepository.hasModelReference(categoryId)) {
            throw new ModelLiteException(ErrorCode.CATEGORY_HAS_MODELS,
                    "分类下存在模型，禁止删除");
        }

        categoryRepository.deleteById(categoryId);
    }

    public void addModelTypeToCategory(UUID categoryId, UUID typeId) {
        throw new UnsupportedOperationException("addModelTypeToCategory with UUID typeId is not supported; use addModelType with name and description instead");
    }

    public void removeModelTypeFromCategory(UUID categoryId, UUID typeId) {
        Category category = categoryRepository.findByIdWithTypes(categoryId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.CATEGORY_NOT_FOUND,
                        "分类不存在"));

        boolean hasReference = categoryRepository.hasModelReferenceByTypeId(typeId);
        category.removeModelType(typeId, hasReference);
        categoryRepository.save(category);
    }

    public CategoryResponse getCategoryById(UUID categoryId) {
        Category category = categoryRepository.findByIdWithTypes(categoryId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.CATEGORY_NOT_FOUND,
                        "分类不存在"));

        return toResponse(category);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllWithTypes().stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getCategoryId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setIsBuiltin(category.getIsBuiltIn());
        response.setModelTypes(category.getModelTypes().stream()
                .map(this::toModelTypeResponse)
                .toList());
        return response;
    }

    private ModelTypeResponse toModelTypeResponse(ModelType modelType) {
        ModelTypeResponse response = new ModelTypeResponse();
        response.setId(modelType.getTypeId());
        response.setName(modelType.getName());
        response.setDescription(modelType.getDescription());
        response.setIsBuiltin(modelType.getIsBuiltIn());
        return response;
    }
}
