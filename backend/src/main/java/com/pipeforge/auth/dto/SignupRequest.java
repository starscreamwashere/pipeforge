package com.pipeforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Signup payload (App Flow §4.2). The role is assigned server-side (never
 * client-supplied) to prevent privilege escalation.
 */
public record SignupRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        // BCrypt only consumes the first 72 bytes; enforce a sane strength window.
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {
}
