package com.gp.GP_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the Spring application context loads without errors.
 *
 * <p>
 * This test should run using the {@code test} Spring profile (H2 in-memory DB)
 * so it stays deterministic and does not depend on external services.
 */
@SpringBootTest
@ActiveProfiles("test")
class GpBackendApplicationTests {

    @Test
    void contextLoads() {
        // If the context starts up without throwing an exception, the test passes.
    }
}
