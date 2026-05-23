package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    private Model createDefaultModel() {
        return Model.createModel(
                "TestModel",
                "A test model",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default-resource-group",
                "admin",
                "AuthorName",
                "SeriesA",
                "10B",
                4096L
        );
    }

    @Nested
    @DisplayName("createModel tests")
    class CreateModelTests {

        @Test
        @DisplayName("should create Model with all fields set correctly")
        void should_createModel_withAllFields() {
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();

            Model model = Model.createModel(
                    "GPT-4",
                    "Large language model",
                    categoryId,
                    typeId,
                    "gpu-group-1",
                    "user123",
                    "OpenAI",
                    "GPT-Series",
                    "1.8T",
                    128000L
            );

            assertNotNull(model.getModelId());
            assertEquals("GPT-4", model.getName());
            assertEquals("Large language model", model.getDescription());
            assertEquals(categoryId, model.getCategoryId());
            assertEquals(typeId, model.getTypeId());
            assertEquals("gpu-group-1", model.getResourceGroup());
            assertEquals("user123", model.getCreateUser());
            assertEquals("OpenAI", model.getAuthor());
            assertEquals("GPT-Series", model.getSeriesName());
            assertEquals("1.8T", model.getModelSize());
            assertEquals(128000L, model.getMaxSeqLength());
            assertNotNull(model.getVersions());
            assertNotNull(model.getTagIds());
            assertTrue(model.getTagIds().isEmpty());
        }

        @Test
        @DisplayName("should auto-create first version (v1, NoWeight, not registered)")
        void should_autoCreateFirstVersion() {
            Model model = createDefaultModel();

            assertEquals(1, model.getVersions().size());

            ModelVersion v1 = model.getVersions().get(0);
            assertEquals(1, v1.getVersionNumber());
            assertEquals(VersionStatus.NO_WEIGHT, v1.getStatus());
            assertFalse(v1.isRegistered());
            assertFalse(v1.isLocked());
            assertNull(v1.getWeightType());
            assertNull(v1.getTrainingMetadata());
        }

        @Test
        @DisplayName("should throw when name is null")
        void should_throw_whenNameIsNull() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel(null, "desc", UUID.randomUUID(), UUID.randomUUID(),
                            "rg", "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should throw when name is empty")
        void should_throw_whenNameIsEmpty() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel("", "desc", UUID.randomUUID(), UUID.randomUUID(),
                            "rg", "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should throw when name is blank")
        void should_throw_whenNameIsBlank() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel("   ", "desc", UUID.randomUUID(), UUID.randomUUID(),
                            "rg", "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should throw when name exceeds 255 characters")
        void should_throw_whenNameExceeds255() {
            String longName = "a".repeat(256);
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel(longName, "desc", UUID.randomUUID(), UUID.randomUUID(),
                            "rg", "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should allow name with exactly 255 characters")
        void should_allowNameWith255Characters() {
            String name = "a".repeat(255);
            Model model = Model.createModel(name, "desc", UUID.randomUUID(), UUID.randomUUID(),
                    "rg", "user", "author", "series", "10B", 4096L);
            assertEquals(name, model.getName());
        }

        @Test
        @DisplayName("should throw when name contains invalid characters")
        void should_throw_whenNameContainsInvalidCharacters() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel("model@name", "desc", UUID.randomUUID(), UUID.randomUUID(),
                            "rg", "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_NAME_EXISTS, ex.getCode());
        }

        @Test
        @DisplayName("should throw when resourceGroup is null")
        void should_throw_whenResourceGroupIsNull() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel("Model", "desc", UUID.randomUUID(), UUID.randomUUID(),
                            null, "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_RESOURCE_GROUP_IMMUTABLE, ex.getCode());
        }

        @Test
        @DisplayName("should throw when resourceGroup is empty")
        void should_throw_whenResourceGroupIsEmpty() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Model.createModel("Model", "desc", UUID.randomUUID(), UUID.randomUUID(),
                            "", "user", "author", "series", "10B", 4096L));
            assertEquals(ErrorCode.MODEL_RESOURCE_GROUP_IMMUTABLE, ex.getCode());
        }
    }

    @Nested
    @DisplayName("modifyMetadata tests")
    class ModifyMetadataTests {

        @Test
        @DisplayName("should modify mutable fields: description, author, seriesName, modelSize, maxSeqLength")
        void should_modifyMutableFields() {
            Model model = createDefaultModel();

            model.modifyMetadata("Updated description", "NewAuthor", "NewSeries", "20B", 8192L);

            assertEquals("Updated description", model.getDescription());
            assertEquals("NewAuthor", model.getAuthor());
            assertEquals("NewSeries", model.getSeriesName());
            assertEquals("20B", model.getModelSize());
            assertEquals(8192L, model.getMaxSeqLength());
        }

        @Test
        @DisplayName("should not change name when modifying metadata")
        void should_notChangeName() {
            Model model = createDefaultModel();
            String originalName = model.getName();

            model.modifyMetadata("new desc", "author", "series", "size", 100L);

            assertEquals(originalName, model.getName());
        }

        @Test
        @DisplayName("should not change resourceGroup when modifying metadata")
        void should_notChangeResourceGroup() {
            Model model = createDefaultModel();
            String originalResourceGroup = model.getResourceGroup();

            model.modifyMetadata("new desc", "author", "series", "size", 100L);

            assertEquals(originalResourceGroup, model.getResourceGroup());
        }

        @Test
        @DisplayName("should allow null description")
        void should_allowNullDescription() {
            Model model = createDefaultModel();
            model.modifyMetadata(null, "author", "series", "size", 100L);
            assertNull(model.getDescription());
        }
    }

    @Nested
    @DisplayName("createVersion tests")
    class CreateVersionTests {

        @Test
        @DisplayName("should create version with auto-incremented version number")
        void should_createVersion_withAutoIncrement() {
            Model model = createDefaultModel();
            assertEquals(1, model.getLatestVersionNumber());

            StoragePath path = StoragePath.ofPvc("model-pvc");
            ModelVersion v2 = model.createVersion(path, "fp16", VersionStatus.UPLOADING, false, null);

            assertEquals(2, v2.getVersionNumber());
            assertEquals(2, model.getVersions().size());
            assertEquals(2, model.getLatestVersionNumber());
        }

        @Test
        @DisplayName("should create version with all fields set")
        void should_createVersion_withAllFields() {
            Model model = createDefaultModel();
            TrainingMetadata metadata = new TrainingMetadata("PyTorch", "Pretrain", "AdamW", 3600L, "0.001", "v1.0");
            StoragePath path = StoragePath.ofNfs("10.0.0.1", "/data/model");

            ModelVersion v2 = model.createVersion(path, "bf16", VersionStatus.AVAILABLE, true, metadata);

            assertNotNull(v2.getVersionId());
            assertEquals(2, v2.getVersionNumber());
            assertEquals(path, v2.getStoragePath());
            assertEquals("bf16", v2.getWeightType());
            assertEquals(VersionStatus.AVAILABLE, v2.getStatus());
            assertTrue(v2.isRegistered());
            assertFalse(v2.isLocked());
            assertNotNull(v2.getTrainingMetadata());
        }

        @Test
        @DisplayName("should auto-increment across multiple versions")
        void should_autoIncrementAcrossVersions() {
            Model model = createDefaultModel();

            for (int i = 2; i <= 10; i++) {
                ModelVersion v = model.createVersion(
                        StoragePath.ofPvc("pvc-" + i), "fp16", VersionStatus.AVAILABLE, false, null);
                assertEquals(i, v.getVersionNumber());
            }

            assertEquals(10, model.getVersions().size());
            assertEquals(10, model.getLatestVersionNumber());
        }

        @Test
        @DisplayName("should throw VERSION_CAPACITY_EXCEEDED when exceeding 50 versions")
        void should_throw_whenExceeding50Versions() {
            Model model = createDefaultModel();

            for (int i = 2; i <= 50; i++) {
                model.createVersion(StoragePath.ofPvc("pvc-" + i), "fp16",
                        VersionStatus.AVAILABLE, false, null);
            }
            assertEquals(50, model.getVersions().size());

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> model.createVersion(StoragePath.ofPvc("pvc-51"), "fp16",
                            VersionStatus.AVAILABLE, false, null));

            assertEquals(ErrorCode.VERSION_CAPACITY_EXCEEDED, ex.getCode());
        }

        @Test
        @DisplayName("should allow exactly 50 versions")
        void should_allowExactly50Versions() {
            Model model = createDefaultModel();

            for (int i = 2; i <= 50; i++) {
                model.createVersion(StoragePath.ofPvc("pvc-" + i), "fp16",
                        VersionStatus.AVAILABLE, false, null);
            }

            assertEquals(50, model.getVersions().size());
            assertEquals(50, model.getLatestVersionNumber());
        }
    }

    @Nested
    @DisplayName("register tests")
    class RegisterTests {

        @Test
        @DisplayName("should register NoWeight version to Available with storage path")
        void should_register_noWeightVersion() {
            Model model = createDefaultModel();
            ModelVersion v1 = model.getVersions().get(0);

            assertEquals(VersionStatus.NO_WEIGHT, v1.getStatus());
            assertFalse(v1.isRegistered());

            StoragePath path = StoragePath.ofPvc("registered-pvc", "/weights");
            v1.register(path, "safetensors", null);

            assertEquals(VersionStatus.AVAILABLE, v1.getStatus());
            assertTrue(v1.isRegistered());
            assertEquals(path, v1.getStoragePath());
            assertEquals("safetensors", v1.getWeightType());
            assertNotNull(v1.getTrainingMetadata());
        }

        @Test
        @DisplayName("should register with training metadata")
        void should_register_withTrainingMetadata() {
            Model model = createDefaultModel();
            ModelVersion v1 = model.getVersions().get(0);

            StoragePath path = StoragePath.ofNfs("10.0.1.100", "/data/model");
            TrainingMetadata metadata = new TrainingMetadata("PyTorch", "SFT", "LoRA", 36000L, "0.0023", "v1.0");

            v1.register(path, "bf16", metadata);

            assertEquals(VersionStatus.AVAILABLE, v1.getStatus());
            assertTrue(v1.isRegistered());
            assertEquals(metadata, v1.getTrainingMetadata());
        }

        @Test
        @DisplayName("should throw VERSION_STATUS_INVALID_FOR_REGISTER when version is already Available")
        void should_throw_whenVersionAlreadyAvailable() {
            Model model = createDefaultModel();
            ModelVersion v1 = model.getVersions().get(0);

            StoragePath path = StoragePath.ofPvc("first-pvc");
            v1.register(path, "fp16", null);

            StoragePath anotherPath = StoragePath.ofPvc("second-pvc");
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> v1.register(anotherPath, "bf16", null));

            assertEquals(ErrorCode.VERSION_STATUS_INVALID_FOR_REGISTER, ex.getCode());
        }

        @Test
        @DisplayName("should throw VERSION_STATUS_INVALID_FOR_REGISTER when version is Uploading")
        void should_throw_whenVersionUploading() {
            Model model = createDefaultModel();

            ModelVersion uploadingVersion = model.createVersion(
                    StoragePath.empty(), null, VersionStatus.UPLOADING, false, null);

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> uploadingVersion.register(StoragePath.ofPvc("pvc"), "fp16", null));

            assertEquals(ErrorCode.VERSION_STATUS_INVALID_FOR_REGISTER, ex.getCode());
        }
    }

    @Nested
    @DisplayName("getLatestVersionNumber tests")
    class GetLatestVersionNumberTests {

        @Test
        @DisplayName("should return 1 for newly created model")
        void should_return1_forNewModel() {
            Model model = createDefaultModel();
            assertEquals(1, model.getLatestVersionNumber());
        }
    }

    @Nested
    @DisplayName("getModelVersion tests")
    class GetModelVersionTests {

        @Test
        @DisplayName("should return version by version number")
        void should_returnVersion_byNumber() {
            Model model = createDefaultModel();
            model.createVersion(StoragePath.ofPvc("pvc-2"), "fp16",
                    VersionStatus.AVAILABLE, false, null);

            ModelVersion v1 = model.getModelVersion(1);
            assertEquals(1, v1.getVersionNumber());

            ModelVersion v2 = model.getModelVersion(2);
            assertEquals(2, v2.getVersionNumber());
        }

        @Test
        @DisplayName("should throw VERSION_NOT_FOUND when version does not exist")
        void should_throw_whenVersionNotFound() {
            Model model = createDefaultModel();

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> model.getModelVersion(99));

            assertEquals(ErrorCode.VERSION_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("should throw VERSION_NOT_FOUND when version number is 0 or negative")
        void should_throw_whenVersionNumberInvalid() {
            Model model = createDefaultModel();

            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> model.getModelVersion(0));
            assertEquals(ErrorCode.VERSION_NOT_FOUND, ex.getCode());
        }
    }

    @Nested
    @DisplayName("TagIds tests")
    class TagIdsTests {

        @Test
        @DisplayName("should return unmodifiable tag list")
        void should_returnUnmodifiableTagList() {
            Model model = createDefaultModel();
            assertThrows(UnsupportedOperationException.class,
                    () -> model.getTagIds().add(UUID.randomUUID()));
        }

        @Test
        @DisplayName("should return empty tag list for new model")
        void should_returnEmptyTagList() {
            Model model = createDefaultModel();
            assertTrue(model.getTagIds().isEmpty());
        }
    }

    @Nested
    @DisplayName("Versions immutability tests")
    class VersionsImmutabilityTests {

        @Test
        @DisplayName("should return unmodifiable versions list")
        void should_returnUnmodifiableVersionsList() {
            Model model = createDefaultModel();
            assertThrows(UnsupportedOperationException.class,
                    () -> model.getVersions().add(
                            new ModelVersion(UUID.randomUUID(), 99, null, null,
                                    VersionStatus.NO_WEIGHT, false, false, null)));
        }
    }

    @Nested
    @DisplayName("Equality tests")
    class EqualityTests {

        @Test
        @DisplayName("should be equal when modelId is the same")
        void should_beEqual_whenModelIdSame() {
            Model model = createDefaultModel();
            assertEquals(model, model);
        }

        @Test
        @DisplayName("should not be equal to null")
        void should_notBeEqualToNull() {
            Model model = createDefaultModel();
            assertNotEquals(model, null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void should_notBeEqualToDifferentType() {
            Model model = createDefaultModel();
            assertNotEquals(model, "string");
        }

        @Test
        @DisplayName("should have same hashCode for same object")
        void should_haveSameHashCode() {
            Model model = createDefaultModel();
            assertEquals(model.hashCode(), model.hashCode());
        }
    }

    @Nested
    @DisplayName("Reconstitution constructor tests")
    class ReconstitutionTests {

        @Test
        @DisplayName("should reconstitute model from persistence with all fields")
        void should_reconstituteModel() {
            UUID modelId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID typeId = UUID.randomUUID();
            List<ModelVersion> versions = new ArrayList<>();
            versions.add(new ModelVersion(UUID.randomUUID(), 1, StoragePath.ofPvc("pvc"),
                    "fp16", VersionStatus.AVAILABLE, true, false, null));
            List<UUID> tagIds = new ArrayList<>();
            tagIds.add(UUID.randomUUID());

            Model model = new Model(modelId, "ModelName", "desc", categoryId, typeId,
                    "rg", "user", "author", "series", "10B", 4096L, versions, tagIds);

            assertEquals(modelId, model.getModelId());
            assertEquals("ModelName", model.getName());
            assertEquals("desc", model.getDescription());
            assertEquals(categoryId, model.getCategoryId());
            assertEquals(typeId, model.getTypeId());
            assertEquals("rg", model.getResourceGroup());
            assertEquals("user", model.getCreateUser());
            assertEquals("author", model.getAuthor());
            assertEquals("series", model.getSeriesName());
            assertEquals("10B", model.getModelSize());
            assertEquals(4096L, model.getMaxSeqLength());
            assertEquals(1, model.getVersions().size());
            assertEquals(1, model.getTagIds().size());
        }

        @Test
        @DisplayName("should handle null versions and tagIds in reconstitution")
        void should_handleNullCollections() {
            Model model = new Model(UUID.randomUUID(), "Name", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "rg", "user", "author", "series", "10B", 4096L,
                    null, null);

            assertNotNull(model.getVersions());
            assertTrue(model.getVersions().isEmpty());
            assertNotNull(model.getTagIds());
            assertTrue(model.getTagIds().isEmpty());
        }
    }
}
