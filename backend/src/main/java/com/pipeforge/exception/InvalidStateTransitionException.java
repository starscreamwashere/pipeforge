package com.pipeforge.exception;

/** Thrown when an execution state transition is not permitted (mapped to HTTP 409). */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
