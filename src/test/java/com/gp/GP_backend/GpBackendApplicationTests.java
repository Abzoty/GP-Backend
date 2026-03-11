package com.gp.GP_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test that verifies the Spring application context loads without errors.
 *
 * <p>
 * <b>Prerequisites:</b> This test requires a running SQL Server instance
 * (configured in {@code application-dev.properties}) because the full context
 * includes JPA/Hibernate, which connects to the database on startup.
 *
 * <p>
 * To run integration tests without a live database, either:
 * <ul>
 * <li>Add an H2 in-memory database as a test-scoped dependency and create
 * an {@code application-test.properties} with H2 settings, or</li>
 * <li>Use {@code @MockBean} / {@code @DataJpaTest} slices for unit tests
 * that don't need the full context.</li>
 * </ul>
 */
@SpringBootTest
class GpBackendApplicationTests {

    @Test
    void contextLoads() {
        // If the context starts up without throwing an exception, the test passes.
    }
}
