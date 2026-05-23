package com.huawei.modellite.repository.modelweight.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.ModelCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelListResponse;
import com.huawei.modellite.repository.modelweight.application.dto.ModelModifyRequest;
import com.huawei.modellite.repository.modelweight.application.dto.ModelResponse;
import com.huawei.modellite.repository.modelweight.application.dto.TagResponse;
import com.huawei.modellite.repository.modelweight.application.dto.TrainingMetadataDto;
import com.huawei.modellite.repository.modelweight.application.dto.VersionCreateRequest;
import com.huawei.modellite.repository.modelweight.application.dto.VersionRegisterRequest;
import com.huawei.modellite.repository.modelweight.application.dto.VersionResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.StoragePath;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.TrainingMetadata;
import com.huawei.modellite.repository.modelweight.domain.aggregate.category.Category;
import com.huawei.modellite.repository.modelweight.domain.repository.CategoryRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;
import com.huawei.modellite.repository.modelweight.domain.service.ModelDomainService;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ModelApplicationService {

    private final ModelRepository modelRepository;
    private final ModelDomainService modelDomainService;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public ModelApplicationService(ModelRepository modelRepository,
                                   ModelDomainService modelDomainService,
                                   CategoryRepository categoryRepository,
                                   TagRepository tagRepository) {
        this.modelRepository = modelRepository;
        this.modelDomainService = modelDomainService;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    public ModelResponse createModel(ModelCreateRequest request) {
        modelDomainService.validateModelCreation(
                request.getName(),
                request.getCategoryId(),
                request.getTypeId(),
                request.getResourceGroup()
        );

        Model model = Model.createModel(
                request.getName(),
                request.getDescription(),
                request.getCategoryId(),
                request.getTypeId(),
                request.getResourceGroup(),
                null,
                request.getAuthor(),
                request.getSeriesName(),
                request.getModelSize() != null ? request.getModelSize().toString() : null,
                request.getMaxSeqLength() != null ? request.getMaxSeqLength().longValue() : null
        );

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            model.setTagIds(request.getTagIds());
        }

        modelRepository.save(model);

        if (request.getTagIds() != null) {
            for (UUID tagId : request.getTagIds()) {
                tagRepository.addModelTag(model.getModelId(), tagId);
            }
        }

        return toModelResponse(model);
    }

    public ModelResponse getModel(UUID modelId, String userResourceGroup) {
        Model model = modelRepository.findByIdWithVersions(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        checkResourceGroupVisibility(model, userResourceGroup);

        List<UUID> tagIds = modelRepository.findTagIdsByModelId(modelId);
        model.setTagIds(tagIds);

        return toModelResponse(model);
    }

    public ModelResponse modifyModel(UUID modelId, ModelModifyRequest request, String userResourceGroup) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        checkResourceGroupVisibility(model, userResourceGroup);

        modelDomainService.validateModelModification(modelId,
                request.getCategoryId(), request.getTypeId());

        model.modifyMetadata(
                request.getDescription(),
                request.getAuthor(),
                request.getSeriesName(),
                request.getModelSize() != null ? request.getModelSize().toString() : null,
                request.getMaxSeqLength() != null ? request.getMaxSeqLength().longValue() : null
        );

        if (request.getTagIds() != null) {
            List<UUID> currentTagIds = modelRepository.findTagIdsByModelId(modelId);

            for (UUID tagId : currentTagIds) {
                if (!request.getTagIds().contains(tagId)) {
                    tagRepository.removeModelTag(modelId, tagId);
                }
            }

            for (UUID tagId : request.getTagIds()) {
                if (!currentTagIds.contains(tagId)) {
                    tagRepository.addModelTag(modelId, tagId);
                }
            }

            model.setTagIds(request.getTagIds());
        }

        modelRepository.update(model);

        return toModelResponse(model);
    }

    public PageResult<ModelListResponse> listModels(ModelQueryCondition condition, String userResourceGroup) {
        condition.setResourceGroups(getVisibleResourceGroups(userResourceGroup));
        PageResult<Model> modelPage = modelRepository.findByResourceGroups(condition.getResourceGroups(), condition);

        PageResult<ModelListResponse> responsePage = new PageResult<>();
        responsePage.setItems(modelPage.getItems().stream()
                .map(this::toModelListResponse)
                .toList());
        responsePage.setTotal(modelPage.getTotal());
        responsePage.setPage(modelPage.getPage());
        responsePage.setPageSize(modelPage.getPageSize());
        responsePage.setTotalPages(modelPage.getTotalPages());

        return responsePage;
    }

    public VersionResponse createVersion(UUID modelId, VersionCreateRequest request, String userResourceGroup) {
        Model model = modelRepository.findByIdWithVersions(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        checkResourceGroupVisibility(model, userResourceGroup);

        boolean registered = request.getRegistered() != null && request.getRegistered();

        StoragePath storagePath = StoragePath.empty();
        VersionStatus status = VersionStatus.NO_WEIGHT;

        if (registered) {
            storagePath = buildStoragePath(request);
            if (storagePath.getSourceType() == null) {
                throw new ModelLiteException(ErrorCode.REGISTER_SOURCE_TYPE_REQUIRED,
                        "纳管模式下必须提供存储路径（sourceType）");
            }
            status = VersionStatus.AVAILABLE;
        }

        TrainingMetadata trainingMetadata = buildTrainingMetadata(request.getTrainingMetadata());

        ModelVersion version = model.createVersion(
                storagePath,
                request.getWeightType(),
                status,
                registered,
                trainingMetadata
        );

        modelRepository.updateVersion(version);

        return toVersionResponse(version, modelId);
    }

    public VersionResponse getVersion(UUID modelId, UUID versionId, String userResourceGroup) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        checkResourceGroupVisibility(model, userResourceGroup);

        ModelVersion version = modelRepository.findVersionById(modelId, versionId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + versionId));

        return toVersionResponse(version, modelId);
    }

    public VersionResponse registerVersion(UUID modelId, UUID versionId,
                                            VersionRegisterRequest request, String userResourceGroup) {
        Model model = modelRepository.findByIdWithVersions(modelId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                        "模型不存在: " + modelId));

        checkResourceGroupVisibility(model, userResourceGroup);

        ModelVersion version = modelRepository.findVersionById(modelId, versionId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + versionId));

        StoragePath storagePath = buildStoragePathFromRegister(request);
        TrainingMetadata trainingMetadata = buildTrainingMetadata(request.getTrainingMetadata());

        version.register(storagePath, request.getWeightType(), trainingMetadata);

        modelRepository.updateVersion(version);

        return toVersionResponse(version, modelId);
    }

    private StoragePath buildStoragePath(VersionCreateRequest request) {
        if ("PVC".equalsIgnoreCase(request.getSourceType())) {
            return StoragePath.ofPvc(request.getPvcName(), request.getInternalPath());
        } else if ("NFS".equalsIgnoreCase(request.getSourceType())) {
            return StoragePath.ofNfs(request.getNfsServer(), request.getNfsPath());
        } else {
            return StoragePath.empty();
        }
    }

    private StoragePath buildStoragePathFromRegister(VersionRegisterRequest request) {
        if ("PVC".equalsIgnoreCase(request.getSourceType())) {
            return StoragePath.ofPvc(request.getPvcName(), request.getInternalPath());
        } else if ("NFS".equalsIgnoreCase(request.getSourceType())) {
            return StoragePath.ofNfs(request.getNfsServer(), request.getNfsPath());
        } else {
            throw new ModelLiteException(ErrorCode.REGISTER_SOURCE_TYPE_REQUIRED,
                    "纳管必须指定有效的存储来源类型（PVC或NFS）");
        }
    }

    private TrainingMetadata buildTrainingMetadata(TrainingMetadataDto dto) {
        if (dto == null) {
            return null;
        }
        return new TrainingMetadata(
                dto.getTrainFrame(),
                dto.getTrainType(),
                dto.getTrainStrategy(),
                dto.getTrainTime(),
                dto.getFinalLoss(),
                dto.getSourceVersion()
        );
    }

    private List<String> getVisibleResourceGroups(String userResourceGroup) {
        if (userResourceGroup == null) {
            return List.of("public");
        }
        return List.of(userResourceGroup, "public");
    }

    private void checkResourceGroupVisibility(Model model, String userResourceGroup) {
        List<String> visibleGroups = getVisibleResourceGroups(userResourceGroup);
        if (model.getResourceGroup() == null
                || !visibleGroups.contains(model.getResourceGroup())) {
            throw new ModelLiteException(ErrorCode.MODEL_NOT_FOUND,
                    "模型不存在: " + model.getModelId());
        }
    }

    private void fillCategoryAndTypeName(Model model, String[] holder) {
        if (model.getCategoryId() != null) {
            categoryRepository.findByIdWithTypes(model.getCategoryId()).ifPresent(category -> {
                holder[0] = category.getName();
                if (model.getTypeId() != null && category.getModelTypes() != null) {
                    category.getModelTypes().stream()
                            .filter(mt -> mt.getTypeId().equals(model.getTypeId()))
                            .findFirst()
                            .ifPresent(mt -> holder[1] = mt.getName());
                }
            });
        }
    }

    private ModelResponse toModelResponse(Model model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getModelId());
        response.setName(model.getName());
        response.setDescription(model.getDescription());
        response.setCategoryId(model.getCategoryId());
        response.setTypeId(model.getTypeId());
        response.setResourceGroup(model.getResourceGroup());
        response.setCreateUser(model.getCreateUser());
        response.setAuthor(model.getAuthor());
        response.setSeriesName(model.getSeriesName());
        response.setModelSize(model.getModelSize() != null ? Long.valueOf(model.getModelSize()) : null);
        response.setMaxSeqLength(model.getMaxSeqLength() != null ? model.getMaxSeqLength().intValue() : null);
        response.setVersions(model.getVersions().stream()
                .map(v -> toVersionResponse(v, model.getModelId()))
                .toList());
        response.setTags(toTagResponses(model.getTagIds()));

        String[] categoryInfo = new String[2];
        fillCategoryAndTypeName(model, categoryInfo);
        response.setCategoryName(categoryInfo[0]);
        response.setTypeName(categoryInfo[1]);

        return response;
    }

    private ModelListResponse toModelListResponse(Model model) {
        ModelListResponse response = new ModelListResponse();
        response.setId(model.getModelId());
        response.setName(model.getName());
        response.setDescription(model.getDescription());
        response.setCategoryId(model.getCategoryId());
        response.setTypeId(model.getTypeId());
        response.setResourceGroup(model.getResourceGroup());
        response.setAuthor(model.getAuthor());
        response.setSeriesName(model.getSeriesName());
        response.setModelSize(model.getModelSize() != null ? Long.valueOf(model.getModelSize()) : null);
        response.setMaxSeqLength(model.getMaxSeqLength() != null ? model.getMaxSeqLength().intValue() : null);
        response.setVersionCount(model.getVersions().size());
        if (!model.getVersions().isEmpty()) {
            response.setLatestVersion(toVersionResponse(
                    model.getVersions().get(model.getVersions().size() - 1), model.getModelId()));
        }
        response.setTags(toTagResponses(model.getTagIds()));

        String[] categoryInfo = new String[2];
        fillCategoryAndTypeName(model, categoryInfo);
        response.setCategoryName(categoryInfo[0]);
        response.setTypeName(categoryInfo[1]);

        return response;
    }

    private VersionResponse toVersionResponse(ModelVersion version, UUID modelId) {
        VersionResponse response = new VersionResponse();
        response.setId(version.getVersionId());
        response.setModelId(modelId);
        response.setVersionNumber(version.getVersionNumber());
        response.setStatus(version.getStatus() != null ? version.getStatus().getDbValue() : null);
        response.setRegistered(version.isRegistered());
        response.setLocked(version.isLocked());
        if (version.getStoragePath() != null) {
            StoragePath sp = version.getStoragePath();
            response.setSourceType(sp.getSourceType() != null ? sp.getSourceType().name() : null);
            response.setPvcName(sp.getPvcName());
            response.setInternalPath(sp.getInternalPath());
            response.setNfsServer(sp.getNfsServer());
            response.setNfsPath(sp.getNfsPath());
        }
        response.setWeightType(version.getWeightType());
        if (version.getTrainingMetadata() != null) {
            response.setTrainingMetadata(toTrainingMetadataDto(version.getTrainingMetadata()));
        }
        return response;
    }

    private TrainingMetadataDto toTrainingMetadataDto(TrainingMetadata metadata) {
        TrainingMetadataDto dto = new TrainingMetadataDto();
        dto.setTrainFrame(metadata.getTrainFrame());
        dto.setTrainType(metadata.getTrainType());
        dto.setTrainStrategy(metadata.getTrainStrategy());
        dto.setTrainTime(metadata.getTrainTime());
        dto.setFinalLoss(metadata.getFinalLoss());
        dto.setSourceVersion(metadata.getSourceVersion());
        return dto;
    }

    private List<TagResponse> toTagResponses(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return tagIds.stream()
                .map(tagId -> {
                    TagResponse tagResponse = new TagResponse();
                    tagResponse.setId(tagId);
                    tagRepository.findById(tagId).ifPresent(tag -> {
                        tagResponse.setName(tag.getName());
                        tagResponse.setTagType(tag.getTagType() != null ? tag.getTagType().getDbValue() : null);
                        tagResponse.setIsBuiltin(tag.getIsBuiltIn());
                    });
                    return tagResponse;
                })
                .toList();
    }
}
