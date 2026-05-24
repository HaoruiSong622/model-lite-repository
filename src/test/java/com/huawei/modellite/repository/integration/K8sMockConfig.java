package com.huawei.modellite.repository.integration;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.mockwebserver.Context;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class K8sMockConfig {

    private static KubernetesMockServer mockServer;

    @Bean
    public KubernetesClient kubernetesClient() {
        if (mockServer == null) {
            mockServer = new KubernetesMockServer(
                    new Context(Serialization.jsonMapper()),
                    new MockWebServer(),
                    new HashMap<>(),
                    new KubernetesMixedDispatcher(new HashMap<>()),
                    true
            );
            mockServer.init();
        }
        return mockServer.createClient();
    }

    public static void destroy() {
        if (mockServer != null) {
            mockServer.destroy();
            mockServer = null;
        }
    }
}
