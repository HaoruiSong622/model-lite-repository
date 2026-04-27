package com.huawei.modellite.repository.modelweight.domain.aggregate.tag;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TagType;
import com.huawei.modellite.repository.common.exception.ModelLiteException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Tag {

    private final UUID tagId;
    private final String name;
    private final TagType tagType;
    private final Boolean isBuiltIn;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    private Tag(UUID tagId, String name, TagType tagType, Boolean isBuiltIn) {
        validateName(name);
        this.tagId = tagId;
        this.name = name;
        this.tagType = tagType;
        this.isBuiltIn = isBuiltIn;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ModelLiteException(ErrorCode.TAG_NAME_EXISTS,
                    "标签名称不能为空");
        }
        if (name.length() > 50) {
            throw new ModelLiteException(ErrorCode.TAG_NAME_EXISTS,
                    "标签名称长度不能超过50个字符");
        }
    }

    public static Tag createUserTag(String name) {
        return new Tag(UUID.randomUUID(), name, TagType.USER, false);
    }

    public static Tag createCapabilityTag(String name) {
        return new Tag(UUID.randomUUID(), name, TagType.CAPABILITY, false);
    }

    public UUID getTagId() {
        return tagId;
    }

    public String getName() {
        return name;
    }

    public TagType getTagType() {
        return tagType;
    }

    public Boolean getIsBuiltIn() {
        return isBuiltIn;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
