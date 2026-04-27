package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import java.util.UUID;

import com.huawei.modellite.repository.common.enums.VersionStatus;

public class ModelVersion {

    private UUID versionId;
    private int versionNumber;
    private StoragePath storagePath;
    private String weightType;
    private VersionStatus status;
    private boolean isRegistered;
    private boolean isLocked;
    private TrainingMetadata trainingMetadata;

    public ModelVersion() {
    }

    public ModelVersion(UUID versionId, int versionNumber, StoragePath storagePath,
                        String weightType, VersionStatus status,
                        boolean isRegistered, boolean isLocked,
                        TrainingMetadata trainingMetadata) {
        this.versionId = versionId;
        this.versionNumber = versionNumber;
        this.storagePath = storagePath;
        this.weightType = weightType;
        this.status = status;
        this.isRegistered = isRegistered;
        this.isLocked = isLocked;
        this.trainingMetadata = trainingMetadata;
    }

    public static ModelVersion createInitialVersion(StoragePath storagePath) {
        return new ModelVersion(
                UUID.randomUUID(),
                1,
                storagePath,
                null,
                VersionStatus.NO_WEIGHT,
                false,
                false,
                null
        );
    }

    public UUID getVersionId() {
        return versionId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public StoragePath getStoragePath() {
        return storagePath;
    }

    public String getWeightType() {
        return weightType;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public boolean getIsRegistered() {
        return isRegistered;
    }

    public boolean getIsLocked() {
        return isLocked;
    }

    public TrainingMetadata getTrainingMetadata() {
        return trainingMetadata;
    }
}
