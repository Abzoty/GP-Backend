package com.gp.GP_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Covalent GP Backend application.
 *
 * <p>
 * Key annotations:
 * <ul>
 * <li>{@code @EnableAsync} — allows {@code @Async} methods (e.g. email sending)
 * to run on a
 * separate thread pool instead of blocking the HTTP request thread.</li>
 * <li>{@code @EnableScheduling} — activates {@code @Scheduled} tasks (e.g.
 * nightly token cleanup).</li>
 * </ul>
 *
 * <p>
 * ModelMapper is configured as a Spring Bean inside
 * {@link com.gp.GP_backend.config.ModelMapperConfig}
 * — do NOT define a second {@code @Bean} here.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GpBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GpBackendApplication.class, args);
	}
}