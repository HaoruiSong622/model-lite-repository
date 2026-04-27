package com.huawei.modellite.repository.modelweight.application.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class VersionCreateRequest {
    private String sourceType;
    private String pvcName;
    private String internalPath;
    private String nfsServer;
    private String nfsPath;
    private String weightType;
    private TrainingMetadataDto trainingMetadata;
}