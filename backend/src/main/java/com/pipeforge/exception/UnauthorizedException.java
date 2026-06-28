package com.pipeforge.exception;

/** Thrown when authentication fails or credentials are invalid (mapped to HTTP 401). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
