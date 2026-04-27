package com.huawei.modellite.repository.infrastructure.persistence.mapper;

import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ModelMapper {

    void insertModel(Model model);

    void insertModelVersion(@Param("modelId") UUID modelId, @Param("version") ModelVersion version);

    void insertModelTag(@Param("modelId") UUID modelId, @Param("id") UUID id, @Param("tagId") UUID tagId);

    Model selectById(UUID modelId);

    Model selectByIdWithVersions(UUID modelId);

    ModelVersion selectVersionById(@Param("modelId") UUID modelId, @Param("versionId") UUID versionId);

    List<Model> selectAll();

    List<Model> selectByCondition(@Param("condition") ModelQueryCondition condition);

    long countByCondition(@Param("condition") ModelQueryCondition condition);

    List<Model> selectByResourceGroups(@Param("resourceGroups") List<String> resourceGroups, @Param("condition") ModelQueryCondition condition);

    long countByResourceGroups(@Param("resourceGroups") List<String> resourceGroups, @Param("condition") ModelQueryCondition condition);

    int existsByCategoryAndTypeAndName(@Param("categoryId") UUID categoryId, @Param("typeId") UUID typeId, @Param("name") String name);

    long countByResourceGroup(@Param("resourceGroup") String resourceGroup);

    long countAll();

    void updateModel(Model model);

    void updateModelVersion(@Param("version") ModelVersion version);

    List<UUID> selectTagIdsByModelId(UUID modelId);

    List<ModelVersion> selectVersionsByModelId(UUID modelId);
}
