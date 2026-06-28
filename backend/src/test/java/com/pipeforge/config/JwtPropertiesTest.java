package com.pipeforge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that environment-style configuration binds into {@link JwtProperties}
 * via Spring Boot's relaxed binding (Phase 1.1 — "app reads env vars").
 */
class JwtPropertiesTest {

    @Test
    void bindsEnvironmentStyleProperties() {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "pipeforge.security.jwt.secret", "test-secret-0123456789-0123456789-abcdef",
                "pipeforge.security.jwt.access-token-expiry", "120000",
                "pipeforge.security.jwt.refresh-token-expiry", "300000"));

        JwtProperties props = new Binder(source)
                .bind("pipeforge.security.jwt", JwtProperties.class)
                .get();

        assertThat(props.getSecret()).isEqualTo("test-secret-0123456789-0123456789-abcdef");
        assertThat(props.getAccessTokenExpiry()).isEqualTo(120_000L);
        assertThat(props.getRefreshTokenExpiry()).isEqualTo(300_000L);
    }

    @Test
    void appliesDefaultsWhenUnset() {
        JwtProperties props = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("pipeforge.security.jwt", JwtProperties.class);

        assertThat(props.getAccessTokenExpiry()).isEqualTo(900_000L);
        assertThat(props.getRefreshTokenExpiry()).isEqualTo(604_800_000L);
    }
}
