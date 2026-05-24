package com.huawei.modellite.repository.modelweight.domain.repository;

import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelRepository {

    void save(Model model);

    Optional<Model> findById(UUID modelId);

    Optional<Model> findByIdWithVersions(UUID modelId);

    Optional<ModelVersion> findVersionById(UUID modelId, UUID versionId);

    List<Model> findAll();

    PageResult<Model> findByCondition(ModelQueryCondition condition);

    PageResult<Model> findByResourceGroups(List<String> resourceGroups, ModelQueryCondition condition);

    boolean existsByCategoryAndTypeAndName(UUID categoryId, UUID typeId, String name);

    long countByResourceGroup(String resourceGroup);

    long countAll();

    void update(Model model);

    void updateVersion(ModelVersion version);

    /**
     * Insert a new model version.
     *
     * @param modelId the model ID
     * @param version the version to insert
     */
    void saveVersion(UUID modelId, ModelVersion version);

    List<UUID> findTagIdsByModelId(UUID modelId);
}
