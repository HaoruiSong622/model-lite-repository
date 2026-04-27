package com.huawei.modellite.repository.modelweight.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ModelListResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID typeId;
    private String typeName;
    private String resourceGroup;
    private String createUser;
    private String author;
    private String seriesName;
    private Long modelSize;
    private Integer maxSeqLength;
    private List<TagResponse> tags;
    private Integer versionCount;
    private VersionResponse latestVersion;
    private Instant createTime;
    private Instant updateTime;
}