package com.pcunha.svt.infrastructure.web.exception;

import com.pcunha.svt.domain.model.ApiError;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles REST errors in one place. Keeps MVC flow separate.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoGameInSessionException.class)
    public ResponseEntity<ApiError> handleNoGameInSession(NoGameInSessionException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Game state changed. Reload and try again."));
    }
}