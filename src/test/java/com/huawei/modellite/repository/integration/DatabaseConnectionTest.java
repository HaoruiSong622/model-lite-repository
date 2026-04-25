package com.huawei.modellite.repository.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("should connect to database successfully")
    void should_connectToDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            var result = connection.createStatement().executeQuery("SELECT 1");
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }
}
