package com.huawei.modellite.repository.modelweight.domain.aggregate.tag;

import com.huawei.modellite.repository.common.enums.TagType;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TagTest {

    @Nested
    @DisplayName("createUserTag")
    class CreateUserTag {

        @Test
        @DisplayName("should create USER tag with correct properties")
        void should_createUserTag() {
            LocalDateTime before = LocalDateTime.now();
            Tag tag = Tag.createUserTag("favorite");
            LocalDateTime after = LocalDateTime.now();

            assertNotNull(tag.getTagId());
            assertEquals("favorite", tag.getName());
            assertEquals(TagType.USER, tag.getTagType());
            assertFalse(tag.getIsBuiltIn());
            assertNotNull(tag.getCreateTime());
            assertNotNull(tag.getUpdateTime());
            assertFalse(tag.getCreateTime().isBefore(before));
            assertFalse(tag.getCreateTime().isAfter(after));
        }

        @Test
        @DisplayName("should reject null name")
        void should_rejectNullName() {
            ModelLiteException ex = assertThrows(ModelLiteException.class,
                    () -> Tag.createUserTag(null));
            assertTrue(ex.getMessage().contains("标签名称") || ex.getMessage().contains("name")
                    || ex.getMessage().contains("非空") || ex.getMessage().contains("不能为空"));
        }

        @Test
        @DisplayName("should reject empty name")
        void should_rejectEmptyName() {
            assertThrows(ModelLiteException.class,
                    () -> Tag.createUserTag(""));
        }

        @Test
        @DisplayName("should reject blank name")
        void should_rejectBlankName() {
            assertThrows(ModelLiteException.class,
                    () -> Tag.createUserTag("   "));
        }

        @Test
        @DisplayName("should reject name exceeding 50 characters")
        void should_rejectTooLongName() {
            String longName = "a".repeat(51);
            assertThrows(ModelLiteException.class,
                    () -> Tag.createUserTag(longName));
        }

        @Test
        @DisplayName("should accept name at exactly 50 characters")
        void should_acceptNameAt50Chars() {
            String name = "a".repeat(50);
            Tag tag = Tag.createUserTag(name);
            assertEquals(50, tag.getName().length());
        }

        @Test
        @DisplayName("should accept name at exactly 1 character")
        void should_acceptNameAt1Char() {
            Tag tag = Tag.createUserTag("a");
            assertEquals("a", tag.getName());
        }
    }

    @Nested
    @DisplayName("createCapabilityTag")
    class CreateCapabilityTag {

        @Test
        @DisplayName("should create CAPABILITY tag with correct properties")
        void should_createCapabilityTag() {
            Tag tag = Tag.createCapabilityTag("NLP");

            assertNotNull(tag.getTagId());
            assertEquals("NLP", tag.getName());
            assertEquals(TagType.CAPABILITY, tag.getTagType());
            assertFalse(tag.getIsBuiltIn());
            assertNotNull(tag.getCreateTime());
            assertNotNull(tag.getUpdateTime());
        }

        @Test
        @DisplayName("should reject null name")
        void should_rejectNullName() {
            assertThrows(ModelLiteException.class,
                    () -> Tag.createCapabilityTag(null));
        }

        @Test
        @DisplayName("should reject empty name")
        void should_rejectEmptyName() {
            assertThrows(ModelLiteException.class,
                    () -> Tag.createCapabilityTag(""));
        }

        @Test
        @DisplayName("should reject name exceeding 50 characters")
        void should_rejectTooLongName() {
            String longName = "a".repeat(51);
            assertThrows(ModelLiteException.class,
                    () -> Tag.createCapabilityTag(longName));
        }
    }

    @Nested
    @DisplayName("tag identity")
    class TagIdentity {

        @Test
        @DisplayName("should generate different tagIds for different tags")
        void should_generateDifferentIds() {
            Tag tag1 = Tag.createUserTag("a");
            Tag tag2 = Tag.createUserTag("b");

            assertNotEquals(tag1.getTagId(), tag2.getTagId());
        }
    }
}
