package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

/**
 * TrainingMetadata value object stub.
 * Full implementation will be done in Task 14.
 */
public class TrainingMetadata {

    private final String trainFrame;
    private final String trainType;
    private final String trainStrategy;
    private final Long trainTime;
    private final String finalLoss;
    private final String sourceVersion;

    public static TrainingMetadata empty() {
        return new TrainingMetadata(null, null, null, null, null, null);
    }

    public TrainingMetadata(String trainFrame, String trainType, String trainStrategy,
                            Long trainTime, String finalLoss, String sourceVersion) {
        this.trainFrame = trainFrame;
        this.trainType = trainType;
        this.trainStrategy = trainStrategy;
        this.trainTime = trainTime;
        this.finalLoss = finalLoss;
        this.sourceVersion = sourceVersion;
    }

    public String getTrainFrame() {
        return trainFrame;
    }

    public String getTrainType() {
        return trainType;
    }

    public String getTrainStrategy() {
        return trainStrategy;
    }

    public Long getTrainTime() {
        return trainTime;
    }

    public String getFinalLoss() {
        return finalLoss;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }
}
