package com.huawei.modellite.repository.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FileSuffixConfigTest {

    private FileSuffixConfig config;

    @BeforeEach
    void setUp() {
        config = new FileSuffixConfig();
        config.setAllowedSuffixes(Arrays.asList(
            ".safetensors", ".bin", ".pt", ".pth", ".onnx",
            ".json", ".txt", ".model", ".ckpt", ".index"
        ));
    }

    @Test
    @DisplayName("should return true for allowed suffix")
    void should_returnTrue_forAllowedSuffix() {
        assertTrue(config.isAllowed("model.safetensors"));
        assertTrue(config.isAllowed("weights.bin"));
        assertTrue(config.isAllowed("config.json"));
    }

    @Test
    @DisplayName("should return false for disallowed suffix")
    void should_returnFalse_forDisallowedSuffix() {
        assertFalse(config.isAllowed("malware.exe"));
        assertFalse(config.isAllowed("script.sh"));
        assertFalse(config.isAllowed("document.pdf"));
    }

    @Test
    @DisplayName("should be case insensitive")
    void should_beCaseInsensitive() {
        assertTrue(config.isAllowed("model.SAFETENSORS"));
        assertTrue(config.isAllowed("weights.BIN"));
        assertTrue(config.isAllowed("config.JSON"));
    }

    @Test
    @DisplayName("should return false for null or empty filename")
    void should_returnFalse_forNullOrEmptyFilename() {
        assertFalse(config.isAllowed(null));
        assertFalse(config.isAllowed(""));
    }
}
