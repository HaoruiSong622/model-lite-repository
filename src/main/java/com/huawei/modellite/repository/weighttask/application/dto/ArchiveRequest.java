package com.huawei.modellite.repository.weighttask.application.dto;

import com.huawei.modellite.repository.modelweight.application.dto.TrainingMetadataDto;
import lombok.Data;

@Data
public class ArchiveRequest {
    private String sourceType;
    private String pvcName;
    private String internalPath;
    private String nfsServer;
    private String nfsPath;
    private String weightType;
    private TrainingMetadataDto trainingMetadata;
}
