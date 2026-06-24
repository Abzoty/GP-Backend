package com.gp.GP_backend.domain.prediction.service;

import com.gp.GP_backend.domain.prediction.dto.PythonServiceRequest;
import com.gp.GP_backend.domain.prediction.dto.PythonServiceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the Python FastAPI prediction service.
 *
 * Calls {@code POST /predict} on the Python service, passing course data and
 * receiving department probabilities.
 *
 * Resilience:
 * - Resilience4j circuit breaker ("prediction-service") wraps every call.
 * - Configurable connect + read timeouts prevent thread starvation.
 * - Fallback returns {@code null}, which the orchestration service treats as
 * "model unavailable" and falls back to questionnaire-only scoring.
 *
 * Configuration (application.properties / application.yml):
 * 
 * <pre>
 * prediction.service.url=http://localhost:5002
 * prediction.service.connect-timeout-ms=5000
 * prediction.service.read-timeout-ms=30000
 * </pre>
 *
 * @since 1.0
 */
@Service
@Slf4j
public class PredictionServiceClient {

        private final RestClient restClient;

        public PredictionServiceClient(
                        @Value("${prediction.service.url:http://localhost:5002}") String serviceUrl,
                        @Value("${prediction.service.connect-timeout-ms:5000}") int connectTimeoutMs,
                        @Value("${prediction.service.read-timeout-ms:30000}") int readTimeoutMs) {

                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(connectTimeoutMs);
                factory.setReadTimeout(readTimeoutMs);

                this.restClient = RestClient.builder()
                                .baseUrl(serviceUrl)
                                .requestFactory(factory)
                                .build();

                log.info("PredictionServiceClient ready — url={}, connectTimeout={}ms, readTimeout={}ms",
                                serviceUrl, connectTimeoutMs, readTimeoutMs);
        }

        /**
         * Sends course data to the Python service and retrieves department
         * probabilities.
         *
         * The Python service is responsible for all feature engineering (one-hot
         * encoding, GPA averages, etc.) and model inference.
         *
         * @param request course data for the current user
         * @return model response containing department probabilities, or {@code null}
         *         if the service is unreachable or the circuit breaker is open
         */
        @CircuitBreaker(name = "prediction-service", fallbackMethod = "predictFallback")
        public PythonServiceResponse predict(PythonServiceRequest request) {
                log.info("Calling Python prediction service — {} course(s) in payload",
                                request.getCourses().size());

                PythonServiceResponse response = restClient.post()
                                .uri("/predict")
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .retrieve()
                                .body(PythonServiceResponse.class);

                log.info("Python service responded — probabilities={}",
                                response != null ? response.getProbabilities() : "null");

                return response;
        }

        /**
         * Circuit-breaker fallback.
         *
         * Returns {@code null} so the orchestration service can degrade gracefully
         * to questionnaire-only scores instead of surfacing a 500 to the client.
         *
         * @param request the original request (unused in fallback)
         * @param ex      the exception that triggered the fallback
         * @return null — signals "model unavailable" to the orchestration service
         */
        public PythonServiceResponse predictFallback(PythonServiceRequest request, Throwable ex) {
                log.warn("Prediction service fallback triggered — cause: {}: {}",
                                ex.getClass().getSimpleName(), ex.getMessage());
                return null;
        }
}