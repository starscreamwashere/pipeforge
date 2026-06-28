package com.pipeforge.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Token-refresh payload. */
public record RefreshRequest(

        @NotBlank
        String refreshToken) {
}
