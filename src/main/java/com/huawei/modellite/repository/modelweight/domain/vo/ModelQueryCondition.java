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
    private Integer page = 1;
    private Integer pageSize = 50;
    private String sortBy = "createTime";
    private String sortOrder = "desc";
}