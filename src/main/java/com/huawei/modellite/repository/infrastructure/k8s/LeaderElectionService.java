package com.huawei.modellite.repository.infrastructure.k8s;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnBean(KubernetesClient.class)
public class LeaderElectionService {

    private static final String LEADER_CONFIGMAP_NAME = "modellite-leader-lock";
    private static final String LEADER_ID_KEY = "leaderId";
    private static final String TIMESTAMP_KEY = "timestamp";

    private final KubernetesClient kubernetesClient;
    private final String namespace;
    private final String instanceId;
    private volatile boolean isLeader = false;

    public LeaderElectionService(KubernetesClient kubernetesClient,
                                  @Value("${weight-import.namespace:default}") String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
        this.instanceId = UUID.randomUUID().toString();
    }

    public boolean isLeader() {
        return isLeader;
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Periodically try to acquire or renew leadership.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000)
    public synchronized void maintainLeadership() {
        if (kubernetesClient.configMaps() == null) {
            log.warn("KubernetesClient.configMaps() returned null, skipping leadership maintenance");
            return;
        }
        if (isLeader) {
            renewLeadership();
        } else {
            tryAcquireLeadership();
        }
    }

    private void renewLeadership() {
        try {
            if (kubernetesClient.configMaps() == null) {
                log.warn("KubernetesClient.configMaps() returned null, cannot renew leadership");
                isLeader = false;
                return;
            }
            ConfigMap lock = kubernetesClient.configMaps()
                    .inNamespace(namespace)
                    .withName(LEADER_CONFIGMAP_NAME)
                    .get();
            if (lock == null) {
                tryAcquireLeadership();
                return;
            }
            String currentLeader = lock.getData().get(LEADER_ID_KEY);
            if (!instanceId.equals(currentLeader)) {
                isLeader = false;
                log.info("Leadership lost: {}", instanceId);
                return;
            }
            ConfigMap updated = new ConfigMapBuilder(lock)
                    .addToData(TIMESTAMP_KEY, Instant.now().toString())
                    .build();
            kubernetesClient.configMaps().inNamespace(namespace).resource(updated).update();
        } catch (Exception e) {
            log.warn("Failed to renew leadership: {}", e.getMessage());
            isLeader = false;
        }
    }

    /**
     * Try to acquire leadership by creating or updating the leader ConfigMap.
     * @return true if this instance became the leader
     */
    public synchronized boolean tryAcquireLeadership() {
        if (kubernetesClient.configMaps() == null) {
            log.warn("KubernetesClient.configMaps() returned null, cannot acquire leadership");
            return false;
        }
        ConfigMap lock = kubernetesClient.configMaps()
                .inNamespace(namespace)
                .withName(LEADER_CONFIGMAP_NAME)
                .get();

        if (lock == null) {
            // No leader exists - try to create
            ConfigMap newLock = new ConfigMapBuilder()
                    .withNewMetadata()
                    .withName(LEADER_CONFIGMAP_NAME)
                    .withNamespace(namespace)
                    .endMetadata()
                    .addToData(LEADER_ID_KEY, instanceId)
                    .addToData(TIMESTAMP_KEY, Instant.now().toString())
                    .build();
            try {
                kubernetesClient.configMaps().inNamespace(namespace).resource(newLock).create();
                isLeader = true;
                log.info("Leader acquired: {}", instanceId);
                return true;
            } catch (Exception e) {
                // Another instance created first
                log.debug("Leader acquisition failed, another instance won: {}", e.getMessage());
                return false;
            }
        }

        String currentLeader = lock.getData().get(LEADER_ID_KEY);
        if (instanceId.equals(currentLeader)) {
            isLeader = true;
            return true;
        }

        // Check if current leader's timestamp is stale (> 60 seconds old)
        String timestampStr = lock.getData().get(TIMESTAMP_KEY);
        if (timestampStr != null) {
            try {
                Instant leaderTime = Instant.parse(timestampStr);
                if (Instant.now().isAfter(leaderTime.plusSeconds(60))) {
                    // Leader is stale - try to take over
                    ConfigMap updated = new ConfigMapBuilder(lock)
                            .addToData(LEADER_ID_KEY, instanceId)
                            .addToData(TIMESTAMP_KEY, Instant.now().toString())
                            .build();
                    kubernetesClient.configMaps().inNamespace(namespace).resource(updated).update();
                    isLeader = true;
                    log.info("Leader taken over (stale): {} -> {}", currentLeader, instanceId);
                    return true;
                }
            } catch (Exception e) {
                log.debug("Failed to parse leader timestamp: {}", e.getMessage());
            }
        }

        isLeader = false;
        return false;
    }

    /**
     * Release leadership by deleting the leader ConfigMap.
     */
    public synchronized void releaseLeadership() {
        if (!isLeader) return;

        try {
            if (kubernetesClient.configMaps() == null) {
                log.warn("KubernetesClient.configMaps() returned null, cannot release leadership");
                isLeader = false;
                return;
            }
            kubernetesClient.configMaps()
                    .inNamespace(namespace)
                    .withName(LEADER_CONFIGMAP_NAME)
                    .delete();
            log.info("Leader released: {}", instanceId);
        } catch (Exception e) {
            log.warn("Failed to release leadership: {}", e.getMessage());
        }
        isLeader = false;
    }
}
