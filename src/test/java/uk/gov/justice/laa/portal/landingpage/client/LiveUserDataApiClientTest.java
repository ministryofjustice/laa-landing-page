package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserAccountStatus;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserProfileSilasStatus;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailView;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

    private UserProfileDetailView buildStubResponse() {
        return new UserProfileDetailView(
                UUID.fromString(USER_OID), true, UserType.EXTERNAL, "test@example.com", "Test User", "Firm Name",
                "Firm123", false, UserAccountStatus.ACTIVE, UserProfileSilasStatus.COMPLETE, false, UUID.randomUUID(), false, false, null, null);
    }

    @SuppressWarnings("unchecked")
    private void stubGetRequest(String uri, UserProfileDetailView responseBody) {
        when(userDataApiRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(uri)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(responseBody);
    }
}
