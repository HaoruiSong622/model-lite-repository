package com.huawei.modellite.repository.modelweight.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class TagResponse {
    private UUID id;
    private String name;
    private String tagType;
    private Boolean isBuiltin;
    private Instant createTime;
    private Instant updateTime;
}