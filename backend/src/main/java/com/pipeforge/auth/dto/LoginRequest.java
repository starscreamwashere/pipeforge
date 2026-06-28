package com.pipeforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login payload (App Flow §4.3). */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password) {
}
