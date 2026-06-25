package com.gp.GP_backend.domain.prediction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gp.GP_backend.domain.prediction.dto.PythonErrorResponse;
import com.gp.GP_backend.domain.prediction.dto.PythonServiceRequest;
import com.gp.GP_backend.domain.prediction.dto.PythonServiceResponse;
import com.gp.GP_backend.domain.prediction.exception.InsufficientCourseDataException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class PredictionServiceClient {

        private final RestClient restClient;
        private final ObjectMapper objectMapper;

        public PredictionServiceClient(
                        @Value("${prediction.service.url:http://localhost:5002}") String serviceUrl,
                        @Value("${prediction.service.connect-timeout-ms:5000}") int connectTimeoutMs,
                        @Value("${prediction.service.read-timeout-ms:30000}") int readTimeoutMs,
                        ObjectMapper objectMapper) {

                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(connectTimeoutMs);
                factory.setReadTimeout(readTimeoutMs);

                this.restClient = RestClient.builder()
                                .baseUrl(serviceUrl)
                                .requestFactory(factory)
                                .build();

                this.objectMapper = objectMapper;

                log.info("PredictionServiceClient ready — url={}, connectTimeout={}ms, readTimeout={}ms",
                                serviceUrl, connectTimeoutMs, readTimeoutMs);
        }

        @CircuitBreaker(name = "prediction-service", fallbackMethod = "predictFallback")
        public PythonServiceResponse predict(PythonServiceRequest request) {
                log.info("Calling Python prediction service — {} course(s) in payload",
                                request.getCourses().size());

                PythonServiceResponse response = restClient.post()
                                .uri("/predict")
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .retrieve()
                                // ── THE FIX: Compare the integer value (422) ──
                                .onStatus(
                                                status -> status.value() == 422,
                                                (req, resp) -> {
                                                        try {
                                                                String body = new String(resp.getBody().readAllBytes(),
                                                                                StandardCharsets.UTF_8);
                                                                log.debug("Python service 422 body: {}", body);

                                                                PythonErrorResponse error = objectMapper.readValue(body,
                                                                                PythonErrorResponse.class);

                                                                throw new InsufficientCourseDataException(
                                                                                error.getMessage(),
                                                                                error.getMissingCourses(),
                                                                                error.getIncompleteCourses()); // Now
                                                                                                               // passes
                                                                                                               // List<String>

                                                        } catch (IOException e) {
                                                                log.error("Failed to parse 422 error response from Python service",
                                                                                e);
                                                                throw new RuntimeException(
                                                                                "Failed to parse error response from Python service",
                                                                                e);
                                                        }
                                                })
                                .body(PythonServiceResponse.class);

                log.info("Python service responded — probabilities={}",
                                response != null ? response.getProbabilities() : "null");

                return response;
        }

        public PythonServiceResponse predictFallback(PythonServiceRequest request, Throwable ex) {
                if (ex instanceof InsufficientCourseDataException) {
                        throw (InsufficientCourseDataException) ex;
                }
                if (ex.getCause() instanceof InsufficientCourseDataException) {
                        throw (InsufficientCourseDataException) ex.getCause();
                }

                log.warn("Prediction service fallback triggered — cause: {}: {}",
                                ex.getClass().getSimpleName(), ex.getMessage());
                return null;
        }
}