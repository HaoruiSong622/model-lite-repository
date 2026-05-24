package com.huawei.modellite.repository.infrastructure.persistence;

import com.huawei.modellite.repository.modelweight.domain.aggregate.model.Model;
import com.huawei.modellite.repository.modelweight.domain.aggregate.model.ModelVersion;
import com.huawei.modellite.repository.modelweight.domain.repository.ModelRepository;
import com.huawei.modellite.repository.modelweight.domain.vo.ModelQueryCondition;
import com.huawei.modellite.repository.modelweight.domain.vo.PageResult;
import com.huawei.modellite.repository.infrastructure.persistence.mapper.ModelMapper;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisModelRepository implements ModelRepository {

    private final ModelMapper modelMapper;

    public MyBatisModelRepository(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public void save(Model model) {
        modelMapper.insertModel(model);
        for (ModelVersion version : model.getVersions()) {
            modelMapper.insertModelVersion(model.getModelId(), version);
        }
        for (UUID tagId : model.getTagIds()) {
            modelMapper.insertModelTag(model.getModelId(), UUID.randomUUID(), tagId);
        }
    }

    @Override
    public Optional<Model> findById(UUID modelId) {
        return Optional.ofNullable(modelMapper.selectById(modelId));
    }

    @Override
    public Optional<Model> findByIdWithVersions(UUID modelId) {
        Optional<Model> modelOpt = findById(modelId);
        modelOpt.ifPresent(model -> {
            List<ModelVersion> versions = modelMapper.selectVersionsByModelId(modelId);
            model.setVersions(versions);
        });
        return modelOpt;
    }

    @Override
    public Optional<ModelVersion> findVersionById(UUID modelId, UUID versionId) {
        return Optional.ofNullable(modelMapper.selectVersionById(modelId, versionId));
    }

    @Override
    public List<Model> findAll() {
        return modelMapper.selectAll();
    }

    @Override
    public PageResult<Model> findByCondition(ModelQueryCondition condition) {
        List<Model> items = modelMapper.selectByCondition(condition);
        long total = modelMapper.countByCondition(condition);
        return buildPageResult(items, total, condition);
    }

    @Override
    public PageResult<Model> findByResourceGroups(List<String> resourceGroups, ModelQueryCondition condition) {
        List<Model> items = modelMapper.selectByResourceGroups(resourceGroups, condition);
        long total = modelMapper.countByResourceGroups(resourceGroups, condition);
        return buildPageResult(items, total, condition);
    }

    private PageResult<Model> buildPageResult(List<Model> items, long total, ModelQueryCondition condition) {
        int page = condition.getPage() != null ? condition.getPage() : 1;
        int pageSize = condition.getPageSize() != null ? condition.getPageSize() : 10;
        PageResult<Model> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotalPages(pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
        return result;
    }

    @Override
    public boolean existsByCategoryAndTypeAndName(UUID categoryId, UUID typeId, String name) {
        return modelMapper.existsByCategoryAndTypeAndName(categoryId, typeId, name) > 0;
    }

    @Override
    public long countByResourceGroup(String resourceGroup) {
        return modelMapper.countByResourceGroup(resourceGroup);
    }

    @Override
    public long countAll() {
        return modelMapper.countAll();
    }

    @Override
    public void update(Model model) {
        modelMapper.updateModel(model);
    }

    @Override
    public void updateVersion(ModelVersion version) {
        modelMapper.updateModelVersion(version);
    }

    @Override
    public void saveVersion(UUID modelId, ModelVersion version) {
        modelMapper.insertModelVersion(modelId, version);
    }

    @Override
    public List<UUID> findTagIdsByModelId(UUID modelId) {
        return modelMapper.selectTagIdsByModelId(modelId);
    }
}
