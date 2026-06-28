package com.pipeforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code pipeforge.security.jwt.*}.
 *
 * <p>Values are sourced from {@code application.yml}, which in turn reads the
 * {@code JWT_SECRET}, {@code JWT_EXPIRY} and {@code REFRESH_TOKEN_EXPIRY}
 * environment variables (Technical Requirements §18).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pipeforge.security.jwt")
public class JwtProperties {

    /** HS256 signing secret (>= 256 bits). */
    private String secret;

    /** Access-token lifetime in milliseconds. */
    private long accessTokenExpiry = 900_000L;

    /** Refresh-token lifetime in milliseconds. */
    private long refreshTokenExpiry = 604_800_000L;
}
