package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LiveUserDataApiClientCreateUserTest {

    private static final String CREATE_URL = "http://data-api/api/v1/commands/users/create";
    private static final UUID OID = UUID.randomUUID();
    private MockRestServiceServer server;
    private LiveUserDataApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data-api");
        server = MockRestServiceServer.bindTo(builder).build();
        OboTokenService tokens = mock(OboTokenService.class);
        when(tokens.acquireOboToken("user-token", "actor")).thenReturn("obo-token");
        client = new LiveUserDataApiClient(tokens, builder.build());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createsOnlyExternalAccountWithoutAccessAssignments(boolean multiFirm) {
        String firmJson = multiFirm ? "" : ",\"firmId\":\"00123\"";
        String expected = "{\"email\":\"test@example.org\",\"firstName\":\"Test\",\"lastName\":\"User\","
                + "\"userType\":\"EXTERNAL\",\"multiFirmUser\":" + multiFirm + firmJson + "}";
        server.expect(requestTo(CREATE_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer obo-token"))
                .andExpect(header("X-Correlation-ID", "cid"))
                .andExpect(content().json(expected, org.springframework.test.json.JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatusCode.valueOf(201)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":true,\"message\":\"created\",\"userEntraObjectId\":\"" + OID + "\"}"));
        CreateExternalUserResult result = client.createExternalUser("user-token", "actor", "cid", request(multiFirm));
        assertThat(result.userEntraObjectId()).isEqualTo(OID);
        assertThat(result.userProfileId()).isNull();
        server.verify();
    }

    @Test
    void emailQueryIsEncodedAndPaginated() {
        server.expect(requestTo("http://data-api/api/v1/queries/users?email_query=test%2Balias%40example.org"
                        + "&page_number=1&page_size=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer obo-token"))
                .andRespond(withSuccess("{\"items\":[],\"page\":{\"page_number\":1,\"page_size\":100,"
                        + "\"total_elements\":101,\"total_pages\":2}}", MediaType.APPLICATION_JSON));
        assertThat(client.queryUsersByEmail("user-token", "actor", "cid", "test+alias@example.org", 1)
                .page().pageNumber()).isEqualTo(1);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 502, 503, 504})
    void propagatesStatusWithoutLeakingPayloadOrTreatingConflictAsSuccess(int status) {
        server.expect(requestTo(CREATE_URL)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":true,\"userEntraObjectId\":\"" + OID + "\",\"message\":\"PRIVATE USER DATA\"}"));
        assertThatThrownBy(() -> client.createExternalUser("user-token", "actor", "cid", request(false)))
                .isInstanceOfSatisfying(UserDataApiClientException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(status);
                    assertThat(error.getMessage()).doesNotContain("PRIVATE", OID.toString());
                });
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 202, 204})
    void rejectsUnexpectedSuccessfulStatus(int status) {
        server.expect(requestTo(CREATE_URL)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"success\":true,\"userEntraObjectId\":\"" + OID + "\"}"));
        assertThatThrownBy(() -> client.createExternalUser("user-token", "actor", "cid", request(false)))
                .isInstanceOfSatisfying(UserDataApiClientException.class, error ->
                        assertThat(error.getStatusCode()).isEqualTo(502));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"success\":true}", "{\"success\":false}"})
    void rejectsMissingOrUnsuccessfulResult(String body) {
        server.expect(requestTo(CREATE_URL)).andRespond(withStatus(HttpStatusCode.valueOf(201))
                .contentType(MediaType.APPLICATION_JSON).body(body));
        assertThatThrownBy(() -> client.createExternalUser("user-token", "actor", "cid", request(false)))
                .isInstanceOf(UserDataApiClientException.class);
    }

    @Test
    void disabledClientFailsExplicitly() {
        DoNothingUserDataApiClient disabled = new DoNothingUserDataApiClient();
        assertThatThrownBy(() -> disabled.createExternalUser("token", "actor", "cid", request(false)))
                .isInstanceOfSatisfying(UserDataApiClientException.class, error ->
                        assertThat(error.getStatusCode()).isEqualTo(503));
        assertThatThrownBy(() -> disabled.queryUsersByEmail("token", "actor", "cid", "test@example.org", 0))
                .isInstanceOf(UserDataApiClientException.class);
    }

    private CreateExternalUserRequest request(boolean multiFirm) {
        return new CreateExternalUserRequest("test@example.org", "Test", "User", UserType.EXTERNAL,
                multiFirm ? null : "00123", multiFirm);
    }
}
