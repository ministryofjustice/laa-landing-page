package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetails;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;
import uk.gov.justice.laa.portal.landingpage.registry.CcmsUdaRegistry;

import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CcmsUserDetailsServiceTest {

    private static final String APP_OID = "app-oid";
    private static final String LEGACY_USER_ID = "legacy123";
    private static final String BASE_URL = "http://uda-host";
    private static final String API_KEY = "api-key";

    @Mock
    private CcmsUdaRegistry ccmsUdaRegistry;

    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private CcmsUserDetailsService service;

    @BeforeEach
    void setUp() {
        lenient().when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(Predicate.class), any(RestClient.ResponseSpec.ErrorHandler.class))).thenReturn(responseSpec);
    }

    @Test
    void getUserDetailsByLegacyUserId_WhenCallFails_ReturnsNull() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(responseSpec.body(CcmsUserDetailsResponse.class)).thenThrow(new RuntimeException("boom"));

        CcmsUserDetailsResponse response = service.getUserDetailsByLegacyUserId(APP_OID, "some-legacy-id");

        assertNull(response);
    }

    @Test
    void getUserDetailsByLegacyUserId_WhenCallSucceeds_ReturnsBody() {
        CcmsUserDetails ccmsUserDetails = new CcmsUserDetails();
        ccmsUserDetails.setUserName("CCMS_USER_FROM_API");
        CcmsUserDetailsResponse body = new CcmsUserDetailsResponse();
        body.setCcmsUserDetails(ccmsUserDetails);

        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(responseSpec.body(CcmsUserDetailsResponse.class)).thenReturn(body);

        CcmsUserDetailsResponse response = service.getUserDetailsByLegacyUserId(APP_OID, "legacy-id");

        assertEquals("CCMS_USER_FROM_API", response.getCcmsUserDetails().getUserName());
    }

    @Test
    void shouldReturnNullWhenBaseUrlMissing() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.empty());
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));

        CcmsUserDetailsResponse result = service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
        verifyNoInteractions(restClientBuilder);
    }

    @Test
    void shouldReturnNullWhenApiKeyMissing() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.empty());

        CcmsUserDetailsResponse result = service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
        verifyNoInteractions(restClientBuilder);
    }

    @Test
    void shouldReturnNullWhenBaseUrlIsNone() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of("NONE"));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));

        CcmsUserDetailsResponse result = service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
        verifyNoInteractions(restClientBuilder);
    }

    @Test
    void shouldReturnUserDetailsOn2xxResponse() {
        CcmsUserDetailsResponse responseBody = new CcmsUserDetailsResponse();

        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(responseSpec.body(CcmsUserDetailsResponse.class)).thenReturn(responseBody);

        CcmsUserDetailsResponse result = service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertSame(responseBody, result);
    }

    @Test
    void shouldReturnNullOn404Response() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(responseSpec.body(CcmsUserDetailsResponse.class)).thenReturn(null);

        CcmsUserDetailsResponse result = service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
    }

    @Test
    void shouldReturnNullOnUnexpectedHttpStatus() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(responseSpec.body(CcmsUserDetailsResponse.class)).thenReturn(null);

        CcmsUserDetailsResponse result = service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
    }

    @Test
    void shouldCallCorrectUrlAndSetAuthorizationHeader() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(responseSpec.body(CcmsUserDetailsResponse.class)).thenReturn(new CcmsUserDetailsResponse());

        service.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        verify(restClientBuilder).baseUrl(BASE_URL);

        verify(requestHeadersSpec).header("X-Authorization", API_KEY);
    }
}
