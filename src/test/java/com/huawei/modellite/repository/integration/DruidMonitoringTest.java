package com.huawei.modellite.repository.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class DruidMonitoringTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("should not expose druid monitoring in test profile")
    void should_notExposeDruidMonitoring() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/druid/index.html", String.class);

        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }
}
