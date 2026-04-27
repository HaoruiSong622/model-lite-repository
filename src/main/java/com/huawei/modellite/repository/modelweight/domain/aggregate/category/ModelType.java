package com.huawei.modellite.repository.modelweight.domain.aggregate.category;

import java.util.UUID;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;

public class ModelType {

    private UUID typeId;
    private String name;
    private String description;
    private Boolean isBuiltIn;

    // Default constructor for MyBatis
    public ModelType() {
    }

    public ModelType(UUID typeId, String name, String description, Boolean isBuiltIn) {
        if (name == null || name.isEmpty() || name.length() > 100) {
            throw new ModelLiteException(ErrorCode.MODEL_TYPE_NAME_EXISTS,
                    "模型类型名称不能为空且长度不能超过100");
        }
        this.typeId = typeId;
        this.name = name;
        this.description = description;
        this.isBuiltIn = isBuiltIn;
    }

    public UUID getTypeId() {
        return typeId;
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
}
