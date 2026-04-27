package com.huawei.modellite.repository.modelweight.application.service;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import com.huawei.modellite.repository.modelweight.application.dto.TagRequest;
import com.huawei.modellite.repository.modelweight.application.dto.TagResponse;
import com.huawei.modellite.repository.modelweight.domain.aggregate.tag.Tag;
import com.huawei.modellite.repository.modelweight.domain.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagApplicationServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagApplicationService service;

    @BeforeEach
    void setUp() {
        service = new TagApplicationService(tagRepository);
    }

    private Tag createBuiltInTag(UUID tagId, String name) {
        try {
            java.lang.reflect.Constructor<Tag> constructor = Tag.class.getDeclaredConstructor(
                    UUID.class, String.class, com.huawei.modellite.repository.common.enums.TagType.class, Boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(tagId, name, com.huawei.modellite.repository.common.enums.TagType.CAPABILITY, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("createTag tests")
    class CreateTagTests {

        @Test
        @DisplayName("should create tag successfully")
        void should_createTag_successfully() {
            TagRequest request = new TagRequest();
            request.setName("AI");

            when(tagRepository.existsByName("AI")).thenReturn(false);

            TagResponse response = service.createTag(request);

            assertNotNull(response);
            assertEquals("AI", response.getName());
            assertEquals("USER", response.getTagType());
            assertFalse(response.getIsBuiltin());
            verify(tagRepository).save(any(Tag.class));
        }

        @Test
        @DisplayName("should throw TAG_NAME_EXISTS when name already exists")
        void should_throw_whenNameExists() {
            TagRequest request = new TagRequest();
            request.setName("AI");

            when(tagRepository.existsByName("AI")).thenReturn(true);

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.createTag(request));

            assertEquals(ErrorCode.TAG_NAME_EXISTS, exception.getCode());
            verify(tagRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteTag tests")
    class DeleteTagTests {

        @Test
        @DisplayName("should delete tag successfully")
        void should_deleteTag_successfully() {
            UUID tagId = UUID.randomUUID();
            Tag tag = Tag.createUserTag("AI");

            when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

            service.deleteTag(tagId);

            verify(tagRepository).removeModelTagsByTagId(tagId);
            verify(tagRepository).removeModelTypeTagsByTagId(tagId);
            verify(tagRepository).deleteById(tagId);
        }

        @Test
        @DisplayName("should throw TAG_NOT_FOUND when tag does not exist")
        void should_throw_whenTagNotFound() {
            UUID tagId = UUID.randomUUID();

            when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.deleteTag(tagId));

            assertEquals(ErrorCode.TAG_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw TAG_BUILTIN when deleting built-in tag")
        void should_throw_whenBuiltInTag() {
            UUID tagId = UUID.randomUUID();
            Tag tag = createBuiltInTag(tagId, "System");

            when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.deleteTag(tagId));

            assertEquals(ErrorCode.TAG_BUILTIN, exception.getCode());
            verify(tagRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("addTagToModel tests")
    class AddTagToModelTests {

        @Test
        @DisplayName("should add model tags successfully")
        void should_addModelTags_successfully() {
            UUID modelId = UUID.randomUUID();
            UUID tagId = UUID.randomUUID();
            Tag tag = Tag.createUserTag("AI");

            when(tagRepository.findTagsByModelId(modelId)).thenReturn(List.of());
            when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

            service.addTagToModel(modelId, List.of(tagId));

            verify(tagRepository).addModelTag(modelId, tagId);
        }

        @Test
        @DisplayName("should throw TAG_NOT_FOUND when tag does not exist")
        void should_throw_whenTagNotFound() {
            UUID modelId = UUID.randomUUID();
            UUID tagId = UUID.randomUUID();

            when(tagRepository.findTagsByModelId(modelId)).thenReturn(List.of());
            when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.addTagToModel(modelId, List.of(tagId)));

            assertEquals(ErrorCode.TAG_NOT_FOUND, exception.getCode());
        }

        @Test
        @DisplayName("should throw MODEL_TAG_LIMIT_EXCEEDED when tag count exceeds 20")
        void should_throw_whenTagLimitExceeded() {
            UUID modelId = UUID.randomUUID();
            UUID tagId = UUID.randomUUID();

            List<Tag> existingTags = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                existingTags.add(Tag.createUserTag("Tag" + i));
            }
            when(tagRepository.findTagsByModelId(modelId)).thenReturn(existingTags);

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.addTagToModel(modelId, List.of(tagId)));

            assertEquals(ErrorCode.MODEL_TAG_LIMIT_EXCEEDED, exception.getCode());
        }
    }

    @Nested
    @DisplayName("removeTagFromModel tests")
    class RemoveTagFromModelTests {

        @Test
        @DisplayName("should remove model tag successfully")
        void should_removeModelTag_successfully() {
            UUID modelId = UUID.randomUUID();
            UUID tagId = UUID.randomUUID();
            Tag tag = Tag.createUserTag("AI");

            when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

            service.removeTagFromModel(modelId, tagId);

            verify(tagRepository).removeModelTag(modelId, tagId);
        }

        @Test
        @DisplayName("should throw TAG_NOT_FOUND when tag does not exist")
        void should_throw_whenTagNotFound() {
            UUID modelId = UUID.randomUUID();
            UUID tagId = UUID.randomUUID();

            when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.removeTagFromModel(modelId, tagId));

            assertEquals(ErrorCode.TAG_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("getTag tests")
    class GetTagTests {

        @Test
        @DisplayName("should return tag successfully")
        void should_returnTag_successfully() {
            UUID tagId = UUID.randomUUID();
            Tag tag = Tag.createUserTag("AI");

            when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

            TagResponse response = service.getTag(tagId);

            assertNotNull(response);
            assertEquals("AI", response.getName());
            assertEquals("USER", response.getTagType());
        }

        @Test
        @DisplayName("should throw TAG_NOT_FOUND when tag does not exist")
        void should_throw_whenTagNotFound() {
            UUID tagId = UUID.randomUUID();

            when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> service.getTag(tagId));

            assertEquals(ErrorCode.TAG_NOT_FOUND, exception.getCode());
        }
    }

    @Nested
    @DisplayName("getAllTags tests")
    class GetAllTagsTests {

        @Test
        @DisplayName("should return all tags")
        void should_returnAllTags() {
            Tag tag1 = Tag.createUserTag("AI");
            Tag tag2 = Tag.createCapabilityTag("NLP");

            when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));

            List<TagResponse> responses = service.getAllTags();

            assertEquals(2, responses.size());
        }

        @Test
        @DisplayName("should return empty list when no tags")
        void should_returnEmptyList() {
            when(tagRepository.findAll()).thenReturn(List.of());

            List<TagResponse> responses = service.getAllTags();

            assertTrue(responses.isEmpty());
        }
    }
}
