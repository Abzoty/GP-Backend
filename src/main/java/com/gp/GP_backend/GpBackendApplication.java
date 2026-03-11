package com.gp.GP_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Covalent GP Backend application.
 *
 * <p>
 * Key annotations:
 * <ul>
 * <li>{@code @EnableAsync} – allows {@code @Async} methods (e.g. email sending)
 * to run
 * on a background thread pool instead of the request thread.</li>
 * <li>{@code @EnableScheduling} – activates {@code @Scheduled} tasks (e.g.
 * nightly token cleanup).</li>
 * </ul>
 *
 * <p>
 * Beans such as {@link org.modelmapper.ModelMapper} are declared in their own
 * {@code @Configuration} classes under {@code config/} to keep this class
 * minimal.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GpBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GpBackendApplication.class, args);
    }
}
