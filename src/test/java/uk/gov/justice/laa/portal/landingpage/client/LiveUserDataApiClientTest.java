package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailResponse;
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
        UserProfileDetailResponse expected = buildStubResponse();
        when(oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID)).thenReturn(OBO_TOKEN);
        stubGetRequest("/api/v1/me", expected);

        UserProfileDetailResponse result = client.me(USER_ACCESS_TOKEN, USER_OID, CORRELATION_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void me_generatesCorrelationId_whenNullProvided() {
        UserProfileDetailResponse expected = buildStubResponse();
        when(oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID)).thenReturn(OBO_TOKEN);
        stubGetRequest("/api/v1/me", expected);

        UserProfileDetailResponse result = client.me(USER_ACCESS_TOKEN, USER_OID, null);

        assertThat(result).isEqualTo(expected);
    }

    private UserProfileDetailResponse buildStubResponse() {
        return new UserProfileDetailResponse(
                UUID.fromString(USER_OID),
                USER_OID, "INTERNAL", "test@example.com", "Test User",
                null, null, "ACTIVE", "ACTIVE", true, false, false);
    }

    @SuppressWarnings("unchecked")
    private void stubGetRequest(String uri, UserProfileDetailResponse responseBody) {
        when(userDataApiRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(uri)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(responseBody);
    }
}
