package com.huawei.modellite.repository.modelweight.domain.repository;

import com.huawei.modellite.repository.common.enums.TagType;
import com.huawei.modellite.repository.modelweight.domain.aggregate.tag.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository {

    void save(Tag tag);

    Optional<Tag> findById(UUID tagId);

    List<Tag> findAll();

    List<Tag> findByTagType(TagType tagType);

    boolean existsByName(String name);

    boolean hasReference(UUID tagId);

    void deleteById(UUID tagId);

    int removeModelTagsByTagId(UUID tagId);

    int removeModelTypeTagsByTagId(UUID tagId);

    void addModelTag(UUID modelId, UUID tagId);

    void removeModelTag(UUID modelId, UUID tagId);

    List<Tag> findTagsByModelId(UUID modelId);

    List<UUID> findModelIdsByTagId(UUID tagId);

    void addModelTypeTag(UUID typeId, UUID tagId);

    void removeModelTypeTag(UUID typeId, UUID tagId);

    List<Tag> findTagsByModelTypeId(UUID typeId);
}
