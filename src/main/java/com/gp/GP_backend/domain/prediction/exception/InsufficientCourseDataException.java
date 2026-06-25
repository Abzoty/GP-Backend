package com.gp.GP_backend.domain.prediction.exception;

import lombok.Getter;
import java.util.List;

@Getter
public class InsufficientCourseDataException extends RuntimeException {

    private final List<String> missingCourses;
    private final List<String> incompleteCourses; // <-- Changed to List<String>

    public InsufficientCourseDataException(
            String message,
            List<String> missingCourses,
            List<String> incompleteCourses) { // <-- Changed parameter type
        super(message);
        this.missingCourses = missingCourses != null ? missingCourses : List.of();
        this.incompleteCourses = incompleteCourses != null ? incompleteCourses : List.of();
    }
}