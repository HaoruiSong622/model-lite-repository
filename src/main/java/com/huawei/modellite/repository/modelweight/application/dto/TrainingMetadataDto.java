package com.huawei.modellite.repository.modelweight.application.dto;

import lombok.Data;

@Data
public class TrainingMetadataDto {
    private String trainFrame;
    private String trainType;
    private String trainStrategy;
    private Long trainTime;
    private String finalLoss;
    private String sourceVersion;
}