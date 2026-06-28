package com.pipeforge.exception;

/** Thrown when a pipeline's task dependencies would form a cycle (mapped to HTTP 400). */
public class PipelineCycleException extends RuntimeException {

    public PipelineCycleException(String message) {
        super(message);
    }
}
