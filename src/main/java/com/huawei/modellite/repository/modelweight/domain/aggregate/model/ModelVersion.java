package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import java.util.UUID;

import com.huawei.modellite.repository.common.enums.VersionStatus;
import lombok.Getter;

@Getter
public class ModelVersion {

    private UUID versionId;
    private int versionNumber;
    private StoragePath storagePath;
    private String weightType;
    private VersionStatus status;
    private boolean registered;
    private boolean locked;
    private TrainingMetadata trainingMetadata;

    public ModelVersion() {
    }

    public ModelVersion(UUID versionId, int versionNumber, StoragePath storagePath,
                        String weightType, VersionStatus status,
                        boolean registered, boolean locked,
                        TrainingMetadata trainingMetadata) {
        this.versionId = versionId;
        this.versionNumber = versionNumber;
        this.storagePath = storagePath;
        this.weightType = weightType;
        this.status = status;
        this.registered = registered;
        this.locked = locked;
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
}
