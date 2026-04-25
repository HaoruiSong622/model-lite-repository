package com.huawei.modellite.repository.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceTypeTest {

    @Test
    @DisplayName("should return correct enum when fromDbValue with valid values")
    void should_returnCorrectEnum_when_fromDbValue() {
        assertEquals(SourceType.NFS, SourceType.fromDbValue("NFS"));
        assertEquals(SourceType.CIFS, SourceType.fromDbValue("CIFS"));
        assertEquals(SourceType.PVC, SourceType.fromDbValue("PVC"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when fromDbValue with invalid value")
    void should_throwException_when_fromDbValueInvalid() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SourceType.fromDbValue("INVALID")
        );
        assertTrue(exception.getMessage().contains("Unknown SourceType: INVALID"));
    }

    @Test
    @DisplayName("should have unique dbValue for each enum constant")
    void should_haveUniqueDbValues() {
        long distinctCount = java.util.Arrays.stream(SourceType.values())
            .map(SourceType::getDbValue)
            .distinct()
            .count();
        assertEquals(SourceType.values().length, distinctCount);
    }

    @Test
    @DisplayName("should return correct displayName")
    void should_returnCorrectDisplayName() {
        assertEquals("NFS 存储", SourceType.NFS.getDisplayName());
        assertEquals("PVC 存储", SourceType.PVC.getDisplayName());
    }
}
