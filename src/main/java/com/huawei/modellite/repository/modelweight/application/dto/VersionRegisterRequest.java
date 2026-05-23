package com.huawei.modellite.repository.modelweight.application.dto;

import lombok.Data;

@Data
public class VersionRegisterRequest {
    private String sourceType;
    private String pvcName;
    private String internalPath;
    private String nfsServer;
    private String nfsPath;
    private String weightType;
    private TrainingMetadataDto trainingMetadata;
}