package com.huawei.modellite.repository.infrastructure.persistence;

import com.huawei.modellite.repository.common.enums.TagType;
import com.huawei.modellite.repository.modelweight.domain.aggregate.tag.Tag;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;
import com.huawei.modellite.repository.infrastructure.persistence.mapper.TagMapper;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MyBatisTagRepository implements TagRepository {

    private final TagMapper tagMapper;

    public MyBatisTagRepository(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public void save(Tag tag) {
        tagMapper.insert(tag);
    }

    @Override
    public Optional<Tag> findById(UUID tagId) {
        return Optional.ofNullable(tagMapper.selectById(tagId));
    }

    @Override
    public List<Tag> findAll() {
        return tagMapper.selectAll();
    }

    @Override
    public List<Tag> findByTagType(TagType tagType) {
        return tagMapper.selectByTagType(tagType);
    }

    @Override
    public boolean existsByName(String name) {
        return tagMapper.countByName(name) > 0;
    }

    @Override
    public boolean hasReference(UUID tagId) {
        return tagMapper.countModelTagReferences(tagId) > 0 || tagMapper.countModelTypeTagReferences(tagId) > 0;
    }

    @Override
    public void deleteById(UUID tagId) {
        tagMapper.deleteModelTagsByTagId(tagId);
        tagMapper.deleteModelTypeTagsByTagId(tagId);
        tagMapper.deleteById(tagId);
    }

    @Override
    public int removeModelTagsByTagId(UUID tagId) {
        return tagMapper.deleteModelTagsByTagId(tagId);
    }

    @Override
    public int removeModelTypeTagsByTagId(UUID tagId) {
        return tagMapper.deleteModelTypeTagsByTagId(tagId);
    }

    @Override
    public void addModelTag(UUID modelId, UUID tagId) {
        tagMapper.insertModelTag(modelId, tagId, UUID.randomUUID());
    }

    @Override
    public void removeModelTag(UUID modelId, UUID tagId) {
        tagMapper.deleteModelTag(modelId, tagId);
    }

    @Override
    public List<Tag> findTagsByModelId(UUID modelId) {
        return tagMapper.selectTagsByModelId(modelId);
    }

    @Override
    public List<UUID> findModelIdsByTagId(UUID tagId) {
        return tagMapper.selectModelIdsByTagId(tagId);
    }

    @Override
    public void addModelTypeTag(UUID typeId, UUID tagId) {
        tagMapper.insertModelTypeTag(typeId, tagId, UUID.randomUUID());
    }

    @Override
    public void removeModelTypeTag(UUID typeId, UUID tagId) {
        tagMapper.deleteModelTypeTag(typeId, tagId);
    }

    @Override
    public List<Tag> findTagsByModelTypeId(UUID typeId) {
        return tagMapper.selectTagsByModelTypeId(typeId);
    }
}
