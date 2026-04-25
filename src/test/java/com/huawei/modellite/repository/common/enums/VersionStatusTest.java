package com.huawei.modellite.repository.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionStatusTest {

    @Test
    @DisplayName("should return correct enum when fromDbValue with valid values")
    void should_returnCorrectEnum_when_fromDbValue() {
        assertEquals(VersionStatus.NO_WEIGHT, VersionStatus.fromDbValue("NoWeight"));
        assertEquals(VersionStatus.UPLOADING, VersionStatus.fromDbValue("Uploading"));
        assertEquals(VersionStatus.AVAILABLE, VersionStatus.fromDbValue("Available"));
        assertEquals(VersionStatus.UPLOAD_FAILED, VersionStatus.fromDbValue("UploadFailed"));
        assertEquals(VersionStatus.VALIDATION_FAILED, VersionStatus.fromDbValue("ValidationFailed"));
        assertEquals(VersionStatus.ERROR, VersionStatus.fromDbValue("Error"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when fromDbValue with invalid value")
    void should_throwException_when_fromDbValueInvalid() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> VersionStatus.fromDbValue("INVALID")
        );
        assertTrue(exception.getMessage().contains("Unknown VersionStatus: INVALID"));
    }

    @Test
    @DisplayName("should have unique dbValue for each enum constant")
    void should_haveUniqueDbValues() {
        long distinctCount = java.util.Arrays.stream(VersionStatus.values())
            .map(VersionStatus::getDbValue)
            .distinct()
            .count();
        assertEquals(VersionStatus.values().length, distinctCount);
    }

    @Test
    @DisplayName("should return correct displayName")
    void should_returnCorrectDisplayName() {
        assertEquals("无权重", VersionStatus.NO_WEIGHT.getDisplayName());
        assertEquals("可用", VersionStatus.AVAILABLE.getDisplayName());
    }
}
