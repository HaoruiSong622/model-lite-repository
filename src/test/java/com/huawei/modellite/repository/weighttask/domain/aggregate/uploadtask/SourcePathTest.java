package com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask;

import com.huawei.modellite.repository.common.enums.ErrorCode;
import com.huawei.modellite.repository.common.enums.SourceType;
import com.huawei.modellite.repository.common.exception.ModelLiteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourcePathTest {

    @Nested
    @DisplayName("ofNfs factory tests")
    class OfNfsTests {

        @Test
        @DisplayName("ofNfs_createsCorrectPath")
        void ofNfs_createsCorrectPath() {
            SourcePath sourcePath = SourcePath.ofNfs("10.0.1.100", "/data/models");

            assertEquals(SourceType.NFS, sourcePath.getSourceType());
            assertEquals("10.0.1.100:/data/models", sourcePath.getPath());
            assertNull(sourcePath.getCredentials());
        }
    }

    @Nested
    @DisplayName("ofCifs factory tests")
    class OfCifsTests {

        @Test
        @DisplayName("ofCifs_createsCorrectPath")
        void ofCifs_createsCorrectPath() {
            SourcePath sourcePath = SourcePath.ofCifs("file-server", "models", "user", "pass123");

            assertEquals(SourceType.CIFS, sourcePath.getSourceType());
            assertEquals("//file-server/models", sourcePath.getPath());
            assertNotNull(sourcePath.getCredentials());
            assertEquals("user", sourcePath.getCredentials().getUsername());
            assertEquals("pass123", sourcePath.getCredentials().getPassword());
        }

        @Test
        @DisplayName("ofCifs_nullCredentials_throws")
        void ofCifs_nullCredentials_throws() {
            ModelLiteException exception = assertThrows(ModelLiteException.class,
                    () -> SourcePath.ofCifs("file-server", "models", null, null));

            assertEquals(ErrorCode.UPLOAD_CIFS_CREDENTIALS_REQUIRED, exception.getCode());
        }
    }

    @Nested
    @DisplayName("ofPvc factory tests")
    class OfPvcTests {

        @Test
        @DisplayName("ofPvc_createsCorrectPath")
        void ofPvc_createsCorrectPath() {
            SourcePath sourcePath = SourcePath.ofPvc("source-pvc", "/data");

            assertEquals(SourceType.PVC, sourcePath.getSourceType());
            assertEquals("source-pvc:/data", sourcePath.getPath());
            assertNull(sourcePath.getCredentials());
        }
    }

    @Nested
    @DisplayName("path length validation")
    class PathLengthValidationTests {

        @Test
        @DisplayName("path_lengthValidation")
        void path_lengthValidation() {
            String longPath = "a".repeat(1025);

            assertThrows(ModelLiteException.class,
                    () -> SourcePath.ofNfs("server", longPath));
        }
    }
}
