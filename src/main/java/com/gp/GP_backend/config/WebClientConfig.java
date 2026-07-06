package com.gp.GP_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class WebClientConfig {

    @Value("${ml.service.base-url:http://localhost:5001}")
    private String mlServiceBaseUrl;

    @Bean
    public RestClient mlRestClient() {
        return RestClient.builder()
                .baseUrl(mlServiceBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
