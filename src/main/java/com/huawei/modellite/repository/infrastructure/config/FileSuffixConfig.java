package com.huawei.modellite.repository.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "model-lite.upload")
public class FileSuffixConfig {

    private List<String> allowedSuffixes = new ArrayList<>();

    public List<String> getAllowedSuffixes() {
        return allowedSuffixes;
    }

    public void setAllowedSuffixes(List<String> allowedSuffixes) {
        this.allowedSuffixes = allowedSuffixes;
    }

    public boolean isAllowed(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return allowedSuffixes.stream()
                .map(String::toLowerCase)
                .anyMatch(lowerFilename::endsWith);
    }
}
