package com.huawei.modellite.repository.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class HealthEndpointTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("should return UP status from health endpoint")
    void should_returnUpStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    @DisplayName("should not expose sensitive actuator endpoints")
    void should_notExposeSensitiveEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/env", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
