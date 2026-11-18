package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CurrentUserPermissionsClientTest {

    private static final String URL = "https://user-data.example/current-user/permissions";
    private MockRestServiceServer server;
    private CurrentUserPermissionsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://user-data.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CurrentUserPermissionsClient(builder.build());
    }

    @Test
    void makesSelfCallAndChecksKnownPermissions() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(content().string(""))
                .andRespond(withSuccess("""
                    {"permissions":["EDIT_EXTERNAL_USER","FUTURE_PERMISSION","VIEW_EXTERNAL_USER"]}
                    """, MediaType.APPLICATION_JSON).header(HttpHeaders.CACHE_CONTROL, "no-store"));

        var permissions = client.load("test-token");
        assertThat(permissions.allows(Permission.EDIT_EXTERNAL_USER)).isTrue();
        assertThat(permissions.allows(Permission.VIEW_EXTERNAL_USER)).isTrue();
        assertThat(permissions.allows(Permission.VIEW_INTERNAL_USER)).isFalse();
        assertThat(permissions.allows(null)).isFalse();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "{}", "{\"permissions\":null}", "{\"permissions\":\"EDIT_EXTERNAL_USER\"}", "{\"permissions\":[123]}",
        "{\"permissions\":[null]}", "{\"permissions\":[\"edit\"]}", "{\"permissions\":[\"A\",\"A\"]}",
        "{\"permissions\":[\"Z\",\"A\"]}", "{\"permissions\":["
    })
    void rejectsMalformedSuccess(String response) {
        server.expect(requestTo(URL)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertStatus(503);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 503})
    void usesProblemStatusWithoutTreatingTheBodyAsPermissions(int status) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body("{\"title\":\"error\",\"status\":" + status + "}"));
        assertStatus(status);
    }

    @Test
    void networkFailureIsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withException(new IOException("connection lost")));
        assertStatus(503);
    }

    private void assertStatus(int status) {
        assertThatExceptionOfType(UserDataApiClientException.class).isThrownBy(() -> client.load("test-token"))
                .satisfies(exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
        server.verify();
    }
}
