package com.huawei.modellite.repository.infrastructure.persistence.mapper;

import com.huawei.modellite.repository.common.enums.TagType;
import com.huawei.modellite.repository.modelweight.domain.aggregate.tag.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface TagMapper {

    void insert(Tag tag);

    Tag selectById(@Param("tagId") UUID tagId);

    List<Tag> selectAll();

    List<Tag> selectByTagType(@Param("tagType") TagType tagType);

    int countByName(@Param("name") String name);

    int countModelTagReferences(@Param("tagId") UUID tagId);

    int countModelTypeTagReferences(@Param("tagId") UUID tagId);

    void deleteById(@Param("tagId") UUID tagId);

    int deleteModelTagsByTagId(@Param("tagId") UUID tagId);

    int deleteModelTypeTagsByTagId(@Param("tagId") UUID tagId);

    void insertModelTag(@Param("modelId") UUID modelId, @Param("tagId") UUID tagId, @Param("id") UUID id);

    void deleteModelTag(@Param("modelId") UUID modelId, @Param("tagId") UUID tagId);

    List<Tag> selectTagsByModelId(@Param("modelId") UUID modelId);

    List<UUID> selectModelIdsByTagId(@Param("tagId") UUID tagId);

    void insertModelTypeTag(@Param("typeId") UUID typeId, @Param("tagId") UUID tagId, @Param("id") UUID id);

    void deleteModelTypeTag(@Param("typeId") UUID typeId, @Param("tagId") UUID tagId);

    List<Tag> selectTagsByModelTypeId(@Param("typeId") UUID typeId);
}
