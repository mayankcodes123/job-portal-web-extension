package com.jobcrm.reminders;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the real application on a random port and calls it over HTTP, so this also proves
 * the Spring context loads. Uses the JDK HttpClient to avoid depending on Boot test-client
 * classes whose packages changed between Boot versions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthEndpointTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void healthEndpointDoesNotLeakComponentDetails() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.body()).doesNotContain("components").doesNotContain("diskSpace");
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        assertThat(get("/actuator/env").statusCode()).isEqualTo(404);
        assertThat(get("/actuator/beans").statusCode()).isEqualTo(404);
        assertThat(get("/actuator/heapdump").statusCode()).isEqualTo(404);
    }

    @Test
    void unknownPathReturnsJsonErrorEnvelope() throws Exception {
        HttpResponse<String> response = get("/no-such-path");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"NOT_FOUND\"");
    }
}
