package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import java.util.UUID;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
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

    public void register(StoragePath storagePath, String weightType, TrainingMetadata trainingMetadata) {
        if (this.status != VersionStatus.NO_WEIGHT) {
            throw new ModelLiteException(ErrorCode.VERSION_STATUS_INVALID_FOR_REGISTER,
                    "只有NoWeight状态的版本可以纳管，当前状态: " + this.status.getDbValue());
        }
        this.storagePath = storagePath;
        this.weightType = weightType;
        this.trainingMetadata = trainingMetadata != null ? trainingMetadata : TrainingMetadata.empty();
        this.registered = true;
        this.status = VersionStatus.AVAILABLE;
    }
}
