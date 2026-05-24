package com.huawei.modellite.repository.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weight-import.job")
@Getter
@Setter
public class WeightImportConfig {

    private String image = "file-copier:dev-placeholder";
    private String namespace = "default";
    private String cpuRequest = "500m";
    private String memoryRequest = "512Mi";
    private String storageRequest = "1Gi";
    private String suffixWhitelist = ".safetensors,.bin,.pt,.pth,.onnx";
}
