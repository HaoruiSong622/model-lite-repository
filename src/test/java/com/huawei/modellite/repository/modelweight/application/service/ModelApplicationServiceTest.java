package com.huawei.modellite.repository.modelweight.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.ModelCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelListResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelModifyRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelResponse;
import com.huawei.modellite.repository.modelweight.application.dto.TrainingMetadataDto;
import com.huawei.modellite.repository.modelweight.application.dto.VersionCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.StoragePath;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.TrainingMetadata;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.ModelType;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;
import com.huawei.modellite.repository.modelweight.domain.service.ModelDomainService;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelApplicationServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ModelDomainService modelDomainService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    private ModelApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ModelApplicationService(modelRepository, modelDomainService,
                categoryRepository, tagRepository);
        lenient().when(categoryRepository.findByIdWithTypes(any())).thenReturn(Optional.empty());
        lenient().when(tagRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("createModel tests")
    class CreateModelTests {

        @Test
        @DisplayName("should create model successfully without tags")
        void should_createModel_successfully_withoutTags() {
            ModelCreateRequest request = new ModelCreateRequest();
            request.setName("TestModel");
            request.setDescription("Test Description");
            request.setCategoryId(UUID.randomUUID());
            request.setTypeId(UUID.randomUUID());
            request.setResourceGroup("default");
            request.setAuthor("TestAuthor");
            request.setSeriesName("TestSeries");
            request.setModelSize(1000L);
            request.setMaxSeqLength(512);
            request.setTagIds(null);

            doNothing().when(modelDomainService).validateModelCreation(any(), any(), any(), any());
            doNothing().when(modelRepository).save(any(Model.class));

            ModelResponse response = service.createModel(request);

            assertNotNull(response);
            assertEquals("TestModel", response.getName());
            assertEquals("Test Description", response.getDescription());
            assertEquals("default", response.getResourceGroup());
            assertEquals("TestAuthor", response.getAuthor());
            assertEquals("TestSeries", response.getSeriesName());
            assertEquals(Long.valueOf(1000L), response.getModelSize());
            assertEquals(Integer.valueOf(512), response.getMaxSeqLength());
            assertNotNull(response.getVersions());
            assertEquals(1, response.getVersions().size());
            verify(modelRepository).save(any(Model.class));
        }

        @Test
        @DisplayName("should create model successfully with tags")
        void should_createModel_successfully_withTags() {
            UUID tagId1 = UUID.randomUUID();
            UUID tagId2 = UUID.randomUUID();
            ModelCreateRequest request = new ModelCreateRequest();
            request.setName("TestModel");
            request.setDescription("Test Description");
            request.setCategoryId(UUID.randomUUID());
            request.setTypeId(UUID.randomUUID());
            request.setResourceGroup("default");
            request.setAuthor("TestAuthor");
            request.setTagIds(List.of(tagId1, tagId2));

            doNothing().when(modelDomainService).validateModelCreation(any(), any(), any(), any());
            doNothing().when(modelRepository).save(any(Model.class));
            doNothing().when(tagRepository).addModelTag(any(), any());

            ModelResponse response = service.createModel(request);

            assertNotNull(response);
            assertEquals(2, response.getTags().size());
            verify(modelRepository).save(any(Model.class));
            verify(tagRepository, times(2)).addModelTag(any(), any());
        }

        @Test
        @DisplayName("should throw when validation fails")
        void should_throw_whenValidationFails() {
            ModelCreateRequest request = new ModelCreateRequest();
            request.setName("TestModel");
            request.setCategoryId(UUID.randomUUID());
            request.setTypeId(UUID.randomUUID());
            request.setResourceGroup("default");

            doThrow(new ModelLiteException(ErrorCode.MODEL_NAME_EXISTS, "名称已存在"))
                    .when(modelDomainService).validateModelCreation(any(), any(), any(), any());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createModel(request));

            assertEquals(ErrorCode.MODEL_NAME_EXISTS, exception.getCode());
            verify(modelRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getModel tests")
    class GetModelTests {

        @Test
        @DisplayName("should return model successfully")
        void should_returnModel_successfully() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            when(modelRepository.findTagIdsByModelId(modelId)).thenReturn(List.of());

            ModelResponse response = service.getModel(modelId, "default");

            assertNotNull(response);
            assertEquals("TestModel", response.getName());
            assertEquals("desc", response.getDescription());
            assertEquals(1, response.getVersions().size());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist")
        void should_throw_whenModelNotFound() {
            UUID modelId = UUID.randomUUID();

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getModel(modelId, "default"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when resource group not visible")
        void should_throw_whenResourceGroupNotVisible() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "private-group", null, "author", "series", "1000", 512L);

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getModel(modelId, "other-group"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should return model when resource group is public")
        void should_returnModel_whenResourceGroupIsPublic() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "public", null, "author", "series", "1000", 512L);

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            when(modelRepository.findTagIdsByModelId(modelId)).thenReturn(List.of());

            ModelResponse response = service.getModel(modelId, null);

            assertNotNull(response);
            assertEquals("TestModel", response.getName());
        }
    }

    @Nested
    @DisplayName("modifyModel tests")
    class ModifyModelTests {

        @Test
        @DisplayName("should modify model metadata successfully")
        void should_modifyModel_successfully() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            ModelModifyRequest request = new ModelModifyRequest();
            request.setDescription("New Description");
            request.setCategoryId(model.getCategoryId());
            request.setTypeId(model.getTypeId());
            request.setAuthor("NewAuthor");
            request.setSeriesName("NewSeries");
            request.setModelSize(2000L);
            request.setMaxSeqLength(1024);
            request.setTagIds(null);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelDomainService).validateModelModification(any(), any(), any());
            doNothing().when(modelRepository).update(any(Model.class));

            ModelResponse response = service.modifyModel(modelId, request, "default");

            assertNotNull(response);
            assertEquals("New Description", response.getDescription());
            assertEquals("NewAuthor", response.getAuthor());
            assertEquals("NewSeries", response.getSeriesName());
            assertEquals(Long.valueOf(2000L), response.getModelSize());
            assertEquals(Integer.valueOf(1024), response.getMaxSeqLength());
            verify(modelRepository).update(any(Model.class));
        }

        @Test
        @DisplayName("should update tags when tags changed")
        void should_updateTags_whenTagsChanged() {
            UUID modelId = UUID.randomUUID();
            UUID oldTagId = UUID.randomUUID();
            UUID newTagId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            ModelModifyRequest request = new ModelModifyRequest();
            request.setCategoryId(model.getCategoryId());
            request.setTypeId(model.getTypeId());
            request.setTagIds(List.of(newTagId));

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelDomainService).validateModelModification(any(), any(), any());
            when(modelRepository.findTagIdsByModelId(modelId)).thenReturn(List.of(oldTagId));
            doNothing().when(tagRepository).removeModelTag(any(), any());
            doNothing().when(tagRepository).addModelTag(any(), any());
            doNothing().when(modelRepository).update(any(Model.class));

            ModelResponse response = service.modifyModel(modelId, request, "default");

            assertNotNull(response);
            verify(tagRepository).removeModelTag(modelId, oldTagId);
            verify(tagRepository).addModelTag(modelId, newTagId);
            verify(modelRepository).update(any(Model.class));
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist")
        void should_throw_whenModelNotFound() {
            UUID modelId = UUID.randomUUID();
            ModelModifyRequest request = new ModelModifyRequest();
            request.setCategoryId(UUID.randomUUID());
            request.setTypeId(UUID.randomUUID());

            when(modelRepository.findById(modelId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.modifyModel(modelId, request, "default"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when resource group not visible on modify")
        void should_throw_whenResourceGroupNotVisibleOnModify() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "private-group", null, "author", "series", "1000", 512L);

            ModelModifyRequest request = new ModelModifyRequest();
            request.setCategoryId(model.getCategoryId());
            request.setTypeId(model.getTypeId());

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.modifyModel(modelId, request, "other-group"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("listModels tests")
    class ListModelsTests {

        @Test
        @DisplayName("should return models with resource groups filter")
        void should_returnModels_withResourceGroups() {
            ModelQueryCondition condition = new ModelQueryCondition();

            Model model1 = Model.createModel("Model1", "desc1", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author1", "series1", "1000", 512L);
            Model model2 = Model.createModel("Model2", "desc2", UUID.randomUUID(),
                    UUID.randomUUID(), "admin", null, "author2", "series2", "2000", 1024L);

            PageResult<Model> pageResult = new PageResult<>();
            pageResult.setItems(List.of(model1, model2));
            pageResult.setTotal(2);
            pageResult.setPage(1);
            pageResult.setPageSize(10);
            pageResult.setTotalPages(1);

            when(modelRepository.findByResourceGroups(any(List.class), any(ModelQueryCondition.class)))
                    .thenReturn(pageResult);

            PageResult<ModelListResponse> response = service.listModels(condition, "default");

            assertNotNull(response);
            assertEquals(2, response.getItems().size());
            assertEquals(2, response.getTotal());
            assertEquals("Model1", response.getItems().get(0).getName());
            assertEquals("Model2", response.getItems().get(1).getName());
            verify(modelRepository).findByResourceGroups(any(List.class), any(ModelQueryCondition.class));
        }

        @Test
        @DisplayName("should return models with public only when userResourceGroup is null")
        void should_returnModels_withPublicOnly() {
            ModelQueryCondition condition = new ModelQueryCondition();

            PageResult<Model> pageResult = new PageResult<>();
            pageResult.setItems(List.of());
            pageResult.setTotal(0);
            pageResult.setPage(1);
            pageResult.setPageSize(10);
            pageResult.setTotalPages(0);

            when(modelRepository.findByResourceGroups(any(List.class), any(ModelQueryCondition.class)))
                    .thenReturn(pageResult);

            PageResult<ModelListResponse> response = service.listModels(condition, null);

            assertNotNull(response);
            assertTrue(response.getItems().isEmpty());
            assertEquals(0, response.getTotal());
            verify(modelRepository).findByResourceGroups(any(List.class), any(ModelQueryCondition.class));
        }
    }

    @Nested
    @DisplayName("createVersion tests")
    class CreateVersionTests {

        @Test
        @DisplayName("should create version successfully with PVC storage")
        void should_createVersion_successfully_withPvc() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            VersionCreateRequest request = new VersionCreateRequest();
            request.setSourceType("PVC");
            request.setPvcName("test-pvc");
            request.setInternalPath("/models");
            request.setWeightType("FP32");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            VersionResponse response = service.createVersion(modelId, request, "default");

            assertNotNull(response);
            assertEquals(2, response.getVersionNumber());
            assertEquals("PVC", response.getSourceType());
            assertEquals("test-pvc", response.getPvcName());
            assertEquals("/models", response.getInternalPath());
            assertEquals("FP32", response.getWeightType());
            assertEquals(VersionStatus.AVAILABLE.getDbValue(), response.getStatus());
            verify(modelRepository).updateVersion(any(ModelVersion.class));
        }

        @Test
        @DisplayName("should create version successfully with NFS storage")
        void should_createVersion_successfully_withNfs() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            VersionCreateRequest request = new VersionCreateRequest();
            request.setSourceType("NFS");
            request.setNfsServer("192.168.1.1");
            request.setNfsPath("/nfs/models");
            request.setWeightType("INT8");

            TrainingMetadataDto trainingDto = new TrainingMetadataDto();
            trainingDto.setTrainFrame("PyTorch");
            trainingDto.setTrainType("Supervised");
            request.setTrainingMetadata(trainingDto);

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            VersionResponse response = service.createVersion(modelId, request, "default");

            assertNotNull(response);
            assertEquals(2, response.getVersionNumber());
            assertEquals("NFS", response.getSourceType());
            assertEquals("192.168.1.1", response.getNfsServer());
            assertEquals("/nfs/models", response.getNfsPath());
            assertEquals("INT8", response.getWeightType());
            assertNotNull(response.getTrainingMetadata());
            assertEquals("PyTorch", response.getTrainingMetadata().getTrainFrame());
            assertEquals(VersionStatus.AVAILABLE.getDbValue(), response.getStatus());
        }

        @Test
        @DisplayName("should create version with empty storage path")
        void should_createVersion_withEmptyStoragePath() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            VersionCreateRequest request = new VersionCreateRequest();
            request.setWeightType("FP16");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));
            doNothing().when(modelRepository).updateVersion(any(ModelVersion.class));

            VersionResponse response = service.createVersion(modelId, request, "default");

            assertNotNull(response);
            assertEquals(2, response.getVersionNumber());
            assertEquals(VersionStatus.NO_WEIGHT.getDbValue(), response.getStatus());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist")
        void should_throw_whenModelNotFound() {
            UUID modelId = UUID.randomUUID();
            VersionCreateRequest request = new VersionCreateRequest();

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createVersion(modelId, request, "default"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw VERSION_CAPACITY_EXCEEDED when max versions reached")
        void should_throw_whenCapacityExceeded() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            for (int i = 0; i < 49; i++) {
                model.createVersion(StoragePath.empty(), null, VersionStatus.NO_WEIGHT, false, null);
            }

            VersionCreateRequest request = new VersionCreateRequest();
            request.setWeightType("FP32");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createVersion(modelId, request, "default"));

            assertEquals(ErrorCode.VERSION_CAPACITY_EXCEEDED, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when resource group not visible on createVersion")
        void should_throw_whenResourceGroupNotVisibleOnCreateVersion() {
            UUID modelId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "private-group", null, "author", "series", "1000", 512L);

            VersionCreateRequest request = new VersionCreateRequest();
            request.setWeightType("FP32");

            when(modelRepository.findByIdWithVersions(modelId)).thenReturn(Optional.of(model));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createVersion(modelId, request, "other-group"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("getVersion tests")
    class GetVersionTests {

        @Test
        @DisplayName("should return version successfully")
        void should_returnVersion_successfully() {
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);
            ModelVersion version = new ModelVersion(versionId, 1, StoragePath.empty(),
                    "FP32", VersionStatus.AVAILABLE, true, false, null);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.of(version));

            VersionResponse response = service.getVersion(modelId, versionId, "default");

            assertNotNull(response);
            assertEquals(versionId, response.getId());
            assertEquals(1, response.getVersionNumber());
            assertEquals("FP32", response.getWeightType());
        }

        @Test
        @DisplayName("should throw VERSION_NOT_FOUND when version does not exist")
        void should_throw_whenVersionNotFound() {
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "default", null, "author", "series", "1000", 512L);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));
            when(modelRepository.findVersionById(modelId, versionId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getVersion(modelId, versionId, "default"));

            assertEquals(ErrorCode.VERSION_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when model does not exist for getVersion")
        void should_throw_whenModelNotFoundOnGetVersion() {
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            when(modelRepository.findById(modelId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getVersion(modelId, versionId, "default"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_NOT_FOUND when resource group not visible on getVersion")
        void should_throw_whenResourceGroupNotVisibleOnGetVersion() {
            UUID modelId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            Model model = Model.createModel("TestModel", "desc", UUID.randomUUID(),
                    UUID.randomUUID(), "private-group", null, "author", "series", "1000", 512L);

            when(modelRepository.findById(modelId)).thenReturn(Optional.of(model));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getVersion(modelId, versionId, "other-group"));

            assertEquals(ErrorCode.MODEL_NOT_FOUND, exception.getCode());
        }
    }
}
