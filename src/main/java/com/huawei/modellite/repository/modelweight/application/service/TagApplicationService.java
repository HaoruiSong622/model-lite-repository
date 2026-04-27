package com.huawei.modellite.repository.modelweight.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.TagType;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.TagRequest;
import com.huawei.modellite.repository.modelweight.application.dto.TagResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.tag.Tag;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TagApplicationService {

    private final TagRepository tagRepository;

    public TagApplicationService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByName(request.getName())) {
            throw new ModelLiteException(ErrorCode.TAG_NAME_EXISTS,
                    "标签名称已存在: " + request.getName());
        }

        Tag tag = Tag.createUserTag(request.getName());
        tagRepository.save(tag);

        return toResponse(tag);
    }

    public void deleteTag(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.TAG_NOT_FOUND,
                        "标签不存在"));

        if (Boolean.TRUE.equals(tag.getIsBuiltIn())) {
            throw new ModelLiteException(ErrorCode.TAG_BUILTIN,
                    "内置标签不允许删除");
        }

        tagRepository.removeModelTagsByTagId(tagId);
        tagRepository.removeModelTypeTagsByTagId(tagId);
        tagRepository.deleteById(tagId);
    }

    public void addTagToModel(UUID modelId, List<UUID> tagIds) {
        List<Tag> existingTags = tagRepository.findTagsByModelId(modelId);
        if (existingTags.size() + tagIds.size() > 20) {
            throw new ModelLiteException(ErrorCode.MODEL_TAG_LIMIT_EXCEEDED,
                    "模型标签数量超过上限(≤20)");
        }

        for (UUID tagId : tagIds) {
            if (tagRepository.findById(tagId).isEmpty()) {
                throw new ModelLiteException(ErrorCode.TAG_NOT_FOUND,
                        "标签不存在: " + tagId);
            }
        }

        for (UUID tagId : tagIds) {
            tagRepository.addModelTag(modelId, tagId);
        }
    }

    public void removeTagFromModel(UUID modelId, UUID tagId) {
        if (tagRepository.findById(tagId).isEmpty()) {
            throw new ModelLiteException(ErrorCode.TAG_NOT_FOUND,
                    "标签不存在");
        }

        tagRepository.removeModelTag(modelId, tagId);
    }

    public TagResponse getTag(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ModelLiteException(ErrorCode.TAG_NOT_FOUND,
                        "标签不存在"));

        return toResponse(tag);
    }

    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private TagResponse toResponse(Tag tag) {
        TagResponse response = new TagResponse();
        response.setId(tag.getTagId());
        response.setName(tag.getName());
        response.setTagType(tag.getTagType().name());
        response.setIsBuiltin(tag.getIsBuiltIn());
        response.setCreateTime(tag.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
        response.setUpdateTime(tag.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
        return response;
    }
}
