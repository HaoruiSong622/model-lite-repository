package com.huawei.modellite.repository.modelweight.domain.vo;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ModelQueryCondition {
    private UUID categoryId;
    private UUID typeId;
    private UUID tagId;
    private String keyword;
    private List<String> resourceGroups;
    private Integer page;
    private Integer pageSize;
    private String sortBy;
    private String sortOrder;
}