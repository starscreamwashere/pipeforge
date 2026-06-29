package com.pipeforge.metrics;

import com.pipeforge.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actuator + Prometheus exposure (Phases 8.2/8.3). These endpoints are public
 * (SecurityConfig) so monitoring tools can scrape without a token.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void healthEndpointIsUp() {
        ResponseEntity<String> health = rest.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("UP");
    }

    @Test
    void prometheusExposesCustomPipeforgeMetrics() {
        ResponseEntity<String> scrape = rest.getForEntity("/actuator/prometheus", String.class);
        assertThat(scrape.getStatusCode()).isEqualTo(HttpStatus.OK);

        String body = scrape.getBody();
        assertThat(body)
                .contains("pipeforge_executions_triggered_total")
                .contains("pipeforge_tasks_succeeded_total")
                .contains("pipeforge_pipelines_count")
                .contains("pipeforge_queue_ready_depth");
    }
}
