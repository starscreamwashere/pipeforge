package com.pipeforge.exception;

/** Thrown for business-rule validation failures (mapped to HTTP 400). */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
