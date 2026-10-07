package io.github.ysuestc.offerflow.health;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("standalone")
class HealthHttpTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void healthIsAvailableOnActualHttpServerWithoutDatabase() {
        var response = http.getForEntity("/api/v1/health", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody().path("code").asText()).isEqualTo("SUCCESS");
        assertThat(response.getBody().path("message").asText()).isNotBlank();
        assertThat(response.getBody().path("data").path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().has("errors")).isFalse();
    }

    @Test
    void unknownRouteReturnsCommon404() {
        var response = http.getForEntity("/api/v1/missing", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("code").asText()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().path("data").isNull()).isTrue();
    }

    @Test
    void unsupportedMethodPreservesAllowHeader() {
        var response = http.postForEntity("/api/v1/health", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().path("code").asText()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET);
    }

    @Test
    void clientExcludingJsonReceives406() {
        var headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_XML));
        var response = http.exchange("/api/v1/health", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    }

    @Test
    void testHelperEndpointsAreAbsentFromProductionContext() {
        var response = http.getForEntity("/contract-probe/quantity?quantity=1", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("code").asText()).isEqualTo("NOT_FOUND");
    }
}
