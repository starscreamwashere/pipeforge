package com.pipeforge.exception;

import java.time.Instant;

/** Standard error envelope (Technical Requirements §12). */
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, path);
    }
}
