package com.gp.GP_backend.domain.prediction.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the FastAPI prediction service.
 *
 * Wraps calls to the FastAPI `/predict` endpoint with a Resilience4j circuit
 * breaker.
 *
 * Currently provides a stub implementation that returns fixed probabilities.
 * When FastAPI is ready, replace the stub with actual RestClient calls.
 *
 * Circuit breaker name: "prediction-service"
 * Fallback: returns stub probabilities and modelAvailable=false with warning.
 *
 * @since 1.0
 */
@Service
@Slf4j
public class PredictionServiceClient {

    /**
     * Calls the FastAPI prediction service to get department probabilities.
     *
     * Stub implementation: Returns fixed probabilities.
     * To-do: Replace with actual RestClient call when FastAPI is available.
     *
     * @param courseCodesForPrediction list of course codes
     * @return map of department -> probability (BigDecimal between 0 and 1)
     */
    @CircuitBreaker(name = "prediction-service", fallbackMethod = "predictFallback")
    public Map<String, BigDecimal> predict(List<String> courseCodesForPrediction) {
        // STUB: Return fixed probabilities
        // TODO: Replace with actual RestClient call to FastAPI /predict endpoint
        log.info("Prediction request (stub): courses={}", courseCodesForPrediction);

        return getStubProbabilities();
    }

    /**
     * Fallback method when the prediction service is unavailable.
     *
     * Returns fixed stub probabilities and indicates model is unavailable.
     *
     * @param courseCodesForPrediction the course list (unused in fallback)
     * @param ex                       the exception that triggered the fallback
     * @return map of department -> probability (stub values)
     */
    public Map<String, BigDecimal> predictFallback(List<String> courseCodesForPrediction, Throwable ex) {
        log.warn("Prediction service fallback triggered", ex);
        return getStubProbabilities();
    }

    /**
     * Returns fixed stub probabilities for testing.
     *
     * @return map of departments -> fixed probabilities
     */
    private Map<String, BigDecimal> getStubProbabilities() {
        Map<String, BigDecimal> probs = new HashMap<>();
        probs.put("AI", new BigDecimal("0.25"));
        probs.put("Systems", new BigDecimal("0.25"));
        probs.put("Web", new BigDecimal("0.25"));
        probs.put("Security", new BigDecimal("0.25"));
        return probs;
    }
}