package com.quikko.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Converts strict input-validation failures (e.g. a malformed capsule
 * token) into a clean 400 response instead of the default 500 that a
 * {@code @Validated}-annotated controller's ConstraintViolationException
 * would otherwise produce.
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraintViolation(ConstraintViolationException ex) {
        return Map.of("error", "Invalid request");
    }
}
