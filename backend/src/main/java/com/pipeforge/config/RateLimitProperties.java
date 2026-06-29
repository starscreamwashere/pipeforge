package com.pipeforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate-limit configuration (Technical Requirements §3). Limits are per client IP
 * per fixed window, backed by Redis.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pipeforge.security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Max auth requests (/api/v1/auth/**) per window. */
    private int authLimit = 100;

    /** Max execution-trigger requests per window. */
    private int triggerLimit = 120;

    /** Window length in seconds. */
    private long windowSeconds = 60;
}
