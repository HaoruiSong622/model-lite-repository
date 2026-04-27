package com.huawei.modellite.repository.modelweight.application.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ModelCreateRequest {
    private String name;
    private String description;
    private UUID categoryId;
    private UUID typeId;
    private String resourceGroup;
    private String author;
    private String seriesName;
    private Long modelSize;
    private Integer maxSeqLength;
    private List<UUID> tagIds;
}