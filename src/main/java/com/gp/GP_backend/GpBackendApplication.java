package com.gp.GP_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GpBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GpBackendApplication.class, args);
    }
}


