package com.huawei.modellite.repository.integration;

import com.huawei.modellite.repository.weighttask.domain.service.K8sJobService;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class K8sJobServiceTestConfig {

    @Bean
    @ConditionalOnMissingBean(KubernetesClient.class)
    public K8sJobService k8sJobService() {
        return mock(K8sJobService.class);
    }
}
