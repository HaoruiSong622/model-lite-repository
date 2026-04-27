package com.huawei.modellite.repository.modelweight.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class ModelTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean isBuiltin;
    private Instant createTime;
    private Instant updateTime;
}