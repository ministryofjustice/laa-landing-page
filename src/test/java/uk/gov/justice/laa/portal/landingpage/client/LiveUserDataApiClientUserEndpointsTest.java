package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.request.CreateUserAccountRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CreateUserAccountResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class LiveUserDataApiClientUserEndpointsTest {

    private static final String BASE = "http://data-api";
    private static final String FIRM_ID = "11111111-2222-3333-4444-555555555555";

    @Mock
    private OboTokenService oboTokenService;

    private MockRestServiceServer server;
    private LiveUserDataApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LiveUserDataApiClient(oboTokenService, builder.build());
        when(oboTokenService.acquireOboToken("user", "oid")).thenReturn("obo");
    }

    @Test
    void queryUsersByEmail_sendsEmailQueryAndPaging() {
        server.expect(requestTo(BASE + "/api/v1/queries/users?email_query=a@b.com&page_number=0&page_size=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer obo"))
                .andExpect(header("X-Correlation-ID", "cid"))
                .andRespond(withSuccess("{\"items\":[{\"email\":\"a@b.com\",\"multiFirmUser\":true}],"
                        + "\"page\":{\"page_number\":0,\"page_size\":100,\"total_elements\":1,\"total_pages\":1}}",
                        MediaType.APPLICATION_JSON));

        UserAccountSummaryPage result = client.queryUsersByEmail("user", "oid", "cid", "a@b.com", 0, 100);

        assertThat(result.items()).singleElement().satisfies(u -> {
            assertThat(u.email()).isEqualTo("a@b.com");
            assertThat(u.multiFirmUser()).isTrue();
        });
        server.verify();
    }

    @Test
    void createUser_postsExternalUserRequestWithoutRolesOrOffices() {
        UUID entraOid = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/commands/users/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer obo"))
                .andExpect(jsonPath("$.email").value("a@b.com"))
                .andExpect(jsonPath("$.userType").value("EXTERNAL"))
                .andExpect(jsonPath("$.firmId").value(FIRM_ID))
                .andExpect(jsonPath("$.multiFirmUser").value(false))
                .andExpect(jsonPath("$.appRoleIds").doesNotExist())
                .andExpect(jsonPath("$.officeIds").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":true,\"userEntraObjectId\":\"" + entraOid + "\"}"));

        CreateUserAccountResponse result = client.createUser("user", "oid", "cid",
                new CreateUserAccountRequest("a@b.com", "First", "Last", UserType.EXTERNAL, false, FIRM_ID));

        assertThat(result.success()).isTrue();
        assertThat(result.userEntraObjectId()).isEqualTo(entraOid.toString());
        server.verify();
    }

    @Test
    void createUser_mapsConflictToClientExceptionWithoutExposingBody() {
        server.expect(requestTo(BASE + "/api/v1/commands/users/create"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"message\":\"secret detail\"}"));

        assertThatThrownBy(() -> client.createUser("user", "oid", "cid",
                new CreateUserAccountRequest("a@b.com", "First", "Last", UserType.EXTERNAL, false, FIRM_ID)))
                .isInstanceOfSatisfying(UserDataApiClientException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(409);
                    assertThat(e.getMessage()).doesNotContain("secret detail");
                });
    }
}
