package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelVersionTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("should create ModelVersion with correct defaults")
        void should_createModelVersion_withCorrectDefaults() {
            UUID versionId = UUID.randomUUID();
            StoragePath storagePath = StoragePath.ofPvc("my-pvc", "/data");

            ModelVersion version = new ModelVersion(versionId, 1, storagePath,
                    "fp16", VersionStatus.NO_WEIGHT, false, false, null);

            assertEquals(versionId, version.getVersionId());
            assertEquals(1, version.getVersionNumber());
            assertEquals(storagePath, version.getStoragePath());
            assertEquals("fp16", version.getWeightType());
            assertEquals(VersionStatus.NO_WEIGHT, version.getStatus());
            assertFalse(version.getIsRegistered());
            assertFalse(version.getIsLocked());
            assertNull(version.getTrainingMetadata());
        }

        @Test
        @DisplayName("should create ModelVersion with training metadata")
        void should_createModelVersion_withTrainingMetadata() {
            TrainingMetadata metadata = new TrainingMetadata(
                    "PyTorch", "Pretrain", "AdamW", 3600L, "0.001", "v1.0");

            ModelVersion version = new ModelVersion(
                    UUID.randomUUID(), 1, StoragePath.ofPvc("pvc", "/data"),
                    "fp32", VersionStatus.AVAILABLE, true, false, metadata);

            assertNotNull(version.getTrainingMetadata());
            assertEquals("PyTorch", version.getTrainingMetadata().getTrainFrame());
            assertEquals("Pretrain", version.getTrainingMetadata().getTrainType());
        }
    }

    @Nested
    @DisplayName("Getter tests")
    class GetterTests {

        @Test
        @DisplayName("should return all fields correctly")
        void should_returnAllFields() {
            UUID versionId = UUID.randomUUID();
            StoragePath path = StoragePath.ofNfs("10.0.0.1", "/data/models");
            TrainingMetadata metadata = new TrainingMetadata("TF", "Finetune", "SGD", 1800L, "0.01", "v2.0");

            ModelVersion version = new ModelVersion(versionId, 3, path,
                    "bf16", VersionStatus.UPLOADING, true, true, metadata);

            assertEquals(versionId, version.getVersionId());
            assertEquals(3, version.getVersionNumber());
            assertEquals(path, version.getStoragePath());
            assertEquals("bf16", version.getWeightType());
            assertEquals(VersionStatus.UPLOADING, version.getStatus());
            assertTrue(version.getIsRegistered());
            assertTrue(version.getIsLocked());
            assertEquals(metadata, version.getTrainingMetadata());
        }
    }

    @Nested
    @DisplayName("Status tests")
    class StatusTests {

        @Test
        @DisplayName("should support all VersionStatus values")
        void should_supportAllVersionStatuses() {
            for (VersionStatus status : VersionStatus.values()) {
                ModelVersion version = new ModelVersion(
                        UUID.randomUUID(), 1, StoragePath.ofPvc("pvc", "/data"),
                        "fp16", status, false, false, null);
                assertEquals(status, version.getStatus());
            }
        }
    }

    @Nested
    @DisplayName("createInitialVersion factory tests")
    class CreateInitialVersionTests {

        @Test
        @DisplayName("should create initial version with v1 and NoWeight status")
        void should_createInitialVersion() {
            StoragePath path = StoragePath.ofPvc("init-pvc", "/data");
            ModelVersion version = ModelVersion.createInitialVersion(path);

            assertNotNull(version.getVersionId());
            assertEquals(1, version.getVersionNumber());
            assertEquals(path, version.getStoragePath());
            assertNull(version.getWeightType());
            assertEquals(VersionStatus.NO_WEIGHT, version.getStatus());
            assertFalse(version.getIsRegistered());
            assertFalse(version.getIsLocked());
            assertNull(version.getTrainingMetadata());
        }
    }
}
