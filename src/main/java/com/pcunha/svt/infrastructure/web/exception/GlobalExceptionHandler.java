package com.pcunha.svt.infrastructure.web.exception;

import com.pcunha.svt.domain.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised error handling for all @RestController classes.
 * @RestControllerAdvice scopes this to REST endpoints only — MVC
 * controllers keep their own redirect-based flow.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoGameInSessionException.class)
    public ResponseEntity<ApiError> handleNoGameInSession(NoGameInSessionException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
    }
}
