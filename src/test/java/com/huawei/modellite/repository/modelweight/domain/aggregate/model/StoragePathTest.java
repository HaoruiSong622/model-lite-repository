package com.huawei.modellite.repository.modelweight.domain.aggregate.model;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoragePathTest {

    @Nested
    @DisplayName("ofPvc factory tests")
    class OfPvcTests {

        @Test
        @DisplayName("should create StoragePath with PVC type")
        void should_createStoragePath_withPvcType() {
            StoragePath storagePath = StoragePath.ofPvc("my-pvc", "/data/models");

            assertEquals(SourcePathType.PVC, storagePath.getSourceType());
            assertEquals("my-pvc", storagePath.getPvcName());
            assertEquals("/data/models", storagePath.getInternalPath());
            assertNull(storagePath.getNfsServer());
            assertNull(storagePath.getNfsPath());
        }

        @Test
        @DisplayName("should throw exception when pvcName is null")
        void should_throwException_whenPvcNameIsNull() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> StoragePath.ofPvc(null, "/data/models"));

            assertEquals(ErrorCode.STORAGE_PATH_PVC_NAME_REQUIRED, exception.getCode());
        }

        @Test
        @DisplayName("should throw exception when pvcName is empty")
        void should_throwException_whenPvcNameIsEmpty() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> StoragePath.ofPvc("", "/data/models"));

            assertEquals(ErrorCode.STORAGE_PATH_PVC_NAME_REQUIRED, exception.getCode());
        }

        @Test
        @DisplayName("should allow null internalPath")
        void should_allowNullInternalPath() {
            StoragePath storagePath = StoragePath.ofPvc("my-pvc", null);

            assertEquals("my-pvc", storagePath.getPvcName());
            assertNull(storagePath.getInternalPath());
        }
    }

    @Nested
    @DisplayName("ofNfs factory tests")
    class OfNfsTests {

        @Test
        @DisplayName("should create StoragePath with NFS type")
        void should_createStoragePath_withNfsType() {
            StoragePath storagePath = StoragePath.ofNfs("nfs-server", "/shared/data");

            assertEquals(SourcePathType.NFS, storagePath.getSourceType());
            assertNull(storagePath.getPvcName());
            assertNull(storagePath.getInternalPath());
            assertEquals("nfs-server", storagePath.getNfsServer());
            assertEquals("/shared/data", storagePath.getNfsPath());
        }

        @Test
        @DisplayName("should throw exception when nfsServer is null")
        void should_throwException_whenNfsServerIsNull() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> StoragePath.ofNfs(null, "/shared/data"));

            assertEquals(ErrorCode.STORAGE_PATH_NFS_REQUIRED, exception.getCode());
        }

        @Test
        @DisplayName("should throw exception when nfsPath is null")
        void should_throwException_whenNfsPathIsNull() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> StoragePath.ofNfs("nfs-server", null));

            assertEquals(ErrorCode.STORAGE_PATH_NFS_REQUIRED, exception.getCode());
        }

        @Test
        @DisplayName("should throw exception when both nfsServer and nfsPath are null")
        void should_throwException_whenBothAreNull() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> StoragePath.ofNfs(null, null));

            assertEquals(ErrorCode.STORAGE_PATH_NFS_REQUIRED, exception.getCode());
        }
    }

    @Nested
    @DisplayName("empty factory tests")
    class EmptyTests {

        @Test
        @DisplayName("should create empty StoragePath with null fields")
        void should_createEmptyStoragePath_withNullFields() {
            StoragePath storagePath = StoragePath.empty();

            assertNull(storagePath.getSourceType());
            assertNull(storagePath.getPvcName());
            assertNull(storagePath.getInternalPath());
            assertNull(storagePath.getNfsServer());
            assertNull(storagePath.getNfsPath());
        }
    }

    @Nested
    @DisplayName("immutability tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("should not have setters")
        void should_notHaveSetters() {
            StoragePath storagePath = StoragePath.ofPvc("my-pvc", "/data");

            assertFalse(hasSetterMethod(StoragePath.class, "sourceType"));
            assertFalse(hasSetterMethod(StoragePath.class, "pvcName"));
            assertFalse(hasSetterMethod(StoragePath.class, "internalPath"));
            assertFalse(hasSetterMethod(StoragePath.class, "nfsServer"));
            assertFalse(hasSetterMethod(StoragePath.class, "nfsPath"));
        }
    }

    private boolean hasSetterMethod(Class<?> clazz, String fieldName) {
        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            clazz.getMethod(setterName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}