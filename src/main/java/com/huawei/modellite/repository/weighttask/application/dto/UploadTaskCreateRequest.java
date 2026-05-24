package com.huawei.modellite.repository.weighttask.application.dto;

import com.huawei.modellite.repository.modelweight.application.dto.TrainingMetadataDto;
import lombok.Data;

@Data
public class UploadTaskCreateRequest {
    private String sourceType;
    private String nfsServer;
    private String nfsPath;
    private String cifsServer;
    private String cifsShare;
    private String cifsPath;
    private String cifsUsername;
    private String cifsPassword;
    private String sourcePvcName;
    private String sourceInternalPath;
    private String weightType;
    private TrainingMetadataDto trainingMetadata;
}
