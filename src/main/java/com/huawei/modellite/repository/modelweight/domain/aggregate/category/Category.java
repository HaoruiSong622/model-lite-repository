package com.huawei.modellite.repository.modelweight.domain.aggregate.category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;

public class Category {

    private UUID categoryId;
    private String name;
    private String description;
    private Boolean isBuiltIn;
    private List<ModelType> modelTypes;

    // Default constructor for MyBatis
    public Category() {
        this.modelTypes = new ArrayList<>();
    }

    public Category(String name, String description) {
        this(name, description, false);
    }

    public Category(String name, String description, Boolean isBuiltIn) {
        validateName(name);
        this.categoryId = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.isBuiltIn = isBuiltIn;
        this.modelTypes = new ArrayList<>();
    }

    public Category(UUID categoryId, String name, String description, Boolean isBuiltIn) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.isBuiltIn = isBuiltIn;
        this.modelTypes = new ArrayList<>();
    }

    private void validateName(String name) {
        if (name == null || name.isEmpty() || name.length() > 100) {
            throw new ModelLiteException(ErrorCode.CATEGORY_NAME_EXISTS,
                    "分类名称不能为空且长度不能超过100");
        }
    }

    public ModelType addModelType(String name, String description) {
        boolean nameExists = modelTypes.stream()
                .anyMatch(mt -> mt.getName().equals(name));
        if (nameExists) {
            throw new ModelLiteException(ErrorCode.MODEL_TYPE_NAME_EXISTS,
                    "分类下已存在同名模型类型: " + name);
        }

        ModelType modelType = new ModelType(UUID.randomUUID(), name, description, false);
        modelTypes.add(modelType);
        return modelType;
    }

    public void removeModelType(UUID typeId, boolean hasModelReference) {
        ModelType modelType = modelTypes.stream()
                .filter(mt -> mt.getTypeId().equals(typeId))
                .findFirst()
                .orElseThrow(() -> new ModelLiteException(
                        ErrorCode.MODEL_TYPE_NOT_FOUND, "模型类型不存在"));

        if (modelType.getIsBuiltIn()) {
            throw new ModelLiteException(ErrorCode.MODEL_TYPE_BUILTIN,
                    "内置模型类型不允许删除");
        }

        if (hasModelReference) {
            throw new ModelLiteException(ErrorCode.CATEGORY_HAS_MODELS,
                    "模型类型下存在模型，禁止删除");
        }

        modelTypes.remove(modelType);
    }

    public void setModelTypes(List<ModelType> modelTypes) {
        this.modelTypes = modelTypes != null ? new ArrayList<>(modelTypes) : new ArrayList<>();
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getIsBuiltIn() {
        return isBuiltIn;
    }

    public List<ModelType> getModelTypes() {
        return modelTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(categoryId, category.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId);
    }
}
