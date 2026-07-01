package com.huawei.modellite.repository.infrastructure.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
public class KubernetesClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
