package com.huawei.modellite.repository.modelweight.domain.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModelDomainService {

    private static final long MAX_MODELS_PER_RESOURCE_GROUP = 100;
    private static final long MAX_MODELS_GLOBAL = 1000;

    private final CategoryRepository categoryRepository;
    private final ModelRepository modelRepository;
    private final TagRepository tagRepository;

    public ModelDomainService(CategoryRepository categoryRepository,
                              ModelRepository modelRepository,
                              TagRepository tagRepository) {
        this.categoryRepository = categoryRepository;
        this.modelRepository = modelRepository;
        this.tagRepository = tagRepository;
    }

    public void validateModelCreation(String name, UUID categoryId, UUID typeId, String resourceGroup) {
        validateCategoryAndType(categoryId, typeId);

        if (modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, name)) {
            throw new ModelLiteException(ErrorCode.MODEL_NAME_EXISTS,
                    "模型名称在同分类同类型下已存在: " + name);
        }

        if (modelRepository.countByResourceGroup(resourceGroup) >= MAX_MODELS_PER_RESOURCE_GROUP) {
            throw new ModelLiteException(ErrorCode.MODEL_CAPACITY_EXCEEDED,
                    "资源组下模型数量已达上限: " + resourceGroup);
        }

        if (modelRepository.countAll() >= MAX_MODELS_GLOBAL) {
            throw new ModelLiteException(ErrorCode.MODEL_GLOBAL_CAPACITY_EXCEEDED,
                    "全局模型数量已达上限");
        }
    }

    public void validateModelModification(UUID modelId, UUID categoryId, UUID typeId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        boolean categoryChanged = !model.getCategoryId().equals(categoryId);
        boolean typeChanged = !model.getTypeId().equals(typeId);

        if (categoryChanged || typeChanged) {
            validateCategoryAndType(categoryId, typeId);

            if (modelRepository.existsByCategoryAndTypeAndName(categoryId, typeId, model.getName())) {
                throw new ModelLiteException(ErrorCode.MODEL_NAME_EXISTS,
                        "新分类+类型下模型名称已存在: " + model.getName());
            }
        }
    }

    private void validateCategoryAndType(UUID categoryId, UUID typeId) {
        Category category = categoryRepository.findByIdWithTypes(categoryId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.CATEGORY_NOT_FOUND,
                        "分类不存在: " + categoryId));

        boolean typeBelongsToCategory = category.getModelTypes().stream()
                .anyMatch(mt -> mt.getTypeId().equals(typeId));

        if (!typeBelongsToCategory) {
            throw new ModelLiteException(ErrorCode.MODEL_TYPE_NOT_BELONG_TO_CATEGORY,
                    "模型类型不属于该分类: typeId=" + typeId + ", categoryId=" + categoryId);
        }
    }
}
