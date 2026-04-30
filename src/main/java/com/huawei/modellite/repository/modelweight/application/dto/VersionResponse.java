package com.huawei.modellite.repository.modelweight.application.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class VersionResponse {
    private UUID id;
    private UUID modelId;
    private Integer versionNumber;
    private String status;
    private Boolean registered;
    private Boolean locked;
    private String sourceType;
    private String pvcName;
    private String internalPath;
    private String nfsServer;
    private String nfsPath;
    private String weightType;
    private TrainingMetadataDto trainingMetadata;
    private Instant createTime;
    private Instant updateTime;
}