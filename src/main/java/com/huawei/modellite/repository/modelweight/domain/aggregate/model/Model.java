package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.VersionStatus;
import com.huawei.modellite.repository.common.exception.ModelLiteException;

public class Model {

    private static final int MAX_VERSIONS = 50;

    private UUID modelId;
    private String name;
    private String description;
    private UUID categoryId;
    private UUID typeId;
    private String resourceGroup;
    private String createUser;
    private String author;
    private String seriesName;
    private String modelSize;
    private Long maxSeqLength;
    private List<ModelVersion> versions;
    private List<UUID> tagIds;

    public Model() {
        this.versions = new ArrayList<>();
        this.tagIds = new ArrayList<>();
    }

    // Package-private constructor for reconstitution from persistence (used by tests and repository)
    Model(UUID modelId, String name, String description, UUID categoryId,
          UUID typeId, String resourceGroup, String createUser,
          String author, String seriesName, String modelSize,
          Long maxSeqLength, List<ModelVersion> versions, List<UUID> tagIds) {
        this.modelId = modelId;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.typeId = typeId;
        this.resourceGroup = resourceGroup;
        this.createUser = createUser;
        this.author = author;
        this.seriesName = seriesName;
        this.modelSize = modelSize;
        this.maxSeqLength = maxSeqLength;
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
        this.tagIds = tagIds != null ? new ArrayList<>(tagIds) : new ArrayList<>();
    }

    public static Model createModel(String name, String description, UUID categoryId,
                                    UUID typeId, String resourceGroup, String createUser,
                                    String author, String seriesName, String modelSize,
                                    Long maxSeqLength) {
        validateName(name);
        validateResourceGroup(resourceGroup);

        List<ModelVersion> versions = new ArrayList<>();
        versions.add(ModelVersion.createInitialVersion(null));

        return new Model(
                UUID.randomUUID(),
                name,
                description,
                categoryId,
                typeId,
                resourceGroup,
                createUser,
                author,
                seriesName,
                modelSize,
                maxSeqLength,
                versions,
                new ArrayList<>()
        );
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new ModelLiteException(ErrorCode.MODEL_NAME_EXISTS,
                    "模型名称不能为空且长度不能超过100");
        }
    }

    private static void validateResourceGroup(String resourceGroup) {
        if (resourceGroup == null || resourceGroup.isBlank()) {
            throw new ModelLiteException(ErrorCode.MODEL_RESOURCE_GROUP_IMMUTABLE,
                    "资源组不能为空");
        }
    }

    public void modifyMetadata(String description, String author, String seriesName,
                               String modelSize, Long maxSeqLength) {
        this.description = description;
        this.author = author;
        this.seriesName = seriesName;
        this.modelSize = modelSize;
        this.maxSeqLength = maxSeqLength;
    }

    public ModelVersion createVersion(StoragePath storagePath, String weightType,
                                      VersionStatus status, boolean isRegistered,
                                      TrainingMetadata trainingMetadata) {
        if (versions.size() >= MAX_VERSIONS) {
            throw new ModelLiteException(ErrorCode.VERSION_CAPACITY_EXCEEDED,
                    "版本数量不能超过" + MAX_VERSIONS);
        }

        int nextVersionNumber = getLatestVersionNumber() + 1;
        ModelVersion version = new ModelVersion(
                UUID.randomUUID(),
                nextVersionNumber,
                storagePath,
                weightType,
                status,
                isRegistered,
                false,
                trainingMetadata
        );
        versions.add(version);
        return version;
    }

    public int getLatestVersionNumber() {
        return versions.get(versions.size() - 1).getVersionNumber();
    }

    public ModelVersion getModelVersion(int versionNumber) {
        return versions.stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst()
                .orElseThrow(() -> new ModelLiteException(ErrorCode.VERSION_NOT_FOUND,
                        "版本不存在: " + versionNumber));
    }

    public UUID getModelId() {
        return modelId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getCreateUser() {
        return createUser;
    }

    public String getAuthor() {
        return author;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public String getModelSize() {
        return modelSize;
    }

    public Long getMaxSeqLength() {
        return maxSeqLength;
    }

    public List<ModelVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public List<UUID> getTagIds() {
        return Collections.unmodifiableList(tagIds);
    }

    public void setVersions(List<ModelVersion> versions) {
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }

    public void setTagIds(List<UUID> tagIds) {
        this.tagIds = tagIds != null ? new ArrayList<>(tagIds) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model model = (Model) o;
        return Objects.equals(modelId, model.modelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId);
    }
}
