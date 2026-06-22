// package com.gp.GP_backend.domain.user.dto;

// import jakarta.validation.ConstraintViolation;
// import jakarta.validation.Validation;
// import jakarta.validation.Validator;
// import jakarta.validation.ValidatorFactory;
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;

// import java.math.BigDecimal;
// import java.util.Set;

// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// class UpdateCourseRegistrationRequestValidationTest {

//     private static ValidatorFactory factory;
//     private static Validator validator;

//     @BeforeAll
//     static void setupValidator() {
//         factory = Validation.buildDefaultValidatorFactory();
//         validator = factory.getValidator();
//     }

//     @AfterAll
//     static void closeFactory() {
//         factory.close();
//     }

//     @Test
//     void shouldAcceptResultWithinRangeAndScale() {
//         UpdateCourseRegistrationRequest request = new UpdateCourseRegistrationRequest();
//         request.setResult(new BigDecimal("99.9"));

//         Set<ConstraintViolation<UpdateCourseRegistrationRequest>> violations = validator.validate(request);

//         assertTrue(violations.isEmpty());
//     }

//     @Test
//     void shouldRejectResultAboveMax() {
//         UpdateCourseRegistrationRequest request = new UpdateCourseRegistrationRequest();
//         request.setResult(new BigDecimal("100.1"));

//         Set<ConstraintViolation<UpdateCourseRegistrationRequest>> violations = validator.validate(request);

//         assertFalse(violations.isEmpty());
//     }

//     @Test
//     void shouldRejectResultWithMoreThanOneDecimal() {
//         UpdateCourseRegistrationRequest request = new UpdateCourseRegistrationRequest();
//         request.setResult(new BigDecimal("85.55"));

//         Set<ConstraintViolation<UpdateCourseRegistrationRequest>> violations = validator.validate(request);

//         assertFalse(violations.isEmpty());
//     }
// }
