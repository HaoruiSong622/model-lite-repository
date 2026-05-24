package com.huawei.modellite.repository.infrastructure.k8s;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderElectionServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMapsMixedOp;

    @Mock
    private Resource<ConfigMap> configMapResource;

    private LeaderElectionService service;

    @BeforeEach
    void setUp() {
        service = new LeaderElectionService(kubernetesClient, "default");
        when(kubernetesClient.configMaps()).thenReturn(configMapsMixedOp);
        when(configMapsMixedOp.inNamespace("default")).thenReturn(configMapsMixedOp);
    }

    @Test
    @DisplayName("should succeed when no ConfigMap exists")
    void should_succeed_whenNoConfigMapExists() {
        when(configMapsMixedOp.withName("modellite-leader-lock")).thenReturn(configMapResource);
        when(configMapResource.get()).thenReturn(null);

        Resource<ConfigMap> createResource = mock(Resource.class);
        when(configMapsMixedOp.resource(any(ConfigMap.class))).thenReturn(createResource);
        when(createResource.create()).thenReturn(new ConfigMap());

        assertTrue(service.tryAcquireLeadership());
        assertTrue(service.isLeader());
    }

    @Test
    @DisplayName("should fail when ConfigMap exists with different leader")
    void should_fail_whenConfigMapExistsWithDifferentLeader() {
        Map<String, String> data = new HashMap<>();
        data.put("leaderId", "other-instance-id");
        data.put("timestamp", Instant.now().toString());

        ConfigMap lock = new ConfigMapBuilder()
                .withNewMetadata()
                .withName("modellite-leader-lock")
                .withNamespace("default")
                .endMetadata()
                .withData(data)
                .build();

        when(configMapsMixedOp.withName("modellite-leader-lock")).thenReturn(configMapResource);
        when(configMapResource.get()).thenReturn(lock);

        assertFalse(service.tryAcquireLeadership());
        assertFalse(service.isLeader());
    }

    @Test
    @DisplayName("should succeed when ConfigMap exists with stale timestamp")
    void should_succeed_whenConfigMapExistsWithStaleTimestamp() {
        Map<String, String> data = new HashMap<>();
        data.put("leaderId", "other-instance-id");
        data.put("timestamp", Instant.now().minusSeconds(61).toString());

        ConfigMap lock = new ConfigMapBuilder()
                .withNewMetadata()
                .withName("modellite-leader-lock")
                .withNamespace("default")
                .endMetadata()
                .withData(data)
                .build();

        when(configMapsMixedOp.withName("modellite-leader-lock")).thenReturn(configMapResource);
        when(configMapResource.get()).thenReturn(lock);

        Resource<ConfigMap> updateResource = mock(Resource.class);
        when(configMapsMixedOp.resource(any(ConfigMap.class))).thenReturn(updateResource);
        when(updateResource.update()).thenReturn(new ConfigMap());

        assertTrue(service.tryAcquireLeadership());
        assertTrue(service.isLeader());
    }

    @Test
    @DisplayName("should return true when already leader")
    void should_returnTrue_whenAlreadyLeader() {
        String instanceId = service.getInstanceId();

        Map<String, String> data = new HashMap<>();
        data.put("leaderId", instanceId);
        data.put("timestamp", Instant.now().toString());

        ConfigMap lock = new ConfigMapBuilder()
                .withNewMetadata()
                .withName("modellite-leader-lock")
                .withNamespace("default")
                .endMetadata()
                .withData(data)
                .build();

        when(configMapsMixedOp.withName("modellite-leader-lock")).thenReturn(configMapResource);
        when(configMapResource.get()).thenReturn(lock);

        assertTrue(service.tryAcquireLeadership());
        assertTrue(service.isLeader());
    }

    @Test
    @DisplayName("should release leadership and set isLeader to false")
    void should_releaseLeadership_andSetIsLeaderToFalse() {
        when(configMapsMixedOp.withName("modellite-leader-lock")).thenReturn(configMapResource);
        when(configMapResource.get()).thenReturn(null);

        Resource<ConfigMap> createResource = mock(Resource.class);
        when(configMapsMixedOp.resource(any(ConfigMap.class))).thenReturn(createResource);
        when(createResource.create()).thenReturn(new ConfigMap());

        service.tryAcquireLeadership();
        assertTrue(service.isLeader());

        service.releaseLeadership();
        assertFalse(service.isLeader());
    }

    @Test
    @DisplayName("should return correct leader state")
    void should_returnCorrectLeaderState() {
        assertFalse(service.isLeader());

        when(configMapsMixedOp.withName("modellite-leader-lock")).thenReturn(configMapResource);
        when(configMapResource.get()).thenReturn(null);

        Resource<ConfigMap> createResource = mock(Resource.class);
        when(configMapsMixedOp.resource(any(ConfigMap.class))).thenReturn(createResource);
        when(createResource.create()).thenReturn(new ConfigMap());

        service.tryAcquireLeadership();
        assertTrue(service.isLeader());
    }
}
