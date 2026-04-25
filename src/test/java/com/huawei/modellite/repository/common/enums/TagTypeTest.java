package com.huawei.modellite.repository.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TagTypeTest {

    @Test
    @DisplayName("should return correct enum when fromDbValue with valid values")
    void should_returnCorrectEnum_when_fromDbValue() {
        assertEquals(TagType.USER, TagType.fromDbValue("USER"));
        assertEquals(TagType.CAPABILITY, TagType.fromDbValue("CAPABILITY"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when fromDbValue with invalid value")
    void should_throwException_when_fromDbValueInvalid() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TagType.fromDbValue("INVALID")
        );
        assertTrue(exception.getMessage().contains("Unknown TagType: INVALID"));
    }

    @Test
    @DisplayName("should have unique dbValue for each enum constant")
    void should_haveUniqueDbValues() {
        long distinctCount = java.util.Arrays.stream(TagType.values())
            .map(TagType::getDbValue)
            .distinct()
            .count();
        assertEquals(TagType.values().length, distinctCount);
    }

    @Test
    @DisplayName("should return correct displayName")
    void should_returnCorrectDisplayName() {
        assertEquals("用户自定义标签", TagType.USER.getDisplayName());
        assertEquals("能力标签", TagType.CAPABILITY.getDisplayName());
    }
}
