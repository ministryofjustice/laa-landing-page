package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserAccountStatus;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserProfileSilasStatus;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailView;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class LiveUserDataApiClientTest {

    private static final String OBO_TOKEN = "obo-access-token";
    private static final String USER_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String USER_ACCESS_TOKEN = "user-access-token";
    private static final String CORRELATION_ID = "test-correlation-id";

    @Mock
    private OboTokenService oboTokenService;

    @Mock
    private RestClient userDataApiRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private LiveUserDataApiClient client;

    @Test
    void me_returnsUserProfile_whenOboTokenAcquiredSuccessfully() {
        UserProfileDetailView expected = buildStubResponse();
        when(oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID)).thenReturn(OBO_TOKEN);
        stubGetRequest("/api/v1/me", expected);

        UserProfileDetailView result = client.me(USER_ACCESS_TOKEN, USER_OID, CORRELATION_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void me_generatesCorrelationId_whenNullProvided() {
        UserProfileDetailView expected = buildStubResponse();
        when(oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID)).thenReturn(OBO_TOKEN);
        stubGetRequest("/api/v1/me", expected);

        UserProfileDetailView result = client.me(USER_ACCESS_TOKEN, USER_OID, null);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void me_deserializesSharedProfileView() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://data-api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LiveUserDataApiClient liveClient = new LiveUserDataApiClient(oboTokenService, builder.build());
        when(oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID)).thenReturn(OBO_TOKEN);
        server.expect(requestTo("http://data-api/api/v1/me")).andRespond(withSuccess("""
                {
                  "userProfileId": "%s",
                  "activeProfile": true,
                  "userType": "INTERNAL",
                  "email": "test@example.com",
                  "fullName": "Test User",
                  "firmName": "Test Firm",
                  "firmId": "00123",
                  "multiFirmUser": false,
                  "accountStatus": "ACTIVE",
                  "profileStatus": "COMPLETE",
                  "hasAppRoles": false,
                  "userEntraObjectId": "%s",
                  "unrestrictedOfficeAccess": false,
                  "lastSyncSuccessful": true,
                  "roles": [],
                  "offices": []
                }
                """.formatted(USER_OID, USER_OID), MediaType.APPLICATION_JSON));

        assertThat(liveClient.me(USER_ACCESS_TOKEN, USER_OID, CORRELATION_ID)).isEqualTo(buildStubResponse());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {USER_OID, ""})
    void disabledClientReturnsSharedViewWithUnknownStatusesAndNoAccess(String oid) {
        UserProfileDetailView result = new DoNothingUserDataApiClient().me(USER_ACCESS_TOKEN, oid, CORRELATION_ID);
        UUID expectedId = oid.isBlank() ? new UUID(0, 0) : UUID.fromString(oid);
        assertThat(result.userProfileId()).isEqualTo(expectedId);
        assertThat(result.userEntraObjectId()).isEqualTo(expectedId);
        assertThat(result.accountStatus()).isEqualTo(UserAccountStatus.UNKNOWN);
        assertThat(result.profileStatus()).isEqualTo(UserProfileSilasStatus.UNKNOWN);
        assertThat(result.userType()).isNull();
        assertThat(result.activeProfile()).isFalse();
        assertThat(result.multiFirmUser()).isFalse();
        assertThat(result.hasAppRoles()).isFalse();
        assertThat(result.unrestrictedOfficeAccess()).isFalse();
        assertThat(result.lastSyncSuccessful()).isFalse();
        assertThat(result.roles()).isEmpty();
        assertThat(result.offices()).isEmpty();
    }

    private UserProfileDetailView buildStubResponse() {
        return new UserProfileDetailView(UUID.fromString(USER_OID), true, UserType.INTERNAL,
                "test@example.com", "Test User", "Test Firm", "00123", false,
                UserAccountStatus.ACTIVE, UserProfileSilasStatus.COMPLETE, false, UUID.fromString(USER_OID),
                false, true, List.of(), List.of());
    }

    @SuppressWarnings("unchecked")
    private void stubGetRequest(String uri, UserProfileDetailView responseBody) {
        when(userDataApiRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(uri)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(UserProfileDetailView.class)).thenReturn(responseBody);
    }
}
