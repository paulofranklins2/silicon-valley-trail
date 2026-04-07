package com.pcunha.svt.infrastructure.web.exception;

/**
 * Thrown when a request needs a game but none exists in session.
 */
public class NoGameInSessionException extends RuntimeException {
    public NoGameInSessionException() {
        super("No game in progress");
    }
}
