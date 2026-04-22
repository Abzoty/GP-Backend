package com.gp.GP_backend.shared.exception;

import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionShouldReturnStatusAndFailureEnvelope() {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "duplicate resource");

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("duplicate resource", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentialsShouldReturnGenericUnauthorizedMessage() {
        BadCredentialsException ex = new BadCredentialsException("bad creds");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid email or password", response.getBody().getMessage());
    }

    @Test
    void handleResponseStatusShouldPreserveReasonAndStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "material not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("material not found", response.getBody().getMessage());
    }

    @Test
    void handleGeneralShouldReturnSafeInternalErrorMessage() {
        RuntimeException ex = new RuntimeException("sensitive stack detail");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
    }

    @Test
    void handleUnreadableShouldIncludeParsingCauseMessage() {
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException(
                "unexpected token at position 4",
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Malformed JSON request:"));
        assertTrue(response.getBody().getMessage().contains("unexpected token at position 4"));
    }

    @Test
    void handleValidationShouldAggregateFieldErrors() throws Exception {
        Method method = DummyController.class.getDeclaredMethod("create", DummyBody.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new DummyBody(), "dummyBody");
        bindingResult.addError(new FieldError("dummyBody", "email", "must not be blank"));
        bindingResult.addError(new FieldError("dummyBody", "password", "must be at least 8 characters"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("email: must not be blank"));
        assertTrue(response.getBody().getMessage().contains("password: must be at least 8 characters"));
    }

    static class DummyController {
        void create(@Valid DummyBody body) {
        }
    }

    static class DummyBody {
    }
}
