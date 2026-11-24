package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class LiveUserDataApiClientProfileQueriesTest {

    private static final String PROFILES = "https://data-api.example/api/v1/queries/user-profiles";
    private static final UUID BOB_ID = UUID.fromString("12345000-0000-0000-0000-000000000001");
    private static final UUID BILLING_ID = UUID.fromString("12345000-0000-0000-0000-000000000002");
    private MockRestServiceServer server;
    private LiveUserDataApiClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://data-api.example");
        server = MockRestServiceServer.bindTo(builder).build();
        var tokens = mock(OboTokenService.class);
        when(tokens.acquireOboToken("alice-login-token", "alice-oid")).thenReturn("alice-delegated-token");
        client = new LiveUserDataApiClient(tokens, builder.build());
    }

    @Test
    void listsOnlyTheProfilesReturnedByTheApiWithoutSendingAnActorFirmFilter() {
        server.expect(requestTo(PROFILES + "?page_number=0&page_size=10&sort_by=fullName,asc"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer alice-delegated-token"))
                .andRespond(withSuccess("""
                    {"items":[{"userProfileId":"%s","userType":"EXTERNAL","fullName":"Bob",
                    "email":"bob@example.test","firmId":"12345","firmName":"Legal Services",
                    "profileStatus":"NO_ACCESS_ASSIGNED","accountStatus":"ACTIVE","hasAppRoles":false}],
                    "page":{"page_number":0,"page_size":10,"total_elements":1,"total_pages":1}}
                    """.formatted(BOB_ID), MediaType.APPLICATION_JSON));

        var query = new UserProfileQuery(0, 10, "fullName,asc", "", null, null, null, false, false);
        var response = client.queryUserProfiles("alice-login-token", "alice-oid", "cid", query);

        assertThat(response.items()).singleElement().satisfies(profile -> assertThat(profile.userProfileId()).isEqualTo(BOB_ID));
        assertThat(response.page().totalElements()).isEqualTo(1);
        server.verify();
    }

    @Test
    void encodesUserSelectedFiltersAndKeepsThemSeparateFromActorIdentity() {
        server.expect(requestTo(PROFILES + "?page_number=1&page_size=20&sort_by=email,desc"
                        + "&user=bob%2Btest%40example.test&user_type=EXTERNAL&firm_id=12345&firm_admin_filter=true"))
                .andRespond(withSuccess("""
                    {"items":[],"page":{"page_number":1,"page_size":20,"total_elements":0,"total_pages":0}}
                    """, MediaType.APPLICATION_JSON));

        var query = new UserProfileQuery(1, 20, "email,desc", "bob+test@example.test", "EXTERNAL", null, "12345", true, false);
        client.queryUserProfiles("alice-login-token", "alice-oid", "cid", query);
        server.verify();
    }

    @Test
    void readsBobsFirmAndAssignedRolesWithAlicesDelegatedToken() {
        server.expect(requestTo(PROFILES + "/" + BOB_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer alice-delegated-token"))
                .andExpect(header("X-Correlation-ID", "cid"))
                .andRespond(withSuccess("""
                    {"userProfileId":"%s","userType":"EXTERNAL","fullName":"Bob","email":"bob@example.test",
                    "firmId":"12345","firmName":"Legal Services","profileStatus":"COMPLETE",
                    "accountStatus":"ACTIVE","activeProfile":true,"multiFirmUser":false,"hasAppRoles":true,
                    "unrestrictedOfficeAccess":false,"lastSyncSuccessful":true,
                    "roles":[{"name":"Billing Approver","appName":"Billing","authzRole":false,"legacySync":false}],"offices":[]}
                    """.formatted(BOB_ID), MediaType.APPLICATION_JSON));

        var profile = client.queryUserProfileById("alice-login-token", "alice-oid", "cid", BOB_ID);
        assertThat(profile.firmId()).isEqualTo("12345");
        assertThat(profile.roles()).singleElement().satisfies(role -> assertThat(role.name()).isEqualTo("Billing Approver"));
        server.verify();
    }

    @Test
    void loadsOnlyRolesReturnedByTheTargetAwareEndpoint() {
        server.expect(requestTo(PROFILES + "/" + BOB_ID + "/assignable-roles"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer alice-delegated-token"))
                .andRespond(withSuccess("""
                    {"items":[{"appRoleId":"%s","appRoleName":"Billing Approver"}]}
                    """.formatted(BILLING_ID), MediaType.APPLICATION_JSON));
        assertThat(client.queryAssignableRoles("alice-login-token", "alice-oid", "cid", BOB_ID).items())
                .containsExactly(new RoleAssignmentContract.Role(BILLING_ID, "Billing Approver"));
        server.verify();
    }

    @Test
    void submitsOnlyTheTargetAndSelectedRoleIdsAndSurfacesBackendDenial() {
        server.expect(requestTo("https://data-api.example/api/v1/commands/user-profiles/assign-app-roles"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer alice-delegated-token"))
                .andExpect(content().json("""
                    {"userProfileId":"%s","appRoleIds":["%s"]}
                    """.formatted(BOB_ID, BILLING_ID), org.springframework.test.json.JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        var command = new RoleAssignmentContract.Command(BOB_ID, java.util.List.of(BILLING_ID));
        assertThatThrownBy(() -> client.assignAppRoles("alice-login-token", "alice-oid", "cid", command))
                .isInstanceOfSatisfying(UserDataApiClientException.class, e -> assertThat(e.getStatusCode()).isEqualTo(403));
        server.verify();
    }
}
