package com.pipeforge.exception;

/** Thrown when a task fails during execution (drives retry / dead-letter). */
public class JobExecutionException extends RuntimeException {

    public JobExecutionException(String message) {
        super(message);
    }
}
