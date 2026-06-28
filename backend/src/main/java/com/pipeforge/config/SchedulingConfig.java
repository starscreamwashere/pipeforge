package com.pipeforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Spring {@code @Scheduled} support (used by the retry-queue scanner). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
