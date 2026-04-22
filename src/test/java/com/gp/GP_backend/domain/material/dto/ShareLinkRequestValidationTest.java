package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareLinkRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @Test
    void shouldAcceptHttpOrHttpsUrl() {
        ShareLinkRequest request = new ShareLinkRequest();
        request.setSpaceId(UUID.randomUUID());
        request.setTitle("Official docs");
        request.setUrl("https://example.com/docs");

        Set<ConstraintViolation<ShareLinkRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectNonHttpScheme() {
        ShareLinkRequest request = new ShareLinkRequest();
        request.setSpaceId(UUID.randomUUID());
        request.setTitle("Bad link");
        request.setUrl("javascript:alert(1)");

        Set<ConstraintViolation<ShareLinkRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }
}
